package com.example.bookreader.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.example.bookreader.models.ModelBook
import com.example.bookreader.adapters.AdapterBookAdmin
import com.example.bookreader.databinding.ActivityBooksListAdminBinding
import com.example.bookreader.utils.MyApplication
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BooksListAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBooksListAdminBinding

    private companion object{
        const val TAG = "BOOKS_LIST_ADMIN_TAG"
    }

    private var categoryId = ""
    private var category = ""

    private lateinit var booksArrayList:ArrayList<ModelBook>

    private lateinit var adapterBookAdmin: AdapterBookAdmin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBooksListAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        categoryId = intent.getStringExtra("categoryId")!!
        category = intent.getStringExtra("category")!!

        binding.subTitleTv.text = category

        loadBooksList()

        binding.searchEt.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, p1: Int, p2: Int, p3: Int) {
                try {
                    adapterBookAdmin.filter!!.filter(s)
                }
                catch (e: Exception){
                    Log.d(TAG, "onTextChanged: ${e.message}")
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        val selectedBackground = getSelectedBackground()
        window.setBackgroundDrawableResource(selectedBackground)

        val selectedColor = MyApplication.getSelectedColor(this)
        setAllButtonsColor(selectedColor)

        window.statusBarColor = MyApplication.getStatusBarColor(this)
    }

    private fun loadBooksList() {
        booksArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.orderByChild("categoryId").equalTo(categoryId)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                booksArrayList.clear()
                    for (ds in snapshot.children){
                        val model = ds.getValue(ModelBook::class.java)
                        if (model != null) {
                            booksArrayList.add(model)
                            Log.d(TAG, "onDataChange: ${model.title} ${model.categoryId}")
                        }
                    }
                    adapterBookAdmin = AdapterBookAdmin(this@BooksListAdminActivity, booksArrayList)
                    binding.BooksRv.adapter = adapterBookAdmin
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "onCancelled: DatabaseError: ${error.message}")
                }
            })


    }

    private fun setAllButtonsColor(color: Int) {
        val rootView = window.decorView.rootView
        MyApplication.setViewBackgroundColor(rootView, color)
    }



    private fun getSelectedBackground(): Int {
        return MyApplication.getSelectedBackground(this)
    }

}