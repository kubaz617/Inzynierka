package com.example.bookreader.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.example.bookreader.models.ModelCategory

import com.example.bookreader.databinding.ActivityAddBookBinding
import com.google.android.gms.tasks.Task

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class AddBook : AppCompatActivity() {

    private lateinit var  binding: ActivityAddBookBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var categoryArrayList: ArrayList<ModelCategory>
    private var pdfUri: Uri? = null
    private val TAG = "PDF_ADD_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        loadPDFCategories()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Proszę czekać")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.categoryTv.setOnClickListener{
            categoryPickDialog()
        }

        binding.uploadPdfBtn.setOnClickListener{
            pdfPickIntent()
        }

        binding.submitBtn.setOnClickListener{

            validateData()
        }
    }
    private var title = ""
    private var description = ""
    private var category = ""
    private var author = ""
    private fun validateData() {
        Log.d(TAG, "validateData: Walidacja danych")

        title = binding.titleEt.text.toString().trim()
        author = binding.authorEt.text.toString().trim()
        description = binding.descriptionEt.text.toString().trim()
        category = binding.categoryTv.text.toString().trim()

        if (title.isEmpty()){
            Toast.makeText(this,"Wprowadź tytuł !", Toast.LENGTH_SHORT).show()
        }
        else if (author.isEmpty()){
            Toast.makeText(this, "Wprowadź autora !", Toast.LENGTH_SHORT).show()
        }
        else if (description.isEmpty()){
            Toast.makeText(this, "Wprowadź opis !", Toast.LENGTH_SHORT).show()
        }
        else if (category.isEmpty()){
            Toast.makeText(this,"Wybierz kategorie",Toast.LENGTH_SHORT).show()
        }
        else if (pdfUri == null){
            Toast.makeText(this,"Wybierz książkę",Toast.LENGTH_SHORT).show()
        }
        else{
            uploadPdfToStorage()
        }
    }

    private fun uploadPdfToStorage() {
        Log.d(TAG, "uploadPdfToStorage: Dodawanie do schowka ")

        progressDialog.setMessage("Dodawanie książki...")
        progressDialog.show()

        val timestamp = System.currentTimeMillis()

        val  filePathAndName = "Books/$timestamp"

        val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
        storageReference.putFile(pdfUri!!)
            .addOnSuccessListener {taskSnapshot ->
                Log.d(TAG, "uploadPdfToStorage: Plik załadowany poprawnie ")

                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadedPdfUrl = "${uriTask.result}"

                uploadPdfInfoToDb(uploadedPdfUrl, timestamp)

            }
            .addOnFailureListener{e->
                Log.d(TAG, "uploadPdfToStorage: Nie udało się załadować na serwer z powodu błędu ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this,"Nie udało się załadować z powodu błędu ${e.message} ",Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadPdfInfoToDb(uploadedPdfUrl: String, timestamp: Long) {
        Log.d(TAG, "uploadPdfInfoToDb: ")
        progressDialog.setMessage("Ładowanie informacji o pliku")

        val uid = firebaseAuth.uid

        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["uid"] = "$uid"
        hashMap["id"] = "$timestamp"
        hashMap["title"] = "$title"
        hashMap["author"] = "$author"
        hashMap["description"] = "$description"
        hashMap["categoryId"] = "$selectedCategoryId"
        hashMap["url"] = "$uploadedPdfUrl"
        hashMap["timestamp"] = timestamp
        hashMap["viewsCount"] = 0

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child("$timestamp")
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "uploadPdfInfoToDb: Dodane do bazy danych")
                progressDialog.dismiss()
                Toast.makeText(this,"Załadowane do bazy danych",Toast.LENGTH_SHORT).show()
                pdfUri = null
            }
            .addOnFailureListener{e->
                Log.d(TAG, "uploadPdfInfoToDb: Nie udało się załadować na serwer z powodu błędu ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this,"Nie udało się załadować z powodu błędu ${e.message} ",Toast.LENGTH_SHORT).show()
            }

    }

    private fun loadPDFCategories() {
        Log.d(TAG, "loadPDFCategories: Ładowanie kategorii")
        categoryArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryArrayList.clear()
                for (ds in snapshot.children){
                    val model = ds.getValue(ModelCategory::class.java)
                    categoryArrayList.add(model!!)
                    Log.d(TAG, "onDataChange: ${model.category}")
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private var selectedCategoryId = ""
    private var selectedCategoryTitle = ""
    private fun categoryPickDialog(){
        Log.d(TAG, "categoryPickDialog: Napis wyboru kategorii")

        val categoriesArray = arrayOfNulls<String>(categoryArrayList.size)
        for (i in categoryArrayList.indices){
            categoriesArray[i] = categoryArrayList[i].category
        }

        val builder =  AlertDialog.Builder(this)
        builder.setTitle("Wybierz kategorie")
            .setItems(categoriesArray){dialog, which ->
            selectedCategoryTitle = categoryArrayList[which].category
            selectedCategoryId = categoryArrayList[which].id

                binding.categoryTv.text = selectedCategoryTitle

                Log.d(TAG, "categoryPickDialog: Selected Category ID: $selectedCategoryId")
                Log.d(TAG, "categoryPickDialog: Selected Category Title: $selectedCategoryTitle")
            }
            .show()
    }

    private fun pdfPickIntent(){
        Log.d(TAG, "pdfPickIntent: Rozpoczynam wybierać pliki pdf")

        val intent = Intent()
        intent.type = "application/pdf"
        intent.action = Intent.ACTION_GET_CONTENT
        pdfActivityResultLauncher.launch(intent)

    }

    val pdfActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<androidx.activity.result.ActivityResult>{result ->
        if (result.resultCode == RESULT_OK){
            Log.d(TAG, "PDF Picked: ")
            pdfUri = result.data!!.data
        }
            else{
            Log.d(TAG, "PDF Pick cancelled: ")
            Toast.makeText(this, "Przerwano", Toast.LENGTH_SHORT).show()
            }
        }
    )


}