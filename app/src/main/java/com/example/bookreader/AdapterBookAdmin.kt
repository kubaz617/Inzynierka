package com.example.bookreader

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
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

        MyApplication.loadBookFromUrlSinglePage(bookUrl, title, holder.bookView, holder.progressBar, null)

        MyApplication.loadBookSize(bookUrl, title, holder.sizeTv)
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