package com.pab.ecommerce_katalog

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pab.ecommerce_katalog.model.Order
import com.pab.ecommerce_katalog.model.OrderContentItem
import com.pab.ecommerce_katalog.model.OrderDisplayItem
import com.pab.ecommerce_katalog.model.UserHeaderItem
import java.text.NumberFormat
import java.util.Locale

class OrderAdminAdapter(
    private var displayItems: List<OrderDisplayItem>,
    private val onAcceptClick: (Order) -> Unit,
    private val onRejectClick: (Order) -> Unit,
    private val onHeaderClick: (UserHeaderItem, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTENT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayItems[position]) {
            is UserHeaderItem -> TYPE_HEADER
            is OrderContentItem -> TYPE_CONTENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_user_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_CONTENT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_admin, parent, false)
                ContentViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val headerItem = displayItems[position] as UserHeaderItem
                holder.bind(headerItem)
                holder.itemView.setOnClickListener {
                    onHeaderClick(headerItem, position)
                }
            }
            is ContentViewHolder -> {
                val contentItem = displayItems[position] as OrderContentItem
                holder.bind(contentItem.order)
            }
        }
    }

    override fun getItemCount(): Int = displayItems.size

    // ViewHolder for User Header
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tv_user_name_header)
        private val tvOrderCount: TextView = itemView.findViewById(R.id.tv_order_count)
        private val ivExpandArrow: ImageView = itemView.findViewById(R.id.iv_expand_arrow)

        fun bind(header: UserHeaderItem) {
            tvUserName.text = header.userName
            tvOrderCount.text = "${header.orderCount} Pesanan"
            ivExpandArrow.rotation = if (header.isExpanded) 180f else 0f
        }
    }

    // ViewHolder for Order Content
    inner class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderId: TextView = itemView.findViewById(R.id.tv_order_id)
        private val tvOrderStatus: TextView = itemView.findViewById(R.id.tv_order_status)
        private val tvOrderUsername: TextView = itemView.findViewById(R.id.tv_order_username)
        private val tvOrderDetails: TextView = itemView.findViewById(R.id.tv_order_details)
        private val tvOrderPhone: TextView = itemView.findViewById(R.id.tv_order_phone)
        private val tvOrderAddress: TextView = itemView.findViewById(R.id.tv_order_address)
        private val tvOrderTotal: TextView = itemView.findViewById(R.id.tv_order_total)
        private val btnAccept: Button = itemView.findViewById(R.id.btn_accept)
        private val btnReject: Button = itemView.findViewById(R.id.btn_reject)
        private val layoutAdminActions: View = itemView.findViewById(R.id.layout_admin_actions)
        private val ivProductImage: ImageView = itemView.findViewById(R.id.iv_product_image)

        fun bind(order: Order) {
            tvOrderId.text = "ID: ${order.id}"
            tvOrderUsername.text = order.userName // Username is now here
            tvOrderPhone.text = order.phone
            tvOrderAddress.text = order.alamat

            // Set Status and Color
            tvOrderStatus.text = order.status
            val statusBackground = tvOrderStatus.background as GradientDrawable
            statusBackground.setColor(when (order.status) {
                "Accepted" -> Color.parseColor("#4CAF50")
                "Rejected" -> Color.parseColor("#F44336")
                else -> Color.parseColor("#FF9800")
            })

            val detailsBuilder = StringBuilder()
            order.products?.forEachIndexed { index, it ->
                detailsBuilder.append("- ${it.quantity}x ${it.productName} (${it.size}, ${it.color})\n")
                if (index == 0) { 
                    Glide.with(itemView.context)
                        .load(it.imagePath)
                        .placeholder(R.drawable.ic_placeholder)
                        .into(ivProductImage)
                }
            }
            tvOrderDetails.text = detailsBuilder.toString().trim()

            val localeID = Locale.forLanguageTag("in-ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            tvOrderTotal.text = "Total: ${numberFormat.format(order.totalHarga)}"

            if (order.status == "Pending") {
                layoutAdminActions.visibility = View.VISIBLE
                btnAccept.setOnClickListener { onAcceptClick(order) }
                btnReject.setOnClickListener { onRejectClick(order) }
            } else {
                layoutAdminActions.visibility = View.GONE
            }
        }
    }
}
