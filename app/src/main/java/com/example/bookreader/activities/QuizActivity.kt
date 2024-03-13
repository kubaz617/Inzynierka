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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class QuizActivity : AppCompatActivity(), View.OnClickListener {

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
            selectedAnswer = clickedBtn.text.toString()
            clickedBtn.setBackgroundColor(getColor(R.color.cyan))
        }
    }

    private fun shuffleQuestions() {
        questionModelList = questionModelList.shuffled()
    }

    private fun displayTrophy(trophyImageView: ImageView, scorePercentage: Int) {
        when {
            scorePercentage == 100 -> trophyImageView.setImageResource(R.drawable.ic_trophy_plt)
            scorePercentage in 75..99 -> trophyImageView.setImageResource(R.drawable.ic_trophy_gld)
            scorePercentage in 50..74 -> trophyImageView.setImageResource(R.drawable.ic_trophy_slvr)
            scorePercentage in 25..49 -> trophyImageView.setImageResource(R.drawable.ic_trophy_brwn)
            else -> trophyImageView.setImageDrawable(null)
        }
    }

    private fun addPlatinumTrophyToUser() {
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

            userRef.child("statsDetails").child("platinumTrophy").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val platinumTrophyCount = dataSnapshot.getValue(Int::class.java) ?: 0
                    val newPlatinumTrophyCount = platinumTrophyCount + 1

                    userRef.child("statsDetails").child("platinumTrophy").setValue(newPlatinumTrophyCount)
                        .addOnSuccessListener {
                            Log.d("PlatinumTrophy", "Pomyślnie dodano platynowy puchar.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("PlatinumTrophy", "Błąd podczas dodawania platynowego pucharu: ${e.message}")
                        }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("PlatinumTrophy", "Błąd pobierania danych o platynowych pucharach: ${databaseError.message}")
                }
            })
        }
    }


    private fun addGoldTrophyToUser() {
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

            userRef.child("statsDetails").child("goldTrophy").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val goldTrophyCount = dataSnapshot.getValue(Int::class.java) ?: 0
                    val newGoldTrophyCount = goldTrophyCount + 1

                    userRef.child("statsDetails").child("goldTrophy").setValue(newGoldTrophyCount)
                        .addOnSuccessListener {
                            Log.d("GoldTrophy", "Pomyślnie dodano złoty puchar.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("GoldTrophy", "Błąd podczas dodawania złotego pucharu: ${e.message}")
                        }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("GoldTrophy", "Błąd pobierania danych o złotych pucharach: ${databaseError.message}")
                }
            })
        }
    }


    private fun addSilverTrophyToUser() {
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

            userRef.child("statsDetails").child("silverTrophy").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val silverTrophyCount = dataSnapshot.getValue(Int::class.java) ?: 0
                    val newSilverTrophyCount = silverTrophyCount + 1

                    userRef.child("statsDetails").child("silverTrophy").setValue(newSilverTrophyCount)
                        .addOnSuccessListener {
                            Log.d("SilverTrophy", "Pomyślnie dodano srebrny puchar.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("SilverTrophy", "Błąd podczas dodawania srebrnego pucharu: ${e.message}")
                        }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("SilverTrophy", "Błąd pobierania danych o srebrnych pucharach: ${databaseError.message}")
                }
            })
        }
    }


    private fun addBronzeTrophyToUser() {
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

            userRef.child("statsDetails").child("bronzeTrophy").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val bronzeTrophyCount = dataSnapshot.getValue(Int::class.java) ?: 0
                    val newBronzeTrophyCount = bronzeTrophyCount + 1

                    userRef.child("statsDetails").child("bronzeTrophy").setValue(newBronzeTrophyCount)
                        .addOnSuccessListener {
                            Log.d("BronzeTrophy", "Pomyślnie dodano brązowy puchar.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("BronzeTrophy", "Błąd podczas dodawania brązowego pucharu: ${e.message}")
                        }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("BronzeTrophy", "Błąd pobierania danych o brązowych pucharach: ${databaseError.message}")
                }
            })
        }
    }



    private fun finishQuiz(){
        val dialogBinding = ScoreScreenBinding.inflate(layoutInflater)
        val totalQuestions = questionModelList.size
        val percentage = ((score.toFloat() / totalQuestions.toFloat() ) *100 ).toInt()

        displayTrophy(dialogBinding.trophy, percentage)
        if (percentage == 100) {
            addPlatinumTrophyToUser()
        } else if (percentage >= 75) {
            addGoldTrophyToUser()
        } else if (percentage in 50 until 75) {
            addSilverTrophyToUser()
        } else if (percentage in 25 until 50) {
            addBronzeTrophyToUser()
        }

        dialogBinding.apply {
            scoreProgressIndicator.progress = percentage
            scoreProgressText.text = "$percentage %"
            if(percentage>25){
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