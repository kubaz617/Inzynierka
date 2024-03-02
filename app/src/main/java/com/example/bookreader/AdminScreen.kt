package com.example.bookreader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.bookreader.databinding.ActivityAdminScreenBinding
import com.google.firebase.auth.FirebaseAuth

class AdminScreen : AppCompatActivity() {

    private lateinit var binding: ActivityAdminScreenBinding

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val logout_Btn = findViewById<Button>(R.id.logout_btn)
        val book_Btn = findViewById<Button>(R.id.book_btn)
        val challenge_btn = findViewById<Button>(R.id.challenge_btn)
        val quiz_btn = findViewById<Button>(R.id.quiz_btn)
        val similiar_btn = findViewById<Button>(R.id.similiar_btn)
        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        logout_Btn.setOnClickListener {
            firebaseAuth.signOut()
            navigateToActivity(SignInActivity::class.java)
            finish()
        }

        book_Btn.setOnClickListener {
            navigateToActivity(AdminBooksActivity::class.java)
        }

        challenge_btn.setOnClickListener {
            navigateToActivity(ChallengeActivity::class.java)
        }

        quiz_btn.setOnClickListener {
            navigateToActivity(QuizActivity::class.java)
        }

        similiar_btn.setOnClickListener {
            navigateToActivity(SimiliarActivity::class.java)
        }

    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null){
            startActivity(Intent(this,SignInActivity::class.java))
            finish()
        }
        else {
            val email = firebaseUser.email
            binding.titleTv.text = email
        }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }
}