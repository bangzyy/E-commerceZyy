package com.pab.ecommerce_katalog.ui.overview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.pab.ecommerce_katalog.R
import com.pab.ecommerce_katalog.model.Order
import com.pab.ecommerce_katalog.model.OrderItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AcceptedProductsFragment : Fragment() {

    private lateinit var rvAcceptedProducts: RecyclerView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var spinnerMonth: Spinner
    
    private val allAcceptedItems = mutableListOf<Triple<Order, OrderItem, Calendar>>()
    private val displayItems = mutableListOf<Triple<Order, OrderItem, Calendar>>()

    private val months = arrayOf(
        "Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_accepted_products, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        rvAcceptedProducts = view.findViewById(R.id.rv_accepted_products)
        rvAcceptedProducts.layoutManager = LinearLayoutManager(context)
        
        spinnerMonth = view.findViewById(R.id.spinner_month)
        setupMonthSpinner()
        
        loadAcceptedProducts()
    }

    private fun setupMonthSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = adapter

        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterByMonth(position) // 0 is All, 1 is Jan, etc.
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadAcceptedProducts() {
        firestore.collection("orders")
            .get()
            .addOnSuccessListener { documents ->
                allAcceptedItems.clear()
                
                for (document in documents) {
                    val status = document.getString("status") ?: ""
                    if (status.equals("Accepted", ignoreCase = true)) {
                        val order = document.toObject(Order::class.java)
                        if (order != null && order.timestamp != null) {
                            val calendar = Calendar.getInstance()
                            calendar.time = order.timestamp!!
                            
                            for (item in order.products) {
                                allAcceptedItems.add(Triple(order, item, calendar))
                            }
                        }
                    }
                }
                // Sort by date descending
                allAcceptedItems.sortByDescending { it.third.timeInMillis }
                filterByMonth(spinnerMonth.selectedItemPosition)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterByMonth(monthIndex: Int) {
        displayItems.clear()
        if (monthIndex == 0) {
            displayItems.addAll(allAcceptedItems)
        } else {
            val selectedMonth = monthIndex - 1 // Calendar.JANUARY is 0
            for (item in allAcceptedItems) {
                if (item.third.get(Calendar.MONTH) == selectedMonth) {
                    displayItems.add(item)
                }
            }
        }
        rvAcceptedProducts.adapter = AcceptedProductsAdapter(displayItems)
    }

    class AcceptedProductsAdapter(private val items: List<Triple<Order, OrderItem, Calendar>>) :
        RecyclerView.Adapter<AcceptedProductsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_accepted_product, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (order, item, calendar) = items[position]
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            
            holder.tvBuyerName.text = "Pembeli: ${order.userName ?: "User"}"
            holder.tvOrderDate.text = sdf.format(calendar.time)
            holder.tvProductName.text = item.productName
            holder.tvVariantInfo.text = "Size: ${item.size} | Color: ${item.color}"
            holder.tvQuantity.text = "x${item.quantity}"
            
            val localeID = Locale.forLanguageTag("in-ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            holder.tvTotalPrice.text = numberFormat.format(item.productPrice * item.quantity)

            Glide.with(holder.itemView.context)
                .load(item.imagePath)
                .placeholder(R.drawable.ic_product)
                .into(holder.ivProduct)
        }

        override fun getItemCount() = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvBuyerName: TextView = view.findViewById(R.id.tv_buyer_name)
            val tvOrderDate: TextView = view.findViewById(R.id.tv_order_date)
            val ivProduct: ImageView = view.findViewById(R.id.iv_product_image)
            val tvProductName: TextView = view.findViewById(R.id.tv_product_name)
            val tvVariantInfo: TextView = view.findViewById(R.id.tv_variant_info)
            val tvQuantity: TextView = view.findViewById(R.id.tv_quantity)
            val tvTotalPrice: TextView = view.findViewById(R.id.tv_total_price)
        }
    }
}
