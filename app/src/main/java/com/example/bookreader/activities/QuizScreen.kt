package com.example.bookreader.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookreader.adapters.AdapterQuiz
import com.example.bookreader.databinding.ActivityQuizScreenBinding
import com.example.bookreader.models.QuizModel
import com.google.firebase.database.FirebaseDatabase

class QuizScreen : AppCompatActivity() {
    lateinit var binding: ActivityQuizScreenBinding
    lateinit var quizModelList : MutableList<QuizModel>
    lateinit var adapter: AdapterQuiz

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        quizModelList = mutableListOf()
        getDataFromFirebase()


    }

    private fun setupRecyclerView(){
        binding.progressBar.visibility = View.GONE
        adapter = AdapterQuiz(quizModelList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun getDataFromFirebase(){
        binding.progressBar.visibility = View.VISIBLE
        FirebaseDatabase.getInstance().getReference("Quizes")
            .get()
            .addOnSuccessListener { dataSnapshot->
                if(dataSnapshot.exists()){
                    for (snapshot in dataSnapshot.children){
                        val quizModel = snapshot.getValue(QuizModel::class.java)
                        if (quizModel != null) {
                            quizModelList.add(quizModel)
                        }
                    }
                }
                setupRecyclerView()
            }


    }
}









