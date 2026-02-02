package com.pab.ecommerce_katalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pab.ecommerce_katalog.model.Order
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class OrderAdapter(private val orderList: List<Order>) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orderList[position])
    }

    override fun getItemCount(): Int = orderList.size

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderId: TextView = itemView.findViewById(R.id.tv_order_id)
        private val tvOrderDate: TextView = itemView.findViewById(R.id.tv_order_date)
        private val tvOrderStatus: TextView = itemView.findViewById(R.id.tv_order_status)
        private val ivOrderProduct: ImageView = itemView.findViewById(R.id.iv_order_product)
        private val tvOrderItems: TextView = itemView.findViewById(R.id.tv_order_items)
        private val tvVariantInfo: TextView = itemView.findViewById(R.id.tv_product_variant)
        private val tvOrderQty: TextView = itemView.findViewById(R.id.tv_order_qty)
        private val tvMoreItems: TextView = itemView.findViewById(R.id.tv_more_items)
        private val tvOrderTotal: TextView = itemView.findViewById(R.id.tv_order_total)

        fun bind(order: Order) {
            tvOrderId.text = "ORD-${order.id?.takeLast(8)?.uppercase() ?: "UNKNOWN"}"
            
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            tvOrderDate.text = order.timestamp?.let { sdf.format(it) } ?: "No date"

            tvOrderStatus.text = order.status.uppercase()
            
            // Atur warna status lebih profesional
            when (order.status.lowercase()) {
                "accepted" -> {
                    tvOrderStatus.setTextColor(0xFF2E7D32.toInt()) // Deep Green
                    tvOrderStatus.background.setTint(0xFFE8F5E9.toInt()) 
                }
                "pending" -> {
                    tvOrderStatus.setTextColor(0xFFEF6C00.toInt()) // Deep Orange
                    tvOrderStatus.background.setTint(0xFFFFF3E0.toInt())
                }
                "rejected" -> {
                    tvOrderStatus.setTextColor(0xFFC62828.toInt()) // Deep Red
                    tvOrderStatus.background.setTint(0xFFFFEBEE.toInt())
                }
            }

            if (order.products.isNotEmpty()) {
                val firstProduct = order.products[0]
                tvOrderItems.text = firstProduct.productName
                tvOrderQty.text = "${firstProduct.quantity} barang"
                
                // Set Variant (Size & Color)
                val variantText = "Size: ${firstProduct.size} | Color: ${firstProduct.color}"
                tvVariantInfo.text = variantText

                Glide.with(itemView.context)
                    .load(firstProduct.imagePath)
                    .placeholder(R.drawable.ic_product)
                    .error(R.drawable.ic_product)
                    .into(ivOrderProduct)

                if (order.products.size > 1) {
                    tvMoreItems.visibility = View.VISIBLE
                    tvMoreItems.text = "+ ${order.products.size - 1} produk lainnya"
                } else {
                    tvMoreItems.visibility = View.GONE
                }
            }

            val localeID = Locale.forLanguageTag("in-ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            tvOrderTotal.text = numberFormat.format(order.totalHarga)
        }
    }
}
