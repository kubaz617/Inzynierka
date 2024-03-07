package com.example.bookreader.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookreader.R
import com.example.bookreader.adapters.AdapterBookUser
import com.example.bookreader.databinding.ActivitySimiliarBinding
import com.example.bookreader.models.ModelBook
import com.example.bookreader.utils.MyApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SimiliarActivity : AppCompatActivity() {

    private lateinit var adapter: AdapterBookUser
    private lateinit var bookList: ArrayList<ModelBook>
    private lateinit var binding: ActivitySimiliarBinding

    private lateinit var suggestedCategoryTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimiliarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        suggestedCategoryTextView = findViewById(R.id.titleTextView)

        bookList = ArrayList()
        adapter = AdapterBookUser(this, bookList)
        binding.booksRecyclerView.adapter = adapter
        binding.booksRecyclerView.layoutManager = LinearLayoutManager(this)

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let { uid ->
            val userBookDetailsRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("bookDetails")
            val categoriesRef = FirebaseDatabase.getInstance().getReference("Categories")

            userBookDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userBookDetailsSnapshot: DataSnapshot) {
                    Log.d("SimiliarActivity", "User book details snapshot: $userBookDetailsSnapshot")
                    categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(categoriesSnapshot: DataSnapshot) {
                            Log.d("SimiliarActivity", "Categories snapshot: $categoriesSnapshot")
                            findMostReadCategory(userBookDetailsSnapshot, categoriesSnapshot)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("SimiliarActivity", "Failed to read categories: $error")
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SimiliarActivity", "Failed to read user book details: $error")
                }
            })
        }
    }

    private fun findMostReadCategory(userBookDetailsSnapshot: DataSnapshot, categoriesSnapshot: DataSnapshot) {
        val categoryCountMap = mutableMapOf<String, Int>()

        for (userBookDetailSnapshot in userBookDetailsSnapshot.children) {
            val categoryId = userBookDetailSnapshot.child("categoryId").getValue(String::class.java)
            val isBookFullyRead = userBookDetailSnapshot.child("isBookFullyRead").getValue(Boolean::class.java)

            if (isBookFullyRead == true) {
                categoryId?.let {
                    val count = categoryCountMap[it] ?: 0
                    categoryCountMap[it] = count + 1

                    Log.d("SimiliarActivity", "CategoryId: $it, Count: ${categoryCountMap[it]}")
                }
            }
        }

        var maxCategory: String? = null
        var maxCount = 0
        for ((categoryId, count) in categoryCountMap) {
            if (count > maxCount) {
                maxCount = count
                maxCategory = categoryId
            }
        }

        maxCategory?.let { categoryId ->
            val categoryName = categoriesSnapshot.child(categoryId).child("category").getValue(String::class.java)
            categoryName?.let {
                suggestedCategoryTextView.text = "Najczęściej czytana kategoria: $it"
                recommendedBookFromCategory(categoryId)
                suggestedCategoryTextView.visibility = View.VISIBLE
            }
        }
    }

    private fun recommendedBookFromCategory(categoryId: String) {
        val booksRef = FirebaseDatabase.getInstance().getReference("Books")

        booksRef.orderByChild("categoryId").equalTo(categoryId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val booksInCategory = mutableListOf<ModelBook>()

                for (bookSnapshot in dataSnapshot.children) {
                    val book = bookSnapshot.getValue(ModelBook::class.java)
                    book?.let {
                        booksInCategory.add(it)
                    }
                }

                if (booksInCategory.isNotEmpty()) {
                    val randomBook = booksInCategory.random()
                    Log.d("SimiliarActivity", "Recommended book: ${randomBook.title}")
                    Log.d("SimiliarActivity", "Recommended book: ${randomBook.categoryId}")
                    Log.d("SimiliarActivity", "Recommended book: ${randomBook.author}")
                    Log.d("SimiliarActivity", "Recommended book: ${randomBook.id}")
                    Log.d("SimiliarActivity", "Recommended book: ${randomBook.uid}")
                    Log.d("SimiliarActivity", "Recommended book: ${randomBook.url}")
                    Log.d("SimiliarActivity", "Recommended book: ${randomBook.timestamp}")
                    Log.d("SimiliarActivity", "Recommended book: ${randomBook.viewsCount}")
                    adapter.setRandomBook(randomBook)
                } else {
                    Log.d("SimiliarActivity", "No books found in this category")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SimiliarActivity", "Failed to read books in category: $error")
            }
        })
    }
}
