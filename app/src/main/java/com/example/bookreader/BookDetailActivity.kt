package com.example.bookreader

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.bookreader.databinding.ActivityBookDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.FirebaseDatabase.*
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.FileOutputStream
import kotlin.math.log

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

        binding.downloadBookBtn.setOnClickListener{
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "onCreate: Dostęp do schowka już jest przyznany")
                downloadBook()
            }
            else{
                Log.d(TAG, "onCreate: Dostęp do schowka odrzucony")
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

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
            downloadBook()
        }
        else{
            Log.d(TAG, "onCreate: Dostęp do schowka odrzucony")
            Toast.makeText(this, "Dostęp do schowka odrzucony", Toast.LENGTH_SHORT).show()
        }
    }


    private fun loadBookDetails() {
        val ref = getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object: ValueEventListener{
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

                    MyApplication.loadBookFromUrlSinglePage("$bookUrl", "$bookTitle", binding.pdfView, binding.progressBar, binding.pagesTv)
                    MyApplication.loadBookSize("$bookUrl", "$bookTitle", binding.sizeTv)

                    binding.titleTv.text = bookTitle
                    binding.descriptionTv.text = description
                    binding.viewsTv.text = viewsCount
                    binding.downloadsTv.text = downloadCount
                    binding.dateTv.text = date

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
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
                        binding.favoriteBookBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0,R.drawable.ic_favorite_on,0,0)
                    }
                    else{
                        Log.d(TAG, "onDataChange: Nie jest dostępna w ulubionych")
                        binding.favoriteBookBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0,R.drawable.ic_favorite_off,0,0)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
    private fun downloadBook(){

        Log.d(TAG, "downloadBook: Pobieranie książki")
        progressDialog.setMessage("Pobieranie książki")
        progressDialog.show()

        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
        storageReference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener {bytes ->
                Log.d(TAG, "downloadBook: Książka pobrana")
                saveToDownloadsFolder(bytes)
            }
            .addOnFailureListener{e->
                progressDialog.dismiss()
                Log.d(TAG, "downloadBook: Nie udało się pobrać książki z powodu${e.message}")
                Toast.makeText(this, "Nie udało się pobrać książki z powodu${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToDownloadsFolder(bytes: ByteArray) {
        Log.d(TAG, "saveToDownloadsFolder: zapisywanie pobranej książki")

        val nameWithExtension = "$bookTitle.pdf"

        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsFolder.mkdirs()

            val filePath = downloadsFolder.path +"/"+ nameWithExtension

            val out = FileOutputStream(filePath)
            out.write(bytes)
            out.close()

            Toast.makeText(this, "Zapisano w folderze Pobrane", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "saveToDownloadsFolder: Zapisano w folderze Pobrane")
            progressDialog.dismiss()
            incrementDownloadCount()
        }
        catch (e: Exception){
            progressDialog.dismiss()
            Log.d(TAG, "saveToDownloadsFolder: nie udało się zapisać z powodu${e.message}")
            Toast.makeText(this, "nie udało się zapisać z powodu${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun incrementDownloadCount() {
        Log.d(TAG, "incrementDownloadCount: ")

        val ref = getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object: ValueEventListener{
                @SuppressLint("SuspiciousIndentation")
                override fun onDataChange(snapshot: DataSnapshot) {
                    var downloadsCount = "${snapshot.child("downloadCount").value}"
                    Log.d(TAG, "onDataChange: Obecna ilość pobrań: $downloadsCount")

                    if (downloadsCount == "" || downloadsCount == "null"){
                        downloadsCount = "0"
                    }

                    val newDownloadCount: Long = downloadsCount.toLong() + 1
                    Log.d(TAG, "onDataChange: Obecna ilość pobrań: $downloadsCount")

                    val hashMap: HashMap<String, Any> = HashMap()
                    hashMap["downloadCount"] = newDownloadCount

                    val dbRef = getInstance().getReference("Books")
                        dbRef.child(bookId)
                            .updateChildren(hashMap)
                            .addOnSuccessListener {
                                Log.d(TAG, "onDataChange: Udało się zwiększyć wartość")
                            }
                            .addOnFailureListener{e->
                                Log.d(TAG, "onDataChange: Nie udało się zwiększyć wartości z powodu ${e.message}")
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