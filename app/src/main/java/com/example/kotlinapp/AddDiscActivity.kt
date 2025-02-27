package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class AddDiscActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextPrice: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var editTextAge: EditText
    private lateinit var textViewGenres: TextView
    private val selectedGenres = mutableListOf<String>()
    private val genres = arrayOf("Экшен", "РПГ", "Шутер", "Хоррор", "Гонки", "Приключения", "Стратегия", "Спортивные", "Файтинг", "Слэшер")
    private val selectedGenresChecked = BooleanArray(genres.size)
    private lateinit var buttonAddDisc: Button
    private lateinit var imageUrlsContainer: LinearLayout
    private val editTextUrls = mutableListOf<EditText>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_disc)

        imageUrlsContainer = findViewById(R.id.imageUrlsContainer)
        editTextName = findViewById(R.id.editTextName)
        editTextPrice = findViewById(R.id.editTextPrice)
        editTextDescription = findViewById(R.id.editTextDescription)
        editTextAge = findViewById(R.id.editTextAge)
        textViewGenres = findViewById(R.id.textViewGenres)
        buttonAddDisc = findViewById(R.id.buttonAddDisc)


        textViewGenres.setOnClickListener {
            showGenreSelectionDialog()
        }

        val fabBack = findViewById<FloatingActionButton>(R.id.fabBack)
        fabBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        buttonAddDisc.setOnClickListener {
            addNewDisc()
        }

        addUrlField()
    }
    private fun showGenreSelectionDialog() {
        val builder = AlertDialog.Builder(this, R.style.DarkAlertDialog)
        builder.setTitle("Выберите жанры")
        builder.setMultiChoiceItems(genres, selectedGenresChecked) { _, index, isChecked ->
            if (isChecked) {
                selectedGenres.add(genres[index])
            } else {
                selectedGenres.remove(genres[index])
            }
        }
        builder.setPositiveButton("OK") { _, _ ->
            textViewGenres.text = if (selectedGenres.isNotEmpty()) selectedGenres.joinToString(", ") else "Выберите жанры"
        }
        builder.setNegativeButton("Отмена", null)
        builder.show()
    }
    private fun addUrlField() {
        if (editTextUrls.size >= 7) return

        val editText = EditText(this).apply {
            hint = "URL изображения ${editTextUrls.size + 1}"
            setSingleLine(true)
            setPadding(16, 16, 16, 16)
            setTextColor(resources.getColor(R.color.white, theme))
            setHintTextColor(resources.getColor(R.color.white, theme))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty() && editTextUrls.size < 7) {
                    if (editText == editTextUrls.last()) {
                        addUrlField()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        editTextUrls.add(editText)
        imageUrlsContainer.addView(editText)
    }

    private fun addNewDisc() {
        val name = findViewById<EditText>(R.id.editTextName).text.toString().trim()
        val priceText = findViewById<EditText>(R.id.editTextPrice).text.toString().trim()
        val description = findViewById<EditText>(R.id.editTextDescription).text.toString().trim()
        val ageText = findViewById<EditText>(R.id.editTextAge).text.toString().trim()

        if (name.isEmpty() || priceText.isEmpty() || description.isEmpty() || ageText.isEmpty() || selectedGenres.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceText.toDoubleOrNull()
        val age = ageText.toIntOrNull()
        if (price == null || age == null) {
            Toast.makeText(this, "Введите корректные числовые значения", Toast.LENGTH_SHORT).show()
            return
        }

        val imageUrls = editTextUrls.mapNotNull { it.text.toString().trim().takeIf { it.isNotEmpty() } }

        val newDisc = hashMapOf(
            "name" to name,
            "price" to price,
            "description" to description,
            "age" to age,
            "genre" to selectedGenres.joinToString(", ")

        )

        db.collection("discs").add(newDisc)
            .addOnSuccessListener { documentReference ->
                val discId = documentReference.id
                db.collection("discs").document(discId).update("id", discId)

                val imagesCollection = db.collection("discs").document(discId).collection("imageURLS")
                imageUrls.forEachIndexed { index, url ->
                    imagesCollection.document((index + 1).toString()).set(mapOf("url" to url))
                }


                Toast.makeText(this, "Диск добавлен!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка добавления", Toast.LENGTH_SHORT).show()
            }
    }
}
