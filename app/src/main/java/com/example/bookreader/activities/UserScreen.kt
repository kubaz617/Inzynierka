package com.example.bookreader.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
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
        val menu_Btn = findViewById<ImageButton>(R.id.menuBtn)

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

        /*
        menu_Btn.setOnClickListener { view ->
            showPopupMenu(view)
        }
         */
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        val email = firebaseUser!!.email
        binding.titleTv.text = email
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.theme_menu, menu)
        return true
    }


    /*
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.theme_default -> {
                changeTheme(R.style.Theme_Default)
                return true
            }
            R.id.theme_pink -> {
                changeTheme(R.style.Theme_Pink)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.theme_menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.theme_default -> {
                    changeTheme(R.style.Theme_Default)
                    return@setOnMenuItemClickListener true
                }
                R.id.theme_pink -> {
                    changeTheme(R.style.Theme_Pink)
                    return@setOnMenuItemClickListener true
                }
            }
            false
        }
        popupMenu.show()
    }

    private fun changeTheme(themeId: Int) {
        setTheme(themeId)
        recreate()
    }


     */
}
