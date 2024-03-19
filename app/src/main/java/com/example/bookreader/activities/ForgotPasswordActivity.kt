package com.example.bookreader.activities

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.example.bookreader.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Proszę czekać")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.submitBtn.setOnClickListener {
            validateData()
        }
    }

    private var email = ""
    private fun validateData() {

        email = binding.emailEt.text.toString().trim()

        if (email.isEmpty()){
            Toast.makeText(this, "Wprowadź adres e-mail", Toast.LENGTH_SHORT).show()
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this, "Błędny adres e-mail", Toast.LENGTH_SHORT).show()
        }
        else{
            recoverPassword()
        }
    }

    private fun recoverPassword() {

        progressDialog.setMessage("Wysyłanie wiadomości na adres e-mail")
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Instrukcja wysłana na \n$email", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Toast.makeText(this, "Nie udało się wysłać wiadomości z powodu błędu ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}