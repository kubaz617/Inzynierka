package com.example.bookreader.activities

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.bookreader.models.QuestionModel
import com.example.bookreader.R
import com.example.bookreader.databinding.ActivityQuizBinding
import com.example.bookreader.databinding.ScoreScreenBinding


class QuizActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var dialogBinding: ScoreScreenBinding
    companion object {
        var questionModelList: List<QuestionModel> = listOf()
        var time: String = ""
    }

    lateinit var binding: ActivityQuizBinding

    var currentQuestionIndex = 0;
    var selectedAnswer = ""
    var score = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            btnA.setOnClickListener(this@QuizActivity)
            btnB.setOnClickListener(this@QuizActivity)
            btnC.setOnClickListener(this@QuizActivity)
            btnD.setOnClickListener(this@QuizActivity)
            nextBtn.setOnClickListener(this@QuizActivity)
        }

        shuffleQuestions()
        loadQuestions()
        startTimer()
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setMessage("Czy na pewno chcesz przerwać quiz?")
            .setPositiveButton("Tak") { dialog, which ->
                super.onBackPressed()
            }
            .setNegativeButton("Nie", null)
            .show()
    }

    private fun startTimer(){
        val totalTimeInMillis = time.toInt() * 60 * 1000L
        object : CountDownTimer(totalTimeInMillis,1000L){
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished /1000
                val minutes = seconds/60
                val remainingSeconds = seconds % 60
                binding.timerIndicatorTextview.text = String.format("%02d:%02d", minutes,remainingSeconds)

            }

            override fun onFinish() {
            }

        }.start()
    }

    private fun loadQuestions(){
        selectedAnswer = ""
        if(currentQuestionIndex == questionModelList.size){
            finishQuiz()
            return
        }

        binding.apply {
            questionIndicatorTextview.text = "Pytanie ${currentQuestionIndex+1}/ ${questionModelList.size} "
            questionProgressIndicator.progress =
                ( currentQuestionIndex.toFloat() / questionModelList.size.toFloat() * 100 ).toInt()
            questionTextview.text = questionModelList[currentQuestionIndex].question
            btnA.text = questionModelList[currentQuestionIndex].options[0]
            btnB.text = questionModelList[currentQuestionIndex].options[1]
            btnC.text = questionModelList[currentQuestionIndex].options[2]
            btnD.text = questionModelList[currentQuestionIndex].options[3]
        }
    }

    override fun onClick(view: View?) {

        binding.apply {
            btnA.setBackgroundColor(getColor(R.color.grey))
            btnB.setBackgroundColor(getColor(R.color.grey))
            btnC.setBackgroundColor(getColor(R.color.grey))
            btnD.setBackgroundColor(getColor(R.color.grey))
        }

        val clickedBtn = view as Button
        if(clickedBtn.id== R.id.next_btn){
            //next button is clicked
            if(selectedAnswer.isEmpty()){
                Toast.makeText(applicationContext,"Proszę wybrać odpowiedź żeby móc kontynuować", Toast.LENGTH_SHORT).show()
                return;
            }
            if(selectedAnswer == questionModelList[currentQuestionIndex].correct){
                score++
                Log.i("Punktacja quizu",score.toString())
            }
            currentQuestionIndex++
            loadQuestions()
        }else{
            //options button is clicked
            selectedAnswer = clickedBtn.text.toString()
            clickedBtn.setBackgroundColor(getColor(R.color.cyan))
        }
    }

    private fun shuffleQuestions() {
        questionModelList = questionModelList.shuffled()
    }

    private fun displayTrophy(trophyImageView: ImageView, scorePercentage: Int) {
        if (scorePercentage > 0) {
            when {
                scorePercentage >= 75 -> trophyImageView.setImageResource(R.drawable.ic_trophy_gld)
                scorePercentage >= 50 -> trophyImageView.setImageResource(R.drawable.ic_trophy_slvr)
                else -> trophyImageView.setImageResource(R.drawable.ic_trophy_brwn)
            }
        } else {
            trophyImageView.setImageDrawable(null)
        }
    }
    private fun finishQuiz(){
        val dialogBinding = ScoreScreenBinding.inflate(layoutInflater)
        val totalQuestions = questionModelList.size
        val percentage = ((score.toFloat() / totalQuestions.toFloat() ) *100 ).toInt()

        displayTrophy(dialogBinding.trophy, percentage)

        dialogBinding.apply {
            scoreProgressIndicator.progress = percentage
            scoreProgressText.text = "$percentage %"
            if(percentage>40){
                scoreTitle.text = "Gratulacje! Udało ci się przejść quiz"
                scoreTitle.setTextColor(Color.GREEN)
            }else{
                scoreTitle.text = "Niestety, nie udało ci się przejść quizu"
                scoreTitle.setTextColor(Color.RED)
            }
            scoreSubtitle.text = "$score na $totalQuestions poprawnych"
            finishBtn.setOnClickListener {
                finish()
            }
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .show()

    }
}