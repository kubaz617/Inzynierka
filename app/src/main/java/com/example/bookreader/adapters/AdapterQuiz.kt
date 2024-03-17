package com.example.bookreader.adapters


import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bookreader.models.QuizModel
import com.example.bookreader.activities.QuizActivity
import com.example.bookreader.databinding.RowQuizBinding

class AdapterQuiz(private val quizModelList : List<QuizModel>) :
    RecyclerView.Adapter<AdapterQuiz.HolderQuiz>() {

    class HolderQuiz : RecyclerView.ViewHolder {
        private val binding: RowQuizBinding
        constructor(binding: RowQuizBinding) : super(binding.root) {
            this.binding = binding
        }

        fun bind(model : QuizModel){
            binding.apply {
                quizTitleText.text = model.title
                quizSubtitleText.text = model.subtitle
                quizTimeText.text = model.time + " min"
                root.setOnClickListener {
                    val intent  = Intent(root.context, QuizActivity::class.java)
                    QuizActivity.questionModelList = model.questionList
                    QuizActivity.time = model.time
                    root.context.startActivity(intent)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderQuiz {
        val binding = RowQuizBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return HolderQuiz(binding)
    }

    override fun getItemCount(): Int {
        return quizModelList.size
    }

    override fun onBindViewHolder(holder: HolderQuiz, position: Int) {
        holder.bind(quizModelList[position])
    }
}
