package com.example.bookreader.utils

import android.app.Application
import android.app.ProgressDialog
import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar
import java.util.Locale


class MyApplication:Application() {
    override fun onCreate() {
        super.onCreate()
    }

    companion object{

        var furthestPageRead: Int = 0
        var isBookFullyRead: Boolean = false
        fun formatTimeStamp(timestamp: Long) :String{
            val cal = Calendar.getInstance(Locale.ENGLISH)
            cal.timeInMillis = timestamp
            return DateFormat.format("dd/MM/yyyy",cal).toString()

        }


        fun loadBookFromUrlSinglePage(
            bookUrl: String,
            bookTitle: String,
            pdfView: PDFView,
            progressBar: ProgressBar,
            pagesTv: TextView?
        ){

            val TAG = "PDF_THUMBNAIL_TAG"

            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
            ref.getBytes(Constants.MAX_BYTES_PDF)
                .addOnSuccessListener { bytes ->
                    Log.d(TAG, "loadBookSize: Rozmiar w bajtach $bytes")

                  pdfView.fromBytes(bytes)
                      .pages(0)
                      .spacing(0)
                      .swipeHorizontal(false)
                      .enableSwipe(false)
                      .onError{t->
                          progressBar.visibility = View.INVISIBLE
                          Log.d(TAG, "loadBookFromUrlSinglePage: ${t.message}")
                      }
                      .onPageError{page, t->
                          progressBar.visibility = View.INVISIBLE
                          Log.d(TAG, "loadBookFromUrlSinglePage: ${t.message}")
                      }
                      .onLoad {nbPages ->
                          Log.d(TAG, "loadBookFromUrlSinglePage: Pages: $nbPages")
                          progressBar.visibility = View.INVISIBLE

                          if (pagesTv != null){
                              pagesTv.text = "$nbPages"
                          }
                      }
                      .load()
                }
                .addOnFailureListener{e->
                    Log.d(TAG, "loadBookSize: Błąd podczas pobierania metadanych ${e.message}")

                }
        }



        fun loadCategory(categoryId: String, categoryTv: TextView){

            val ref = FirebaseDatabase.getInstance().getReference("Categories")
            ref.child(categoryId)
                .addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val category = "${snapshot.child("category").value}"

                        categoryTv.text = category
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }

        fun deleteBook(context: Context, bookId: String, bookUrl: String, bookTitle: String){

            val TAG = "DELETE_BOOK_TAG"

            Log.d(TAG, "deleteBook: Usuwanie książki")

            val progressDialog = ProgressDialog(context)
            progressDialog.setTitle("Proszę czekać")
            progressDialog.setMessage("Usuwanie $bookTitle")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()

            Log.d(TAG, "deleteBook: Usuwanie z bazy danych")
            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
            storageReference.delete()
                .addOnSuccessListener {
                    Log.d(TAG, "deleteBook: Usuwanie z bazy danych")
                    Log.d(TAG, "deleteBook: Książka usunięta")

                    val ref = FirebaseDatabase.getInstance().getReference("Books")
                    ref.child(bookId)
                        .removeValue()
                        .addOnSuccessListener {
                        progressDialog.dismiss()
                            Toast.makeText(context, " Książka usunięta", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "deleteBook: Usuwanie z bazy danych")
                        }
                        .addOnFailureListener {e->
                            progressDialog.dismiss()
                            Log.d(TAG, "deleteBook: Nie udało się usunąć książki z bazy danych z powodu ${e.message}")
                            Toast.makeText(context, " Nie udało się usunąć książki z powodu ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener{ e->
                    progressDialog.dismiss()
                    Log.d(TAG, "deleteBook: Nie udało się usunąć książki z schowka z powodu ${e.message}")
                    Toast.makeText(context, " Nie udało się usunąć książki z powodu ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        fun incrementBookViewCount(bookId: String){
            val ref = FirebaseDatabase.getInstance().getReference("Books")
            ref.child(bookId)
                .addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var viewsCount = "${snapshot.child("viewsCount").value}"

                        if (viewsCount == "" || viewsCount == "null"){
                            viewsCount = "0";
                        }

                        val newViewsCount = viewsCount.toLong() + 1

                        val hashMap = HashMap<String, Any>()
                        hashMap["viewsCount"] = newViewsCount

                        val dbRef = FirebaseDatabase.getInstance().getReference("Books")
                        dbRef.child(bookId)
                            .updateChildren(hashMap)
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }
    }


}