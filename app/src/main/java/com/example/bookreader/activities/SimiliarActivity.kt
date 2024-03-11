package com.example.bookreader.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.bookreader.R
import com.example.bookreader.databinding.ActivitySimiliarBinding
import com.example.bookreader.models.ModelBook
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SimiliarActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimiliarBinding

    private lateinit var recommendedBookTitleTextView: TextView
    private lateinit var recommendedBookAuthorTextView: TextView
    private lateinit var no_books: ImageView
    private lateinit var suggestedCategoryTextView: TextView
    private lateinit var noBooksTextViewAbv: TextView
    private lateinit var noBooksTextViewBlw: TextView
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimiliarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        suggestedCategoryTextView = findViewById(R.id.titleTextView)
        recommendedBookTitleTextView = findViewById(R.id.recommendedBookTitleTextView)
        recommendedBookAuthorTextView = findViewById(R.id.recommendedBookAuthorTextView)
        no_books = findViewById(R.id.noBooksImageView)
        noBooksTextViewAbv = findViewById(R.id.noBooksTextAbv)
        noBooksTextViewBlw = findViewById(R.id.noBooksTextBlw)

        suggestedCategoryTextView.textSize = 20f
        recommendedBookTitleTextView.textSize = 20f
        recommendedBookAuthorTextView.textSize = 20f

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

        if (maxCategory != null) {
            val maxCategories = categoryCountMap.filterValues { it == maxCount }.keys
            if (maxCategories.size == 1) {
                val categoryName = categoriesSnapshot.child(maxCategory).child("category").getValue(String::class.java)
                categoryName?.let {
                    suggestedCategoryTextView.text = "Najczęściej czytana kategoria: $it"
                    recommendedBookFromCategory(maxCategory, uid)
                    suggestedCategoryTextView.visibility = View.VISIBLE
                }
            } else {
                suggestedCategoryTextView.text = "Masz dwie lub więcej ulubionych kategorii, przeczytaj z jednej z nich coś jeszcze a coś dla ciebie wybierzemy"
                suggestedCategoryTextView.visibility = View.VISIBLE
            }
        } else {
            no_books.visibility = View.VISIBLE
            noBooksTextViewAbv.text = "Ups, nic tutaj nie ma."
            noBooksTextViewBlw.text = "Zacznij czytać a coś ci zaproponujemy"
            noBooksTextViewAbv.visibility = View.VISIBLE
            noBooksTextViewBlw.visibility = View.VISIBLE

        }
    }


    private fun checkUser()  {
        val firebaseUser = firebaseAuth.currentUser
        val email = firebaseUser!!.email
        binding.titleTv.text = email
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
                                    val randomBooks = unreadBooks.shuffled().take(5)

                                    val recommendedBooksTitlesAndAuthors = randomBooks.joinToString("\n\n") { book ->
                                        "Tytuł: ${book.title}\nAutor: ${book.author}"
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