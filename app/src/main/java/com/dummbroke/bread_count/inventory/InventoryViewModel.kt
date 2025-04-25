package com.dummbroke.bread_count.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dummbroke.bread_count.model.InventoryItem
import com.dummbroke.bread_count.utils.FirestoreUtils
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class InventoryViewModel : ViewModel() {
    private val _inventoryItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventoryItems: StateFlow<List<InventoryItem>> = _inventoryItems.asStateFlow()

    private var firestoreListener: ListenerRegistration? = null
    private var currentCategory: String = DISPLAY_BREAD

    companion object {
        const val DISPLAY_BREAD = "Display Bread"
        const val DISPLAY_BEVERAGES = "Display Beverages"
        const val DELIVERY_BREAD = "Delivery Bread"
    }

    fun setCategory(category: String) {
        currentCategory = category
        setupFirestoreListener()
    }

    fun addItem(name: String, price: Double, category: String) {
        viewModelScope.launch {
            try {
                val newItem = InventoryItem(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    category = category,
                    price = price,
                    quantity = 0
                )

                val collectionName = when (category) {
                    DISPLAY_BREAD -> "display_bread"
                    DISPLAY_BEVERAGES -> "display_beverages"
                    DELIVERY_BREAD -> "delivery_bread"
                    else -> "products"
                }

                FirestoreUtils.createDocument(collectionName, newItem.id, newItem)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            try {
                val collectionName = when (item.category) {
                    DISPLAY_BREAD -> "display_bread"
                    DISPLAY_BEVERAGES -> "display_beverages"
                    DELIVERY_BREAD -> "delivery_bread"
                    else -> "products"
                }
                FirestoreUtils.deleteDocument(collectionName, item.id)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun setupFirestoreListener() {
        firestoreListener?.remove()

        val collectionName = when (currentCategory) {
            DISPLAY_BREAD -> "display_bread"
            DISPLAY_BEVERAGES -> "display_beverages"
            DELIVERY_BREAD -> "delivery_bread"
            else -> "products"
        }

        firestoreListener = FirestoreUtils.db.collection(collectionName)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(InventoryItem::class.java)
                } ?: emptyList()

                _inventoryItems.value = items
            }
    }

    override fun onCleared() {
        super.onCleared()
        firestoreListener?.remove()
    }
} 