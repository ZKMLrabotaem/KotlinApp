package com.example.kotlinapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

data class Comment(
    val userId: String = "",
    var username: String = "",
    var userAvatar: String = "",
    val grade: Int = 0,
    val text: String = ""
)

class CommentAdapter(private var comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]

        holder.ratingBar.rating = comment.grade.toFloat()

        if (comment.text.isNotEmpty()) {
            holder.commentTextView.text = comment.text
            holder.commentTextView.visibility = View.VISIBLE
        } else {
            holder.commentTextView.visibility = View.GONE
        }

        if (comment.username.isEmpty()) {
            fetchUserData(comment.userId) { username, avatarUrl ->
                comment.username = username
                comment.userAvatar = avatarUrl
                holder.usernameTextView.text = username
                Glide.with(holder.itemView.context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.avatarImageView)
            }
        } else {
            holder.usernameTextView.text = comment.username
            Glide.with(holder.itemView.context)
                .load(comment.userAvatar)
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.avatarImageView)
        }
    }




    override fun getItemCount(): Int = comments.size

    private fun fetchUserData(userId: String, callback: (String, String) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("name") ?: "Unknown"
                    val avatarUrl = document.getString("avatar") ?: ""
                    callback(username, avatarUrl)
                }
            }
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarImageView: ImageView = itemView.findViewById(R.id.avatarImageView)
        val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBarComment)
        val commentTextView: TextView = itemView.findViewById(R.id.commentTextView)
    }
}
