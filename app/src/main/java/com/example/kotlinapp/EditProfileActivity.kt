package com.example.kotlinapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var avatarEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var birthdateTextView: TextView
    private lateinit var telephoneEditText: EditText
    private lateinit var countrySpinner: Spinner
    private lateinit var platformSpinner: Spinner
    private lateinit var descriptionEditText: EditText
    private lateinit var favoriteGenresEditText: EditText
    private lateinit var dislikedGenresEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button
    private val genresList = arrayOf("Экшен", "РПГ", "Шутер", "Хоррор", "Гонки", "Приключения", "Стратегия", "Спортивные", "Файтинг", "Слэшер")
    private val selectedFavoriteGenres = mutableListOf<String>()
    private val selectedDislikedGenres = mutableListOf<String>()
    val countries = Locale.getISOCountries().map { code ->
        Locale("", code).displayCountry
    }.sorted().toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        avatarEditText = findViewById(R.id.avatarEditText)
        nameEditText = findViewById(R.id.nameEditText)
        genderSpinner = findViewById(R.id.genderSpinner)
        birthdateTextView = findViewById(R.id.birthdateTextView)
        telephoneEditText = findViewById(R.id.telephoneEditText)
        countrySpinner = findViewById(R.id.countrySpinner)
        platformSpinner = findViewById(R.id.platformSpinner)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        favoriteGenresEditText = findViewById(R.id.favoriteGenresEditText)
        dislikedGenresEditText = findViewById(R.id.dislikedGenresEditText)
        saveButton = findViewById(R.id.saveButton)
        favoriteGenresEditText.setOnClickListener {
            showMultiChoiceDialog("Выберите любимые жанры", genresList, selectedFavoriteGenres, favoriteGenresEditText)
        }

        dislikedGenresEditText.setOnClickListener {
            showMultiChoiceDialog("Выберите нелюбимые жанры", genresList, selectedDislikedGenres, dislikedGenresEditText)
        }
        val fabBack = findViewById<FloatingActionButton>(R.id.fabBack)

        fabBack.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        deleteButton = findViewById(R.id.deleteButton)
        deleteButton.setOnClickListener {
            confirmDeleteProfile()
        }
        val adapter3 = ArrayAdapter(this, R.layout.spinner_item, countries)
        adapter3.setDropDownViewResource(R.layout.spinner_item)
        countrySpinner.adapter = adapter3
        val genderOptions = arrayOf("Не указано","Мужской", "Женский")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, genderOptions)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        genderSpinner.adapter = adapter

        val platformOptions = arrayOf("Не указано","PlayStation", "Xbox", "PC")
        val adapter2 = ArrayAdapter(this, R.layout.spinner_item, platformOptions)
        adapter2.setDropDownViewResource(R.layout.spinner_item)
        platformSpinner.adapter = adapter2
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        avatarEditText.setText(document.getString("avatar"))
                        nameEditText.setText(document.getString("name"))
                        val country = document.getString("country") ?: "Не указано"
                        countrySpinner.setSelection(countries.indexOf(country))
                        telephoneEditText.setText(document.getString("phone"))
                        descriptionEditText.setText(document.getString("description"))
                        birthdateTextView.text = document.getString("birthdate")
                        favoriteGenresEditText.setText((document.get("favoriteGenres") as? List<*>)?.joinToString(", "))
                        dislikedGenresEditText.setText((document.get("dislikedGenres") as? List<*>)?.joinToString(", "))

                        val platform = document.getString("platform")
                        platformSpinner.setSelection(platformOptions.indexOf(platform ?: "Не указано"))
                        val gender = document.getString("gender")
                        genderSpinner.setSelection(genderOptions.indexOf(gender ?: "Не указано"))
                    }
                }
                .addOnFailureListener { e -> showToast("Ошибка загрузки профиля: ${e.message}") }
        }

        birthdateTextView.setOnClickListener {
            showDatePicker()
        }

        saveButton.setOnClickListener {
            saveProfileData()
        }
    }

    private fun confirmDeleteProfile() {
        val alertDialog = AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Удаление профиля")
            .setMessage("Вы уверены, что хотите удалить свой профиль? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ -> showPasswordDialog() }
            .setNegativeButton("Отмена", null)
            .create()

        alertDialog.show()
    }

    private fun showPasswordDialog() {
        val builder = AlertDialog.Builder(this, R.style.DarkAlertDialog)
        builder.setTitle("Введите пароль для удаления аккаунта")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.setTextColor(resources.getColor(android.R.color.white, theme))
        input.setHintTextColor(resources.getColor(android.R.color.darker_gray, theme))

        builder.setView(input)

        builder.setPositiveButton("Подтвердить") { _, _ ->
            val password = input.text.toString()
            if (password.isNotEmpty()) {
                reauthenticateAndDelete(password)
            } else {
                showToast("Пароль не может быть пустым")
            }
        }
        builder.setNegativeButton("Отмена") { dialog, _ -> dialog.cancel() }

        builder.show()
    }


    private fun reauthenticateAndDelete(password: String) {
        val user = auth.currentUser ?: return

        val credential = EmailAuthProvider.getCredential(user.email!!, password)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                deleteProfile()
            }
            .addOnFailureListener { e ->
                showToast("Ошибка аутентификации: ${e.message}")
            }
    }

    private fun deleteProfile() {
        val user = auth.currentUser ?: return

        db.collection("users").document(user.uid).delete()
            .addOnSuccessListener {
                user.delete()
                    .addOnSuccessListener {
                        showToast("Профиль удалён")
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e -> showToast("Ошибка удаления аккаунта: ${e.message}") }
            }
            .addOnFailureListener { e -> showToast("Ошибка удаления профиля: ${e.message}") }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            R.style.CustomDatePickerDialog,
            { _, selectedYear, selectedMonth, selectedDay ->
                birthdateTextView.text = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            },
            year, month, day
        )
        datePicker.show()
    }
    private fun showMultiChoiceDialog(title: String, genres: Array<String>, selectedGenres: MutableList<String>, targetEditText: EditText) {
        val selectedItems = BooleanArray(genres.size) { selectedGenres.contains(genres[it]) }

        val builder = AlertDialog.Builder(this, R.style.DarkAlertDialog)
        builder.setTitle(title)
        builder.setMultiChoiceItems(genres, selectedItems) { _, which, isChecked ->
            if (isChecked) {
                selectedGenres.add(genres[which])
            } else {
                selectedGenres.remove(genres[which])
            }
        }

        builder.setPositiveButton("ОК") { _, _ ->
            targetEditText.setText(selectedGenres.joinToString(", "))
        }
        builder.setNegativeButton("Отмена", null)
        builder.show()
    }

    private fun saveProfileData() {
        val user = auth.currentUser ?: return

        val updatedProfile = mapOf(
            "avatar" to avatarEditText.text.toString().trim(),
            "name" to nameEditText.text.toString().trim(),
            "gender" to genderSpinner.selectedItem.toString(),
            "birthdate" to birthdateTextView.text.toString().trim(),
            "phone" to telephoneEditText.text.toString(),
            "country" to countrySpinner.selectedItem.toString(),
            "platform" to platformSpinner.selectedItem.toString(),
            "description" to descriptionEditText.text.toString().trim(),
            "favoriteGenres" to selectedFavoriteGenres,
            "dislikedGenres" to selectedDislikedGenres
        )

        db.collection("users").document(user.uid).update(updatedProfile)
            .addOnSuccessListener {
                showToast("Профиль обновлён")

                val intent = Intent(this, ProfileActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e -> showToast("Ошибка сохранения: ${e.message}") }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
