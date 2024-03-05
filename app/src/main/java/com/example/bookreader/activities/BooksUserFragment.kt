package com.example.bookreader.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.example.bookreader.models.ModelBook
import com.example.bookreader.adapters.AdapterBookUser
import com.example.bookreader.databinding.FragmentBooksUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BooksUserFragment : Fragment {

    private lateinit var binding: FragmentBooksUserBinding

    private lateinit var firebaseAuth: FirebaseAuth
    public companion object{
        private const val TAG = "BOOKS_USER_TAG"
        public fun newInstance(categoryId: String, category: String, uid: String): BooksUserFragment {
            val fragment = BooksUserFragment()

            val args = Bundle()
            args.putString("categoryId", categoryId)
            args.putString("category", category)
            args.putString("uid", uid)
            fragment.arguments = args
            return fragment
        }
    }

    private var categoryId = ""
    private var category = ""
    private var uid = ""

    private lateinit var pdfArrayList: ArrayList<ModelBook>
    private lateinit var adapterBookUser: AdapterBookUser

    constructor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        val args = arguments
        if (args != null){
            categoryId = args.getString("categoryId")!!
            category = args.getString("category")!!
            uid = args.getString("uid")!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentBooksUserBinding.inflate(LayoutInflater.from(context), container, false)
        Log.d(TAG, "onCreateView: Category: $category")
        if (category == "Wszystkie"){
            loadAllBooks()
        }
        else if (category == "Najczęściej oglądane"){
            loadMostViewedDownloadedBooks("viewsCount")
        }
        else if (category == "Najczęściej pobierane"){
            loadMostViewedDownloadedBooks("downloadCount")
        }
        else if (category == "Ulubione"){
            loadFavoriteBooks()
        }
        else{
            loadCategorizedBooks()
        }


        binding.searchEt.addTextChangedListener { object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    adapterBookUser.filter.filter(s)
                } catch (e: Exception) {
                    Log.d(TAG, "onTextChanged: Search Exception: ${e.message}")
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        } }

        return binding.root
    }


    private fun loadAllBooks() {
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                pdfArrayList.clear()
                for (ds in snapshot.children){
                    val model = ds.getValue(ModelBook::class.java)

                    pdfArrayList.add(model!!)
                }
                adapterBookUser = AdapterBookUser(context!!, pdfArrayList)
                binding.BooksRv.adapter = adapterBookUser
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun loadFavoriteBooks() {
        pdfArrayList = ArrayList()
        val favoriteBookIds = mutableListOf<String>()
        val ref = FirebaseDatabase.getInstance().getReference("Users").child(firebaseAuth.uid!!).child("Favorites")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (ds in snapshot.children) {
                        val bookId = ds.child("bookId").getValue(String::class.java)
                        bookId?.let { favoriteBookIds.add(it) }
                    }
                    val booksRef = FirebaseDatabase.getInstance().getReference("Books")
                    booksRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(booksSnapshot: DataSnapshot) {
                            pdfArrayList.clear()
                            for (ds in booksSnapshot.children) {
                                val model = ds.getValue(ModelBook::class.java)
                                model?.let {
                                    if (favoriteBookIds.contains(model.id)) {
                                        pdfArrayList.add(model)
                                    }
                                }
                            }
                            adapterBookUser = AdapterBookUser(context!!, pdfArrayList)
                            binding.BooksRv.adapter = adapterBookUser
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                } else {
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun loadMostViewedDownloadedBooks(orderBy: String) {
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.orderByChild(orderBy).limitToLast(10)
            .addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                pdfArrayList.clear()
                for (ds in snapshot.children){
                    val model = ds.getValue(ModelBook::class.java)

                    pdfArrayList.add(model!!)
                }
                adapterBookUser = AdapterBookUser(context!!, pdfArrayList)
                binding.BooksRv.adapter = adapterBookUser
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun loadCategorizedBooks() {
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.orderByChild("categoryId").equalTo(categoryId)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    pdfArrayList.clear()
                    for (ds in snapshot.children){
                        val model = ds.getValue(ModelBook::class.java)

                        pdfArrayList.add(model!!)
                    }
                    adapterBookUser = AdapterBookUser(context!!, pdfArrayList)
                    binding.BooksRv.adapter = adapterBookUser
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

}