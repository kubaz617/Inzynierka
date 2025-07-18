package com.example.bookreader.filters

import android.widget.Filter
import com.example.bookreader.models.ModelCategory
import com.example.bookreader.adapters.AdapterCategory

class FilterCategory: Filter {

    var filterList: ArrayList<ModelCategory>
    private var adapterCategory: AdapterCategory

    constructor(filterList: ArrayList<ModelCategory>, adapterCategory: AdapterCategory) : super() {
        this.filterList = filterList
        this.adapterCategory = adapterCategory
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint = constraint
        val results = FilterResults()

        if (constraint != null && constraint.isNotEmpty()){

            constraint = constraint.toString().uppercase()
            val filteredModels:ArrayList<ModelCategory> = ArrayList()
            for (i in 0 until filterList.size){
                if(filterList[i].category.uppercase().contains(constraint)){
                    filteredModels.add(filterList[i])
                }
            }
            results.count = filteredModels.size
            results.values = filteredModels
        }

        else{
            results.count = filterList.size
            results.values = filterList
        }
        return results
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults) {
        adapterCategory.categoryArrayList = results.values as ArrayList<ModelCategory>

        adapterCategory.notifyDataSetChanged()
    }
}