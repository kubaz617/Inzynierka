package com.example.bookreader.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.bookreader.models.ModelBook
import com.example.bookreader.adapters.AdapterBookUser
import com.example.bookreader.databinding.FragmentBooksUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BooksUserFragment : Fragment() {

    private lateinit var binding: FragmentBooksUserBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var adapterBookUser: AdapterBookUser
    private lateinit var pdfArrayList: ArrayList<ModelBook>

    private var categoryId = ""
    private var category = ""
    private var uid = ""

    private val TAG = "BOOKS_USER_TAG"

    companion object {
        fun newInstance(categoryId: String, category: String, uid: String): BooksUserFragment {
            val fragment = BooksUserFragment()
            val args = Bundle().apply {
                putString("categoryId", categoryId)
                putString("category", category)
                putString("uid", uid)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        pdfArrayList = ArrayList()
        adapterBookUser = AdapterBookUser(requireContext(), pdfArrayList)

        val args = arguments
        if (args != null) {
            categoryId = args.getString("categoryId") ?: ""
            category = args.getString("category") ?: ""
            uid = args.getString("uid") ?: ""
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentBooksUserBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView: Category: $category")
        binding.BooksRv.adapter = adapterBookUser

        loadBooks()

        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    adapterBookUser.filter.filter(s)
                } catch (e: Exception) {
                    Log.d(TAG, "onTextChanged: Search Exception: ${e.message}")
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        return binding.root
    }

    private fun loadBooks() {
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        val query = when (category) {
            "Wszystkie" -> ref
            "Najczęściej oglądane" -> ref.orderByChild("viewsCount").limitToLast(10)
            "Najczęściej pobierane" -> ref.orderByChild("downloadCount").limitToLast(10)
            "Ulubione" -> {
                loadFavoriteBooks()
                return
            }
            else -> ref.orderByChild("categoryId").equalTo(categoryId)
        }

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                pdfArrayList.clear()
                for (ds in snapshot.children) {
                    val model = ds.getValue(ModelBook::class.java)
                    model?.let { pdfArrayList.add(it) }
                }
                adapterBookUser.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled: DatabaseError: ${error.message}")
            }
        })
    }

    private fun loadFavoriteBooks() {
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
                            adapterBookUser.notifyDataSetChanged()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "loadFavoriteBooks onCancelled: ${error.message}")
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "loadFavoriteBooks onCancelled: ${error.message}")
            }
        })
    }
}