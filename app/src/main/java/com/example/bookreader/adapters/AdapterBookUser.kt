package com.example.bookreader.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.example.bookreader.activities.BookDetailActivity
import com.example.bookreader.filters.FilterBookUser
import com.example.bookreader.models.ModelBook
import com.example.bookreader.utils.MyApplication
import com.example.bookreader.databinding.RowBookUserBinding

class AdapterBookUser : RecyclerView.Adapter<AdapterBookUser.HolderBookUser>, Filterable{

    private var context: Context

    private val filterList: ArrayList<ModelBook>
    public var bookArrayList: ArrayList<ModelBook>
    private val TAG = "PDF_ADD_TAG"

    private lateinit var binding: RowBookUserBinding

    private var filter: FilterBookUser? = null
    private var randomBook: ModelBook? = null

    constructor(context: Context, bookArrayList: ArrayList<ModelBook>) : super() {
        this.context = context
        this.bookArrayList = bookArrayList
        this.filterList = bookArrayList
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderBookUser {
        binding = RowBookUserBinding.inflate(LayoutInflater.from(context),parent,false)
        return HolderBookUser(binding.root)
    }
    override fun getItemCount(): Int {
        return bookArrayList.size
    }

    override fun onBindViewHolder(holder: HolderBookUser, position: Int) {
        val model = bookArrayList[position]
        val bookId = model.id
        val categoryId = model.categoryId
        val title = model.title
        val author = model.author
        val description = model.description
        val bookUrl = model.url
        val timestamp = model.timestamp
        val date = MyApplication.formatTimeStamp(timestamp)

        holder.titleTv.text = title
        holder.descriptionTv.text = description
        holder.dateTv.text = date
        holder.authorTv.text = author

        MyApplication.loadCategory(categoryId, holder.categoryTv)

        MyApplication.loadBookFromUrlSinglePage(
            bookUrl,
            title,
            holder.bookView,
            holder.progressBar,
            null
        )



        holder.itemView.setOnClickListener{
            val intent = Intent(context, BookDetailActivity::class.java)
            intent.putExtra("bookId", bookId)
            context.startActivity(intent)
        }
    }


    override fun getFilter(): Filter {
        if (filter == null) {
            filter = FilterBookUser(filterList, this)
        }
        Log.d(TAG, "getFilter: Rozpoczynam filtrowanie")
        return filter as FilterBookUser
    }

    inner class HolderBookUser(itemView: View): RecyclerView.ViewHolder(itemView){
        var bookView = binding.bookView
        var progressBar = binding.progressBar
        var titleTv = binding.titleTv
        var descriptionTv = binding.descriptionTv
        var categoryTv = binding.categoryTv
        var authorTv = binding.authorTv
        var dateTv = binding.dateTv

    }

    fun setRandomBook(randomBook: ModelBook?) {
        this.randomBook = randomBook
        notifyDataSetChanged()
    }
}