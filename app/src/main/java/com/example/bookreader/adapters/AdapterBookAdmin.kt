package com.example.bookreader.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.example.bookreader.activities.BookDetailActivity
import com.example.bookreader.activities.BookEditActivity
import com.example.bookreader.filters.FilterBookAdmin
import com.example.bookreader.models.ModelBook
import com.example.bookreader.utils.MyApplication
import com.example.bookreader.databinding.RowBookAdminBinding

class AdapterBookAdmin :RecyclerView.Adapter<AdapterBookAdmin.HolderBookAdmin>, Filterable{

    private var context: Context

    public var bookArrayList: ArrayList<ModelBook>
    private val filterList:ArrayList<ModelBook>

    private lateinit var binding:RowBookAdminBinding

    private var filter: FilterBookAdmin? = null

    constructor(context: Context, bookArrayList: ArrayList<ModelBook>) : super() {
        this.context = context
        this.bookArrayList = bookArrayList
        this.filterList = bookArrayList
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderBookAdmin {
        binding = RowBookAdminBinding.inflate(LayoutInflater.from(context),parent,false)
        return HolderBookAdmin(binding.root)
    }

    override fun getItemCount(): Int {
        return bookArrayList.size
    }

    override fun onBindViewHolder(holder: HolderBookAdmin, position: Int) {
        val model = bookArrayList[position]
        val bookId = model.id
        val categoryId = model.categoryId
        val title = model.title
        val description = model.description
        val bookUrl = model.url
        val timestamp = model.timestamp
        val formattedDate = MyApplication.formatTimeStamp(timestamp)

        holder.titleTv.text = title
        holder.descriptionTv.text = description
        holder.dateTv.text = formattedDate

        MyApplication.loadCategory(categoryId, holder.categoryTv)

        MyApplication.loadBookFromUrlSinglePage(
            bookUrl,
            title,
            holder.bookView,
            holder.progressBar,
            null
        )

        MyApplication.loadBookSize(bookUrl, title, holder.sizeTv)

        holder.moreBtn.setOnClickListener{
            moreOptionsDialog(model, holder)
        }

        holder.itemView.setOnClickListener{
            val intent = Intent(context, BookDetailActivity::class.java)
            intent.putExtra("bookId", bookId)
            context.startActivity(intent)
        }
    }

    private fun moreOptionsDialog(model: ModelBook, holder: HolderBookAdmin) {
        val bookId = model.id
        val bookUrl = model.url
        val bookTitle = model.title

        val options = arrayOf("Edytuj", "Usuń")
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Wybierz opcje")
            .setItems(options){dialog, position ->
                if (position== 0){
                    val intent = Intent(context, BookEditActivity::class.java)
                    intent.putExtra("bookId", bookId)
                    context.startActivity(intent)
                }
                else if (position == 1){
                    MyApplication.deleteBook(context, bookId, bookUrl, bookTitle)
                }
            }
            .show()
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = FilterBookAdmin(filterList, this)
        }
        return filter as FilterBookAdmin
    }

    inner class HolderBookAdmin(itemView: View) : RecyclerView.ViewHolder(itemView){
        val bookView = binding.bookView
        val progressBar = binding.progressBar
        val titleTv = binding.titleTv
        val descriptionTv = binding.descriptionTv
        val categoryTv = binding.categoryTv
        val sizeTv = binding.sizeTv
        val dateTv = binding.dateTv
        val moreBtn = binding.moreBtn
    }
}