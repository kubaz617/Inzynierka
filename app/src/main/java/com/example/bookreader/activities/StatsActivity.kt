package com.example.bookreader.activities

import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.bookreader.R
import com.example.bookreader.databinding.ActivityStatsBinding
import com.example.bookreader.databinding.ActivityUserScreenBinding
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
    }

    private fun checkUser()  {
        val firebaseUser = firebaseAuth.currentUser
        val email = firebaseUser!!.email
        binding.titleTv.text = email

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

                    // Wyświetl łączną ilość stron w polu tekstowym z odpowiednim opisem
                    binding.totalPagesTv.text = "Łączna ilość przeczytanych stron: $totalPageCount"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.d(TAG, "getTotalPagesRead: Failed to retrieve total pages: ${databaseError.message}")
                }
            })
        } else {
            Log.d(TAG, "getTotalPagesRead: User not authenticated.")
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
                    Log.d(TAG, "getTotalBooksRead: Failed to retrieve total books: ${databaseError.message}")
                }
            })
        } else {
            Log.d(TAG, "getTotalBooksRead: User not authenticated.")
        }
    }

    private fun getCompletedChallenges() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId)

            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val completedChallenges = dataSnapshot.child("completedChallenges").getValue(Int::class.java) ?: 0

                    binding.completedChallengesTv.text = "Łączna ilość wykonanych wyzwań: $completedChallenges"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.d(TAG, "getStartedChallengesCount: Failed to retrieve started challenges count: ${databaseError.message}")
                }
            })
        } else {
            Log.d(TAG, "getStartedChallengesCount: User not authenticated.")
        }
    }

    private fun getStartedChallenges() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId)

            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val startedChallenges = dataSnapshot.child("startedChallenges").getValue(Int::class.java) ?: 0

                    binding.startedChallengesTv.text = "Łączna ilość rozpoczętych wyzwań: $startedChallenges"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.d(TAG, "getStartedChallengesCount: Failed to retrieve started challenges count: ${databaseError.message}")
                }
            })
        } else {
            Log.d(TAG, "getStartedChallengesCount: User not authenticated.")
        }
    }

}