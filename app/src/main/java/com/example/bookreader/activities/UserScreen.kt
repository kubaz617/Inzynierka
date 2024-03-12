package com.example.bookreader.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.bookreader.R
import com.example.bookreader.databinding.ActivityUserScreenBinding
import com.google.firebase.auth.FirebaseAuth

class UserScreen : AppCompatActivity() {

    private lateinit var binding: ActivityUserScreenBinding

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val book_Btn = findViewById<Button>(R.id.book_btn)
        val quiz_Btn = findViewById<Button>(R.id.quiz_btn)
        val stats_Btn = findViewById<Button>(R.id.stats_btn)
        val similiar_btn = findViewById<Button>(R.id.similiar_btn)
        val logout_Btn = findViewById<Button>(R.id.logout_btn)

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()



        logout_Btn.setOnClickListener {
            firebaseAuth.signOut()
            navigateToActivity(SignInActivity::class.java)
            finish()
        }

        book_Btn.setOnClickListener {
            navigateToActivity(UserBooksActivity::class.java)
        }

        quiz_Btn.setOnClickListener {
            navigateToActivity(QuizScreen::class.java)
        }



        similiar_btn.setOnClickListener {
            navigateToActivity(SimiliarActivity::class.java)
        }

        stats_Btn.setOnClickListener {
            navigateToActivity(StatsActivity::class.java)
        }
    }


    private fun checkUser()  {
    val firebaseUser = firebaseAuth.currentUser
        val email = firebaseUser!!.email
        binding.titleTv.text = email

}

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }


}