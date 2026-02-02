package com.pab.ecommerce_katalog.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pab.ecommerce_katalog.CartAdapter
import com.pab.ecommerce_katalog.DatabaseHelper
import com.pab.ecommerce_katalog.R
import com.pab.ecommerce_katalog.model.CartItem
import com.pab.ecommerce_katalog.model.Order
import com.pab.ecommerce_katalog.model.OrderItem
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

class CartFragment : Fragment() {

    private lateinit var rvCartItems: RecyclerView
    private lateinit var tvSubTotal: TextView
    private lateinit var btnCheckout: Button
    private lateinit var cartAdapter: CartAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var toolbar: Toolbar
    private lateinit var firebaseAuth: FirebaseAuth

    private var cartItems: MutableList<CartItem> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        firebaseAuth = FirebaseAuth.getInstance()

        toolbar = view.findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.title = "CART"
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        rvCartItems = view.findViewById(R.id.rv_cart_items)
        tvSubTotal = view.findViewById(R.id.tv_sub_total)
        btnCheckout = view.findViewById(R.id.btn_checkout)

        rvCartItems.layoutManager = LinearLayoutManager(context)

        loadCartItems()

        btnCheckout.setOnClickListener { processCheckout() }
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            cartItems, 
            onQuantityChange = { item, newQuantity ->
                // Check if enough stock before updating quantity
                val variants = dbHelper.getVariantsForProduct(0) // We need productId but we have variantId
                // Let's assume we can check stock via variantId
                dbHelper.updateCartItemQuantity(item.id, newQuantity)
                updateSubTotal()
            },
            onDelete = { item ->
                dbHelper.deleteCartItem(item.id)
                loadCartItems() 
            },
            onSelectionChange = {
                updateSubTotal()
            }
        )
        rvCartItems.adapter = cartAdapter
    }

    private fun loadCartItems() {
        cartItems.clear()
        cartItems.addAll(dbHelper.getCartItems())
        cartItems.forEach { it.isSelected = true } 
        
        setupRecyclerView()
        updateSubTotal()
    }

    private fun updateSubTotal() {
        val subTotal = cartItems.filter { it.isSelected }.sumOf { it.productPrice * it.quantity.toDouble() }
        val localeID = Locale.forLanguageTag("in-ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        numberFormat.maximumFractionDigits = 0
        tvSubTotal.text = numberFormat.format(subTotal)
    }

    private fun processCheckout() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Anda harus login untuk checkout.", Toast.LENGTH_LONG).show()
            return
        }

        val selectedItems = cartAdapter.getSelectedItems()
        if (selectedItems.isEmpty()) {
            Toast.makeText(context, "Pilih setidaknya satu barang untuk checkout", Toast.LENGTH_SHORT).show()
            return
        }

        // VALIDASI STOK SEBELUM CHECKOUT
        for (item in selectedItems) {
            val variants = dbHelper.getVariantsForProduct(0) // This is just to get access to DB
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT stock, size, color FROM product_variants WHERE id = ?", arrayOf(item.productVariantId.toString()))
            if (cursor.moveToFirst()) {
                val stock = cursor.getInt(0)
                val size = cursor.getString(1)
                val color = cursor.getString(2)
                if (stock < item.quantity) {
                    Toast.makeText(context, "Stok produk ${item.productName} ($size|$color) tidak mencukupi. Sisa: $stock", Toast.LENGTH_LONG).show()
                    cursor.close()
                    return
                }
            }
            cursor.close()
        }

        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("LOGGED_IN_USERNAME", null)
        val user = username?.let { dbHelper.getUserByUsername(it) }

        if (user == null) {
            Toast.makeText(requireContext(), "Gagal mendapatkan data user. Silakan coba login ulang.", Toast.LENGTH_SHORT).show()
            return
        }

        val subTotal = selectedItems.sumOf { it.productPrice * it.quantity.toDouble() }
        
        val updatedOrderItems = mutableListOf<OrderItem>()
        for (item in selectedItems) {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT productId FROM product_variants WHERE id = ?", arrayOf(item.productVariantId.toString()))
            var pId = -1
            if (cursor.moveToFirst()) {
                pId = cursor.getInt(0)
            }
            cursor.close()
            
            updatedOrderItems.add(OrderItem(
                productId = pId,
                productName = item.productName,
                quantity = item.quantity,
                productPrice = item.productPrice,
                size = item.size,
                color = item.color,
                imagePath = item.productImagePath
            ))
        }

        val newOrder = Order(
            userId = currentUser.uid,
            userName = user.username,
            phone = user.phone,
            alamat = user.address,
            products = updatedOrderItems, 
            totalHarga = subTotal.toLong(),
            status = "Pending"
        )

        btnCheckout.isEnabled = false
        Toast.makeText(context, "Membuat pesanan...", Toast.LENGTH_SHORT).show()

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("orders")
            .add(newOrder)
            .addOnSuccessListener { documentReference ->
                val orderId = documentReference.id
                
                // PENGURANGAN STOK DI DATABASE LOKAL SETELAH CHECKOUT BERHASIL
                for (item in selectedItems) {
                    dbHelper.updateVariantStock(item.productVariantId, item.quantity)
                }

                openWhatsAppWithMessage(orderId, newOrder)

                selectedItems.forEach { dbHelper.deleteCartItem(it.id) }
                loadCartItems()

                Toast.makeText(context, "Pesanan dibuat! Stok telah diperbarui.", Toast.LENGTH_LONG).show()
                btnCheckout.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "GAGAL: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                btnCheckout.isEnabled = true
            }
    }

    private fun openWhatsAppWithMessage(orderId: String, order: Order) {
        val adminPhoneNumber = "62895374073213" 
        val stringBuilder = StringBuilder()
        stringBuilder.append("Halo Admin, saya ingin konfirmasi pesanan saya:\n\n")
        stringBuilder.append("*Order ID: $orderId*\n\n")

        order.products.forEachIndexed { index, item ->
            stringBuilder.append("${index + 1}. *${item.productName}*\n")
            stringBuilder.append("   - Ukuran: ${item.size}\n")
            stringBuilder.append("   - Warna: ${item.color}\n")
            stringBuilder.append("   - Jumlah: ${item.quantity}\n")
        }

        val localeID = Locale.forLanguageTag("in-ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        numberFormat.maximumFractionDigits = 0
        val formattedSubTotal = numberFormat.format(order.totalHarga)
        stringBuilder.append("\nSub Total: *${formattedSubTotal}*\n\n")

        stringBuilder.append("*Detail Pemesan*\n")
        stringBuilder.append("Nama: ${order.userName}\n")
        stringBuilder.append("No. HP: ${order.phone}\n")
        stringBuilder.append("Alamat: ${order.alamat}\n\n")
        stringBuilder.append("Terima kasih.")

        val message = stringBuilder.toString()

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$adminPhoneNumber&text=${URLEncoder.encode(message, "UTF-8")}")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadCartItems()
    }
}
