package com.example.bookreader.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.bookreader.R
import com.example.bookreader.models.ModelBook
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SimiliarActivity : AppCompatActivity() {

    private lateinit var recommendedBookTitleTextView: TextView
    private lateinit var recommendedBookAuthorTextView: TextView

    private lateinit var suggestedCategoryTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_similiar)

        suggestedCategoryTextView = findViewById(R.id.titleTextView)
        recommendedBookTitleTextView = findViewById(R.id.recommendedBookTitleTextView)
        recommendedBookAuthorTextView = findViewById(R.id.recommendedBookAuthorTextView)

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
                            findMostReadCategory(userBookDetailsSnapshot, categoriesSnapshot, uid)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("SimiliarActivity", "Nie udało się odczytać kategori: $error")
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SimiliarActivity", "Nie udało się pobrać informacji o użytkowniku: $error")
                }
            })
        }
    }

    private fun findMostReadCategory(userBookDetailsSnapshot: DataSnapshot, categoriesSnapshot: DataSnapshot, uid: String) {
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
                recommendedBookFromCategory(categoryId, uid)
                suggestedCategoryTextView.visibility = View.VISIBLE
            }
        }
    }

    private fun recommendedBookFromCategory(categoryId: String, uid: String) {
        val booksRef = FirebaseDatabase.getInstance().getReference("Books")

        booksRef.orderByChild("categoryId").equalTo(categoryId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val unreadBooks = mutableListOf<ModelBook>()

                for (bookSnapshot in dataSnapshot.children) {
                    val book = bookSnapshot.getValue(ModelBook::class.java)
                    book?.let {
                        val bookDetailsRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("bookDetails").child(book.id)
                        bookDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(bookDetailsSnapshot: DataSnapshot) {
                                val isBookFullyReadSnapshot = bookDetailsSnapshot.child("isBookFullyRead")
                                val isBookFullyRead = if (isBookFullyReadSnapshot.exists()) {
                                    isBookFullyReadSnapshot.getValue(Boolean::class.java)
                                } else {
                                    null
                                }

                                if (isBookFullyRead == false || isBookFullyRead == null) {
                                    unreadBooks.add(it)
                                }

                                if (unreadBooks.isNotEmpty()) {
                                    val randomBooks = unreadBooks.shuffled().take(3)

                                    val recommendedBooksTitlesAndAuthors = randomBooks.joinToString("\n\n") {
                                        "${it.title}\n${it.author}"
                                    }

                                    recommendedBookTitleTextView.text = recommendedBooksTitlesAndAuthors
                                    recommendedBookTitleTextView.visibility = View.VISIBLE
                                } else {
                                    Log.d("SimiliarActivity", "Brak nieprzeczytanych książek w tej kategorii")
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("SimiliarActivity", "Nie udało się załadować szczegółów: $error")
                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SimiliarActivity", "Nie udało się załadować książek w tej kategorii: $error")
            }
        })
    }



}
