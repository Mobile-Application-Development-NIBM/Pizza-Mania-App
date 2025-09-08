package com.example.pizzamaniaapp

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import android.util.Log

class ChatbotDialogFragment : DialogFragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var userInput: EditText
    private lateinit var sendButton: Button
    private val messages = mutableListOf<ChatMessage>()
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private var currentPizza: String? = null // Track selected pizza for context
    private var selectedBranch: String? = null // Track selected branch

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chatbot, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.chatRecyclerView)
        userInput = view.findViewById(R.id.userInput)
        sendButton = view.findViewById(R.id.sendButton)
        chatAdapter = ChatAdapter(messages)
        recyclerView.adapter = chatAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        sendButton.setOnClickListener {
            val message = userInput.text.toString().trim()
            if (message.isNotEmpty()) {
                messages.add(ChatMessage(message, true))
                chatAdapter.notifyItemInserted(messages.size - 1)
                userInput.text.clear()
                processUserMessage(message)
                recyclerView.scrollToPosition(messages.size - 1)
            }
        }

        // Welcome message
        messages.add(ChatMessage("👋 Welcome to PizzaBot! 🍕\n" +
                "Please select a branch with 'select branch <branch_name>' (e.g., 'select branch Colombo') or type 'help' to start.", false))
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt() // 90% of screen width
            val height = (resources.displayMetrics.heightPixels * 0.6).toInt() // 60% of screen height
            setLayout(width, height)
            setGravity(Gravity.CENTER)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private fun processUserMessage(message: String) {
        val lowerMessage = message.lowercase()
        val response = when {
            listOf("hi", "hello", "yo", "hey").any { lowerMessage.contains(it) } -> {
                "👋 Hey there! Welcome back to PizzaBot. 🍕 Please select a branch with 'select branch <branch_name>' or type 'show menu' to start."
            }
            lowerMessage.contains("show menu") || lowerMessage.contains("list items") -> showMenu()
            lowerMessage.startsWith("select branch") -> {
                val branchInput = lowerMessage.replace("select branch", "").trim()
                val branchMap = mapOf("b001" to "Colombo", "b002" to "Galle", "b003" to "Kandy", "b004" to "Jaffna", "b005" to "Matara")
                val branchCode = branchMap.entries.find { it.value.lowercase() == branchInput.lowercase() }?.key
                if (branchCode != null) {
                    selectedBranch = branchCode
                    messages.add(ChatMessage("Branch selected: ${branchMap[branchCode]}. Now showing menu...", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                    showMenu() // Trigger menu display
                } else {
                    messages.add(ChatMessage("Invalid branch. Available branches: ${branchMap.values.joinToString(", ")}", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                }
                return // Exit early as response is handled asynchronously
            }
            lowerMessage.contains("vegetarian") || lowerMessage.contains("non-veg") -> selectCategory(lowerMessage)
            lowerMessage.contains("add topping") || lowerMessage.contains("extra") -> addToppings(lowerMessage)
            lowerMessage.contains("add to cart") -> addToCart()
            lowerMessage.contains("show cart") || lowerMessage.contains("view cart") -> viewCart()
            lowerMessage.contains("pay") || lowerMessage.contains("checkout") -> initiatePayment(lowerMessage)
            lowerMessage.contains("track order") || lowerMessage.contains("status") -> trackOrder()
            lowerMessage.contains("order history") || lowerMessage.contains("past orders") -> showOrderHistory()
            lowerMessage.contains("promo") || lowerMessage.contains("discount") -> applyPromo(lowerMessage)
            lowerMessage.contains("profile") || lowerMessage.contains("address") -> updateProfile(lowerMessage)
            lowerMessage.contains("feedback") || lowerMessage.contains("rate") -> submitFeedback()
            lowerMessage.contains("support") || lowerMessage.contains("help") -> contactSupport()
            lowerMessage.contains("settings") || lowerMessage.contains("password") || lowerMessage.contains("log out") -> manageAccount(lowerMessage)
            else -> {
                // Check if user is selecting a menu item
                var selected = false
                if (selectedBranch != null) {
                    database.child("menu").get().addOnSuccessListener { snapshot ->
                        Log.d("Chatbot", "Menu selection - Snapshot exists: ${snapshot.exists()} | Count: ${snapshot.childrenCount}")
                        for (item in snapshot.children) {
                            val branches = item.child("branches")
                                .getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                            val itemName = item.child("name").getValue(String::class.java)?.lowercase() ?: ""
                            Log.d("Chatbot", "Checking item: $itemName | Branches: $branches | User input: $lowerMessage")
                            if (branches.contains(selectedBranch) && itemName.contains(lowerMessage)) {
                                currentPizza = item.child("name").getValue(String::class.java)
                                messages.add(ChatMessage("Selected $currentPizza. Add toppings (e.g., 'add extra cheese') or say 'add to cart'.", false))
                                chatAdapter.notifyItemInserted(messages.size - 1)
                                selected = true
                            }
                        }
                        if (!selected) {
                            messages.add(ChatMessage("I didn't understand. Try 'show menu', 'select branch <branch_name>', or 'help'.", false))
                            chatAdapter.notifyItemInserted(messages.size - 1)
                        }
                        recyclerView.scrollToPosition(messages.size - 1)
                    }.addOnFailureListener { exception ->
                        Log.e("Chatbot", "Error selecting menu item: ${exception.message}", exception)
                        messages.add(ChatMessage("Error fetching menu items: ${exception.message}", false))
                        chatAdapter.notifyItemInserted(messages.size - 1)
                        recyclerView.scrollToPosition(messages.size - 1)
                    }
                } else {
                    messages.add(ChatMessage("Please select a branch first with 'select branch <branch_name>' to view menu items.", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                }
                return // Exit early as response is handled asynchronously
            }
        }
        messages.add(ChatMessage(response, false))
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun showMenu(): String {
        Log.d("Chatbot", "Starting menu fetch for branch: $selectedBranch")
        if (selectedBranch == null) {
            messages.add(ChatMessage("Please select a branch first with 'select branch <branch_name>'.", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
            return "Please select a branch first."
        }

        database.child("menu").get().addOnSuccessListener { snapshot ->
            Log.d("Chatbot", "Snapshot exists: ${snapshot.exists()} | Child count: ${snapshot.childrenCount}")
            if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                Log.w("Chatbot", "No menu items found in the database.")
                messages.add(ChatMessage("No menu items found in the database.", false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                recyclerView.scrollToPosition(messages.size - 1)
                return@addOnSuccessListener
            }

            var hasItems = false
            for (item in snapshot.children) {
                Log.d("Chatbot", "Item key: ${item.key} | Value: ${item.value}")
                val branches = item.child("branches").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                Log.d("Chatbot", "Branches for ${item.key}: $branches | Selected branch: $selectedBranch")
                if (branches.contains(selectedBranch)) {
                    hasItems = true
                    val name = item.child("name").getValue(String::class.java) ?: "Unknown Item"
                    val price = item.child("price").getValue(Double::class.java) ?: 0.0
                    val description = item.child("description").getValue(String::class.java) ?: "No description"
                    val paddedName = String.format("%-20s", name)
                    val priceText = String.format("LKR %.0f", price)
                    val formattedText = "• $paddedName ....... $priceText\n  - $description"
                    Log.d("Chatbot", "Adding menu item: $formattedText")
                    messages.add(ChatMessage(formattedText, false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                } else {
                    Log.d("Chatbot", "Item ${item.key} not available for branch $selectedBranch")
                }
            }
            if (!hasItems) {
                Log.w("Chatbot", "No menu items available for branch: $selectedBranch")
                messages.add(ChatMessage("No menu items available for $selectedBranch.", false))
                chatAdapter.notifyItemInserted(messages.size - 1)
            }
            recyclerView.scrollToPosition(messages.size - 1)
        }.addOnFailureListener { exception ->
            Log.e("Chatbot", "Error fetching menu: ${exception.message}", exception)
            messages.add(ChatMessage("Error fetching menu for $selectedBranch: ${exception.message}", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }
        return "Fetching menu for $selectedBranch..."
    }

    private fun selectCategory(message: String): String {
        val category = if (message.contains("vegetarian")) "Vegetarian" else "Non-Vegetarian"
        var response = "Showing $category items:\n"
        if (selectedBranch != null) {
            database.child("menu").orderByChild("category").equalTo(category).get().addOnSuccessListener { snapshot ->
                Log.d("Chatbot", "Category $category - Snapshot exists: ${snapshot.exists()} | Count: ${snapshot.childrenCount}")
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    messages.add(ChatMessage("No $category items found for $selectedBranch.", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                    return@addOnSuccessListener
                }
                for (item in snapshot.children) {
                    val branches = item.child("branches").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                    if (branches.contains(selectedBranch)) {
                        val name = item.child("name").getValue(String::class.java) ?: ""
                        val price = item.child("price").getValue(Double::class.java) ?: 0.0
                        response += "$name - LKR $price\n"
                    }
                }
                messages.add(ChatMessage(response, false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                recyclerView.scrollToPosition(messages.size - 1)
            }.addOnFailureListener { exception ->
                Log.e("Chatbot", "Error fetching $category items: ${exception.message}", exception)
                messages.add(ChatMessage("Error fetching $category items: ${exception.message}", false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                recyclerView.scrollToPosition(messages.size - 1)
            }
        } else {
            response = "Please select a branch first with 'select branch <branch_name>'."
            messages.add(ChatMessage(response, false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }
        return "Fetching $category items..."
    }

    private fun addToppings(message: String): String {
        if (currentPizza == null) return "Please select a menu item first."
        val topping = message.substringAfter("add").trim()
        return "Added $topping to $currentPizza!"
    }

    private fun addToCart(): String {
        val userId = auth.currentUser?.uid ?: return "Please log in to add to cart."
        if (currentPizza == null) return "Please select a menu item first."
        database.child("menu").orderByChild("name").equalTo(currentPizza).get().addOnSuccessListener { snapshot ->
            Log.d("Chatbot", "Add to cart - Snapshot exists: ${snapshot.exists()} | Count: ${snapshot.childrenCount}")
            val price = snapshot.children.firstOrNull()?.child("price")?.getValue(Double::class.java) ?: 0.0
            database.child("carts").child(userId).push().setValue(mapOf("name" to currentPizza, "price" to price))
            messages.add(ChatMessage("$currentPizza added to cart! Say 'show cart' or 'pay'.", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }.addOnFailureListener { exception ->
            Log.e("Chatbot", "Error adding to cart: ${exception.message}", exception)
            messages.add(ChatMessage("Error adding to cart: ${exception.message}", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }
        return "Adding $currentPizza to cart..."
    }

    private fun viewCart(): String {
        val userId = auth.currentUser?.uid ?: return "Please log in to view cart."
        var response = "Your Cart:\n"
        database.child("carts").child(userId).get().addOnSuccessListener { snapshot ->
            Log.d("Chatbot", "View cart - Snapshot exists: ${snapshot.exists()} | Count: ${snapshot.childrenCount}")
            if (!snapshot.exists()) {
                response = "Your cart is empty."
            } else {
                for (item in snapshot.children) {
                    val name = item.child("name").getValue(String::class.java) ?: ""
                    val price = item.child("price").getValue(Double::class.java) ?: 0.0
                    response += "$name - LKR $price\n"
                }
            }
            messages.add(ChatMessage(response, false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }.addOnFailureListener { exception ->
            Log.e("Chatbot", "Error fetching cart: ${exception.message}", exception)
            messages.add(ChatMessage("Error fetching cart: ${exception.message}", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }
        return "Fetching cart..."
    }

    private fun initiatePayment(message: String): String {
        val userId = auth.currentUser?.uid ?: return "Please log in to pay."
        return when {
            message.contains("card online") -> {
                "Redirecting to online card payment... [Stripe integration required]"
            }
            message.contains("card on delivery") -> {
                database.child("orders").child(userId).push().setValue(
                    mapOf("name" to currentPizza, "payment" to "Card on Delivery", "status" to "Pending")
                )
                "Order placed with card on delivery. You'll provide card details to the delivery agent."
            }
            message.contains("cash") || message.contains("cod") -> {
                database.child("orders").child(userId).push().setValue(
                    mapOf("name" to currentPizza, "payment" to "Cash on Delivery", "status" to "Pending")
                )
                "Order placed with cash on delivery."
            }
            else -> "Please specify payment method: 'pay card online', 'pay card on delivery', or 'pay cash'."
        }
    }

    private fun trackOrder(): String {
        val userId = auth.currentUser?.uid ?: return "Please log in to track orders."
        var response = "Order Status:\n"
        database.child("orders").child(userId).get().addOnSuccessListener { snapshot ->
            Log.d("Chatbot", "Track order - Snapshot exists: ${snapshot.exists()} | Count: ${snapshot.childrenCount}")
            if (!snapshot.exists()) {
                response = "No orders found."
            } else {
                for (order in snapshot.children) {
                    val name = order.child("name").getValue(String::class.java) ?: ""
                    val status = order.child("status").getValue(String::class.java) ?: ""
                    response += "Order #${order.key}: $name ($status)\n"
                }
            }
            messages.add(ChatMessage(response, false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }.addOnFailureListener { exception ->
            Log.e("Chatbot", "Error tracking order: ${exception.message}", exception)
            messages.add(ChatMessage("Error tracking order: ${exception.message}", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }
        return "Checking order status..."
    }

    private fun showOrderHistory(): String {
        val userId = auth.currentUser?.uid ?: return "Please log in to view order history."
        var response = "Order History:\n"
        database.child("orders").child(userId).get().addOnSuccessListener { snapshot ->
            Log.d("Chatbot", "Order history - Snapshot exists: ${snapshot.exists()} | Count: ${snapshot.childrenCount}")
            if (!snapshot.exists()) {
                response = "No past orders."
            } else {
                for (order in snapshot.children) {
                    val name = order.child("name").getValue(String::class.java) ?: ""
                    val status = order.child("status").getValue(String::class.java) ?: ""
                    response += "Order #${order.key}: $name ($status)\n"
                }
            }
            messages.add(ChatMessage(response, false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }.addOnFailureListener { exception ->
            Log.e("Chatbot", "Error fetching order history: ${exception.message}", exception)
            messages.add(ChatMessage("Error fetching order history: ${exception.message}", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }
        return "Fetching order history..."
    }

    private fun applyPromo(message: String): String {
        val promo = message.substringAfter("promo").trim()
        return if (promo.isNotEmpty()) "Applied promo code: $promo (10% off)!" else "Please provide a promo code (e.g., 'promo SAVE10')."
    }

    private fun updateProfile(message: String): String {
        val userId = auth.currentUser?.uid ?: return "Please log in to update profile."
        return when {
            message.contains("address") -> {
                val address = message.substringAfter("address").trim()
                database.child("users").child(userId).child("address").setValue(address)
                "Address updated to: $address"
            }
            message.contains("name") -> {
                val name = message.substringAfter("name").trim()
                database.child("users").child(userId).child("name").setValue(name)
                "Name updated to: $name"
            }
            else -> "Please specify what to update (e.g., 'update address to 123 Main St' or 'update name to John')."
        }
    }

    private fun submitFeedback(): String {
        return "Thanks for your feedback! Please share your comments, and I'll save them."
    }

    private fun contactSupport(): String {
        return "Contact support at support@pizzaapp.com or call 123-456-7890."
    }

    private fun manageAccount(message: String): String {
        return when {
            message.contains("log out") -> {
                auth.signOut()
                "Logged out successfully."
            }
            message.contains("password") -> "To update password, please use the main app's account settings."
            else -> "Account options: Say 'log out' or 'update password'."
        }
    }
}