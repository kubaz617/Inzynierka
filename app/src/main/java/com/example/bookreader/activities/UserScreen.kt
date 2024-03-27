package com.example.bookreader.activities

import android.content.Context
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
import com.example.bookreader.utils.MyApplication
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
        val challenge_btn = findViewById<Button>(R.id.challenge_btn)
        val theme_btn = findViewById<ImageButton>(R.id.themeBtn)

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

        challenge_btn.setOnClickListener {
            navigateToActivity(ChallengeActivity::class.java)
        }

        theme_btn.setOnClickListener {
            showThemeMenu()
        }

        val selectedBackground = getSelectedBackground()

        setAppBackground(selectedBackground)


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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.theme_default -> {
                applyTheme(R.style.Theme_Default)
                return true
            }
            R.id.theme_pink -> {
                applyTheme(R.style.Theme_Pink)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun applyTheme(themeId: Int) {
        setTheme(themeId)
        recreate() // Przeładuj aktywność, aby zastosować nowy motyw
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.theme_menu, menu)
        return true
    }

    private fun showThemeMenu() {
        val popupMenu = PopupMenu(this, binding.themeBtn)
        popupMenu.menuInflater.inflate(R.menu.theme_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.theme_default -> {
                    setAppBackground(R.drawable.screen_1)
                    saveSelectedBackground(R.drawable.screen_1) // Zapisz wybrane tło do SharedPreferences
                    true
                }
                R.id.theme_pink -> {
                    setAppBackground(R.drawable.screen_2)
                    saveSelectedBackground(R.drawable.screen_2) // Zapisz wybrane tło do SharedPreferences
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }


    private fun setAppBackground(backgroundId: Int) {
        window.setBackgroundDrawableResource(backgroundId)
    }

    private fun saveSelectedBackground(backgroundId: Int) {
        MyApplication.saveSelectedBackground(this, backgroundId)
    }

    // Wywołanie metody pobierającej tło w innej aktywności
    private fun getSelectedBackground(): Int {
        return MyApplication.getSelectedBackground(this)
    }

}
