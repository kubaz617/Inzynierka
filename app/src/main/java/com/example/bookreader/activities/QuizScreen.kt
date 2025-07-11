package com.example.bookreader.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookreader.adapters.AdapterQuiz
import com.example.bookreader.databinding.ActivityQuizScreenBinding
import com.example.bookreader.models.QuizModel
import com.example.bookreader.utils.MyApplication
import com.google.firebase.database.FirebaseDatabase

class QuizScreen : AppCompatActivity() {
    private lateinit var binding: ActivityQuizScreenBinding
    private lateinit var quizModelList : MutableList<QuizModel>
    private lateinit var adapter: AdapterQuiz

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        quizModelList = mutableListOf()
        getDataFromFirebase()

        val selectedBackground = getSelectedBackground()
        window.setBackgroundDrawableResource(selectedBackground)
        window.statusBarColor = MyApplication.getStatusBarColor(this)

    }

    private fun getSelectedBackground(): Int {
        return MyApplication.getSelectedBackground(this)
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









