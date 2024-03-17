package com.example.bookreader.models


data class QuizModel(
    var id : String,
    var title : String,
    var subtitle : String,
    var time : String,
    var questionList : List<QuestionModel>
){
    constructor() : this("","","","", emptyList())
}

data class QuestionModel(
    var question : String,
    var options : List<String>,
    var correct : String,
){
    constructor() : this ("", emptyList(),"")
}

