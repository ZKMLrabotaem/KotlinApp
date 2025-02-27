package com.example.kotlinapp

import GameDisc
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var genreSpinner: Spinner
    private lateinit var priceSpinner: Spinner

    private val discs = mutableListOf<GameDisc>()
    private var filteredDiscs = mutableListOf<GameDisc>()
    private lateinit var fabAdd: FloatingActionButton
    private var userRole: String = "user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.discRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        genreSpinner = findViewById(R.id.genreSpinner)
        priceSpinner = findViewById(R.id.priceSpinner)
        fabAdd = findViewById(R.id.fabAdd)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = DiscAdapter()

        val fabBack = findViewById<FloatingActionButton>(R.id.fabBack)
        fabBack.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        fabAdd.setOnClickListener {
            startActivity(Intent(this, AddDiscActivity::class.java))
        }

        fabAdd.visibility = View.GONE

        checkUserRole()
        setupSpinners()
        setupSearch()
        fetchDiscs()
    }

    private fun checkUserRole() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    userRole = document.getString("role") ?: "user"
                    if (userRole == "admin") {
                        fabAdd.visibility = View.VISIBLE
                    }
                }
            }
            .addOnFailureListener {
                Log.e("MainActivity", "Ошибка получения роли пользователя")
            }
    }


    private fun setupSpinners() {
        val genres = listOf("Все жанры", "Экшен", "РПГ", "Шутер", "Хоррор", "Гонки", "Приключения", "Стратегия", "Спортивные", "Файтинг", "Слэшер")
        val priceOptions = listOf("Цена", "Дешевые сверху", "Дорогие сверху")

        val genreAdapter = ArrayAdapter(this, R.layout.spinner_item, genres)
        genreAdapter.setDropDownViewResource(R.layout.spinner_item)
        genreSpinner.adapter = genreAdapter

        val priceAdapter = ArrayAdapter(this, R.layout.spinner_item, priceOptions)
        priceAdapter.setDropDownViewResource(R.layout.spinner_item)
        priceSpinner.adapter = priceAdapter

        genreSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        priceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                applyFilters()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun fetchDiscs() {
        val db = FirebaseFirestore.getInstance()
        db.collection("discs").get()
            .addOnSuccessListener { documents ->
                discs.clear()
                for (document in documents) {
                    val disc = document.toObject(GameDisc::class.java)
                    val discId = document.id

                    db.collection("discs").document(discId).collection("imageURLS")
                        .get()
                        .addOnSuccessListener { imageDocs ->
                            val imageUrls = imageDocs.mapNotNull { it.getString("url") }
                            val updatedDisc = disc.copy(imageUrls = imageUrls)

                            discs.add(updatedDisc)
                            applyFilters()
                        }
                        .addOnFailureListener { e ->
                            Log.e("MainActivity", "Ошибка загрузки изображений", e)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка при загрузке данных: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilters() {
        val query = searchEditText.text.toString().lowercase()
        val selectedGenre = genreSpinner.selectedItem.toString()
        val selectedPrice = priceSpinner.selectedItem.toString()

        filteredDiscs = discs.filter { disc ->
            (query.isEmpty() || disc.name.lowercase().contains(query)) &&
                    (selectedGenre == "Все жанры" || disc.genre.contains(selectedGenre))
        }.toMutableList()
        filteredDiscs.sortBy { it.name.lowercase() }
        if (selectedPrice == "Дешевые сверху") {
            filteredDiscs.sortBy { it.price }
        } else if (selectedPrice == "Дорогие сверху") {
            filteredDiscs.sortByDescending { it.price }
        }

        recyclerView.adapter?.notifyDataSetChanged()
    }

    inner class DiscAdapter : RecyclerView.Adapter<DiscAdapter.DiscViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game_disc, parent, false)
            return DiscViewHolder(view)
        }

        override fun onBindViewHolder(holder: DiscViewHolder, position: Int) {
            val disc = filteredDiscs[position]
            holder.nameTextView.text = disc.name
            holder.priceTextView.text = "Цена: ${disc.price} руб."

            Glide.with(holder.itemView.context)
                .load(disc.imageUrls.firstOrNull())
                .into(holder.imageView)

            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, DiscDetailActivity::class.java).apply {
                    putExtra("disc_name", disc.name)
                    putExtra("disc_description", disc.description)
                    putExtra("disc_id", disc.id)
                    putExtra("disc_price", disc.price)
                    putExtra("disc_genre", disc.genre)
                    putExtra("disc_age", disc.age)
                    putStringArrayListExtra("disc_images", ArrayList(disc.imageUrls))
                }
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = filteredDiscs.size

        inner class DiscViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
            val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
            val imageView: ImageView = itemView.findViewById(R.id.imageView)
        }
    }
}
