package com.pab.ecommerce_katalog.ui.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pab.ecommerce_katalog.R
import com.pab.ecommerce_katalog.model.Review
import com.pab.ecommerce_katalog.model.User
import java.io.File

class UsersFragment : Fragment() {

    private lateinit var rvUsers: RecyclerView
    private lateinit var userAdapter: UserAdminAdapter
    private lateinit var searchView: SearchView
    private lateinit var firestore: FirebaseFirestore
    private var allUsers = mutableListOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        rvUsers = view.findViewById(R.id.rv_users)
        searchView = view.findViewById(R.id.search_view_users)

        setupRecyclerView()
        loadUsersFromFirestore()
        setupSearch()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdminAdapter(mutableListOf()) { user ->
            showUserDetailDialog(user)
        }
        rvUsers.layoutManager = LinearLayoutManager(context)
        rvUsers.adapter = userAdapter
    }

    private fun loadUsersFromFirestore() {
        firestore.collection("users")
            .addSnapshotListener { documents, e ->
                if (e != null) {
                    Toast.makeText(context, "Gagal memuat user: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (documents != null) {
                    allUsers.clear()
                    for (document in documents) {
                        val email = document.getString("email") ?: ""
                        
                        // Filter agar Admin tidak muncul di daftar User
                        if (email != "admin@example.com") {
                            val user = User(
                                id = 0,
                                username = document.getString("username") ?: "",
                                email = email,
                                password = "",
                                address = document.getString("address") ?: "Belum diisi",
                                phone = document.getString("phone") ?: "Belum diisi"
                            )
                            allUsers.add(user)
                        }
                    }
                    userAdapter.updateData(allUsers)
                }
            }
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = allUsers.filter {
                    it.username.contains(newText ?: "", ignoreCase = true) ||
                    it.email.contains(newText ?: "", ignoreCase = true)
                }
                userAdapter.updateData(filteredList)
                return true
            }
        })
    }

    private fun showUserDetailDialog(user: User) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_user_detail, null)
        val ivProfile = dialogView.findViewById<ImageView>(R.id.iv_detail_user_profile)
        val tvName = dialogView.findViewById<TextView>(R.id.tv_detail_user_name)
        val tvEmail = dialogView.findViewById<TextView>(R.id.tv_detail_user_email)
        val tvPhone = dialogView.findViewById<TextView>(R.id.tv_detail_user_phone)
        val tvAddress = dialogView.findViewById<TextView>(R.id.tv_detail_user_address)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btn_close_dialog)

        tvName.text = user.username
        tvEmail.text = user.email
        tvPhone.text = user.phone
        tvAddress.text = user.address

        firestore.collection("users").whereEqualTo("email", user.email).get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val path = docs.documents[0].getString("profileImageUrl")
                    if (!path.isNullOrEmpty()) {
                        val file = File(path)
                        if (file.exists()) {
                            Glide.with(this).load(file).placeholder(R.drawable.ic_person).into(ivProfile)
                        } else {
                            ivProfile.setImageResource(R.drawable.ic_person)
                        }
                    }
                }
            }

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    class UserAdminAdapter(
        private var users: List<User>,
        private val onDetailClick: (User) -> Unit
    ) : RecyclerView.Adapter<UserAdminAdapter.UserViewHolder>() {

        fun updateData(newUsers: List<User>) {
            this.users = newUsers
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_admin, parent, false)
            return UserViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            val user = users[position]
            holder.tvName.text = user.username
            holder.tvEmail.text = user.email
            holder.ivProfile.setImageResource(R.drawable.ic_person)
            
            // Mencoba load foto profil dari firestore di adapter jika perlu
            // Namun untuk performa sebaiknya ditangani di loadUsers
            
            holder.btnDetail.setOnClickListener { onDetailClick(user) }
        }

        override fun getItemCount(): Int = users.size

        class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivProfile: ImageView = view.findViewById(R.id.iv_user_profile)
            val tvName: TextView = view.findViewById(R.id.tv_user_name)
            val tvEmail: TextView = view.findViewById(R.id.tv_user_email)
            val btnDetail: MaterialButton = view.findViewById(R.id.btn_user_detail)
        }
    }
}
