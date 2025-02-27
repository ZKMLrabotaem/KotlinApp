package com.example.kotlinapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RateDiscActivity : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var editTextComment: EditText
    private lateinit var buttonSubmit: Button
    private lateinit var buttonDeleteReview: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var discId: String
    private lateinit var userId: String
    private lateinit var commentsRecyclerView: RecyclerView
    private val commentsList = mutableListOf<Comment>()
    private lateinit var commentAdapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rate_disc)

        ratingBar = findViewById(R.id.ratingBar)
        editTextComment = findViewById(R.id.editTextComment)
        buttonSubmit = findViewById(R.id.buttonSubmit)
        buttonDeleteReview = findViewById(R.id.buttonDeleteReview)

        discId = intent.getStringExtra("disc_id") ?: ""
        userId = auth.currentUser?.uid ?: ""

        if (discId.isEmpty() || userId.isEmpty()) {
            Toast.makeText(this, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView)
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter(commentsList)
        commentsRecyclerView.adapter = commentAdapter

        loadComments()

        loadUserReview()

        buttonSubmit.setOnClickListener {
            saveRatingAndComment()
        }

        buttonDeleteReview.setOnClickListener {
            deleteReview()
        }

        findViewById<FloatingActionButton>(R.id.fabBack).setOnClickListener {
            finish()
        }
    }
    private fun loadComments() {
        db.collection("discs").document(discId)
            .collection("comments")
            .get()
            .addOnSuccessListener { documents ->
                commentsList.clear()
                for (document in documents) {
                    val userId = document.id
                    val commentText = document.getString("text") ?: ""

                    db.collection("discs").document(discId)
                        .collection("rating").document(userId)
                        .get()
                        .addOnSuccessListener { ratingDoc ->
                            val grade = ratingDoc.getLong("grade")?.toInt() ?: 0

                            db.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val username = userDoc.getString("name") ?: "Неизвестный"
                                    val avatarUrl = userDoc.getString("avatar") ?: ""

                                    val comment = Comment(
                                        userId = userId,
                                        username = username,
                                        userAvatar = avatarUrl,
                                        grade = grade,
                                        text = commentText
                                    )
                                    commentsList.add(comment)
                                    commentAdapter.notifyDataSetChanged()
                                }
                        }
                }
            }
    }


    private fun loadUserReview() {
        db.collection("discs").document(discId)
            .collection("rating").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val grade = document.getLong("grade")?.toInt() ?: 0
                    ratingBar.rating = grade.toFloat()
                }
            }

        db.collection("discs").document(discId)
            .collection("comments").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    editTextComment.setText(document.getString("text") ?: "")
                }
            }
    }

    private fun saveRatingAndComment() {
        val grade = ratingBar.rating.toInt()
        val commentText = editTextComment.text.toString().trim()

        if (grade in 1..5) {
            db.collection("discs").document(discId)
                .collection("rating").document(userId)
                .set(mapOf("grade" to grade))
        }

        if (commentText.isNotEmpty()) {
            db.collection("discs").document(discId)
                .collection("comments").document(userId)
                .set(mapOf("text" to commentText))
        }

        Toast.makeText(this, "Ваш отзыв сохранен!", Toast.LENGTH_SHORT).show()
        finish()
    }



    private fun deleteReview() {
        db.collection("discs").document(discId)
            .collection("rating").document(userId)
            .delete()
            .addOnSuccessListener {
                ratingBar.rating = 0f
            }

        db.collection("discs").document(discId)
            .collection("comments").document(userId)
            .delete()
            .addOnSuccessListener {
                editTextComment.text.clear()
                Toast.makeText(this, "Ваш отзыв удален!", Toast.LENGTH_SHORT).show()
            }
    }
}
