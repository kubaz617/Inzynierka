package com.example.bookreader.activities

import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.bookreader.databinding.ActivityStatsBinding
import com.example.bookreader.utils.MyApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        getTotalPagesRead()
        getTotalBooksRead()
        getCompletedChallenges()
        getStartedChallenges()
        getCompletedTrophies()

        val selectedBackground = getSelectedBackground()
        window.setBackgroundDrawableResource(selectedBackground)
    }

    private fun checkUser()  {
        val firebaseUser = firebaseAuth.currentUser
        val email = firebaseUser!!.email
        binding.titleTv.text = email

    }

    private fun getSelectedBackground(): Int {
        return MyApplication.getSelectedBackground(this)
    }

    private fun getTotalPagesRead() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("bookDetails")

            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var totalPageCount = 0

                    for (bookSnapshot in dataSnapshot.children) {
                        val furthestPageRead = bookSnapshot.child("furthestPageRead").getValue(Int::class.java) ?: 0
                        totalPageCount += furthestPageRead
                    }

                    binding.totalPagesTv.text = "Łączna ilość przeczytanych stron: $totalPageCount"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.d(TAG, "getTotalPagesRead: Nie udało się pobrać łącznej sumy stron: ${databaseError.message}")
                }
            })
        } else {
            Log.d(TAG, "getTotalPagesRead: Użytkownik nie zalogowany")
        }
    }

    private fun getTotalBooksRead() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("bookDetails")

            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var totalBooksRead = 0

                    for (bookSnapshot in dataSnapshot.children) {
                        val isBookFullyRead = bookSnapshot.child("isBookFullyRead").getValue(Boolean::class.java) ?: false
                        if (isBookFullyRead) {
                            totalBooksRead++
                        }
                    }

                    binding.totalBooksTv.text = "Łączna ilość przeczytanych książek: $totalBooksRead"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.d(TAG, "getTotalBooksRead: Nie udało się pobrać łącznej ilości ksiązek: ${databaseError.message}")
                }
            })
        } else {
            Log.d(TAG, "getTotalBooksRead: Użytkownik nie zalogowany")
        }
    }


    private fun getCompletedChallenges() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("statsDetails")

            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val completedChallenges = dataSnapshot.child("completedChallenges").getValue(Int::class.java) ?: 0

                    binding.completedChallengesTv.text = "Łączna ilość wykonanych wyzwań: $completedChallenges"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.d(TAG, "getCompletedChallenges: Nie udało się pobrać łączej ilości wykonanych wyzwań ${databaseError.message}")
                }
            })
        } else {
            Log.d(TAG, "getCompletedChallenges: Użytkownik nie zalogowany")
        }
    }

    private fun getStartedChallenges() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("statsDetails")

            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val startedChallenges = dataSnapshot.child("startedChallenges").getValue(Int::class.java) ?: 0

                    binding.startedChallengesTv.text = "Łączna ilość rozpoczętych wyzwań: $startedChallenges"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.d(TAG, "getStartedChallenges: Nie udało się pobrać łącznej ilości rozpoczętych wyzwań ${databaseError.message}")
                }
            })
        } else {
            Log.d(TAG, "getStartedChallenges: Użytkownik nie zalogowany")
        }
    }

    private fun getCompletedTrophies() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("statsDetails")

            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val platinumTrophies = dataSnapshot.child("platinumTrophy").getValue(Int::class.java) ?: 0
                    val goldTrophies = dataSnapshot.child("goldTrophy").getValue(Int::class.java) ?: 0
                    val silverTrophies = dataSnapshot.child("silverTrophy").getValue(Int::class.java) ?: 0
                    val bronzeTrophies = dataSnapshot.child("bronzeTrophy").getValue(Int::class.java) ?: 0

                     binding.allPlatinumTrophiesTv.text = "Łączna ilość trofeów platynowych: $platinumTrophies"
                     binding.allGoldTrophiesTv.text = "Łączna ilość trofeów złotych: $goldTrophies"
                     binding.allSilverTrophiesTv.text = "Łączna ilość trofeów srebrnych: $silverTrophies"
                     binding.allBronzeTrophiesTv.text = "Łączna ilość trofeów brązowych: $bronzeTrophies"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.d(TAG, "getCompletedTrophies: Nie udało się pobrać łącznej ilości trofeów ${databaseError.message}")
                }
            })
        } else {
            Log.d(TAG, "getCompletedTrophies: Użytkownik nie zalogowany")
        }
    }

}