package com.example.kotlinapp

import GameDisc
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val discs = mutableListOf<GameDisc>()

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
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = DiscAdapter()
        val fabBack = findViewById<FloatingActionButton>(R.id.fabBack)
        fabBack.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        fetchDiscs()
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
                            recyclerView.adapter?.notifyDataSetChanged()

                            Log.d("MainActivity", "Загружены изображения: $imageUrls")
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





    inner class DiscAdapter : RecyclerView.Adapter<DiscAdapter.DiscViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game_disc, parent, false)
            return DiscViewHolder(view)
        }

        override fun onBindViewHolder(holder: DiscViewHolder, position: Int) {
            val disc = discs[position]
            holder.nameTextView.text = disc.name
            holder.descriptionTextView.text = disc.genre
            holder.priceTextView.text = "Цена: ${disc.price} руб."

            Glide.with(holder.itemView.context)
                .load(disc.imageUrls.firstOrNull())
                .into(holder.imageView)

            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, DiscDetailActivity::class.java).apply {
                    putExtra("disc_name", disc.name)
                    putExtra("disc_description", disc.description)
                    putExtra("disc_price", disc.price)
                    putExtra("disc_genre", disc.genre)
                    putExtra("disc_age", disc.age)
                    putStringArrayListExtra("disc_images", ArrayList(disc.imageUrls))
                }
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = discs.size

        inner class DiscViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
            val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
            val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
            val imageView: ImageView = itemView.findViewById(R.id.imageView)
        }
    }


}
