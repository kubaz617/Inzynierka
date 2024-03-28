package com.example.bookreader.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import com.example.bookreader.models.ModelCategory
import com.example.bookreader.R
import com.example.bookreader.adapters.AdapterCategory
import com.example.bookreader.databinding.ActivityBooksBinding
import com.example.bookreader.utils.MyApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminBooksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBooksBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var categoryArrayList: ArrayList<ModelCategory>

    private lateinit var adapterCategory: AdapterCategory
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val category_btn = findViewById<Button>(R.id.addCategory)
        val book_btn = findViewById<Button>(R.id.add_Book)
        val logout_btn = findViewById<ImageButton>(R.id.logout_btn)
        val theme_btn = findViewById<ImageButton>(R.id.themeBtn)
        firebaseAuth = FirebaseAuth.getInstance()

        val selectedBackground = getSelectedBackground()
        window.setBackgroundDrawableResource(selectedBackground)

        val selectedColor = MyApplication.getSelectedColor(this)
        setAllButtonsColor(selectedColor)

        window.statusBarColor = MyApplication.getStatusBarColor(this)

        checkUser()
        loadCategories()

        binding.searchEt.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    adapterCategory.filter.filter(s)
                }
                catch (e:Exception){

                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        category_btn.setOnClickListener {
            navigateToActivity(AddCategory::class.java)
        }

        book_btn.setOnClickListener {
            navigateToActivity(AddBook::class.java)
        }

        logout_btn.setOnClickListener {
            firebaseAuth.signOut()
            navigateToActivity(SignInActivity::class.java)
            finish()
        }

        theme_btn.setOnClickListener {
            showThemeMenu()
        }


    }

    private fun loadCategories() {
        categoryArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryArrayList.clear()
                for (ds in snapshot.children){
                    val model = ds.getValue(ModelCategory::class.java)

                    categoryArrayList.add(model!!)
                }
                adapterCategory = AdapterCategory(this@AdminBooksActivity,categoryArrayList)
                binding.categoriesRv.adapter = adapterCategory
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
            val email = firebaseUser!!.email
            binding.titleTv.text = email

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
                    // Zmiana tła aplikacji
                    setAppBackground(R.drawable.screen_1)
                    setAllButtonsColor(getColor(R.color.cyan))
                    saveSelectedBackground(R.drawable.screen_1)
                    MyApplication.saveSelectedColor(this, getColor(R.color.cyan))
                    // Zmiana koloru paska stanu
                    window.statusBarColor = getColor(R.color.cyan)
                    // Zapisanie wybranego koloru paska stanu
                    MyApplication.saveStatusBarColor(this, getColor(R.color.cyan))
                    true
                }
                R.id.theme_pink -> {
                    setAppBackground(R.drawable.screen_2)
                    setAllButtonsColor(getColor(R.color.pink))
                    saveSelectedBackground(R.drawable.screen_2)
                    MyApplication.saveSelectedColor(this, getColor(R.color.pink))
                    window.statusBarColor = getColor(R.color.pink) // Zmiana koloru paska stanu
                    MyApplication.saveStatusBarColor(this, getColor(R.color.pink))
                    true
                }
                R.id.theme_green -> {
                    setAppBackground(R.drawable.screen_3)
                    setAllButtonsColor(getColor(R.color.light_yellow))
                    saveSelectedBackground(R.drawable.screen_3)
                    MyApplication.saveSelectedColor(this, getColor(R.color.light_yellow))

                    window.statusBarColor = getColor(R.color.light_yellow) // Zmiana koloru paska stanu
                    MyApplication.saveStatusBarColor(this, getColor(R.color.light_yellow))
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun setAllButtonsColor(color: Int) {
        val rootView = window.decorView.rootView
        MyApplication.setViewBackgroundColor(rootView, color)
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