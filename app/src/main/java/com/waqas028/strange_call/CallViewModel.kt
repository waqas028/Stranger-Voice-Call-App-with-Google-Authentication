package com.waqas028.strange_call

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) : ViewModel() {
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState

    var currentUser: FirebaseUser? = auth.currentUser

    private var callRoomId: String? = null
    private var listener: ValueEventListener? = null

    init {
        viewModelScope.launch {
            callState.collect { state ->
                Log.d("CallViewModel", "State changed to: $state")
            }
        }

        fetchUserAvailability(currentUser?.uid.orEmpty())

        auth.addAuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
    }

    fun updateCallState(){
        _callState.value = CallState.Idle
    }

    fun findStranger() {
        viewModelScope.launch {
            try {
                _callState.value = CallState.Searching
                val currentUserId = auth.currentUser?.uid ?: return@launch

                // CORRECTED: Proper await() usage for database operations
                database.reference.child("users").child(currentUserId).updateChildren(
                    mapOf(
                        "available" to true,
                        "timestamp" to ServerValue.TIMESTAMP
                    )
                ).await()

                Log.d("CallViewModel", "User status updated successfully")

                // Search for available users
                val usersRef = database.reference.child("users")
                val query = usersRef.orderByChild("available").equalTo(true)

                listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d("CallViewModel", "Database query returned ${snapshot.childrenCount} users")

                        val availableUsers = mutableListOf<String>()
                        snapshot.children.forEach { userSnapshot ->
                            val userId = userSnapshot.key
                            if (userId != currentUserId) {
                                availableUsers.add(userId!!)
                                Log.d("CallViewModel", "Found available user: $userId")
                            }
                        }

                        if (availableUsers.isNotEmpty()) {
                            val matchedUserId = availableUsers.random()
                            Log.d("CallViewModel", "Matched with user: $matchedUserId")
                            createCallRoom(matchedUserId)
                        } else {
                            Log.d("CallViewModel", "No available users found")
                            _callState.value = CallState.Error("No users available")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("CallViewModel", "Database error: ${error.message}")
                        _callState.value = CallState.Error("Database error: ${error.message}")
                    }
                }
                query.addValueEventListener(listener!!)

            } catch (e: Exception) {
                Log.e("CallViewModel", "Error finding stranger", e)
                _callState.value = CallState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    private fun createCallRoom(matchedUserId: String) {
        viewModelScope.launch {
            try {
                // Create a unique room ID
                callRoomId = listOf(currentUser?.uid.orEmpty(), matchedUserId)
                    .sorted()
                    .joinToString("_") { it.takeLast(8) }

                // Update both users' status
                val updates = hashMapOf<String, Any>(
                    "users/${currentUser?.uid}/available" to _isAvailable.value,
                    "users/${currentUser?.uid}/inCall" to true,
                    "users/${currentUser?.uid}/currentRoom" to callRoomId.orEmpty(),
                    "users/$matchedUserId/available" to true,
                    "users/$matchedUserId/inCall" to true,
                    "users/$matchedUserId/currentRoom" to callRoomId.orEmpty(),
                    "calls/$callRoomId/user1" to currentUser?.uid.orEmpty(),
                    "calls/$callRoomId/user2" to matchedUserId,
                    "calls/$callRoomId/status" to "matched"
                )

                database.reference.updateChildren(updates).await()
                _callState.value = CallState.Matched(callRoomId!!)

                // Remove listener after match
                listener?.let {
                    database.reference.child("users").removeEventListener(it)
                }
            } catch (e: Exception) {
                _callState.value = CallState.Error(e.message ?: "Failed to create call room")
            }
        }
    }

    fun endCall() {
        callRoomId?.let { roomId ->
            viewModelScope.launch {
                try {
                    // Update call status to ended
                    val updates = hashMapOf<String, Any>(
                        "calls/$roomId/status" to "ended",
                        "users/${currentUser?.uid}/inCall" to false,
                        "users/${currentUser?.uid}/currentRoom" to ""
                    )

                    database.reference.updateChildren(updates).await()
                    _callState.value = CallState.Idle
                    callRoomId = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val _isAvailable = mutableStateOf(true)
    val isAvailable get() = _isAvailable

    fun fetchUserAvailability(userId: String) {
        viewModelScope.launch {
            try {
                val snapshot = database.reference.child("users").child(userId).child("available").get().await()
                _isAvailable.value = snapshot.getValue(Boolean::class.java) ?: false
            } catch (e: Exception) {
                Log.i("TAG", "fetchUserAvailabilityInfo: ${e.message}")
                false
            }
        }
    }

    fun updateUserAvailability(userId: String, isAvailable: Boolean) {
        database.reference.child("users").child(userId).updateChildren(
            mapOf(
                "available" to isAvailable,
                "timestamp" to ServerValue.TIMESTAMP
            )
        ).addOnSuccessListener {
            _isAvailable.value = isAvailable
        }.addOnFailureListener {
            Log.i("TAG", "updateUserAvailability: ${it.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.let {
            database.reference.child("users").removeEventListener(it)
        }
    }
}

sealed class CallState {
    object Idle : CallState()
    object Searching : CallState()
    data class Matched(val roomId: String) : CallState()
    data class Error(val message: String) : CallState()
}