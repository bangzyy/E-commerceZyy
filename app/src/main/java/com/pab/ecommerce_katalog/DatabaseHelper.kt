package com.pab.ecommerce_katalog

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.pab.ecommerce_katalog.model.CartItem
import com.pab.ecommerce_katalog.model.User
import com.pab.ecommerce_katalog.Product
import com.pab.ecommerce_katalog.ProductVariant

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "user_db", null, 6) { // Version updated to 6 to trigger onUpgrade

    companion object {
        private const val TABLE_USERS = "users"
        private const val TABLE_PRODUCTS = "products"
        private const val TABLE_PRODUCT_VARIANTS = "product_variants"
        private const val TABLE_CART = "cart"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUserTable = """
            CREATE TABLE $TABLE_USERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE,
                email TEXT,
                password TEXT,
                address TEXT,
                phone TEXT
            )
        """
        db.execSQL(createUserTable)

        val createProductTable = """
            CREATE TABLE $TABLE_PRODUCTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                price REAL,
                description TEXT,
                category TEXT,
                imagePath TEXT,
                rating REAL,
                reviews INTEGER
            )
        """
        db.execSQL(createProductTable)

        val createProductVariantsTable = """
            CREATE TABLE $TABLE_PRODUCT_VARIANTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                productId INTEGER,
                size TEXT,
                color TEXT,
                stock INTEGER,
                FOREIGN KEY(productId) REFERENCES $TABLE_PRODUCTS(id) ON DELETE CASCADE
            )
        """
        db.execSQL(createProductVariantsTable)

        val createCartTable = """
            CREATE TABLE $TABLE_CART (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                productVariantId INTEGER UNIQUE,
                quantity INTEGER,
                FOREIGN KEY(productVariantId) REFERENCES $TABLE_PRODUCT_VARIANTS(id) ON DELETE CASCADE
            )
        """
        db.execSQL(createCartTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop all tables and recreate them. This is a simple but effective strategy for development.
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CART")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCT_VARIANTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun addToCart(variantId: Int, quantity: Int): Long {
        val db = writableDatabase
        var result: Long = -1

        val cursor = db.query(
            TABLE_CART,
            arrayOf("id", "quantity"),
            "productVariantId = ?",
            arrayOf(variantId.toString()),
            null, null, null
        )

        db.beginTransaction()
        try {
            if (cursor != null && cursor.moveToFirst()) {
                // Item exists, update quantity
                val currentQuantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"))
                val newQuantity = currentQuantity + quantity
                val values = ContentValues().apply {
                    put("quantity", newQuantity)
                }
                result = db.update(TABLE_CART, values, "productVariantId = ?", arrayOf(variantId.toString())).toLong()
            } else {
                // Item does not exist, insert new row
                val values = ContentValues().apply {
                    put("productVariantId", variantId)
                    put("quantity", quantity)
                }
                result = db.insert(TABLE_CART, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            cursor?.close()
            db.endTransaction()
        }
        return result
    }

    fun getCartItems(): List<CartItem> {
        val cartItems = mutableListOf<CartItem>()
        val db = readableDatabase
        val query = """
            SELECT
                c.id AS cartId,
                c.quantity,
                p.id AS productId,
                p.name,
                p.price,
                p.imagePath,
                pv.id AS variantId,
                pv.size,
                pv.color
            FROM $TABLE_CART c
            JOIN $TABLE_PRODUCT_VARIANTS pv ON c.productVariantId = pv.id
            JOIN $TABLE_PRODUCTS p ON pv.productId = p.id
        """
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val item = CartItem(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("cartId")),
                    productVariantId = cursor.getInt(cursor.getColumnIndexOrThrow("variantId")),
                    quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
                    productName = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    productPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                    productImagePath = cursor.getString(cursor.getColumnIndexOrThrow("imagePath")),
                    size = cursor.getString(cursor.getColumnIndexOrThrow("size")),
                    color = cursor.getString(cursor.getColumnIndexOrThrow("color"))
                )
                cartItems.add(item)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return cartItems
    }

    fun getUserByUsername(username: String): User? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE username = ?", arrayOf(username))
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
                address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"))
            )
        }
        cursor.close()
        return user
    }

    fun getUserByEmail(email: String): User? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE email = ?", arrayOf(email))
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
                address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"))
            )
        }
        cursor.close()
        return user
    }

    fun updateCartItemQuantity(cartId: Int, newQuantity: Int): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("quantity", newQuantity)
        }
        return db.update(TABLE_CART, values, "id = ?", arrayOf(cartId.toString()))
    }

    fun deleteCartItem(cartId: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_CART, "id = ?", arrayOf(cartId.toString()))
    }

    fun getProductById(productId: Int): Product? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PRODUCTS WHERE id = ?", arrayOf(productId.toString()))
        var product: Product? = null
        if (cursor.moveToFirst()) {
            product = Product(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                imagePath = cursor.getString(cursor.getColumnIndexOrThrow("imagePath")),
                rating = cursor.getFloat(cursor.getColumnIndexOrThrow("rating")),
                reviews = cursor.getInt(cursor.getColumnIndexOrThrow("reviews"))
            )
        }
        cursor.close()
        return product
    }

    fun insertUser(username: String, email: String, password: String, address: String, phone: String): Boolean {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE username = ?", arrayOf(username))
        if (cursor.count > 0) {
            cursor.close()
            return false
        }
        cursor.close()
        val values = ContentValues()
        values.put("username", username)
        values.put("email", email)
        values.put("password", password)
        values.put("address", address)
        values.put("phone", phone)
        val result = db.insert(TABLE_USERS, null, values)
        return result != -1L
    }

    fun checkLogin(username: String, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE username = ? AND password = ?", arrayOf(username, password))
        val loginSuccess = cursor.count > 0
        cursor.close()
        return loginSuccess
    }

    fun checkEmail(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE email = ?", arrayOf(email))
        val emailExists = cursor.count > 0
        cursor.close()
        return emailExists
    }

    fun updatePassword(email: String, newPassword: String): Boolean {
        val db = writableDatabase
        val values = ContentValues()
        values.put("password", newPassword)
        val result = db.update(TABLE_USERS, values, "email = ?", arrayOf(email))
        return result > 0
    }

    fun addProductWithVariants(product: Product, variants: List<ProductVariant>): Long {
        val db = writableDatabase
        db.beginTransaction()
        var productId = -1L
        try {
            val productValues = ContentValues().apply {
                put("name", product.name)
                put("price", product.price)
                put("description", product.description)
                put("category", product.category)
                put("imagePath", product.imagePath)
                put("rating", product.rating)
                put("reviews", product.reviews)
            }
            productId = db.insert(TABLE_PRODUCTS, null, productValues)

            if (productId != -1L) {
                for (variant in variants) {
                    val variantValues = ContentValues().apply {
                        put("productId", productId)
                        put("size", variant.size)
                        put("color", variant.color)
                        put("stock", variant.stock)
                    }
                    db.insert(TABLE_PRODUCT_VARIANTS, null, variantValues)
                }
                db.setTransactionSuccessful()
            }
        } finally {
            db.endTransaction()
        }
        return productId
    }

    fun updateProductWithVariants(product: Product, variants: List<ProductVariant>): Int {
        val db = writableDatabase
        db.beginTransaction()
        var rowsAffected = 0
        try {
            val productValues = ContentValues().apply {
                put("name", product.name)
                put("price", product.price)
                put("description", product.description)
                put("category", product.category)
                put("imagePath", product.imagePath)
                put("rating", product.rating)
                put("reviews", product.reviews)
            }
            rowsAffected = db.update(TABLE_PRODUCTS, productValues, "id = ?", arrayOf(product.id.toString()))

            // Delete old variants and insert new ones
            db.delete(TABLE_PRODUCT_VARIANTS, "productId = ?", arrayOf(product.id.toString()))
            for (variant in variants) {
                val variantValues = ContentValues().apply {
                    put("productId", product.id)
                    put("size", variant.size)
                    put("color", variant.color)
                    put("stock", variant.stock)
                }
                db.insert(TABLE_PRODUCT_VARIANTS, null, variantValues)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return rowsAffected
    }

    fun updateProductRating(productId: Int, addedRating: Float): Boolean {
        val db = writableDatabase
        val product = getProductById(productId) ?: return false

        val currentRating = product.rating
        val currentReviews = product.reviews
        val newReviews = currentReviews + 1
        val newRating = ((currentRating * currentReviews) + addedRating) / newReviews

        val values = ContentValues().apply {
            put("rating", newRating)
            put("reviews", newReviews)
        }

        return db.update(TABLE_PRODUCTS, values, "id = ?", arrayOf(productId.toString())) > 0
    }

    fun getVariantsForProduct(productId: Int): List<ProductVariant> {
        val variantList = mutableListOf<ProductVariant>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PRODUCT_VARIANTS WHERE productId = ?", arrayOf(productId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val variant = ProductVariant(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    productId = cursor.getInt(cursor.getColumnIndexOrThrow("productId")),
                    size = cursor.getString(cursor.getColumnIndexOrThrow("size")),
                    color = cursor.getString(cursor.getColumnIndexOrThrow("color")),
                    stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock"))
                )
                variantList.add(variant)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return variantList
    }

    fun updateVariantStock(variantId: Int, quantityToReduce: Int): Boolean {
        val db = writableDatabase
        val cursor = db.query(TABLE_PRODUCT_VARIANTS, arrayOf("stock"), "id = ?", arrayOf(variantId.toString()), null, null, null)
        
        var success = false
        if (cursor.moveToFirst()) {
            val currentStock = cursor.getInt(0)
            if (currentStock >= quantityToReduce) {
                val newStock = currentStock - quantityToReduce
                val values = ContentValues().apply { put("stock", newStock) }
                db.update(TABLE_PRODUCT_VARIANTS, values, "id = ?", arrayOf(variantId.toString()))
                success = true
            }
        }
        cursor.close()
        return success
    }

    fun getAllProducts(): List<Product> {
        val productList = mutableListOf<Product>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PRODUCTS", null)

        if (cursor.moveToFirst()) {
            do {
                val product = Product(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                    imagePath = cursor.getString(cursor.getColumnIndexOrThrow("imagePath")),
                    rating = cursor.getFloat(cursor.getColumnIndexOrThrow("rating")),
                    reviews = cursor.getInt(cursor.getColumnIndexOrThrow("reviews"))
                )
                productList.add(product)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return productList
    }

    fun getProductsByCategory(category: String): List<Product> {
        val productList = mutableListOf<Product>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PRODUCTS WHERE category = ?", arrayOf(category))

        if (cursor.moveToFirst()) {
            do {
                val product = Product(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                    imagePath = cursor.getString(cursor.getColumnIndexOrThrow("imagePath")),
                    rating = cursor.getFloat(cursor.getColumnIndexOrThrow("rating")),
                    reviews = cursor.getInt(cursor.getColumnIndexOrThrow("reviews"))
                )
                productList.add(product)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return productList
    }

    fun deleteProduct(productId: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_PRODUCTS, "id=?", arrayOf(productId.toString()))
    }

    fun getTotalUsers(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_USERS", null)
        var totalUsers = 0
        if (cursor.moveToFirst()) {
            totalUsers = cursor.getInt(0)
        }
        cursor.close()
        return totalUsers
    }

    fun getTotalProducts(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_PRODUCTS", null)
        var totalProducts = 0
        if (cursor.moveToFirst()) {
            totalProducts = cursor.getInt(0)
        }
        cursor.close()
        return totalProducts
    }

    fun updateUserDetails(email: String, username: String, password: String, address: String, phone: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put("username", username)
        values.put("password", password)
        values.put("address", address)
        values.put("phone", phone)
        // Update user details where the email matches
        val result = db.update(TABLE_USERS, values, "email = ?", arrayOf(email))
        return result > 0
    }

    fun isUserExists(email: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT id FROM $TABLE_USERS WHERE email = ?", arrayOf(email))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }
}
