package com.pab.ecommerce_katalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pab.ecommerce_katalog.model.CartItem
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private val cartItems: List<CartItem>, // Changed to val, list is now read-only from adapter's perspective
    private val onQuantityChange: (CartItem, Int) -> Unit,
    private val onDelete: (CartItem) -> Unit,
    private val onSelectionChange: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = cartItems.size

    // The updateItems function is removed for a simpler data flow.
    // The Fragment will now be responsible for modifying the list and calling notifyDataSetChanged().

    fun getSelectedItems(): List<CartItem> {
        return cartItems.filter { it.isSelected }
    }

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbSelectItem: CheckBox = itemView.findViewById(R.id.cb_select_item)
        private val ivProductImage: ImageView = itemView.findViewById(R.id.iv_product_image)
        private val tvProductName: TextView = itemView.findViewById(R.id.tv_product_name)
        private val tvProductSize: TextView = itemView.findViewById(R.id.tv_product_size)
        private val tvProductColor: TextView = itemView.findViewById(R.id.tv_product_color)
        private val tvTotalPrice: TextView = itemView.findViewById(R.id.tv_total_price)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tv_quantity)
        private val ivMinus: ImageView = itemView.findViewById(R.id.iv_minus)
        private val ivPlus: ImageView = itemView.findViewById(R.id.iv_plus)
        private val ivDelete: ImageView = itemView.findViewById(R.id.iv_delete)

        fun bind(item: CartItem) {
            tvProductName.text = item.productName
            tvProductSize.text = item.size
            tvProductColor.text = item.color
            updateQuantityAndPrice(item)

            Glide.with(itemView.context)
                .load(item.productImagePath)
                .into(ivProductImage)

            cbSelectItem.isChecked = item.isSelected
            cbSelectItem.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
                onSelectionChange()
            }

            ivMinus.setOnClickListener {
                if (item.quantity > 1) {
                    val newQuantity = item.quantity - 1
                    item.quantity = newQuantity // Update local item
                    updateQuantityAndPrice(item)
                    onQuantityChange(item, newQuantity)
                }
            }

            ivPlus.setOnClickListener {
                val newQuantity = item.quantity + 1
                item.quantity = newQuantity // Update local item
                updateQuantityAndPrice(item)
                onQuantityChange(item, newQuantity)
            }

            ivDelete.setOnClickListener {
                onDelete(item)
            }
        }
        
        private fun updateQuantityAndPrice(item: CartItem) {
            tvQuantity.text = item.quantity.toString()
            val totalPrice = item.productPrice * item.quantity
            val localeID = Locale.forLanguageTag("in-ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            tvTotalPrice.text = numberFormat.format(totalPrice)
        }
    }
}
