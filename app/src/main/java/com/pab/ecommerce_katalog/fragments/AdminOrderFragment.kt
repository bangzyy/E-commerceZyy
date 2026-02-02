package com.pab.ecommerce_katalog.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pab.ecommerce_katalog.OrderAdminAdapter
import com.pab.ecommerce_katalog.R
import com.pab.ecommerce_katalog.model.Order
import com.pab.ecommerce_katalog.model.OrderContentItem
import com.pab.ecommerce_katalog.model.OrderDisplayItem
import com.pab.ecommerce_katalog.model.UserHeaderItem

class AdminOrderFragment : Fragment() {

    private lateinit var rvOrders: RecyclerView
    private lateinit var orderAdapter: OrderAdminAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var toolbar: Toolbar

    private var allOrders: List<Order> = listOf()
    private var displayList: MutableList<OrderDisplayItem> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_order, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        toolbar = view.findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.title = "Customer Orders"

        rvOrders = view.findViewById(R.id.rv_orders)
        setupRecyclerView()

        loadOrders()
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdminAdapter(
            displayList,
            onAcceptClick = { order ->
                updateOrderStatus(order, "Accepted")
            },
            onRejectClick = { order ->
                updateOrderStatus(order, "Rejected")
            },
            onHeaderClick = { headerItem, position ->
                toggleUserOrders(headerItem, position)
            }
        )
        rvOrders.layoutManager = LinearLayoutManager(context)
        rvOrders.adapter = orderAdapter
    }

    private fun loadOrders() {
        firestore.collection("orders")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading orders: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    allOrders = snapshots.documents.mapNotNull { doc ->
                        doc.toObject(Order::class.java)?.apply {
                            id = doc.id // Make sure the document ID is assigned to the order ID
                        }
                    }
                    buildDisplayList()
                }
            }
    }

    private fun buildDisplayList() {
        // Preserve the expanded state of each user
        val expandedUserIds = displayList.filterIsInstance<UserHeaderItem>()
            .filter { it.isExpanded }
            .map { it.userId }
            .toSet()

        displayList.clear()
        val groupedOrders = allOrders.groupBy { it.userId }

        groupedOrders.forEach { (userId, orders) ->
            // Fix for nullable userId and ensure orders list is not empty
            if (!userId.isNullOrEmpty() && orders.isNotEmpty()) {
                val isExpanded = expandedUserIds.contains(userId)
                val header = UserHeaderItem(
                    userId = userId,
                    userName = orders.first().userName ?: "Nama Tidak Diketahui", // Handle possible null username
                    orderCount = orders.size,
                    isExpanded = isExpanded
                )
                displayList.add(header)

                // If the group was expanded, add its children back to the list
                if (isExpanded) {
                    val userOrders = orders.map { OrderContentItem(it) }
                    displayList.addAll(userOrders)
                }
            }
        }
        orderAdapter.notifyDataSetChanged()
    }

    private fun toggleUserOrders(headerItem: UserHeaderItem, position: Int) {
        headerItem.isExpanded = !headerItem.isExpanded

        if (headerItem.isExpanded) {
            val userOrders = allOrders
                .filter { it.userId == headerItem.userId }
                .map { OrderContentItem(it) }

            if (position + 1 <= displayList.size) {
                displayList.addAll(position + 1, userOrders)
                orderAdapter.notifyItemRangeInserted(position + 1, userOrders.size)
            }
        } else {
            val startPosition = position + 1
            val itemsToRemove = displayList
                .drop(startPosition)
                .takeWhile { it is OrderContentItem && it.order.userId == headerItem.userId }

            if (itemsToRemove.isNotEmpty()) {
                displayList.removeAll(itemsToRemove.toSet())
                orderAdapter.notifyItemRangeRemoved(startPosition, itemsToRemove.size)
            }
        }
        orderAdapter.notifyItemChanged(position) // To update the arrow icon
    }


    private fun updateOrderStatus(order: Order, newStatus: String) {
        val orderId = order.id
        if (orderId.isNullOrEmpty()) {
            Toast.makeText(context, "Error: Order ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("orders").document(orderId)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(context, "Order status updated", Toast.LENGTH_SHORT).show()
                // The snapshot listener will automatically refresh the list
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
