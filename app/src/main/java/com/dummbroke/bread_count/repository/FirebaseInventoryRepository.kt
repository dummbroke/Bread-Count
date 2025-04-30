package com.dummbroke.bread_count.repository

import com.dummbroke.bread_count.model.InventoryItem
import com.dummbroke.bread_count.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.Transaction as FirebaseTransaction
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date
import android.util.Log

class FirebaseInventoryRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = auth.currentUser

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val TRANSACTIONS_COLLECTION = "transactions"
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
     * Record a sales transaction in Firestore and update inventory quantity.
     */
    suspend fun recordTransaction(
        item: InventoryItem,
        quantity: Int,
        isOwner: Boolean,
        ownerName: String?
    ) {
        if (currentUser == null) throw Exception("User not authenticated")

        val userDoc = db.collection(USERS_COLLECTION).document(currentUser.uid)
        
        val categoryKey = when (item.category) {
            "Display Bread" -> DISPLAY_BREAD
            "Display Beverages" -> DISPLAY_BEVERAGES
            "Delivery Bread" -> DELIVERY_BREAD
            else -> item.category
        }

        try {
            db.runTransaction { firestoreTransaction: FirebaseTransaction -> 
                val itemDocRef = userDoc.collection(categoryKey).document(item.id)
                val currentItemSnapshot = firestoreTransaction.get(itemDocRef)
                val currentQuantity = (currentItemSnapshot.data?.get("quantity") as? Number)?.toInt()
                    ?: throw Exception("Item not found or quantity missing in inventory")

                if (currentQuantity < quantity) {
                    throw Exception("Not enough inventory for ${item.name}. Only $currentQuantity available.")
                }

                val newQuantity = currentQuantity - quantity
                firestoreTransaction.update(itemDocRef, "quantity", newQuantity)

                // Create the Transaction object
                val transactionRecord = Transaction(
                    // id will be generated by Firestore
                    itemId = item.id,
                    itemName = item.name,
                    itemCategory = item.category, // Store the display name like "Display Bread"
                    itemPrice = item.price,
                    quantity = quantity,
                    // Calculate and assign to totalAmount
                    totalAmount = item.price * quantity, 
                    isOwner = isOwner,
                    ownerName = if (isOwner) ownerName else null, // Store name only if owner
                    timestamp = Timestamp.now()
                )

                // Create a new document in the transactions subcollection
                val transactionDocRef = userDoc.collection(TRANSACTIONS_COLLECTION).document()
                 // Pass the object with totalAmount field
                firestoreTransaction.set(transactionDocRef, transactionRecord) 

                // Return null for success in Firestore Transaction lambda
                null
            }.await()
        } catch (e: Exception) {
            if (e.message?.startsWith("Not enough inventory") == true) {
                throw e
            } else {
                throw Exception("Transaction failed: ${e.message}", e)
            }
        }
    }

    /**
     * Gets a Flow of transactions, optionally filtered by category.
     * Orders by timestamp descending.
     */
    fun getTransactionsFlow(categoryFilter: String?): Flow<List<Transaction>> = callbackFlow {
        if (currentUser == null) {
            close(Exception("User not authenticated"))
            return@callbackFlow
        }

        var query: Query = db.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .collection(TRANSACTIONS_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING) // Order by most recent first

        // Apply category filter if provided and not "All Option"
        if (categoryFilter != null && categoryFilter != "All Option") {
            query = query.whereEqualTo("category", categoryFilter)
        }

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val transactions = snapshot?.documents?.mapNotNull { doc ->
                 try {
                     doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                 } catch (e: Exception) {
                     // Log mapping errors for specific documents if needed
                     Log.e("FirebaseInventoryRepo", "Error mapping document ${doc.id}: ${e.message}", e)
                     null // Exclude documents that fail to map
                 }
            } ?: emptyList()

            trySend(transactions).isSuccess
        }

        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Fetches all transactions for a specific date.
     */
    suspend fun getTransactionsForDate(date: Date): List<Transaction> {
        if (currentUser == null) throw Exception("User not authenticated")

        // Calculate start and end Timestamps for the given date
        val calendar = Calendar.getInstance()
        calendar.time = date
        // Start of the day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTimestamp = Timestamp(calendar.time)

        // End of the day
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endTimestamp = Timestamp(calendar.time)

        try {
            val snapshot = db.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .collection(TRANSACTIONS_COLLECTION)
                .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .whereLessThanOrEqualTo("timestamp", endTimestamp)
                .orderBy("timestamp", Query.Direction.ASCENDING) // Order chronologically for export
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                doc.toObject(Transaction::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            throw Exception("Failed to fetch transactions for date: ${e.message}", e)
        }
    }
}