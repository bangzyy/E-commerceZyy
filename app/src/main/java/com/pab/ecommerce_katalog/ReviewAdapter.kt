package com.pab.ecommerce_katalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pab.ecommerce_katalog.model.Review
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewAdapter(private val reviews: List<Review>) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.bind(review)
    }

    override fun getItemCount(): Int = reviews.size

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_reviewer_name)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_review_date)
        private val tvComment: TextView = itemView.findViewById(R.id.tv_comment)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.rating_bar_small)

        fun bind(review: Review) {
            tvName.text = review.userName
            tvComment.text = review.comment
            ratingBar.rating = review.rating
            
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            tvDate.text = review.timestamp?.let { sdf.format(it) } ?: ""
        }
    }
}
