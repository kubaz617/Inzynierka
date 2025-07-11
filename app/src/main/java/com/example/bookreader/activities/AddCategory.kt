package com.example.bookreader.activities

import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.bookreader.databinding.ActivityAddCategoryBinding
import com.example.bookreader.utils.MyApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddCategory : AppCompatActivity() {

    private lateinit var binding:ActivityAddCategoryBinding

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        firebaseAuth = FirebaseAuth.getInstance()

        binding.submitBtn.setOnClickListener{
            validateData()
        }

        val selectedBackground = getSelectedBackground()
        window.setBackgroundDrawableResource(selectedBackground)

        val selectedColor = MyApplication.getSelectedColor(this)
        setAllButtonsColor(selectedColor)

        window.statusBarColor = MyApplication.getStatusBarColor(this)
    }

    private var category = ""

    private fun validateData() {
        category = binding.categoryEt.text.toString().trim()

        if (category.isEmpty()){
            Toast.makeText(this,"Nazwa nie może być pusta!", Toast.LENGTH_SHORT).show()
        }
        else {
            addCategoryFirebase()
        }
    }

    private fun addCategoryFirebase() {
        val timestamp = System.currentTimeMillis()

        val hashMap = HashMap<String, Any>()
        hashMap ["id"] = "$timestamp"
        hashMap ["category"] = category
        hashMap ["timestamp"] = timestamp
        hashMap ["uid"] = "${firebaseAuth.uid}"

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.child("$timestamp")
            .setValue(hashMap)
            .addOnSuccessListener {
                Toast.makeText(this,"Dodawanie zakończone sukcesem", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener{e ->
                Toast.makeText(this,"Błąd podczas dodawania do bazy danych", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setAllButtonsColor(color: Int) {
        val rootView = window.decorView.rootView
        MyApplication.setViewBackgroundColor(rootView, color)
    }



    private fun getSelectedBackground(): Int {
        return MyApplication.getSelectedBackground(this)
    }
}