package com.dummbroke.bread_count.repository

import com.dummbroke.bread_count.model.InventoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await

class FirebaseInventoryRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = auth.currentUser

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val DISPLAY_BREAD = "display_bread"
        private const val DISPLAY_BEVERAGES = "display_beverages"
        private const val DELIVERY_BREAD = "delivery_bread"
    }

    fun getInventoryItems(category: String): Flow<List<InventoryItem>> = callbackFlow {
        if (currentUser == null) {
            close()
            return@callbackFlow
        }

        val listenerRegistration = db.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .collection(category)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data
                    if (data != null) {
                        val name = data["name"] as? String ?: ""
                        val price = (data["price"] as? Number)?.toDouble() ?: 0.0
                        val itemCategory = data["category"] as? String ?: category
                        val quantity = (data["quantity"] as? Number)?.toInt() ?: 0
                        
                        InventoryItem(
                            id = doc.id,
                            name = name,
                            price = price,
                            category = itemCategory,
                            quantity = quantity
                        )
                    } else {
                        null
                    }
                } ?: emptyList()

                trySend(items)
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun addItem(item: InventoryItem, category: String): String {
        if (currentUser == null) throw Exception("User not authenticated")

        val docRef = db.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .collection(category)
            .document()

        val itemData = hashMapOf(
            "name" to item.name,
            "price" to item.price,
            "category" to item.category,
            "quantity" to item.quantity
        )
        
        docRef.set(itemData).await()
        return docRef.id
    }

    suspend fun updateItem(item: InventoryItem, category: String) {
        if (currentUser == null) throw Exception("User not authenticated")

        val itemData = hashMapOf(
            "name" to item.name,
            "price" to item.price,
            "category" to item.category,
            "quantity" to item.quantity
        )

        db.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .collection(category)
            .document(item.id)
            .set(itemData)
            .await()
    }

    suspend fun moveItem(item: InventoryItem, originalCategory: String, newCategory: String) {
        if (currentUser == null) throw Exception("User not authenticated")

        val batch = db.batch()

        // Document reference in the original category
        val originalDocRef = db.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .collection(originalCategory)
            .document(item.id)

        // Document reference in the new category (using the same ID)
        val newDocRef = db.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .collection(newCategory)
            .document(item.id)

        // Data to be set in the new location
        val itemData = hashMapOf(
            "name" to item.name,
            "price" to item.price,
            "category" to item.category, // Ensure this reflects the *new* category display name
            "quantity" to item.quantity
        )

        // Add operations to the batch
        batch.delete(originalDocRef)       // Delete from the old category
        batch.set(newDocRef, itemData)      // Create/Set in the new category with the same ID

        // Commit the batch
        batch.commit().await()
    }

    suspend fun deleteItem(itemId: String, category: String) {
        if (currentUser == null) throw Exception("User not authenticated")

        db.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .collection(category)
            .document(itemId)
            .delete()
            .await()
    }

    /**
     * Record a sales transaction in Firestore and update inventory quantity if not owner.
     */
    suspend fun recordTransaction(
        item: InventoryItem,
        quantity: Int,
        isOwner: Boolean,
        ownerName: String
    ) {
        if (currentUser == null) throw Exception("User not authenticated")

        val totalAmount = item.price * quantity
        val transactionData = hashMapOf(
            "itemId" to item.id,
            "itemName" to item.name,
            "category" to item.category,
            "quantity" to quantity,
            "price" to item.price,
            "isOwner" to isOwner,
            "ownerName" to ownerName,
            "totalAmount" to totalAmount,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        val userDoc = db.collection(USERS_COLLECTION).document(currentUser.uid)
        val transactionsCol = userDoc.collection("transactions")
        val transactionDoc = transactionsCol.document()

        // Start a batch to ensure atomicity if updating inventory
        val batch = db.batch()
        batch.set(transactionDoc, transactionData)

        if (!isOwner) {
            // Update inventory quantity (subtract sold quantity)
            val categoryKey = when (item.category) {
                "Display Bread" -> DISPLAY_BREAD
                "Display Beverages" -> DISPLAY_BEVERAGES
                "Delivery Bread" -> DELIVERY_BREAD
                else -> item.category
            }
            val itemDoc = userDoc.collection(categoryKey).document(item.id)
            val newQuantity = (item.quantity - quantity).coerceAtLeast(0)
            batch.update(itemDoc, "quantity", newQuantity)
        }

        batch.commit().await()
    }
}