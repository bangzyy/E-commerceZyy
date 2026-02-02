package com.pab.ecommerce_katalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private var productList: List<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.imgProduct)
        val productName: TextView = itemView.findViewById(R.id.txtName)
        val productPrice: TextView = itemView.findViewById(R.id.txtPrice)
        val productRating: TextView = itemView.findViewById(R.id.txtRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        holder.productName.text = product.name

        val localeID = Locale.forLanguageTag("in-ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        numberFormat.maximumFractionDigits = 0
        holder.productPrice.text = numberFormat.format(product.price)

        // Memformat rating agar hanya muncul 1 angka di belakang koma (misal: 4.3)
        val formattedRating = String.format("%.1f", product.rating)
        holder.productRating.text = "$formattedRating | ${product.reviews} reviews"

        Glide.with(holder.itemView.context)
            .load(product.imagePath)
            .into(holder.productImage)

        holder.itemView.setOnClickListener { onItemClick(product) }
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    fun updateProducts(newProducts: List<Product>) {
        productList = newProducts
        notifyDataSetChanged()
    }
}
