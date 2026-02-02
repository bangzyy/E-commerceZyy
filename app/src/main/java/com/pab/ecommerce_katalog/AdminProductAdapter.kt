package com.pab.ecommerce_katalog

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class AdminProductAdapter(
    private var productList: List<Product>,
    private val context: Context,
    private val dbHelper: DatabaseHelper,
    private val onProductDeleted: () -> Unit
) : RecyclerView.Adapter<AdminProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_admin, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int = productList.size

    fun updateProducts(newProducts: List<Product>) {
        productList = newProducts
        notifyDataSetChanged()
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProductImage: ImageView = itemView.findViewById(R.id.iv_product_image)
        private val tvProductName: TextView = itemView.findViewById(R.id.tv_product_name)
        private val tvProductPrice: TextView = itemView.findViewById(R.id.tv_product_price)
        private val btnDetail: Button = itemView.findViewById(R.id.btn_detail)
        private val btnEdit: Button = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: Button = itemView.findViewById(R.id.btn_delete)

        fun bind(product: Product) {
            tvProductName.text = product.name
            
            val localeID = Locale.forLanguageTag("in-ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            tvProductPrice.text = numberFormat.format(product.price)

            Glide.with(itemView.context)
                .load(product.imagePath)
                .placeholder(R.drawable.ic_product)
                .into(ivProductImage)

            btnDetail.setOnClickListener {
                val intent = Intent(context, AdminProductDetailActivity::class.java).apply {
                    putExtra(AdminProductDetailActivity.EXTRA_PRODUCT, product)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }

            btnEdit.setOnClickListener {
                val intent = Intent(context, AdminAddEditProductActivity::class.java).apply {
                    putExtra(AdminAddEditProductActivity.EXTRA_PRODUCT_ID, product.id)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }

            btnDelete.setOnClickListener {
                android.app.AlertDialog.Builder(context)
                    .setTitle("Hapus Produk")
                    .setMessage("Apakah Anda yakin ingin menghapus produk ini?")
                    .setPositiveButton("Ya") { _, _ ->
                        dbHelper.deleteProduct(product.id)
                        onProductDeleted()
                        Toast.makeText(context, "Produk dihapus", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }
    }
}
