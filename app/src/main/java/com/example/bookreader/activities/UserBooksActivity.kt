package com.example.bookreader.activities

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.bookreader.databinding.ActivityUserBooksBinding
import com.example.bookreader.models.ModelCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserBooksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserBooksBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var categoryArrayList: ArrayList<ModelCategory>
    private lateinit var viewPagerAdapter: ViewPagerAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        setupWithViewPagerAdapter(binding.viewPager)
        binding.tabLayout.setupWithViewPager(binding.viewPager)

    }


    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
            val email = firebaseUser!!.email
            binding.titleTv.text = email

    }

        private fun setupWithViewPagerAdapter(viewPager: ViewPager){
            viewPagerAdapter = ViewPagerAdapter(
                supportFragmentManager,
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
                this
            )

            categoryArrayList = ArrayList()

            val ref = FirebaseDatabase.getInstance().getReference("Categories")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categoryArrayList.clear()

                    val modelAll = ModelCategory("01,","Wszystkie",1,"")
                    val modelMostViewed = ModelCategory("01,","Najczęściej oglądane",1,"")
                    val modelFavorites = ModelCategory("01,","Ulubione",1,"")
                    categoryArrayList.add(modelAll)
                    categoryArrayList.add(modelMostViewed)
                    categoryArrayList.add(modelFavorites)
                    viewPagerAdapter.addFragment(
                        BooksListUser.newInstance(
                            "${modelAll.id}",
                            "${modelAll.category}",
                            "${modelAll.uid}"
                        ), modelAll.category
                    )
                    viewPagerAdapter.addFragment(
                        BooksListUser.newInstance(
                            "${modelMostViewed.id}",
                            "${modelMostViewed.category}",
                            "${modelMostViewed.uid}"
                        ), modelMostViewed.category
                    )
                    viewPagerAdapter.addFragment(
                        BooksListUser.newInstance(
                            "${modelFavorites.id}",
                            "${modelFavorites.category}",
                            "${modelFavorites.uid}"
                        ), modelFavorites.category
                    )
                    viewPagerAdapter.notifyDataSetChanged()

                    for (ds in snapshot.children){
                        val model = ds.getValue(ModelCategory::class.java)
                        categoryArrayList.add(model!!)
                        viewPagerAdapter.addFragment(
                            BooksListUser.newInstance(
                                "${model.id}",
                                "${model.category}",
                                "${model.uid}"
                            ), model.category
                        )
                        viewPagerAdapter.notifyDataSetChanged()
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

            viewPager.adapter = viewPagerAdapter
        }


    class ViewPagerAdapter(fm: FragmentManager, behavior: Int, context: Context): FragmentPagerAdapter(fm, behavior){
            private val fragmentsList: ArrayList<BooksListUser> = ArrayList()

            private val fragmentTitleList: ArrayList<String> = ArrayList()

            private val context: Context

            init {
                this.context = context
            }

            override fun getCount(): Int {
                return fragmentsList.size
            }

            override fun getItem(position: Int): Fragment {
                return fragmentsList[position]
            }

            override fun getPageTitle(position: Int): CharSequence{
                return fragmentTitleList[position]
            }

        fun addFragment(fragment: BooksListUser, title: String){
                fragmentsList.add(fragment)
                fragmentTitleList.add(title)
            }
        }

}