package com.dummbroke.bread_count.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object FirestoreUtils {
    val db: FirebaseFirestore = Firebase.firestore

    // Create a new document
    suspend fun <T : Any> createDocument(collection: String, documentId: String, data: T) {
        try {
            db.collection(collection)
                .document(documentId)
                .set(data as Any)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    // Read a document
    suspend fun <T> getDocument(collection: String, documentId: String, clazz: Class<T>): T? {
        return try {
            val document = db.collection(collection)
                .document(documentId)
                .get()
                .await()
            document.toObject(clazz)
        } catch (e: Exception) {
            throw e
        }
    }

    // Update a document
    suspend fun <T : Any> updateDocument(collection: String, documentId: String, data: T) {
        try {
            db.collection(collection)
                .document(documentId)
                .set(data as Any)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    // Delete a document
    suspend fun deleteDocument(collection: String, documentId: String) {
        try {
            db.collection(collection)
                .document(documentId)
                .delete()
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    // Get all documents in a collection
    suspend fun <T> getAllDocuments(collection: String, clazz: Class<T>): List<T> {
        return try {
            val documents = db.collection(collection)
                .get()
                .await()
            documents.toObjects(clazz)
        } catch (e: Exception) {
            throw e
        }
    }
} 