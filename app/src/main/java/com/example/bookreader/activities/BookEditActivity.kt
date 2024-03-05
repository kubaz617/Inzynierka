package com.example.bookreader.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.bookreader.databinding.ActivityBookEditBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BookEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookEditBinding

    private companion object{
        private const val TAG = "PDF_EDIT_TAG"
    }

    private var bookId = ""

    private lateinit var progressDialog: ProgressDialog

    private lateinit var categoryTitleArrayList:ArrayList<String>

    private lateinit var categoryIdArrayList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId = intent.getStringExtra("bookId")!!

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Proszę czekać")
        progressDialog.setCanceledOnTouchOutside(false)

        loadCategories()
        loadBookInfo()

        binding.categoryTv.setOnClickListener{
            categoryDialog()
        }

        binding.submitBtn.setOnClickListener{
        validateData()
        }
    }

    private fun loadBookInfo() {
        Log.d(TAG, "loadBookInfo: Ładowanie informacji o książce")

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    selectedCategoryId = snapshot.child("categoryId").value.toString()
                   val description = snapshot.child("description").value.toString()
                    val title = snapshot.child("title").value.toString()

                    binding.titleEt.setText(title)
                    binding.descriptionEt.setText(description)

                    Log.d(TAG, "onDataChange: Ładowanie kategorii")
                    val refBookCategory = FirebaseDatabase.getInstance().getReference("Categories")
                    refBookCategory.child(selectedCategoryId)
                        .addListenerForSingleValueEvent(object: ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val category = snapshot.child("category").value
                                binding.categoryTv.text = category.toString()
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private var title =""
    private var description =""
    private fun validateData() {
            title = binding.titleEt.text.toString().trim()
            description = binding.descriptionEt.text.toString().trim()

            if (title.isEmpty()){
                Toast.makeText(this, "Wprowadź tytuł", Toast.LENGTH_SHORT).show()
            }
        else if (description.isEmpty()){
                Toast.makeText(this, "Wprowadź opis", Toast.LENGTH_SHORT).show()
        }
        else if (selectedCategoryId.isEmpty()){
                Toast.makeText(this, "Wybierz kategorie", Toast.LENGTH_SHORT).show()
        }
        else{
            updateBook()
        }
    }

    private fun updateBook() {
        Log.d(TAG, "updateBook: Rozpoczynanie aktualizacji informacji o książce")

        progressDialog.setMessage("Aktualizacja danych")
        progressDialog.show()

        val hashMap = HashMap<String, Any>()
        hashMap["title"] = "$title"
        hashMap["description"] = "$description"
        hashMap["categoryId"] = "$selectedCategoryId"

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Log.d(TAG, "updateBook: Udało się zaaktualizować informację")
                Toast.makeText(this, "Udało się zaaktualizować informację", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener{e->
                Log.d(TAG, "updateBook: Nie udało się zaaktualizować z powodu błędu ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Nie udało się zaaktualizować z powodu błędu ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private var selectedCategoryId = ""
    private var selectedCategoryTitle = ""
    private fun categoryDialog() {

        val categoriesArray = arrayOfNulls<String>(categoryTitleArrayList.size)
        for (i in categoryTitleArrayList.indices){
            categoriesArray[i] = categoryTitleArrayList[i]
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Wybierz kategorie")
            .setItems(categoriesArray){dialog, position ->

                selectedCategoryId = categoryIdArrayList[position]
                selectedCategoryTitle = categoryTitleArrayList[position]

                binding.categoryTv.text = selectedCategoryTitle
            }
            .show()
    }

    private fun loadCategories() {
        Log.d(TAG, "loadCategories: ładowanie kategorii")

        categoryTitleArrayList = ArrayList()
        categoryIdArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryIdArrayList.clear()
                categoryTitleArrayList.clear()

                for (ds in snapshot.children){
                    val id = "${ds.child("id").value}"
                    val category = "${ds.child("category").value}"

                    categoryIdArrayList.add(id)
                    categoryTitleArrayList.add(category)

                    Log.d(TAG, "onDataChange: Category ID $id")
                    Log.d(TAG, "onDataChange: Category $category")
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}