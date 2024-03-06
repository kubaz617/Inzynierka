package com.example.bookreader.models

class ModelBook {

    var uid:String = ""
    var id:String = ""
    var title:String = ""
    var author:String = ""
    var description:String = ""
    var categoryId:String = ""
    var url:String = ""
    var timestamp:Long = 0
    var viewsCount:Long = 0

    constructor()
    constructor(
        uid: String,
        id: String,
        title: String,
        author:String,
        description: String,
        categoryId: String,
        url: String,
        timestamp: Long,
        viewsCount: Long,
    ) {
        this.uid = uid
        this.id = id
        this.title = title
        this.author = author
        this.description = description
        this.categoryId = categoryId
        this.url = url
        this.timestamp = timestamp
        this.viewsCount = viewsCount
    }


}