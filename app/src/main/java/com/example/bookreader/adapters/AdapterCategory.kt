package com.example.bookreader.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.bookreader.activities.BooksListAdminActivity
import com.example.bookreader.filters.FilterCategory
import com.example.bookreader.models.ModelCategory
import com.example.bookreader.databinding.RowCategoryBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdapterCategory :RecyclerView.Adapter<AdapterCategory.HolderCategory>, Filterable{

    private val context: Context
    public var categoryArrayList: ArrayList<ModelCategory>
    private var filterList: ArrayList<ModelCategory>
    private var filter: FilterCategory? = null

    private lateinit var binding: RowCategoryBinding


    constructor(context: Context, categoryArrayList: ArrayList<ModelCategory>) {
        this.context = context
        this.categoryArrayList = categoryArrayList
        this.filterList = categoryArrayList
    }

    inner class HolderCategory(itemView: View): RecyclerView.ViewHolder(itemView){

        var categoryTv:TextView = binding.categoryTv
        var delete_btn:ImageButton = binding.deleteBtn
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderCategory {
        binding = RowCategoryBinding.inflate(LayoutInflater.from(context), parent, false)


        return HolderCategory(binding.root)
    }

    override fun getItemCount(): Int {
        return categoryArrayList.size
    }

    override fun onBindViewHolder(holder: HolderCategory, position: Int) {
         val model = categoryArrayList[position]
         val id = model.id
         val category = model.category
         val uid = model.uid
         val timestamp = model.timestamp

        holder.categoryTv.text = category

        holder.delete_btn.setOnClickListener{
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Usuń")
                .setMessage("Czy jesteś pewien, że chcesz usunąć tą kategorię?")
                .setPositiveButton("Tak"){a, d->
                    Toast.makeText(context,"Usuwanie",Toast.LENGTH_SHORT).show()
                    deleteCategory(model, holder)
                }
                .setNegativeButton("Nie"){a, d->
                    a.dismiss()
                }
                .show()
        }

        holder.itemView.setOnClickListener{
            val intent = Intent(context, BooksListAdminActivity::class.java)
            intent.putExtra("categoryId", id)
            intent.putExtra("category", category)
            context.startActivity(intent)
        }
    }

    private fun deleteCategory(model: ModelCategory, holder: HolderCategory) {
        val categoryId = model.id
        val categoryRef = FirebaseDatabase.getInstance().getReference("Categories").child(categoryId)

        val ref = FirebaseDatabase.getInstance().getReference("Books").orderByChild("categoryId").equalTo(categoryId)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(context, "Nie można usunąć kategorii, ponieważ zawiera książki", Toast.LENGTH_SHORT).show()
                } else {
                    categoryRef.removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Kategoria usunięta", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Nie udało się usunąć kategorii z powodu błędu ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Błąd podczas sprawdzania książek w kategorii: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    override fun getFilter(): Filter {
        if (filter == null){
            filter = FilterCategory(filterList, this)
        }
        return filter as FilterCategory
    }
}