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

class ChatbotDialogFragment : DialogFragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var userInput: EditText
    private lateinit var sendButton: Button
    private val messages = mutableListOf<ChatMessage>()
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private var currentPizza: String? = null // Track selected pizza for context

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
                "👋 Hey there! Welcome back to PizzaBot. 🍕 Which pizza would you like today?\n" +
                        "👉 I’d recommend trying our classic Pepperoni!"
            }
            lowerMessage.contains("show menu") || lowerMessage.contains("list pizzas") -> showMenu()
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
                // Check if user is selecting a pizza from the menu
                var selected = false
                database.child("pizzas").get().addOnSuccessListener { snapshot ->
                    for (pizza in snapshot.children) {
                        val name = pizza.child("name").getValue(String::class.java)?.lowercase() ?: ""
                        if (lowerMessage.contains(name)) {
                            currentPizza = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            messages.add(ChatMessage("Selected $currentPizza. Add toppings (e.g., 'add extra cheese') or say 'add to cart'.", false))
                            chatAdapter.notifyItemInserted(messages.size - 1)
                            selected = true
                        }
                    }
                    if (!selected) {
                        messages.add(ChatMessage("I didn't understand. Try 'show menu', 'track order', 'update profile', or 'help'.", false))
                        chatAdapter.notifyItemInserted(messages.size - 1)
                    }
                }
                "Processing..."
            }
        }
        messages.add(ChatMessage(response, false))
        chatAdapter.notifyItemInserted(messages.size - 1)
    }

    private fun showMenu(): String {
        var response = "Menu:\n"
        database.child("pizzas").get().addOnSuccessListener { snapshot ->
            for (pizza in snapshot.children) {
                val name = pizza.child("name").getValue(String::class.java) ?: ""
                val price = pizza.child("price").getValue(Double::class.java) ?: 0.0
                response += "$name - $$price\n"
            }
            messages.add(ChatMessage(response, false))
            chatAdapter.notifyItemInserted(messages.size - 1)
        }
        return "Fetching menu..."
    }

    private fun selectCategory(message: String): String {
        val category = if (message.contains("vegetarian")) "Vegetarian" else "Non-Vegetarian"
        var response = "Showing $category pizzas:\n"
        database.child("pizzas").orderByChild("category").equalTo(category).get().addOnSuccessListener { snapshot ->
            for (pizza in snapshot.children) {
                val name = pizza.child("name").getValue(String::class.java) ?: ""
                val price = pizza.child("price").getValue(Double::class.java) ?: 0.0
                response += "$name - $$price\n"
            }
            messages.add(ChatMessage(response, false))
            chatAdapter.notifyItemInserted(messages.size - 1)
        }
        return "Fetching $category pizzas..."
    }

    private fun addToppings(message: String): String {
        if (currentPizza == null) return "Please select a pizza first (e.g., 'Margherita')."
        val topping = message.substringAfter("add").trim()
        return "Added $topping to $currentPizza!"
    }

    private fun addToCart(): String {
        val userId = auth.currentUser?.uid ?: return "Please log in to add to cart."
        if (currentPizza == null) return "Please select a pizza first (e.g., 'Margherita')."
        database.child("carts").child(userId).push().setValue(mapOf("pizza" to currentPizza, "toppings" to "Extra Cheese"))
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
                    val pizza = item.child("pizza").getValue(String::class.java) ?: ""
                    val toppings = item.child("toppings").getValue(String::class.java) ?: ""
                    response += "$pizza ($toppings)\n"
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
                    mapOf("pizza" to currentPizza, "payment" to "Card on Delivery", "status" to "Pending")
                )
                "Order placed with card on delivery. You'll provide card details to the delivery agent."
            }
            message.contains("cash") || message.contains("cod") -> {
                database.child("orders").child(userId).push().setValue(
                    mapOf("pizza" to currentPizza, "payment" to "Cash on Delivery", "status" to "Pending")
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
                    val pizza = order.child("pizza").getValue(String::class.java) ?: ""
                    val status = order.child("status").getValue(String::class.java) ?: ""
                    response += "Order #${order.key}: $pizza ($status)\n"
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
                    val pizza = order.child("pizza").getValue(String::class.java) ?: ""
                    val status = order.child("status").getValue(String::class.java) ?: ""
                    response += "Order #${order.key}: $pizza ($status)\n"
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