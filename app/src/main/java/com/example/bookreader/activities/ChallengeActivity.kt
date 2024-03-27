package com.example.bookreader.activities

import android.content.pm.ActivityInfo
import android.graphics.PorterDuff
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.bookreader.R
import com.example.bookreader.databinding.ActivityChallengeBinding
import com.example.bookreader.databinding.ActivityUserBooksBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChallengeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChallengeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityChallengeBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        binding.challengeBtn.setOnClickListener {
            // Pobierz identyfikator wyzwania
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            userId?.let { uid ->
                getChallengeId(uid) { challengeId ->
                    if (challengeId != null) {
                        // Istnieje aktywne wyzwanie
                        // Wyświetl dialog z pytaniem, czy użytkownik chce rozpocząć nowe wyzwanie
                        AlertDialog.Builder(this)
                            .setTitle("Aktywne wyzwanie")
                            .setMessage("Masz obecnie aktywne wyzwanie. Czy chcesz rozpocząć nowe? Uważaj, rozpoczęcie nowego wyzwania odejmie część posiadanych przez ciebie punktów.")
                            .setPositiveButton("Tak") { _, _ ->
                                // Rozpocznij nowe wyzwanie
                                decreasePoints(100)
                                startChallengeToUser(userId)
                                challengeBook(5)
                            }
                            .setNegativeButton("Nie") { _, _ ->
                                // Pozostaw obecne wyzwanie
                                // Tutaj możesz dodać kod, który ma być wykonany, gdy użytkownik nie chce rozpoczynać nowego wyzwania
                            }
                            .show()
                    } else {
                        startChallengeToUser(userId)
                        challengeBook(5)
                    }
                }
            } ?: run {
                Log.e("ChallengeStatus", "Brak zalogowanego użytkownika.")
            }
        }

        binding.chooseBookBtn.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            userId?.let { uid ->
                getTenPageId(uid) { challengeId ->
                    if (challengeId != null) {
                        // Istnieje aktywne wyzwanie
                        AlertDialog.Builder(this)
                            .setTitle("Aktywne wyzwanie")
                            .setMessage("Masz obecnie aktywne wyzwanie. Czy chcesz rozpocząć nowe? Uważaj, rozpoczęcie nowego wyzwania odejmie część posiadanych przez ciebie punktów")
                            .setPositiveButton("Tak") { _, _ ->
                                startChallengeToUser(userId)
                                decreasePoints(25)
                                showBookSelectionDialog()
                            }
                            .setNegativeButton("Nie") { _, _ ->
                                // Pozostaw obecne wyzwanie
                            }
                            .show()
                    } else {
                        startChallengeToUser(userId)
                        showBookSelectionDialog()
                    }
                }
            } ?: run {
                Log.e("ChallengeStatus", "Brak zalogowanego użytkownika.")
            }
        }


        binding.newBookBtn.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            userId?.let { uid ->
                // Sprawdzenie statusu cooldownu
                isCooldownActive { onCooldownCheck ->
                    if (onCooldownCheck) {
                        // Jeśli cooldown jest aktywny, wyświetl odpowiedni komunikat
                        Toast.makeText(this@ChallengeActivity, "Wyzwanie nie jest jeszcze aktywne", Toast.LENGTH_SHORT).show()
                    } else {
                        startChallengeToUser(userId)
                        challengeWithNewBook()
                    }
                }
            } ?: run {
                Log.e("BookStatus", "Brak zalogowanego użytkownika.")
            }
        }

        binding.chooseBookInfoBtn.setOnClickListener {
            displayChoosenBookDialog()
        }

        binding.randomBookChallengeImageButton.setOnClickListener {
            displayRandomBookDialog()
        }

        binding.newBookInfoBtn.setOnClickListener {
            displayNewBookDialog()
        }

        binding.checkNewBook.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            userId?.let { uid ->
                getChallengeEasyBookId(uid,
                    onSuccess = { challengeId ->
                        Log.d("BookStatus", "Przekazane challengeId: $challengeId")
                        checkIfNewBookPagesRead(challengeId)
                    },
                    onFailure = {
                        Toast.makeText(this@ChallengeActivity, "Nie masz żadnego rozpoczętego wyzwania", Toast.LENGTH_SHORT).show()
                    }
                )
            } ?: run {
                Log.e("BookStatus", "Brak zalogowanego użytkownika.")
            }
        }

        binding.checkButton2.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            userId?.let { uid ->
                getTenPageId(uid) { challengeId ->
                    if (challengeId.isNullOrEmpty()) {
                        Toast.makeText(this@ChallengeActivity, "Nie masz żadnego rozpoczętego wyzwania", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("BookStatus", "Przekazane challengeId: $challengeId")
                        checkIfPagesRead(challengeId)
                    }
                }
            } ?: run {
                Log.e("BookStatus", "Brak zalogowanego użytkownika.")
            }
        }

        binding.checkButton.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            userId?.let { uid ->
                getChallengeId(uid) { challengeId ->
                    if (challengeId.isNullOrEmpty()) {
                        Toast.makeText(this@ChallengeActivity, "Nie masz żadnego rozpoczętego wyzwania", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("BookStatus", "Przekazane challengeId: $challengeId")
                        checkIfChallengeComplete(challengeId)
                    }
                }
            } ?: run {
                Log.e("BookStatus", "Brak zalogowanego użytkownika.")
            }
        }

        getPointsFromFirebase()

        displayRandomBookInfo()
        displayChoosenBookInfo()
        displayNewBookInfo()

        val userPoints = 0 // Pobierz aktualną liczbę punktów użytkownika z odpowiedniego źródła danych
        updateProgressBar(userPoints)

    }

    private fun updateProgressBar(points: Int) {
        val progressBar = findViewById<ProgressBar>(R.id.pointsProgressBar)
        val levelIcon = findViewById<ImageView>(R.id.levelIcon) // ImageView dla ikonki poziomu
        val pointsText = findViewById<TextView>(R.id.pointsTextView) // TextView dla wyświetlenia liczby punktów

        val maxPointsForLevel1 = 500
        val maxPointsForLevel2 = 1500
        val maxPointsForLevel3 = 3000
        val maxPointsForLevel4 = 6000

        // Sprawdź, który poziom użytkownika osiągnął na podstawie liczby punktów
        val level = when {
            points < maxPointsForLevel1 -> 1
            points < maxPointsForLevel2 -> 2
            points < maxPointsForLevel3 -> 3
            else -> 4 // Dodaj więcej poziomów według potrzeb
        }

        val progressBarColor = when (level) {
            1 -> R.color.brown // Kolor brązowy na pierwszym poziomie
            2 -> R.color.grey // Kolor srebrny na drugim poziomie
            3 -> R.color.yellow // Kolor złoty na trzecim poziomie
            else -> R.color.blue // Kolor niebieski na czwartym poziomie
        }

        progressBar.progressDrawable.setColorFilter(
            ContextCompat.getColor(this, progressBarColor),
            PorterDuff.Mode.SRC_IN
        )

        // Oblicz liczbę punktów potrzebnych do obecnie osiągniętego poziomu
        val pointsToCurrentLevel = when (level) {
            1 -> 0
            2 -> maxPointsForLevel1
            3 -> maxPointsForLevel2
            else -> maxPointsForLevel3
        }

        // Oblicz liczbę punktów potrzebnych do następnego poziomu
        val pointsToNextLevel = when (level) {
            1 -> maxPointsForLevel1
            2 -> maxPointsForLevel2
            3 -> maxPointsForLevel3
            else -> maxPointsForLevel4
        }

        // Oblicz postęp w aktualnym poziomie
        val currentLevelProgress = points - pointsToCurrentLevel

        // Ustaw wartość postępu paska
        progressBar.max = pointsToNextLevel - pointsToCurrentLevel
        progressBar.progress = currentLevelProgress

        // Ustaw ikonkę w zależności od poziomu
        val iconResource = when (level) {
            1 -> R.drawable.ic_1_lvl
            2 -> R.drawable.ic_lvl_2
            3 -> R.drawable.ic_lvl_3
            else -> R.drawable.ic_lvl_4 // Dodaj więcej ikon według potrzeb
        }
        levelIcon.setImageResource(iconResource)

        // Wyświetl punkty w TextView
        val pointsDisplay = "$points / $pointsToNextLevel"
        pointsText.text = pointsDisplay
    }





    fun getPointsFromFirebase() {
        val database = FirebaseDatabase.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        val userStatsRef = database.getReference("Users").child(userId!!).child("statsDetails")

        userStatsRef.child("points").get().addOnSuccessListener { snapshot ->
            val points = snapshot.getValue(Int::class.java) ?: 0
            // Po pobraniu punktów z bazy danych, aktualizujemy pasek postępu
            updateProgressBar(points)
        }.addOnFailureListener { e ->
            // Obsługa błędu, jeśli nie uda się pobrać punktów z bazy danych
        }
    }


    fun increasePoints(increaseAmount: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val database = FirebaseDatabase.getInstance()
            val userStatsRef = database.getReference("Users").child(uid).child("statsDetails")

            userStatsRef.child("points").get().addOnSuccessListener { snapshot ->
                val currentPoints = snapshot.value as? Long ?: 0
                val newPoints = currentPoints + increaseAmount

                // Sprawdź, czy nowa liczba punktów nie przekracza 6000
                val updatedPoints = if (newPoints > 6000) 6000 else newPoints

                userStatsRef.child("points").setValue(updatedPoints)
                    .addOnSuccessListener {
                        // Sukces - punkty zostały zaktualizowane i zapisane do bazy danych
                        // Tutaj możesz wykonać dodatkowe operacje, jeśli są wymagane
                    }
                    .addOnFailureListener { e ->
                        // Obsługa błędu
                    }
            }.addOnFailureListener { e ->
                // Obsługa błędu
            }
        }
    }


    fun decreasePoints(decreaseAmount: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val database = FirebaseDatabase.getInstance()
            val userStatsRef = database.getReference("Users").child(uid).child("statsDetails")

            userStatsRef.child("points").get().addOnSuccessListener { snapshot ->
                val currentPoints = snapshot.value as? Long ?: 0
                val newPoints = (currentPoints - decreaseAmount).coerceAtLeast(0)

                userStatsRef.child("points").setValue(newPoints)
                    .addOnSuccessListener {
                        // Sukces - punkty zostały zaktualizowane i zapisane do bazy danych
                        // Tutaj możesz wykonać dodatkowe operacje, jeśli są wymagane
                    }
                    .addOnFailureListener { e ->
                        // Obsługa błędu, jeśli nie udało się zapisać punktów do bazy danych
                    }
            }.addOnFailureListener { e ->
                // Obsługa błędu, jeśli nie udało się pobrać punktów z bazy danych
            }
        }
    }



    private fun displayChoosenBookDialog() {
        // Inflacja widoku dialogu z pliku XML
        val dialogView = LayoutInflater.from(this).inflate(R.layout.challenge_choosen_book_info, null)

        // Utworzenie obiektu AlertDialog.Builder
        val builder = AlertDialog.Builder(this)
            .setView(dialogView) // Ustawienie widoku dla okna dialogowego
            .setCancelable(false) // Okno dialogowe nie będzie zamykane po kliknięciu poza nim

        // Ustawienie obsługi kliknięcia przycisku "OK"
        builder.setPositiveButton("OK") { dialog, _ ->
            // Tutaj możesz dodać kod, który ma być wykonany po kliknięciu przycisku "OK"
            dialog.dismiss() // Zamknięcie okna dialogowego
        }

        // Utworzenie i wyświetlenie okna dialogowego
        val dialog = builder.create()
        dialog.show()
    }

    private fun displayNewBookDialog() {
        // Inflacja widoku dialogu z pliku XML
        val dialogView = LayoutInflater.from(this).inflate(R.layout.challenge_new_book_info, null)

        // Utworzenie obiektu AlertDialog.Builder
        val builder = AlertDialog.Builder(this)
            .setView(dialogView) // Ustawienie widoku dla okna dialogowego
            .setCancelable(false) // Okno dialogowe nie będzie zamykane po kliknięciu poza nim

        // Ustawienie obsługi kliknięcia przycisku "OK"
        builder.setPositiveButton("OK") { dialog, _ ->
            // Tutaj możesz dodać kod, który ma być wykonany po kliknięciu przycisku "OK"
            dialog.dismiss() // Zamknięcie okna dialogowego
        }

        // Utworzenie i wyświetlenie okna dialogowego
        val dialog = builder.create()
        dialog.show()
    }

    private fun displayRandomBookDialog() {
        // Inflacja widoku dialogu z pliku XML
        val dialogView = LayoutInflater.from(this).inflate(R.layout.challenge_random_book_info, null)

        // Utworzenie obiektu AlertDialog.Builder
        val builder = AlertDialog.Builder(this)
            .setView(dialogView) // Ustawienie widoku dla okna dialogowego
            .setCancelable(false) // Okno dialogowe nie będzie zamykane po kliknięciu poza nim

        // Ustawienie obsługi kliknięcia przycisku "OK"
        builder.setPositiveButton("OK") { dialog, _ ->
            // Tutaj możesz dodać kod, który ma być wykonany po kliknięciu przycisku "OK"
            dialog.dismiss() // Zamknięcie okna dialogowego
        }

        // Utworzenie i wyświetlenie okna dialogowego
        val dialog = builder.create()
        dialog.show()
    }


    private var currentBookId: String? = null

    private fun challengeBook(attempts: Int = 5) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            var bookFound = false
            val booksRef = FirebaseDatabase.getInstance().getReference("Books")

            booksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val allBooks = mutableListOf<String>()

                    dataSnapshot.children.forEach { bookSnapshot ->
                        if (bookSnapshot.child("isBookFullyRead").value == null) {
                            allBooks.add(bookSnapshot.key!!)
                        }
                    }

                    if (allBooks.isNotEmpty()) {
                        val randomIndex = (0 until allBooks.size).random()
                        val randomBookId = allBooks[randomIndex]
                        val userBooksRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("bookDetails").child(randomBookId)
                        userBooksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userBookSnapshot: DataSnapshot) {
                                if (!bookFound && !userBookSnapshot.exists() || userBookSnapshot.child("isBookFullyRead").value == false) {
                                    currentBookId = randomBookId

                                    val author = dataSnapshot.child(randomBookId).child("author").getValue(String::class.java)?.toString()
                                    val title = dataSnapshot.child(randomBookId).child("title").getValue(String::class.java)?.toString()

                                    Log.d("BookDetails", "Autor: $author, Tytuł: $title")
                                    bookFound = true

                                    val message = "Wyzwanie rozpoczęte"
                                    Toast.makeText(this@ChallengeActivity, message, Toast.LENGTH_LONG).show()

                                    startChallengeToUser(uid)
                                    saveChallengeDate(uid, currentBookId!!, title!!, author!!)
                                } else if (userBookSnapshot.child("isBookFullyRead").value == true) {
                                    tryChallenge(attempts - 1)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("BookStatus", "Błąd pobierania danych z bazy danych: ${error.message}")
                            }
                        })
                    } else {
                        Log.e("BookStatus", "Nie ma dostępnych książek, których wartość isBookFullyRead wynosi null.")
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

    private fun challengeWithNewBook() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val userBooksRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("bookDetails")
            val booksRef = FirebaseDatabase.getInstance().getReference("Books")

            booksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val allBooks = mutableListOf<String>()
                    val userBooks = mutableListOf<String>()

                    dataSnapshot.children.forEach { bookSnapshot ->
                        val bookId = bookSnapshot.key
                        bookId?.let {
                            allBooks.add(it)
                        }
                    }

                    userBooksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userBooksSnapshot: DataSnapshot) {
                            userBooksSnapshot.children.forEach { userBookSnapshot ->
                                val bookId = userBookSnapshot.key
                                bookId?.let {
                                    userBooks.add(it)
                                }
                            }

                            // Usuń książki, które są już w bookDetails z listy wszystkich książek
                            allBooks.removeAll(userBooks)

                            if (allBooks.isNotEmpty()) {
                                // Losuj jedną z dostępnych książek
                                val randomIndex = (0 until allBooks.size).random()
                                val randomBookId = allBooks[randomIndex]

                                // Pobierz tytuł i autora książki
                                val bookTitle = dataSnapshot.child(randomBookId).child("title").getValue(String::class.java)
                                val bookAuthor = dataSnapshot.child(randomBookId).child("author").getValue(String::class.java)

                                if (bookTitle != null && bookAuthor != null) {
                                    // Dodaj książkę do wyzwania
                                    val challengePage = 15
                                    val challengeData: MutableMap<String, Any> = HashMap()
                                    challengeData["challengePage"] = challengePage
                                    challengeData["bookId"] = randomBookId
                                    challengeData["bookTitle"] = bookTitle // Zapisz tytuł książki
                                    challengeData["bookAuthor"] = bookAuthor // Zapisz autora książki

                                    val challengeEasyRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("challengeDetails").child("ChallengeEasy")

                                    challengeEasyRef.setValue(challengeData)
                                        .addOnSuccessListener {
                                            Log.d("ChallengeStatus", "Utworzono węzeł ChallengeEasy i zapisano dane.")
                                            displayNewBookInfo()
                                            Toast.makeText(applicationContext, "Wyzwanie rozpoczęte", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("ChallengeStatus", "Błąd podczas tworzenia węzła ChallengeEasy: ${e.message}")
                                        }
                                } else {
                                    Log.e("BookStatus", "Nie znaleziono tytułu lub autora książki.")
                                }
                            } else {
                                Log.e("BookStatus", "Nie ma dostępnych książek, której jeszcze nie zostały wylosowane.")
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("BookStatus", "Błąd pobierania danych z bazy danych: ${databaseError.message}")
                        }
                    })
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("BookStatus", "Błąd pobierania danych z bazy danych: ${databaseError.message}")
                }
            })
        } ?: run {
            Log.e("ChallengeStatus", "Brak zalogowanego użytkownika.")
        }
    }





    private fun challengeWithTenPages(bookId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val userBookRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

            userBookRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val currentPage = dataSnapshot.child("bookDetails").child(bookId).child("currentPage").getValue(Int::class.java) ?: 0
                    val currentTimeMillis = System.currentTimeMillis()
                    val challengeTimeMillis = currentTimeMillis + ( 90 * 60 * 1000)

                    val challengePage = currentPage + 30

                    val challengeData: MutableMap<String, Any> = HashMap()
                    challengeData["challengePage"] = challengePage
                    challengeData["bookId"] = bookId
                    challengeData["challengeDate"] = challengeTimeMillis

                    // Pobierz informacje o autorze i tytule książki z węzła "books"
                    val booksRef = FirebaseDatabase.getInstance().getReference("Books").child(bookId)
                    booksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val bookTitle = snapshot.child("title").getValue(String::class.java) ?: ""
                            val bookAuthor = snapshot.child("author").getValue(String::class.java) ?: ""

                            // Dodaj informacje o autorze i tytule książki do danych wyzwania
                            challengeData["bookTitle"] = bookTitle
                            challengeData["bookAuthor"] = bookAuthor

                            // Zapisz dane wyzwania w bazie danych
                            val challengeMediumRef = userBookRef.child("challengeDetails").child("ChallengeMedium")
                            challengeMediumRef.setValue(challengeData)
                                .addOnSuccessListener {
                                    Log.d("ChallengeStatus", "Utworzono węzeł ChallengeMedium i zapisano dane.")
                                    Toast.makeText(applicationContext, "Wyzwanie rozpoczęte", Toast.LENGTH_SHORT).show()
                                    displayChoosenBookInfo()
                                    getPointsFromFirebase()
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ChallengeStatus", "Błąd podczas tworzenia węzła ChallengeMedium: ${e.message}")
                                }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("ChallengeStatus", "Błąd pobierania danych z węzła books: ${error.message}")
                        }
                    })
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("ChallengeStatus", "Błąd pobierania danych z bazy danych: ${databaseError.message}")
                }
            })
        } ?: run {
            Log.e("ChallengeStatus", "Brak zalogowanego użytkownika.")
        }
    }




    private fun tryChallenge(attemptsLeft: Int) {
        if (attemptsLeft > 0) {
            challengeBook(attemptsLeft)
            Log.d("ChallengeStatus", "Wywołano tryChallenge. Pozostałe próby: $attemptsLeft")
        } else {
            Toast.makeText(this@ChallengeActivity, "Nie masz żadnej nieprzeczytanej książki", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addCompletedChallengeToUser(userId: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        userRef.child("statsDetails").child("completedChallenges").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val completedChallenges = dataSnapshot.getValue(Int::class.java) ?: 0
                val newCompletedChallenges = completedChallenges + 1

                userRef.child("statsDetails").child("completedChallenges").setValue(newCompletedChallenges)
                    .addOnSuccessListener {
                        Log.d("CompletedChallenges", "Pomyślnie dodano kolejne zakończone wyzwanie do pola completedChallenges.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("CompletedChallenges", "Błąd podczas dodawania kolejnego zakończonego wyzwania: ${e.message}")
                    }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("CompletedChallenges", "Błąd pobierania danych z pola completedChallenges: ${databaseError.message}")
            }
        })
    }

    private fun startChallengeToUser(userId: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        userRef.child("statsDetails").child("startedChallenges").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val startedChallenges = dataSnapshot.getValue(Int::class.java) ?: 0
                val newStartedChallenges = startedChallenges + 1

                userRef.child("statsDetails").child("startedChallenges").setValue(newStartedChallenges)
                    .addOnSuccessListener {
                        displayRandomBookInfo()
                        Log.d("StartedChallenges", "Pomyślnie dodano kolejne rozpoczęte wyzwanie do pola startedChallenges.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("StartedChallenges", "Błąd podczas dodawania kolejnego rozpoczętego wyzwania: ${e.message}")
                    }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("StartedChallenges", "Błąd pobierania danych z pola startedChallenges: ${databaseError.message}")
            }
        })
    }

    private fun getChallengeId(uid: String, callback: (String?) -> Unit) {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
        val challengeHardRef = userRef.child("challengeDetails").child("ChallengeHard")

        challengeHardRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val challengeId = dataSnapshot.child("bookId").getValue(String::class.java)
                callback(challengeId)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Błąd pobierania danych: ${databaseError.message}")
                callback(null)
            }
        })
    }

    private fun getTenPageId(uid: String, callback: (String?) -> Unit) {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
        val challengeMediumRef = userRef.child("challengeDetails").child("ChallengeMedium")

        challengeMediumRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val bookId = dataSnapshot.child("bookId").getValue(String::class.java)
                callback(bookId)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Błąd pobierania danych: ${databaseError.message}")
                callback(null)
            }
        })
    }

    private fun getChallengeEasyBookId(userId: String, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        val challengeEasyRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("challengeDetails").child("ChallengeEasy")

        challengeEasyRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(challengeDataSnapshot: DataSnapshot) {
                val bookId = challengeDataSnapshot.child("bookId").getValue(String::class.java)
                if (!bookId.isNullOrEmpty()) {
                    onSuccess.invoke(bookId)
                } else {
                    onFailure.invoke()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Błąd pobierania danych: ${databaseError.message}")
                onFailure.invoke()
            }
        })
    }



    private fun saveChallengeDate(userId: String, bookId: String, bookTitle: String, bookAuthor: String) {
        val currentTimeMillis = System.currentTimeMillis()
        val challengeTimeMillis = currentTimeMillis + (21 * 24 * 60 * 60 * 1000)

        val challengeDateRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("challengeDetails").child("ChallengeHard")

        val challengeDetails = HashMap<String, Any>()
        challengeDetails["challengeDate"] = challengeTimeMillis
        challengeDetails["bookId"] = bookId
        challengeDetails["bookTitle"] = bookTitle
        challengeDetails["bookAuthor"] = bookAuthor

        challengeDateRef.setValue(challengeDetails)
            .addOnSuccessListener {
                Log.d("ChallengeDate", "Pomyślnie zapisano nową datę wyzwania w bazie danych.")
                getPointsFromFirebase()
            }
            .addOnFailureListener { e ->
                Log.e("ChallengeDate", "Błąd podczas zapisywania nowej daty wyzwania: ${e.message}")
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

    private fun displayNewBookInfo() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val challengeDetailsRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("challengeDetails").child("ChallengeEasy")

            challengeDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val bookTitle = dataSnapshot.child("bookTitle").getValue(String::class.java)
                        val bookAuthor = dataSnapshot.child("bookAuthor").getValue(String::class.java)

                        if (bookTitle != null && bookAuthor != null) {
                            // Wyświetlenie informacji na ekranie
                            binding.newBookTitle.text = bookTitle
                            binding.newBookAuthor.text = bookAuthor
                        }
                    } else {
                        // Obsługa przypadku, gdy nie ma aktywnego wyzwania
                        binding.newBookAuthor.text = ""
                        binding.newBookTitle.text = ""
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Błąd pobierania danych: ${databaseError.message}")
                }
            })
        }
    }


    private fun displayChoosenBookInfo() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val challengeDetailsRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("challengeDetails").child("ChallengeMedium")

            challengeDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val challengeDate = dataSnapshot.child("challengeDate").getValue(Long::class.java)
                        val bookTitle = dataSnapshot.child("bookTitle").getValue(String::class.java)
                        val bookAuthor = dataSnapshot.child("bookAuthor").getValue(String::class.java)

                        if (challengeDate != null && bookTitle != null && bookAuthor != null) {
                            val formattedDate = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date(challengeDate)) // Dodanie jednej godziny w milisekundach



                            // Wyświetlenie informacji na ekranie
                            binding.challengeDate.text = formattedDate
                            binding.bookTitle.text = bookTitle
                            binding.bookAuthor.text = bookAuthor
                        }
                    } else {
                        // Obsługa przypadku, gdy nie ma aktywnego wyzwania
                        binding.challengeDate.text = "Brak aktywnego wyzwania"
                        binding.bookAuthor.text = ""
                        binding.bookTitle.text = ""
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Błąd pobierania danych: ${databaseError.message}")
                }
            })
        }
    }


    private fun displayRandomBookInfo() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val challengeDetailsRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("challengeDetails").child("ChallengeHard")

            challengeDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val challengeDate = dataSnapshot.child("challengeDate").getValue(Long::class.java)
                        val bookTitle = dataSnapshot.child("bookTitle").getValue(String::class.java)
                        val bookAuthor = dataSnapshot.child("bookAuthor").getValue(String::class.java)

                        if (challengeDate != null && bookTitle != null && bookAuthor != null) {
                            val formattedDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(challengeDate))

                            // Wyświetlenie informacji na ekranie
                            binding.randomChallengeDate.text = formattedDate
                            binding.randomBookTitle.text = bookTitle
                            binding.randomBookAuthor.text = bookAuthor
                        }
                    } else {
                        // Obsługa przypadku, gdy nie ma aktywnego wyzwania
                        binding.randomChallengeDate.text = "Brak aktywnego wyzwania"
                        binding.randomBookAuthor.text = ""
                        binding.randomBookTitle.text = ""
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Błąd pobierania danych: ${databaseError.message}")
                }
            })
        }
    }




    private fun checkTime(uid: String, callback: (Boolean) -> Unit) {
        saveCurrentDateToDatabase(uid)

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val challengeSnapshot = dataSnapshot.child("challengeDetails").child("ChallengeHard")
                val challengeDate = challengeSnapshot.child("challengeDate").getValue(Long::class.java)
                val currentDate = dataSnapshot.child("currentDate").getValue(Long::class.java)

                var isChallengeCompleted = false

                if (challengeDate != null && currentDate != null) {
                    isChallengeCompleted = currentDate < challengeDate
                }

                callback(isChallengeCompleted)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Błąd pobierania danych ${databaseError.message}")
                callback(false)
            }
        })
    }

    private fun checkTimeTenPagesChallenge(uid: String, callback: (Boolean) -> Unit) {
        saveCurrentDateToDatabase(uid)

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val challengeSnapshot = dataSnapshot.child("challengeDetails").child("ChallengeMedium")
                val challengeDate = challengeSnapshot.child("challengeDate").getValue(Long::class.java)
                val currentDate = dataSnapshot.child("currentDate").getValue(Long::class.java)

                // Log aktualnej daty
                Log.d("ChallengeDate", "Aktualna data: ${currentDate}")

                var isChallengeCompleted = false

                if (challengeDate != null && currentDate != null) {
                    isChallengeCompleted = currentDate > challengeDate
                }

                // Log daty wyzwania i czy wyzwanie zostało ukończone
                Log.d("ChallengeDate", "Data wyzwania: ${challengeDate}")
                Log.d("ChallengeStatus", "Wyzwanie ukończone: ${isChallengeCompleted}")

                callback(isChallengeCompleted)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Błąd pobierania danych ${databaseError.message}")
                callback(false)
            }
        })
    }



    private fun checkIfPagesRead(bookId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let { uid ->
            val userBookRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("bookDetails").child(bookId)
            userBookRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val currentPage = dataSnapshot.child("currentPage").getValue(Int::class.java) ?: 0
                    val isBookFullyRead = dataSnapshot.child("isBookFullyRead").getValue(Boolean::class.java) ?: false
                    Log.d("CheckPages", "Current Page: $currentPage")
                    Log.d("CheckPages", "Is Book Fully Read: $isBookFullyRead")

                    val challengeDetailsRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("challengeDetails").child("ChallengeMedium")
                    challengeDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(challengeDataSnapshot: DataSnapshot) {
                            val challengePages = challengeDataSnapshot.child("challengePage").getValue(Int::class.java) ?: 0
                            Log.d("CheckPages", "Challenge Pages: $challengePages")

                            val isPagesRead = currentPage >= challengePages || isBookFullyRead

                            // Sprawdzenie, czy czas wyzwania nie minął
                            checkTimeTenPagesChallenge(uid) { isChallengeTimeUp ->
                                // Usuń węzeł ChallengeMedium, jeśli wyzwanie jest ukończone i czas nie minął
                                if (isPagesRead && !isChallengeTimeUp) {
                                    increasePoints(50)
                                    challengeDetailsRef.removeValue().addOnSuccessListener {
                                        Log.d("Firebase", "Węzeł ChallengeMedium został pomyślnie usunięty po ukończeniu wyzwania.")
                                        Toast.makeText(FirebaseApp.getInstance().applicationContext, "Wyzwanie ukończone, gratulacje", Toast.LENGTH_SHORT).show()
                                        getPointsFromFirebase()
                                        displayChoosenBookInfo()
                                        addCompletedChallengeToUser(userId)
                                    }.addOnFailureListener { e ->
                                        Log.e("Firebase", "Błąd podczas usuwania węzła ChallengeMedium: ${e.message}")
                                    }
                                } else if (!isPagesRead && !isChallengeTimeUp) {
                                    // Wyświetlanie informacji, że wyzwanie nadal trwa
                                    Toast.makeText(FirebaseApp.getInstance().applicationContext, "Wyzwanie nadal trwa, czytaj dalej", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Usuń węzeł ChallengeMedium, jeśli czas wyzwania minął
                                    decreasePoints(25)
                                    challengeDetailsRef.removeValue().addOnSuccessListener {
                                        Log.d("Firebase", "Węzeł ChallengeMedium został pomyślnie usunięty po upływie czasu.")
                                        Toast.makeText(FirebaseApp.getInstance().applicationContext, "Czas wyzwania minął", Toast.LENGTH_SHORT).show()
                                        getPointsFromFirebase()
                                        displayChoosenBookInfo()
                                    }.addOnFailureListener { e ->
                                        Log.e("Firebase", "Błąd podczas usuwania węzła ChallengeMedium: ${e.message}")
                                    }
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("Firebase", "Błąd pobierania danych: ${databaseError.message}")
                        }
                    })
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Błąd pobierania danych: ${databaseError.message}")
                }
            })
        } ?: run {
            Log.e("Firebase", "Brak zalogowanego użytkownika.")
        }
    }

    private fun checkIfNewBookPagesRead(bookId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let { uid ->
            val challengeMediumRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("challengeDetails").child("ChallengeEasy")

            challengeMediumRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(challengeDataSnapshot: DataSnapshot) {
                    // Sprawdź, czy podany bookId jest zgodny z bookId w ChallengeEasy
                    val challengeBookId = challengeDataSnapshot.child("bookId").getValue(String::class.java)
                    if (challengeBookId == bookId) {
                        // Pobierz challengePages z węzła ChallengeEasy
                        val challengePages = challengeDataSnapshot.child("challengePage").getValue(Int::class.java) ?: 0

                        // Sprawdź, czy istnieje węzeł bookDetails dla danego bookId
                        val bookDetailsRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("bookDetails").child(bookId)
                        bookDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(bookDataSnapshot: DataSnapshot) {
                                // Pobierz currentPage z węzła bookDetails
                                val currentPage = bookDataSnapshot.child("currentPage").getValue(Int::class.java) ?: 0

                                // Sprawdź, czy challengePages jest większe lub równe currentPage
                                val isChallengePagesReached = currentPage >= challengePages

                                if (isChallengePagesReached) {
                                    increasePoints(25)
                                    startCooldown()
                                    addCompletedChallengeToUser(userId)
                                    Toast.makeText(applicationContext, "Wyzwanie ukończone, gratulacje", Toast.LENGTH_SHORT).show()

                                    // Usuń węzeł ChallengeEasy, jeśli wyzwanie zostało ukończone
                                    challengeMediumRef.removeValue().addOnSuccessListener {
                                        getPointsFromFirebase()
                                        displayNewBookInfo()
                                        Log.d("Firebase", "Węzeł ChallengeEasy został pomyślnie usunięty po ukończeniu wyzwania.")
                                    }.addOnFailureListener { e ->
                                        Log.e("Firebase", "Błąd podczas usuwania węzła ChallengeEasy: ${e.message}")
                                    }
                                } else {
                                    Log.d("Firebase", "ChallengePages nie zostały jeszcze przeczytane.")
                                    Toast.makeText(applicationContext, "Wyzwanie nie zostało jeszcze ukończone", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.e("Firebase", "Błąd pobierania danych z węzła bookDetails: ${databaseError.message}")
                            }
                        })
                    } else {
                        Log.e("Firebase", "Podany bookId nie zgadza się z bookId w wyzwaniu.")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Błąd pobierania danych: ${databaseError.message}")
                    Toast.makeText(applicationContext, "Błąd pobierania danych: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } ?: run {
            Log.e("Firebase", "Brak zalogowanego użytkownika.")
            Toast.makeText(applicationContext, "Brak zalogowanego użytkownika.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun startCooldown() {
        val cooldownEndTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val cooldownRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("cooldownEndTime")
            cooldownRef.setValue(cooldownEndTime)
                .addOnSuccessListener {
                    Log.d("Firebase", "Czas zakończenia cooldownu został pomyślnie zapisany w bazie danych.")
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Błąd podczas zapisywania czasu zakończenia cooldownu: ${e.message}")
                }
        }
    }

    private fun isCooldownActive(onCooldownCheck: (Boolean) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val cooldownRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("cooldownEndTime")
            cooldownRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val cooldownEndTime = dataSnapshot.getValue(Long::class.java)
                    if (cooldownEndTime != null) {
                        val currentTimeMillis = System.currentTimeMillis()
                        val isCooldownActive = currentTimeMillis < cooldownEndTime
                        onCooldownCheck(isCooldownActive)
                    } else {
                        onCooldownCheck(false)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Błąd pobierania danych: ${databaseError.message}")
                    onCooldownCheck(false)
                }
            })
        } ?: run {
            Log.e("Firebase", "Brak zalogowanego użytkownika.")
            onCooldownCheck(false)
        }
    }




    private fun showBookSelectionDialog() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val userBooksRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("bookDetails")
            val selectedBookTitles = ArrayList<String>()
            val selectedBookIds = ArrayList<String>()

            userBooksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userBooksSnapshot: DataSnapshot) {
                    userBooksSnapshot.children.forEach { bookSnapshot ->
                        val bookId = bookSnapshot.key
                        val isBookFullyRead = bookSnapshot.child("isBookFullyRead").getValue(Boolean::class.java) ?: false
                        if (!isBookFullyRead && bookId != null) {
                            // Pobranie tytułu z węzła książki w węźle Books
                            val booksRef = FirebaseDatabase.getInstance().getReference("Books").child(bookId)
                            booksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(bookSnapshot: DataSnapshot) {
                                    val title = bookSnapshot.child("title").getValue(String::class.java)
                                    title?.let {
                                        selectedBookTitles.add(it)
                                        selectedBookIds.add(bookId)
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    Log.e("Firebase", "Błąd pobierania danych z węzła Books: ${databaseError.message}")
                                }
                            })
                        }
                    }

                    // Tworzenie dialogu z wyborem książki po zakończeniu przetwarzania wszystkich książek
                    userBooksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userBooksSnapshot: DataSnapshot) {
                            val selectedBookTitlesArray = selectedBookTitles.toTypedArray()
                            val selectedBookIdsArray = selectedBookIds.toTypedArray()

                            // Tworzenie dialogu z wyborem książki
                            val builder = AlertDialog.Builder(this@ChallengeActivity)
                            builder.setTitle("Wybierz książkę")
                            builder.setItems(selectedBookTitlesArray) { dialog, which ->
                                val selectedBookId = selectedBookIdsArray[which]
                                challengeWithTenPages(selectedBookId) // Wywołanie funkcji challengeWithTenPages z wybranym id książki
                                dialog.dismiss()
                            }
                            builder.show()
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("Firebase", "Błąd pobierania danych z węzła bookDetails: ${databaseError.message}")
                        }
                    })
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Błąd pobierania danych z węzła bookDetails: ${databaseError.message}")
                }
            })
        } ?: run {
            Log.e("Firebase", "Brak zalogowanego użytkownika.")
        }
    }



    private fun checkIfChallengeComplete(bookId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let { uid ->
            val userBookRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("bookDetails").child(bookId)

            checkTime(uid) { isChallengeCompleted ->
                userBookRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val isBookFullyRead = dataSnapshot.child("isBookFullyRead").getValue(Boolean::class.java)

                        if (isBookFullyRead != null) {
                            Log.d("BookStatus", "Wartość isBookFullyRead: $isBookFullyRead")
                            if (isBookFullyRead && isChallengeCompleted) {
                                increasePoints(500)
                                Toast.makeText(this@ChallengeActivity, "Wyzwanie ukończone, gratulację", Toast.LENGTH_SHORT).show()
                                addCompletedChallengeToUser(userId)
                                val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
                                userRef.child("challengeDetails").child("ChallengeHard").removeValue()
                                    .addOnSuccessListener {
                                        getPointsFromFirebase()
                                        displayRandomBookInfo()
                                        Log.d("ChallengeDetails", "Węzeł challengeDetails został pomyślnie usunięty po przeczytaniu książki.")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ChallengeDetails", "Błąd podczas usuwania węzła challengeDetails: ${e.message}")
                                    }
                            } else if (isChallengeCompleted) {
                                Log.d("BookStatus", "Książka nadal nie jest przeczytana, czytaj dalej")
                                Toast.makeText(this@ChallengeActivity, "Książka nadal nie jest przeczytana, czytaj dalej", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.d("BookStatus", "Wyzwanie niezaliczone, czas upłynął")
                                Toast.makeText(this@ChallengeActivity, "Wyzwanie niezaliczone, czas upłynął", Toast.LENGTH_SHORT).show()
                                decreasePoints(250)
                                val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
                                userRef.child("challengeDetails").child("ChallengeHard").removeValue()
                                    .addOnSuccessListener {
                                        getPointsFromFirebase()
                                        displayRandomBookInfo()
                                    }
                            }
                        } else {
                            Toast.makeText(this@ChallengeActivity, "Książka nie została jeszcze rozpoczęta", Toast.LENGTH_SHORT).show()
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

}