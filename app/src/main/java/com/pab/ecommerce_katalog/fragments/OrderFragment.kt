package com.pab.ecommerce_katalog.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pab.ecommerce_katalog.OrderAdapter
import com.pab.ecommerce_katalog.R
import com.pab.ecommerce_katalog.model.Order

class OrderFragment : Fragment() {

    private lateinit var rvOrders: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoOrders: TextView
    private lateinit var orderAdapter: OrderAdapter
    private val orderList = mutableListOf<Order>()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_order, container, false)
        rvOrders = view.findViewById(R.id.rv_orders)
        progressBar = view.findViewById(R.id.progress_bar)
        tvNoOrders = view.findViewById(R.id.tv_no_orders)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadOrders()
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(orderList)
        rvOrders.layoutManager = LinearLayoutManager(context)
        rvOrders.adapter = orderAdapter
    }

    private fun loadOrders() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            tvNoOrders.text = "Please login to see your orders."
            tvNoOrders.visibility = View.VISIBLE
            return
        }

        progressBar.visibility = View.VISIBLE
        tvNoOrders.visibility = View.GONE

        firestore.collection("orders")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE
                if (documents.isEmpty) {
                    tvNoOrders.text = "You have no orders yet."
                    tvNoOrders.visibility = View.VISIBLE
                } else {
                    orderList.clear()
                    for (document in documents) {
                        val order = document.toObject(Order::class.java)
                        order.id = document.id // Manually set the document ID
                        orderList.add(order)
                    }
                    orderAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                tvNoOrders.text = "Failed to load orders."
                tvNoOrders.visibility = View.VISIBLE
                Log.e("OrderFragment", "Error getting orders", exception)
            }
    }
}
