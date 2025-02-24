package com.example.kotlinapp

import ImageSliderAdapter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.kotlinapp.R

class DiscDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disc_detail)

        val name = intent.getStringExtra("disc_name")
        val description = intent.getStringExtra("disc_description")
        val price = intent.getDoubleExtra("disc_price", 0.0)
        val genre = intent.getStringExtra("disc_genre")
        val age = intent.getLongExtra("disc_age", 0)
        val imageUrls = intent.getStringArrayListExtra("disc_images") ?: arrayListOf()

        findViewById<TextView>(R.id.nameTextView).text = name
        findViewById<TextView>(R.id.descriptionTextView).text = description
        findViewById<TextView>(R.id.priceTextView).text = "Цена: $price руб."
        findViewById<TextView>(R.id.genreTextView).text = "Жанр: $genre"
        findViewById<TextView>(R.id.ageTextView).text = "Возраст: $age"

        val imageSlider = findViewById<ViewPager2>(R.id.imageSlider)
        imageSlider.adapter = ImageSliderAdapter(imageUrls)
    }
}
