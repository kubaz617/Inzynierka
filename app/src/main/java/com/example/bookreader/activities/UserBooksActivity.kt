package com.example.bookreader.activities

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.bookreader.databinding.ActivityUserBooksBinding
import com.example.bookreader.models.ModelBook
import com.example.bookreader.models.ModelCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserBooksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserBooksBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var categoryArrayList: ArrayList<ModelCategory>
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var timer: CountDownTimer
    private var isAppRunning = true



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        setupWithViewPagerAdapter(binding.viewPager)
        binding.tabLayout.setupWithViewPager(binding.viewPager)


        binding.challengeBtn.setOnClickListener {
            challengeBook()
        }

        binding.checkButton.setOnClickListener {
            if (currentBookId.isNullOrEmpty()) {
                // Jeśli nie ma przypisanej książki, wyświetl komunikat
                Toast.makeText(this@UserBooksActivity, "Nie masz aktualnie żadnej książki do przeczytania", Toast.LENGTH_SHORT).show()
            } else {
                // Jeśli jest przypisana książka, sprawdź czy została przeczytana
                checkIfBookIsRead(currentBookId ?: "")
            }
        }


    }

    override fun onStop() {
        super.onStop()

        if (!isAppRunning) {
            // Jeśli użytkownik opuszcza aplikację, zapisz aktualny czas
            saveRemainingTime(getRemainingTime())
        }
    }

    override fun onResume() {
        super.onResume()

        // Ustaw flagę na true, ponieważ aplikacja jest w trakcie działania
        isAppRunning = true
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
            val email = firebaseUser!!.email
            binding.titleTv.text = email

    }

        private fun setupWithViewPagerAdapter(viewPager: ViewPager){
            viewPagerAdapter = ViewPagerAdapter(
                supportFragmentManager,
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
                this
            )

            categoryArrayList = ArrayList()

            val ref = FirebaseDatabase.getInstance().getReference("Categories")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categoryArrayList.clear()

                    val modelAll = ModelCategory("01,","Wszystkie",1,"")
                    val modelMostViewed = ModelCategory("01,","Najczęściej oglądane",1,"")
                    val modelFavorites = ModelCategory("01,","Ulubione",1,"")
                    categoryArrayList.add(modelAll)
                    categoryArrayList.add(modelMostViewed)
                    categoryArrayList.add(modelFavorites)
                    viewPagerAdapter.addFragment(
                        BooksUserFragment.newInstance(
                            "${modelAll.id}",
                            "${modelAll.category}",
                            "${modelAll.uid}"
                        ), modelAll.category
                    )
                    viewPagerAdapter.addFragment(
                        BooksUserFragment.newInstance(
                            "${modelMostViewed.id}",
                            "${modelMostViewed.category}",
                            "${modelMostViewed.uid}"
                        ), modelMostViewed.category
                    )
                    viewPagerAdapter.addFragment(
                        BooksUserFragment.newInstance(
                            "${modelFavorites.id}",
                            "${modelFavorites.category}",
                            "${modelFavorites.uid}"
                        ), modelFavorites.category
                    )
                    viewPagerAdapter.notifyDataSetChanged()

                    for (ds in snapshot.children){
                        val model = ds.getValue(ModelCategory::class.java)
                        categoryArrayList.add(model!!)
                        viewPagerAdapter.addFragment(
                            BooksUserFragment.newInstance(
                                "${model.id}",
                                "${model.category}",
                                "${model.uid}"
                            ), model.category
                        )
                        viewPagerAdapter.notifyDataSetChanged()
                    }

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

            viewPager.adapter = viewPagerAdapter
        }


    private var currentBookId: String? = null

    private fun challengeBook() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let { uid ->
            var bookFound = false // Definicja zmiennej bookFound

            val booksRef = FirebaseDatabase.getInstance().getReference("Books")

            booksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (bookSnapshot in dataSnapshot.children) {
                        val bookId = bookSnapshot.key // Pobranie ID książki
                        Log.d("CurrentBookId", "Aktualnie wylosowane ID książki: $bookId")

                        // Sprawdzenie, czy książka została przeczytana przez użytkownika
                        val userBooksRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("bookDetails").child(bookId!!)
                        userBooksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userBookSnapshot: DataSnapshot) {
                                if (!bookFound && userBookSnapshot.exists()) { // Dodatkowy warunek sprawdzający, czy nie znaleziono jeszcze nieprzeczytanej książki
                                    // Książka została znaleziona w węźle Users - sprawdzenie, czy jest przeczytana
                                    val isBookFullyRead = userBookSnapshot.child("isBookFullyRead").getValue(Boolean::class.java)

                                    if (isBookFullyRead == false) {
                                        // Książka nie została przeczytana przez użytkownika - zapisanie ID książki
                                        currentBookId = bookId // Zapisanie ID książki w zmiennej klasy

                                        // Wyświetlenie informacji o znalezionej książce
                                        val author = bookSnapshot.child("author").getValue(String::class.java)?.toString()
                                        val title = bookSnapshot.child("title").getValue(String::class.java)?.toString()

                                        Log.d("BookDetails", "Autor: $author, Tytuł: $title")
                                        bookFound = true // Ustawienie flagi na true, aby przerwać pętlę

                                        // Wyświetlenie autora i tytułu znalezionej książki jako komunikatu Toast
                                        val message = "Wyzwanie rozpoczęte\nTytuł książki: $title\nAutor książki: $author"
                                        Toast.makeText(this@UserBooksActivity, message, Toast.LENGTH_LONG).show()
                                        startTimer() // Uruchomienie timera po znalezieniu pierwszej nieprzeczytanej książki
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("BookStatus", "Błąd pobierania danych z bazy danych: ${error.message}")
                            }
                        })
                    }

                    // Warunek sprawdzający, czy nie znaleziono żadnej książki
                    if (!bookFound) {
                        Log.d("BookStatus", "Nie znaleziono nieprzeczytanej książki dla użytkownika o ID $uid.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("BookStatus", "Błąd pobierania danych z bazy danych: ${error.message}")
                }
            })
        } ?: run {
            Log.e("BookStatus", "Brak zalogowanego użytkownika.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Jeśli użytkownik opuszcza aplikację, ustaw flagę na false, aby zapisać czas
        isAppRunning = false
    }

    private fun checkIfBookIsRead(bookId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let { uid ->
            val userBookRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("bookDetails").child(bookId)

            userBookRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val isBookFullyRead = dataSnapshot.child("isBookFullyRead").getValue(Boolean::class.java)
                    if (isBookFullyRead == false) {
                        Toast.makeText(this@UserBooksActivity, "Książka nieprzeczytana", Toast.LENGTH_SHORT).show()
                        // Książka nie została przeczytana
                        // Tutaj możesz wykonać odpowiednie działania
                    } else {
                        Toast.makeText(this@UserBooksActivity, "Książka przeczytana", Toast.LENGTH_SHORT).show()
                        // Książka została przeczytana
                        // Tutaj możesz wykonać odpowiednie działania

                        // Jeżeli książka została przeczytana w wyznaczonym czasie, zakończ działanie timera
                        cancelTimer()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("BookStatus", "Błąd pobierania danych z bazy danych: ${databaseError.message}")
                }
            })
        } ?: run {
            Log.e("BookStatus", "Brak zalogowanego użytkownika.")
        }
    }

    private fun startTimer() {
        timer = object : CountDownTimer(60000, 1000) { // Odliczanie od 30 sekund co 1 sekundę
            override fun onTick(millisUntilFinished: Long) {
                // Wykonuje się co sekundę podczas odliczania
                Log.d("Timer", "Time remaining: ${millisUntilFinished / 1000} seconds") // Dodane logi

                // Tutaj można umieścić dowolną logikę związaną z odliczaniem, jeśli jest potrzebna
            }

            override fun onFinish() {
                // Wykonuje się po zakończeniu odliczania (po 30 sekundach)
                Log.d("Timer", "Timer finished")
                val message = "Przegrana"
                Toast.makeText(this@UserBooksActivity, message, Toast.LENGTH_LONG).show()

                // Po zakończeniu odliczania, ustaw wartość currentBookId na null
                currentBookId = null
            }
        }

        timer.start() // Uruchomienie timera
    }

    private fun cancelTimer() {
        // Anuluj działanie timera
        // Sprawdź, czy timer jest włączony, aby uniknąć błędu IllegalStateException
        if (::timer.isInitialized && timer != null) {
            timer.cancel()
        }
    }

    private fun saveRemainingTime(remainingTime: Long) {
        // Zapisz pozostały czas w SharedPreferences
        val sharedPreferences = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong("remaining_time", remainingTime)
        editor.apply()
    }

    private fun getRemainingTime(): Long {
        // Odczytaj pozostały czas z SharedPreferences
        val sharedPreferences = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("remaining_time", 0)
    }

    class ViewPagerAdapter(fm: FragmentManager, behavior: Int, context: Context): FragmentPagerAdapter(fm, behavior){
            private val fragmentsList: ArrayList<BooksUserFragment> = ArrayList()

            private val fragmentTitleList: ArrayList<String> = ArrayList()

            private val context: Context

            init {
                this.context = context
            }

            override fun getCount(): Int {
                return fragmentsList.size
            }

            override fun getItem(position: Int): Fragment {
                return fragmentsList[position]
            }

            override fun getPageTitle(position: Int): CharSequence{
                return fragmentTitleList[position]
            }

            public fun addFragment(fragment: BooksUserFragment, title: String){
                fragmentsList.add(fragment)

                fragmentTitleList.add(title)
            }
        }

}