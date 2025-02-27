package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val db = FirebaseFirestore.getInstance()

        val avatarImageView: ImageView = findViewById(R.id.avatarImageView)
        val nameTextView: TextView = findViewById(R.id.nameTextView)
        val genderTextView: TextView = findViewById(R.id.genderTextView)
        val birthdateTextView: TextView = findViewById(R.id.birthdateTextView)
        val countryTextView: TextView = findViewById(R.id.countryTextView)
        val emailTextView: TextView = findViewById(R.id.emailTextView)
        val phoneTextView: TextView = findViewById(R.id.phoneTextView)
        val platformTextView: TextView = findViewById(R.id.platformTextView)
        val descriptionTextView: TextView = findViewById(R.id.descriptionTextView)
        val favoriteGenresTextView: TextView = findViewById(R.id.favoriteGenresTextView)
        val dislikedGenresTextView: TextView = findViewById(R.id.dislikedGenresTextView)

        val editButton: Button = findViewById(R.id.editButton)
        val logoutButton: Button = findViewById(R.id.logoutButton)
        val favoritesButton: Button = findViewById(R.id.favsButton)
        favoritesButton.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userData = document.data
                    val avatarUrl = userData?.get("avatar") as? String
                    val name = userData?.get("name") as? String
                    val gender = userData?.get("gender") as? String
                    val birthdate = userData?.get("birthdate") as? String
                    val country = userData?.get("country") as? String
                    val email = userData?.get("email") as? String
                    val phone = userData?.get("phone") as? String
                    val platform = userData?.get("platform") as? String
                    val description = userData?.get("description") as? String
                    val favoriteGenres = userData?.get("favoriteGenres") as? List<String>
                    val dislikedGenres = userData?.get("dislikedGenres") as? List<String>

                    Glide.with(this).load(avatarUrl).into(avatarImageView)
                    nameTextView.text = "$name"
                    genderTextView.text = "Пол: $gender"
                    birthdateTextView.text = "Дата Рождения: $birthdate"
                    countryTextView.text = "Страна: $country"
                    emailTextView.text = "Email: $email"
                    phoneTextView.text = "Тел.: $phone"
                    platformTextView.text = "Платформа: $platform"
                    descriptionTextView.text = "Описание: $description"
                    favoriteGenresTextView.text = "Любимые жанры: ${favoriteGenres?.joinToString(", ")}"
                    dislikedGenresTextView.text = "Нелюбимые жанры: ${dislikedGenres?.joinToString(", ")}"
                }
            }
            .addOnFailureListener { exception ->
            }

        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val fabBack = findViewById<FloatingActionButton>(R.id.fabBack)
        fabBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        editButton.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

    }
}
