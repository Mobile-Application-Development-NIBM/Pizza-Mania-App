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
                "You can try commands like:\n" +
                "👉 'show menu'\n" +
                "👉 'track order'\n" +
                "👉 'update profile'\n" +
                "👉 'help'\n\n" +
                "Or just say hi to get started!", false))
        chatAdapter.notifyItemInserted(messages.size - 1)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt() // 90% of screen width
            val height = (resources.displayMetrics.heightPixels * 0.6).toInt() // 60% of screen height
            setLayout(width, height)
            setGravity(Gravity.CENTER) // center it
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private fun processUserMessage(message: String) {
        val lowerMessage = message.lowercase()
        val response = when {
            listOf("hi", "hello", "yo", "hey").any { lowerMessage.contains(it) } -> {
                "👋 Hey there! Welcome back to PizzaBot. 🍕 Please select a branch with 'select branch <branch_name>' or type 'show menu' to start."
            }
            lowerMessage.contains("show menu") || lowerMessage.contains("list pizzas") -> showMenu()
            lowerMessage.startsWith("select branch") -> {
                val branch = lowerMessage.replace("select branch", "").trim()
                database.child("menu").get().addOnSuccessListener { snapshot ->
                    val branches = snapshot.children.flatMap { item ->
                        val branchList = item.child("branches")
                            .getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                        branchList
                    }.distinct()
                    if (branches.contains(branch)) {
                        selectedBranch = branch
                        messages.add(ChatMessage("Branch selected: $branch. Now showing menu...", false))
                        chatAdapter.notifyItemInserted(messages.size - 1)
                        showMenu() // Trigger menu display
                    } else {
                        messages.add(ChatMessage("Invalid branch. Available branches: ${branches.joinToString(", ")}", false))
                        chatAdapter.notifyItemInserted(messages.size - 1)
                    }
                }.addOnFailureListener { exception ->
                    messages.add(ChatMessage("Error checking branches: ${exception.message}", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    Log.e("Chatbot", "Error: ${exception.message}")
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
                        for (item in snapshot.children) {
                            val branches = item.child("branches")
                                .getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                            if (branches.contains(selectedBranch) && (item.child("name").getValue(String::class.java)?.lowercase()
                                    ?: "").contains(lowerMessage)
                            ) {
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
                    }.addOnFailureListener {
                        messages.add(ChatMessage("Error fetching menu items.", false))
                        chatAdapter.notifyItemInserted(messages.size - 1)
                    }
                } else {
                    messages.add(ChatMessage("Please select a branch first with 'select branch <branch_name>' to view menu items.", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                }
                return // Exit early as response is handled asynchronously
            }
        }
        messages.add(ChatMessage(response, false))
        chatAdapter.notifyItemInserted(messages.size - 1)
    }

    private fun showMenu(): String {
        if (selectedBranch == null) {
            database.child("menu").get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    messages.add(ChatMessage("No menu items found in the database.", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    return@addOnSuccessListener
                }
                val branches = snapshot.children.flatMap { item ->
                    val branchList = item.child("branches")
                        .getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                    branchList
                }.distinct()
                if (branches.isNotEmpty()) {
                    messages.add(ChatMessage("Please select a branch from: ${branches.joinToString(", ")}. Type 'select branch <branch_name>' to proceed.", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                }
            }.addOnFailureListener { exception ->
                messages.add(ChatMessage("Error fetching branches: ${exception.message}", false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                Log.e("Chatbot", "Error: ${exception.message}")
            }
            return "Fetching branch options..."
        } else {
            database.child("menu").get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    messages.add(ChatMessage("No menu items found in the database.", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    return@addOnSuccessListener
                }
                for (item in snapshot.children) {
                    val branches = item.child("branches")
                        .getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                    if (branches.contains(selectedBranch)) {
                        val name = item.child("name").getValue(String::class.java) ?: "Unknown Item"
                        val price = item.child("price").getValue(Double::class.java) ?: 0.0
                        val description = item.child("description").getValue(String::class.java) ?: "No description"
                        val paddedName = String.format("%-20s", name)
                        val priceText = String.format("LKR %.0f", price)
                        val formattedText = "• $paddedName ....... $priceText\n  - $description"
                        messages.add(ChatMessage(formattedText, false))
                        chatAdapter.notifyItemInserted(messages.size - 1)
                    }
                }
            }.addOnFailureListener { exception ->
                messages.add(ChatMessage("Error fetching menu: ${exception.message}", false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                Log.e("Chatbot", "Error: ${exception.message}")
            }
            return "Fetching menu for $selectedBranch..."
        }
    }

    private fun selectCategory(message: String): String {
        val category = if (message.contains("vegetarian")) "Vegetarian" else "Non-Vegetarian"
        var response = "Showing $category items:\n"
        if (selectedBranch != null) {
            database.child("menu").orderByChild("category").equalTo(category).get().addOnSuccessListener { snapshot ->
                for (item in snapshot.children) {
                    val branches = item.child("branches")
                        .getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                    if (branches.contains(selectedBranch)) {
                        val name = item.child("name").getValue(String::class.java) ?: ""
                        val price = item.child("price").getValue(Double::class.java) ?: 0.0
                        response += "$name - LKR $price\n"
                    }
                }
                messages.add(ChatMessage(response, false))
                chatAdapter.notifyItemInserted(messages.size - 1)
            }
        } else {
            response = "Please select a branch first with 'select branch <branch_name>'."
            messages.add(ChatMessage(response, false))
            chatAdapter.notifyItemInserted(messages.size - 1)
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
        database.child("carts").child(userId).push().setValue(mapOf("name" to currentPizza, "price" to 2500.0)) // Adjust price dynamically if needed
        return "$currentPizza added to cart! Say 'show cart' or 'pay'."
    }

    private fun viewCart(): String {
        val userId = auth.currentUser?.uid ?: return "Please log in to view cart."
        var response = "Your Cart:\n"
        database.child("carts").child(userId).get().addOnSuccessListener { snapshot ->
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
        }
        return "Fetching cart..."
    }

    private fun initiatePayment(message: String): String {
        val userId = auth.currentUser?.uid ?: return "Please log in to pay."
        return when {
            message.contains("card online") -> {
                // Stripe payment (placeholder)
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
        }
        return "Checking order status..."
    }

    private fun showOrderHistory(): String {
        val userId = auth.currentUser?.uid ?: return "Please log in to view order history."
        var response = "Order History:\n"
        database.child("orders").child(userId).get().addOnSuccessListener { snapshot ->
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
