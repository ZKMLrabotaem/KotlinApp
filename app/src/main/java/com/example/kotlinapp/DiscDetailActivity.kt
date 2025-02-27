package com.example.kotlinapp

import ImageSliderAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class DiscDetailActivity : AppCompatActivity() {

    private lateinit var favoriteButton: Button
    private lateinit var deleteButton: Button
    private lateinit var userId: String
    private lateinit var discName: String
    private lateinit var discId: String
    private lateinit var editButton: Button
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disc_detail)

        val auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid ?: ""

        discName = intent.getStringExtra("disc_name") ?: ""
        discId = intent.getStringExtra("disc_id") ?: ""
        val description = intent.getStringExtra("disc_description")
        val price = intent.getDoubleExtra("disc_price", 0.0)
        val genre = intent.getStringExtra("disc_genre")
        val age = intent.getLongExtra("disc_age", 0)
        val imageUrls = intent.getStringArrayListExtra("disc_images") ?: arrayListOf()

        findViewById<TextView>(R.id.nameTextView).text = discName
        findViewById<TextView>(R.id.descriptionTextView).text = "Описание: $description"
        findViewById<TextView>(R.id.priceTextView).text = "Цена: $price руб."
        findViewById<TextView>(R.id.genreTextView).text = "Жанры: $genre"
        findViewById<TextView>(R.id.ageTextView).text = "Возр. Рейтинг: $age+"

        val imageSlider = findViewById<ViewPager2>(R.id.imageSlider)
        imageSlider.adapter = ImageSliderAdapter(imageUrls)

        favoriteButton = findViewById(R.id.button)
        deleteButton = findViewById(R.id.deleteButton)
        editButton = findViewById(R.id.editButton)

        val fabBack = findViewById<FloatingActionButton>(R.id.fabBack)
        fabBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        val addRateButton = findViewById<FloatingActionButton>(R.id.addRate)
        addRateButton.setOnClickListener {
            val intent = Intent(this, RateDiscActivity::class.java)
            intent.putExtra("disc_id", discId)
            startActivity(intent)
        }


        checkIfFavorite()
        checkIfAdmin()
        loadAverageRating()

        favoriteButton.setOnClickListener {
            toggleFavorite()
        }

        deleteButton.setOnClickListener {
            deleteDisc()
        }
        editButton.setOnClickListener {
            openEditActivity()
        }
    }
    private fun loadAverageRating() {
        db.collection("discs").document(discId).collection("rating")
            .get()
            .addOnSuccessListener { documents ->
                var total = 0.0
                var count = 0

                for (document in documents) {
                    val grade = document.getLong("grade")?.toDouble() ?: 0.0
                    total += grade
                    count++
                }

                val averageRating = if (count > 0) total / count else 0.0
                findViewById<RatingBar>(R.id.ratingBar).rating = averageRating.toFloat()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка загрузки рейтинга", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openEditActivity() {
        val intent = Intent(this, EditDiscActivity::class.java).apply {
            putExtra("disc_id", discId)
            putExtra("disc_name", discName)
            putExtra("disc_description", findViewById<TextView>(R.id.descriptionTextView).text.toString())
            putExtra("disc_price", findViewById<TextView>(R.id.priceTextView).text.toString())
            putExtra("disc_genre", findViewById<TextView>(R.id.genreTextView).text.toString())
            putExtra("disc_age", findViewById<TextView>(R.id.ageTextView).text.toString())
        }
        startActivity(intent)
    }
    private fun checkIfFavorite() {
        if (userId.isEmpty()) return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val favorites = document.get("favorites") as? List<String> ?: emptyList()
                    favoriteButton.text = if (favorites.contains(discId)) "Удалить из избранного" else "Добавить в избранное"
                }
            }
            .addOnFailureListener {
                favoriteButton.text = "Добавить в избранное"
            }
    }

    private fun toggleFavorite() {
        if (userId.isEmpty()) return

        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val favorites = document.get("favorites") as? List<String> ?: emptyList()
                if (favorites.contains(discId)) {
                    userRef.update("favorites", FieldValue.arrayRemove(discId))
                        .addOnSuccessListener {
                            favoriteButton.text = "Добавить в избранное"
                        }
                } else {
                    userRef.update("favorites", FieldValue.arrayUnion(discId))
                        .addOnSuccessListener {
                            favoriteButton.text = "Удалить из избранного"
                        }
                }
            }
        }
    }

    private fun checkIfAdmin() {
        if (userId.isEmpty()) return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.getString("role") == "admin") {
                    deleteButton.visibility = Button.VISIBLE
                    editButton.visibility = Button.VISIBLE

                } else {
                    deleteButton.visibility = Button.GONE
                    editButton.visibility = Button.GONE
                }
            }
            .addOnFailureListener {
                deleteButton.visibility = Button.GONE
                editButton.visibility = Button.GONE
            }
    }

    private fun deleteDisc() {
        db.collection("discs").document(discId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Диск удален", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show()
            }
    }
}
