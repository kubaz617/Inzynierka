package com.example.bookreader.activities

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.bookreader.databinding.ActivityUserBooksBinding
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
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            userId?.let { uid ->
                getChallengeId(uid) { challengeId ->
                    if (challengeId.isNullOrEmpty()) {
                        Toast.makeText(this@UserBooksActivity, "Nie masz żadnego rozpoczętego wyzwania", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("BookStatus", "Przekazane challengeId: $challengeId")
                        checkIfBookIsRead(challengeId)
                    }
                }
            } ?: run {
                Log.e("BookStatus", "Brak zalogowanego użytkownika.")
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
                    val allBooks = mutableListOf<String>()

                    dataSnapshot.children.forEach { bookSnapshot ->
                        allBooks.add(bookSnapshot.key!!)
                    }

                    val randomIndex = (0 until allBooks.size).random()

                    val randomBookId = allBooks[randomIndex]

                    val userBooksRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("bookDetails").child(randomBookId)
                    userBooksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userBookSnapshot: DataSnapshot) {
                            if (!bookFound && userBookSnapshot.exists()) {
                                val isBookFullyRead = userBookSnapshot.child("isBookFullyRead").getValue(Boolean::class.java)

                                if (isBookFullyRead == false) {
                                    currentBookId = randomBookId

                                    val author = dataSnapshot.child(randomBookId).child("author").getValue(String::class.java)?.toString()
                                    val title = dataSnapshot.child(randomBookId).child("title").getValue(String::class.java)?.toString()

                                    Log.d("BookDetails", "Autor: $author, Tytuł: $title")
                                    bookFound = true

                                    val message = "Wyzwanie rozpoczęte\nTytuł książki: $title\nAutor książki: $author"
                                    Toast.makeText(this@UserBooksActivity, message, Toast.LENGTH_LONG).show()

                                    saveChallengeDate(uid, currentBookId!!)
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("BookStatus", "Błąd pobierania danych z bazy danych: ${error.message}")
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("BookStatus", "Błąd pobierania danych z bazy danych: ${error.message}")
                }
            })
        } ?: run {
            Log.e("BookStatus", "Brak zalogowanego użytkownika.")
        }
    }


    private fun getChallengeId(uid: String, callback: (String?) -> Unit) {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
        val challengeDetailsRef = userRef.child("challengeDetails")

        challengeDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val challengeId = dataSnapshot.children.find { it.hasChild("bookId") }?.child("bookId")?.getValue(String::class.java)
                callback(challengeId)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error fetching data: ${databaseError.message}")
                callback(null)
            }
        })
    }

    private fun saveChallengeDate(userId: String, bookId: String) {
        val currentTimeMillis = System.currentTimeMillis()
        val challengeTimeMillis = currentTimeMillis + (10 * 60 * 60 * 1000)

        val challengeDateRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("challengeDetails")

        challengeDateRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    dataSnapshot.children.forEach { challengeSnapshot ->
                        val challengeId = challengeSnapshot.key
                        if (challengeId != null) {
                            val challengeDetails = HashMap<String, Any>()
                            challengeDetails["challengeDate"] = challengeTimeMillis
                            challengeDetails["bookId"] = bookId

                            challengeDateRef.child(challengeId)
                                .setValue(challengeDetails)
                                .addOnSuccessListener {
                                    Log.d("ChallengeDate", "Pomyślnie zaktualizowano datę wyzwania w bazie danych.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ChallengeDate", "Błąd podczas aktualizacji daty wyzwania: ${e.message}")
                                }
                        }
                    }
                } else {
                    val challengeDetails = HashMap<String, Any>()
                    challengeDetails["challengeDate"] = challengeTimeMillis
                    challengeDetails["bookId"] = bookId

                    val challengeId = challengeDateRef.push().key

                    challengeId?.let { id ->
                        challengeDateRef.child(id)
                            .setValue(challengeDetails)
                            .addOnSuccessListener {
                                Log.d("ChallengeDate", "Pomyślnie zapisano nową datę wyzwania w bazie danych.")
                            }
                            .addOnFailureListener { e ->
                                Log.e("ChallengeDate", "Błąd podczas zapisywania nowej daty wyzwania: ${e.message}")
                            }
                    } ?: run {
                        Log.e("ChallengeDate", "Nie udało się wygenerować klucza dla nowego wyzwania.")
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ChallengeDate", "Błąd pobierania danych z bazy danych: ${databaseError.message}")
            }
        })
    }

    private fun saveAndDisplayDates(uid: String, callback: (Boolean) -> Unit) {
        saveCurrentDateToDatabase(uid)

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val challengeDetailsSnapshot = dataSnapshot.child("challengeDetails")
                var isChallengeCompleted = false

                challengeDetailsSnapshot.children.forEach { challengeSnapshot ->
                    val challengeDate = challengeSnapshot.child("challengeDate").getValue(Long::class.java)
                    val currentDate = dataSnapshot.child("currentDate").getValue(Long::class.java)

                    if (challengeDate != null && currentDate != null) {
                        isChallengeCompleted = currentDate < challengeDate
                    }
                }

                callback(isChallengeCompleted)
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

                        if (isBookFullyRead != null) {
                            Log.d("BookStatus", "Wartość isBookFullyRead: $isBookFullyRead")
                            if (isBookFullyRead && isChallengeCompleted) {
                                Log.d("BookStatus", "Wyzwanie ukończone, gratulacje")
                                val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
                                userRef.child("challengeDetails").removeValue()
                                    .addOnSuccessListener {
                                        Log.d("ChallengeDetails", "Węzeł challengeDetails został pomyślnie usunięty po przeczytaniu książki.")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ChallengeDetails", "Błąd podczas usuwania węzła challengeDetails: ${e.message}")
                                    }
                            } else if (isChallengeCompleted) {
                                Log.d("BookStatus", "Książka nadal nie jest przeczytana, czytaj dalej")
                            } else {
                                Log.d("BookStatus", "Wyzwanie niezaliczone, czas upłynął")
                            }
                        } else {
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