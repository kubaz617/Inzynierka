package com.example.bookreader.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.BatteryManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Toast
import com.example.bookreader.R
import com.example.bookreader.utils.Constants
import com.example.bookreader.databinding.ActivityBookViewBinding
import com.example.bookreader.utils.MyApplication
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import android.widget.TextView

class BookViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookViewBinding
    private lateinit var pdfView: PDFView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var mediaPlayer: MediaPlayer
    private var isMusicPlaying = false
    private lateinit var seekBar: SeekBar
    private lateinit var batteryLevelTextView: TextView
    private lateinit var batteryBroadcastReceiver: BroadcastReceiver

    private companion object{
        const val TAG = "BOOK_VIEW_TAG"
        const val PREF_NAME = "BookReaderPrefs"
        const val KEY_BOOKMARK_PAGE = "BookmarkPage"
    }

    private var bookmarkedPage = 0
    private var furthestPageRead = 0
    private var isBookFullyRead = false
    private var categoryId: String = ""


    private var bookId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        batteryLevelTextView = findViewById(R.id.batteryLevelTextView)

        seekBar = findViewById(R.id.seekBar)



        bookId = intent.getStringExtra("bookId")!!
        furthestPageRead = MyApplication.furthestPageRead
        isBookFullyRead = MyApplication.isBookFullyRead
        pdfView = binding.bookView
        loadBookDetails()

        val LabelButton: ImageButton = findViewById(R.id.Label)
        val AddLabel: ImageButton = findViewById(R.id.addLabel)
        val jumpToCurrentPage: ImageButton = findViewById(R.id.jumpToCurrentPage)

        mediaPlayer = MediaPlayer.create(this, R.raw.book_music)



        AddLabel.setOnClickListener {
            val currentPage = pdfView.currentPage
            bookmarkedPage = currentPage
            saveBookmarkPage(bookId, bookmarkedPage)
            Toast.makeText(this, "Zakładka dodana na stronie ${bookmarkedPage + 1}", Toast.LENGTH_SHORT).show()
        }

        LabelButton.setOnClickListener {
            val bookmarkPage = getBookmarkPage(bookId)
            if (bookmarkPage != -1) {
                pdfView.jumpTo(bookmarkPage)
                Toast.makeText(this, "Powrócono do zakładki na stronie ${bookmarkPage + 1}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Brak zapisanych zakładek", Toast.LENGTH_SHORT).show()
            }
        }

        val musicButton: ImageButton = findViewById(R.id.musicBtn)
        musicButton.setOnClickListener {
            toggleMusic()
        }

        jumpToCurrentPage.setOnClickListener{
            moveToLastReadPage()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setBrightnessLevel(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        batteryBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    context.registerReceiver(null, ifilter)
                }
                val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

                val batteryPct: Float = level / scale.toFloat() * 100
                batteryLevelTextView.text = "$batteryPct%"
            }
        }

        registerReceiver(batteryBroadcastReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    }

    private fun loadBookDetails() {
        Log.d(TAG, "loadBookDetails: Pobierz adres url książki z bazy danych")
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pdfUrl = snapshot.child("url").value
                    categoryId = snapshot.child("categoryId").value.toString()
                    Log.d(TAG, "onDataChange: PDF_URL: $pdfUrl")
                    loadBookFromUrl("$pdfUrl", categoryId)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.d(TAG, "loadBookDetails: Błąd w pobieraniu danych: ${error.message}")
                }
            })
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun setBrightnessLevel(brightness: Int) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness / 255.0f
        window.attributes = layoutParams
    }


    override fun onPause() {
        super.onPause()
        MyApplication.furthestPageRead = furthestPageRead
        MyApplication.isBookFullyRead = isBookFullyRead
    }

    private fun toggleMusic() {
        if (isMusicPlaying) {
            stopMusic()
        } else {
            startMusic()
        }
        isMusicPlaying = !isMusicPlaying
    }

    private fun loadBookFromUrl(pdfUrl: String, categoryId: String) {
        Log.d(TAG, "loadBookFromUrl: Pobieranie książki z bazy danych przy pomocy adresu Url")
        val reference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
        reference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener { bytes ->
                Log.d(TAG, "loadBookFromUrl: Udało się pobrać adres url")
                pdfView = binding.bookView
                pdfView.fromBytes(bytes)
                    .swipeHorizontal(false)
                    .onPageChange { page, pageCount ->
                        val currentPage = page + 1
                        binding.toolbarSubtitleTv.text = "$currentPage/$pageCount"
                        Log.d(TAG, "loadBookFromUrl: $currentPage/$pageCount")
                        furthestPageRead = maxOf(furthestPageRead, page + 1)
                        isBookFullyRead = page + 1 == pageCount
                    }
                    .onError { t ->
                        Log.d(TAG, "loadBookFromUrl: ${t.message}")
                    }
                    .onPageError { page, t ->
                        Log.d(TAG, "loadBookFromUrl: ${t.message}")
                    }
                    .load()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "loadBookFromUrl: Nie udało się pobrać adresu url z powodu ${e.message}")
                binding.progressBar.visibility = View.GONE
            }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        saveUserBookDetails(bookId, categoryId, pdfView.currentPage, furthestPageRead, isBookFullyRead)
    }

    private fun moveToLastReadPage() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId!!)
        val bookDetailsRef = databaseReference.child("bookDetails").child(bookId)

        bookDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val currentPage = dataSnapshot.child("currentPage").getValue(Int::class.java) ?: 0
                pdfView.jumpTo(currentPage)
                Toast.makeText(this@BookViewActivity, "Powrócono do ostatnio czytanej strony", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "moveToLastReadPage: Nie udało się pobrać danych ${databaseError.message}")
            }
        })
    }



    private fun saveBookmarkPage(bookId: String, pageNumber: Int) {
        val key = "$KEY_BOOKMARK_PAGE-$bookId"
        val editor = sharedPreferences.edit()
        editor.putInt(key, pageNumber)
        editor.apply()
    }

    private fun getBookmarkPage(bookId: String): Int {
        val key = "$KEY_BOOKMARK_PAGE-$bookId"
        return sharedPreferences.getInt(key, -1)
    }


    private var musicPagesListener: ValueEventListener? = null
    private fun shouldPlayMusic(bookId: String, currentPage: Int) {
        val ref = FirebaseDatabase.getInstance().getReference("Books").child(bookId).child("musicPages")

        musicPagesListener?.let {
            ref.removeEventListener(it)
        }

        musicPagesListener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val musicPages = dataSnapshot.value as Map<String, Boolean>?
                val isMusicPage = musicPages?.get("page$currentPage") ?: false

                if (isMusicPage) {
                    startMusic()
                } else {
                    stopMusic()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "onCancelled: ${databaseError.message}")
            }
        })
    }

    private fun startMusic() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.isLooping = true
            mediaPlayer.start()
        }
    }

    private fun stopMusic() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        unregisterReceiver(batteryBroadcastReceiver)
    }

    private fun saveUserBookDetails(
        bookId: String,
        categoryId: String,
        currentPage: Int,
        furthestPageRead: Int,
        isBookFullyRead: Boolean
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val pdfView = this.pdfView

        if (userId != null && pdfView != null) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId)
            val newFurthestPageRead = if (currentPage > furthestPageRead) currentPage else furthestPageRead
            val isFullyRead = if (currentPage == newFurthestPageRead && currentPage == pdfView.pageCount) true else isBookFullyRead
            val bookDetailsMap = mapOf(
                "currentPage" to currentPage,
                "furthestPageRead" to newFurthestPageRead,
                "isBookFullyRead" to isFullyRead,
                "categoryId" to categoryId
            )
            databaseReference.child("bookDetails").child(bookId).setValue(bookDetailsMap)
                .addOnSuccessListener {
                    Log.d(TAG, "saveUserBookDetails: Dane zostały zapisane")
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "saveUserBookDetails: Błąd podczas zapisu danych ${e.message}")
                }
        } else {
            Log.d(TAG, "saveUserBookDetails: Użytkownik nie jest zalogowany")
        }
    }
}