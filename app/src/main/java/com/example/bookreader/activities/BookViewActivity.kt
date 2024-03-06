package com.example.bookreader.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.example.bookreader.utils.Constants
import com.example.bookreader.databinding.ActivityBookViewBinding
import com.example.bookreader.utils.MyApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class BookViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookViewBinding

    private companion object{
        const val TAG = "BOOK_VIEW_TAG"
    }



    private var currentPage: Int = 0
    private var furthestPageRead = 0
    private var isBookFullyRead = false


    var bookId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId = intent.getStringExtra("bookId")!!
        furthestPageRead = MyApplication.furthestPageRead
        isBookFullyRead = MyApplication.isBookFullyRead
        loadBookDetails()
    }

    private fun loadBookDetails() {
        Log.d(TAG, "loadBookDetails: Get Book Url from db")
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pdfUrl = snapshot.child("url").value
                    Log.d(TAG, "onDataChange: PDF_URL: $pdfUrl")

                    loadBookFromUrl("$pdfUrl")
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    override fun onPause() {
        super.onPause()
        MyApplication.furthestPageRead = furthestPageRead
        MyApplication.isBookFullyRead = isBookFullyRead
    }

    private fun loadBookFromUrl(pdfUrl: String) {
        Log.d(TAG, "loadBookFromUrl: Get Book from firebase storage using URL")

        val reference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
        reference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener {bytes ->
                Log.d(TAG, "loadBookFromUrl: pdf got from url")

                binding.bookView.fromBytes(bytes)
                    .swipeHorizontal(false)
                    .onPageChange{page, pageCount ->
                        val currentPage = page+1
                        furthestPageRead = maxOf(furthestPageRead, page+1)
                        binding.toolbarSubtitleTv.text = "$currentPage/$pageCount"
                        Log.d(TAG, "loadBookFromUrl: $currentPage/$pageCount")

                        isBookFullyRead = page + 1 == pageCount
                        saveUserBookDetails(bookId, currentPage,furthestPageRead,isBookFullyRead)
                    }
                    .onError{t->
                        Log.d(TAG, "loadBookFromUrl: ${t.message}")
                    }
                    .onPageError{page, t->
                        Log.d(TAG, "loadBookFromUrl: ${t.message}")
                    }
                    .load()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener {e->
                Log.d(TAG, "loadBookFromUrl: Failed to get url due to ${e.message}")
                binding.progressBar.visibility = View.GONE
            }
    }


    // Dodaj tę funkcję do swojej klasy BookViewActivity
    private fun saveUserBookDetails(bookId: String, currentPage: Int, furthestPageRead: Int, isBookFullyRead: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId)
            val newFurthestPageRead = if (currentPage > furthestPageRead) currentPage else furthestPageRead
            val bookDetailsMap = mapOf(
                "currentPage" to currentPage,
                "furthestPageRead" to newFurthestPageRead,
                "isBookFullyRead" to isBookFullyRead
            )
            databaseReference.child("bookDetails").child(bookId).setValue(bookDetailsMap)
                .addOnSuccessListener {
                    Log.d(TAG, "saveUserBookDetails: Book details saved successfully.")
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "saveUserBookDetails: Failed to save book details: ${e.message}")
                }
        } else {
            Log.d(TAG, "saveUserBookDetails: User not authenticated.")
        }
    }
}