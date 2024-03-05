package com.example.bookreader.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import com.example.bookreader.models.ModelCategory
import com.example.bookreader.R
import com.example.bookreader.adapters.AdapterCategory
import com.example.bookreader.databinding.ActivityBooksBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminBooksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBooksBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var categoryArrayList: ArrayList<ModelCategory>

    private lateinit var adapterCategory: AdapterCategory
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val category_btn = findViewById<Button>(R.id.addCategory)
        val book_btn = findViewById<Button>(R.id.add_Book)
        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()
        loadCategories()

        binding.searchEt.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    adapterCategory.filter.filter(s)
                }
                catch (e:Exception){

                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        category_btn.setOnClickListener {
            navigateToActivity(AddCategory::class.java)
        }

        book_btn.setOnClickListener {
            navigateToActivity(AddBook::class.java)
        }


    }

    private fun loadCategories() {
        categoryArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryArrayList.clear()
                for (ds in snapshot.children){
                    val model = ds.getValue(ModelCategory::class.java)

                    categoryArrayList.add(model!!)
                }
                adapterCategory = AdapterCategory(this@AdminBooksActivity,categoryArrayList)
                binding.categoriesRv.adapter = adapterCategory
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
            val email = firebaseUser!!.email
            binding.titleTv.text = email

    }
}