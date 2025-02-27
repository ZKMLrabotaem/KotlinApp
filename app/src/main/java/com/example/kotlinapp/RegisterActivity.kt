package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {

    private val emailPattern = Pattern.compile("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPasswordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginTextView = findViewById<TextView>(R.id.loginTextView)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showToast("Поля не могут быть пустыми.")
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                showToast("Некорректный формат email.")
                return@setOnClickListener
            }

            if (password.length < 6) {
                showToast("Пароль должен быть не менее 6 символов.")
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                showToast("Пароли не совпадают.")
                return@setOnClickListener
            }

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        createUserProfile(user)
                        showToast("Регистрация прошла успешно!")
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        showToast("Ошибка регистрации: ${task.exception?.message}")
                    }
                }
        }
        loginTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return emailPattern.matcher(email).matches()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private fun createUserProfile(user: FirebaseUser?) {
        if (user != null) {
            val userProfile = hashMapOf(
                "name" to "?",
                "email" to user.email,
                "phone" to "?",
                "avatar" to "?",
                "gender" to "?",
                "birthdate" to "?",
                "country" to "?",
                "platform" to "?",
                "description" to "?",
                "role" to "user",
                "favs" to listOf<String>(),
                "favoriteGenres" to listOf<String>(),
                "dislikedGenres" to listOf<String>()
            )

            db.collection("users").document(user.uid)
                .set(userProfile)
                .addOnSuccessListener {
                    println("Профиль пользователя создан")
                }
                .addOnFailureListener { e ->
                    println("Ошибка при создании профиля: $e")
                }
        }
    }
}
