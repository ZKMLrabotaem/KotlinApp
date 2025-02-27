package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class EditDiscActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextPrice: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var editTextAge: EditText
    private lateinit var textViewGenres: TextView
    private val selectedGenres = mutableListOf<String>()
    private val genres = arrayOf("Экшен", "РПГ", "Шутер", "Хоррор", "Гонки", "Приключения", "Стратегия", "Спортивные", "Файтинг", "Слэшер")
    private val selectedGenresChecked = BooleanArray(genres.size)
    private lateinit var buttonSave: Button
    private lateinit var discId: String
    private val db = FirebaseFirestore.getInstance()

    private lateinit var imageUrlsContainer: LinearLayout
    private val editTextUrls = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_disc)

        editTextName = findViewById(R.id.editTextName)
        editTextPrice = findViewById(R.id.editTextPrice)
        editTextDescription = findViewById(R.id.editTextDescription)
        editTextAge = findViewById(R.id.editTextAge)
        textViewGenres = findViewById(R.id.textViewGenres)
        buttonSave = findViewById(R.id.buttonSave)
        imageUrlsContainer = findViewById(R.id.imageUrlsContainer)

        discId = intent.getStringExtra("disc_id") ?: ""

        if (discId.isNotEmpty()) {
            loadDiscData()
            loadImageUrls()
        } else {
            Toast.makeText(this, "Ошибка: отсутствует ID диска", Toast.LENGTH_SHORT).show()
            finish()
        }

        buttonSave.setOnClickListener {
            saveDiscChanges()
        }

        textViewGenres.setOnClickListener {
            showGenreSelectionDialog()
        }
        val fabBack = findViewById<FloatingActionButton>(R.id.fabBack)
        fabBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
    private fun loadDiscData() {
        db.collection("discs").document(discId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    editTextName.setText(document.getString("name") ?: "")
                    editTextPrice.setText(document.getDouble("price")?.toString() ?: "")
                    editTextDescription.setText(document.getString("description") ?: "")
                    editTextAge.setText(document.getLong("age")?.toString() ?: "")

                    val genresString = document.getString("genre") ?: ""
                    selectedGenres.clear()
                    selectedGenres.addAll(genresString.split(", ").map { it.trim() }.filter { it.isNotEmpty() })

                    for (i in genres.indices) {
                        selectedGenresChecked[i] = genres[i] in selectedGenres
                    }

                    textViewGenres.text = if (selectedGenres.isNotEmpty()) genresString else "Выберите жанры"
                } else {
                    Toast.makeText(this, "Диск не найден", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                finish()
            }
    }




    private fun loadImageUrls() {
        db.collection("discs").document(discId).collection("imageURLS")
            .get()
            .addOnSuccessListener { snapshot ->
                val existingUrls = mutableListOf<String>()
                snapshot.documents.forEach { document ->
                    val url = document.getString("url")
                    url?.let { existingUrls.add(it) }
                }

                existingUrls.forEach { url ->
                    addUrlField(url)
                }

            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка загрузки изображений", Toast.LENGTH_SHORT).show()
            }
    }


    private fun addUrlField(url: String? = null) {
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
        url?.let { editText.setText(it) }

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




    private fun saveDiscChanges() {
        val name = editTextName.text.toString().trim()
        val price = editTextPrice.text.toString().toDoubleOrNull()
        val description = editTextDescription.text.toString().trim()
        val age = editTextAge.text.toString().toIntOrNull()

        if (name.isEmpty() || price == null || description.isEmpty() || age == null || selectedGenres.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        val newDisc = hashMapOf(
            "name" to name,
            "price" to price,
            "description" to description,
            "age" to age,
            "genre" to selectedGenres.joinToString(", ")
        ) as Map<String, Any>

        db.collection("discs").document(discId).update(newDisc)
            .addOnSuccessListener {
                val imagesCollection = db.collection("discs").document(discId).collection("imageURLS")

                imagesCollection.get().addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc -> doc.reference.delete() }

                    val imageUrls = editTextUrls.mapNotNull { it.text.toString().trim() }.filter { it.isNotEmpty() }

                    imageUrls.forEachIndexed { index, url ->
                        imagesCollection.document((index + 1).toString()).set(mapOf("url" to url))
                    }
                }

                Toast.makeText(this, "Диск обновлен", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка обновления", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showGenreSelectionDialog() {
        val builder = AlertDialog.Builder(this, R.style.DarkAlertDialog)
        builder.setTitle("Выберите жанры")
        builder.setMultiChoiceItems(genres, selectedGenresChecked) { _, index, isChecked ->
            if (isChecked) {
                if (!selectedGenres.contains(genres[index])) {
                    selectedGenres.add(genres[index])
                }
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

}
