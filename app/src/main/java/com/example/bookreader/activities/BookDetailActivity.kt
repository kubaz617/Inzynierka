package com.example.bookreader.activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.bookreader.utils.MyApplication
import com.example.bookreader.R
import com.example.bookreader.databinding.ActivityBookDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BookDetailActivity : AppCompatActivity() {

    private lateinit var binding:ActivityBookDetailBinding

    private companion object{
        const val TAG = "BOOK_DETAILS_TAG"
    }

    private lateinit var firebaseAuth: FirebaseAuth

    private var bookId = ""

    private var bookTitle = ""
    private var bookUrl = ""

    private lateinit var progressDialog: ProgressDialog

    private var isInMyFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        bookId = intent.getStringExtra("bookId")!!

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Prosze czekać")
        progressDialog.setCanceledOnTouchOutside(false)

        MyApplication.incrementBookViewCount(bookId)

        loadBookDetails()

        checkIsFavorite()


        binding.readBookBtn.setOnClickListener{
            val intent = Intent(this, BookViewActivity::class.java)
            intent.putExtra("bookId", bookId);
            startActivity(Intent(intent))
        }

        binding.favoriteBookBtn.setOnClickListener{
            if (isInMyFavorite){
                removeFromFavorite()
            }
            else{
                addToFavorite()
            }
        }
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted:Boolean ->
        if (isGranted){
            Log.d(TAG, "onCreate: Dostęp do schowka przyznany")
        }
        else{
            Log.d(TAG, "onCreate: Dostęp do schowka odrzucony")
            Toast.makeText(this, "Dostęp do schowka odrzucony", Toast.LENGTH_SHORT).show()
        }
    }


    private fun loadBookDetails() {
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val categoryId = "${snapshot.child("categoryId").value}"
                    val description = "${snapshot.child("description").value}"
                    val downloadCount = "${snapshot.child("downloadCount").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    bookTitle = "${snapshot.child("title").value}"
                    val uid = "${snapshot.child("uid").value}"
                    bookUrl = "${snapshot.child("url").value}"
                    val viewsCount = "${snapshot.child("viewsCount").value}"

                    val date = MyApplication.formatTimeStamp(timestamp.toLong())

                    MyApplication.loadCategory(categoryId, binding.categoryTv)

                    MyApplication.loadBookFromUrlSinglePage(
                        "$bookUrl",
                        "$bookTitle",
                        binding.pdfView,
                        binding.progressBar,
                        binding.pagesTv
                    )

                    MyApplication.loadBookSize("$bookUrl", "$bookTitle", binding.sizeTv)

                    binding.titleTv.text = bookTitle
                    binding.descriptionTv.text = description
                    binding.viewsTv.text = viewsCount
                    binding.dateTv.text = date

                    checkIfBookRead()
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun checkIfBookRead() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
            userRef.child("bookDetails").child(bookId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isBookFullyRead = snapshot.child("isBookFullyRead").value as? Boolean ?: false
                    val readStatus = if (isBookFullyRead) "Tak" else "Nie"
                    binding.isDoneTv.text = readStatus
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }

    private fun checkIsFavorite(){
        Log.d(TAG, "checkIsFavorite: Sprawdzanie czy książka jest w ulubionych")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    isInMyFavorite = snapshot.exists()
                    if (isInMyFavorite){
                        Log.d(TAG, "onDataChange: Jest dostępna w ulubionych")
                        binding.favoriteBookBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0,
                            R.drawable.ic_favorite_on,0,0)
                    }
                    else{
                        Log.d(TAG, "onDataChange: Nie jest dostępna w ulubionych")
                        binding.favoriteBookBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0,
                            R.drawable.ic_favorite_off,0,0)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }


    private fun addToFavorite(){
        Log.d(TAG, "addToFavorite: Dodawanie do ulubionych")
        val timestamp = System.currentTimeMillis()

        val hashMap = HashMap<String, Any>()
        hashMap["bookId"] = bookId
        hashMap["timestamp"] = timestamp

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "addToFavorite: Dodano do ulubionych")
                Toast.makeText(this, "Dodano do ulubionych", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener{e->
                Log.d(TAG, "addToFavorite: Nie udało się dodać do ulubionych z powodu${e.message}")
                Toast.makeText(this, "Nie udało się dodać do ulubionych z powodu${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeFromFavorite(){
        Log.d(TAG, "removeFromFavorite: Usuwanie z ulubionych")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .removeValue().addOnSuccessListener {
                Log.d(TAG, "removeFromFavorite: Usunięto z ulubionych")
                Toast.makeText(this, "Usunięto z ulubionych", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {e->
                Log.d(TAG, "removeFromFavorite: Nie udało się usunąć z ulubionych z powodu ${e.message}")
                Toast.makeText(this, "Nie udało się usunąć z ulubionych z powodu${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


}