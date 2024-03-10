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
import com.example.bookreader.utils.MyApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.Date

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

                Toast.makeText(this@UserBooksActivity, "Nie masz aktualnie żadnej książki do przeczytania", Toast.LENGTH_SHORT).show()
            } else {

                checkIfBookIsRead(currentBookId!!)
            }
        }


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
            var bookFound = false

            val booksRef = FirebaseDatabase.getInstance().getReference("Books")

            booksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (bookSnapshot in dataSnapshot.children) {
                        val bookId = bookSnapshot.key

                        val userBooksRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("bookDetails").child(bookId!!)
                        userBooksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userBookSnapshot: DataSnapshot) {
                                if (!bookFound && userBookSnapshot.exists()) {
                                    val isBookFullyRead = userBookSnapshot.child("isBookFullyRead").getValue(Boolean::class.java)

                                    if (isBookFullyRead == false) {
                                        currentBookId = bookId

                                        val author = bookSnapshot.child("author").getValue(String::class.java)?.toString()
                                        val title = bookSnapshot.child("title").getValue(String::class.java)?.toString()

                                        Log.d("BookDetails", "Autor: $author, Tytuł: $title")
                                        bookFound = true

                                        val message = "Wyzwanie rozpoczęte\nTytuł książki: $title\nAutor książki: $author"
                                        Toast.makeText(this@UserBooksActivity, message, Toast.LENGTH_LONG).show()

                                        saveChallengeDate(uid)
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("BookStatus", "Błąd pobierania danych z bazy danych: ${error.message}")
                            }
                        })
                    }

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


    private fun saveChallengeDate(userId: String) {
        val currentTimeMillis = System.currentTimeMillis()
        val challengeTimeMillis = currentTimeMillis + (60 * 1000) // Dodanie 1 minuty w milisekundach

        val challengeDateRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("challengeDate")
        challengeDateRef.setValue(challengeTimeMillis)
            .addOnSuccessListener {
                Log.d("ChallengeDate", "Pomyślnie zapisano datę wyzwania w bazie danych.")
            }
            .addOnFailureListener { e ->
                Log.e("ChallengeDate", "Błąd podczas zapisywania daty wyzwania: ${e.message}")
            }
    }

    private fun saveAndDisplayDates(uid: String, callback: (Boolean) -> Unit) {
        saveCurrentDateToDatabase(uid)

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val challengeDate = dataSnapshot.child("challengeDate").getValue(Long::class.java)
                val currentDate = dataSnapshot.child("currentDate").getValue(Long::class.java)

                if (challengeDate != null && currentDate != null) {
                    val isChallengeCompleted = currentDate < challengeDate
                    callback(isChallengeCompleted)
                } else {
                    callback(false)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error fetching data: ${databaseError.message}")
                callback(false)
            }
        })
    }



    private fun checkIfBookIsRead(bookId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let { uid ->
            val userBookRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("bookDetails").child(bookId)

            saveAndDisplayDates(uid) { isChallengeCompleted ->
                userBookRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val isBookFullyRead = dataSnapshot.child("isBookFullyRead").getValue(Boolean::class.java)

                        isBookFullyRead?.let { isFullyRead ->
                            Log.d("BookStatus", "Wartość isBookFullyRead: $isFullyRead")
                            if (isFullyRead && isChallengeCompleted) {
                                Log.d("BookStatus", "Wyzwanie ukończone, gratulacje")
                            } else if (isChallengeCompleted) {
                                Log.d("BookStatus", "Książka nadal nie jest przeczytana, czytaj dalej")
                            } else {
                                Log.d("BookStatus", "Wyzwanie niezaliczone, czas upłynął")
                            }
                        } ?: run {
                            Log.e("BookStatus", "Brak informacji o stanie przeczytania książki")
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("BookStatus", "Błąd pobierania danych z bazy danych: ${databaseError.message}")
                    }
                })
            }
        } ?: run {
            Log.e("BookStatus", "Brak zalogowanego użytkownika.")
        }
    }


    private fun saveCurrentDateToDatabase(userId: String) {
        val currentTimeMillis = System.currentTimeMillis()
        val currentDateRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("currentDate")
        currentDateRef.setValue(currentTimeMillis)
            .addOnSuccessListener {
                Log.d("ChallengeDate", "Pomyślnie zapisano obecną datę wyzwania w bazie danych.")
            }
            .addOnFailureListener { e ->
                Log.e("ChallengeDate", "Błąd podczas zapisywania obecnej daty wyzwania: ${e.message}")
            }
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