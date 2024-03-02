package com.example.bookreader

import android.widget.Filter

class FilterBookUser: Filter {

    var filterList: ArrayList<ModelBook>

    var adapterBookUser: AdapterBookUser

    constructor(filterList: ArrayList<ModelBook>, adapterBookUser: AdapterBookUser) : super() {
        this.filterList = filterList
        this.adapterBookUser = adapterBookUser
    }

    override fun performFiltering(constraint: CharSequence): FilterResults {
        var constraint: CharSequence? = constraint
        val results = FilterResults()

        if (constraint != null && constraint.isNotEmpty()){

            constraint = constraint.toString().uppercase()
            val filteredModels = ArrayList<ModelBook>()
            for (i in filterList.indices){
                if (filterList[i].title.uppercase().contains(constraint)){
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

    override fun publishResults(constraint: CharSequence, results: FilterResults) {
        adapterBookUser.bookArrayList = results.values as ArrayList<ModelBook>

        adapterBookUser.notifyDataSetChanged()
    }
}