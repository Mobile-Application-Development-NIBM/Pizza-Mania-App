package com.example.pizzamaniaapp

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import android.util.Log
import com.google.firebase.database.DataSnapshot

class ChatbotDialogFragment : DialogFragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var userInput: EditText
    private lateinit var sendButton: Button
    private val messages = mutableListOf<ChatMessage>()
    private val database = FirebaseDatabase.getInstance("https://pizzamaniaapp-89938-default-rtdb.firebaseio.com/").reference
    private val auth = FirebaseAuth.getInstance()
    private var currentPizza: String? = null
    private var selectedBranch: String? = null
    private val handler = Handler(Looper.getMainLooper()) // For timeout
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sharedPreferences: SharedPreferences
    private var currentUserID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        sharedPreferences = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        currentUserID = sharedPreferences.getString("userID", null)
        Log.d("Chatbot", "Current user ID from SharedPreferences: $currentUserID")
    }

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
            if (!isAdded) return@setOnClickListener // Prevent crash if fragment is detached
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
        if (isAdded) {
            messages.add(ChatMessage("👋 Welcome to PizzaBot! 🍕\n" +
                    "You can try commands like:\n" +
                    "👉 'show menu' to view the menu for the nearest branch\n" +
                    "👉 'track order' to check your order status\n" +
                    "👉 'update profile' to update your details\n" +
                    "👉 'help' for more options\n\n" +
                    "Or just say hi to get started!", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }

        // Test Firebase connectivity
        database.child("test").setValue("test").addOnSuccessListener {
            Log.d("Chatbot", "Firebase write test successful")
        }.addOnFailureListener { exception ->
            Log.e("Chatbot", "Firebase write test failed: ${exception.message}", exception)
        }
        database.child("test").removeValue() // Clean up

        // Test Firebase read
        database.child("menu").get().addOnSuccessListener {
            Log.d("Chatbot", "Firebase read test successful: ${it.exists()}, ${it.childrenCount} items")
        }.addOnFailureListener { exception ->
            Log.e("Chatbot", "Firebase read test failed: ${exception.message}", exception)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.6).toInt()
            setLayout(width, height)
            setGravity(Gravity.CENTER)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null) // Prevent leaks
    }

    private fun processUserMessage(message: String) {
        if (!isAdded) return // Prevent crash if fragment is detached
        val lowerMessage = message.lowercase()
        Log.d("Chatbot", "Processing user message: $lowerMessage")
        val response = when {
            listOf("hi", "hello", "yo", "hey").any { lowerMessage.contains(it) } -> {
                "👋 Hey there! Welcome back to PizzaBot. 🍕 Which pizza would you like today?\n" +
                        "👉 I’d recommend trying our classic Pepperoni!\n" +
                        "👉 Type 'help' to get assistance on how to use the bot!"
            }
            lowerMessage.contains("show menu") || lowerMessage.contains("list items") -> {
                findNearestBranchAndShowMenu()
                "Fetching nearest branch and its menu..."
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
            else -> {
                var selected = false
                if (selectedBranch == null) {
                    messages.add(ChatMessage("Please allow location access or try 'show menu' to use the nearest branch.", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                    return
                }
                database.child("menu").get().addOnSuccessListener { snapshot ->
                    if (!isAdded) return@addOnSuccessListener
                    Log.d("Chatbot", "Menu selection - Snapshot exists: ${snapshot.exists()} | Count: ${snapshot.childrenCount}")
                    for (item in snapshot.children) {
                        val branches = item.child("branches").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
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
                        messages.add(ChatMessage("I didn't understand. Try 'show menu', 'add to cart', or 'help'.", false))
                        chatAdapter.notifyItemInserted(messages.size - 1)
                    }
                    recyclerView.scrollToPosition(messages.size - 1)
                }.addOnFailureListener { exception ->
                    if (!isAdded) return@addOnFailureListener
                    Log.e("Chatbot", "Error selecting menu item: ${exception.message}", exception)
                    messages.add(ChatMessage("Error fetching menu items: ${exception.message}", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                }
                return
            }
        }
        if (isAdded) {
            messages.add(ChatMessage(response, false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }
    }

    private fun findNearestBranchAndShowMenu() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            messages.add(ChatMessage("Location permission is required to find the nearest branch. Please enable it in settings.", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
            return
        }

        messages.add(ChatMessage("Fetching your location to find the nearest branch...", false))
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (!isAdded) return@addOnSuccessListener
                if (location == null) {
                    messages.add(ChatMessage("Unable to get location. Please try again or check your location settings.", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                    return@addOnSuccessListener
                }

                database.child("branches").get().addOnSuccessListener { snapshot ->
                    if (!isAdded) return@addOnSuccessListener
                    var nearestBranchID: String? = null
                    var nearestDistance = Double.MAX_VALUE
                    val branchMap = mapOf("b001" to "Galle", "b002" to "Colombo", "b003" to "Gampaha", "b004" to "Kurunegala", "b005" to "Kalutara")

                    for (branchSnap in snapshot.children) {
                        val branch = branchSnap.getValue(CustomerHomeActivity.Branch::class.java)
                        if (branch != null) {
                            val branchLocation = Location("")
                            branchLocation.latitude = branch.latitude
                            branchLocation.longitude = branch.longitude
                            val distance = location.distanceTo(branchLocation).toDouble()
                            if (distance < nearestDistance) {
                                nearestDistance = distance
                                nearestBranchID = branch.branchID
                            }
                        }
                    }

                    if (nearestBranchID != null) {
                        selectedBranch = nearestBranchID
                        messages.add(ChatMessage("Nearest branch: ${branchMap[nearestBranchID]}. Showing menu...", false))
                        chatAdapter.notifyItemInserted(messages.size - 1)
                        recyclerView.scrollToPosition(messages.size - 1)
                        showMenu()
                    } else {
                        messages.add(ChatMessage("No branches found. Please try again or contact support.", false))
                        chatAdapter.notifyItemInserted(messages.size - 1)
                        recyclerView.scrollToPosition(messages.size - 1)
                    }
                }.addOnFailureListener { exception ->
                    if (!isAdded) return@addOnFailureListener
                    Log.e("Chatbot", "Error fetching branches: ${exception.message}", exception)
                    messages.add(ChatMessage("Error fetching branches: ${exception.message}. Please try again.", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                }
            }.addOnFailureListener { exception ->
                if (!isAdded) return@addOnFailureListener
                Log.e("Chatbot", "Error getting location: ${exception.message}", exception)
                messages.add(ChatMessage("Error getting location: ${exception.message}. Please check your location settings.", false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                recyclerView.scrollToPosition(messages.size - 1)
            }
    }

    private fun showMenu(): String {
        Log.d("Chatbot", "Starting menu fetch for branch: $selectedBranch")
        if (selectedBranch == null) {
            if (isAdded) {
                messages.add(ChatMessage("Please allow location access or try 'show menu' again to use the nearest branch.", false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                recyclerView.scrollToPosition(messages.size - 1)
            }
            return "Please allow location access to view the menu."
        }

        // Set up timeout for Firebase query (10 seconds)
        val timeoutRunnable = Runnable {
            if (isAdded) {
                Log.e("Chatbot", "Menu fetch timed out for branch: $selectedBranch, retrying once...")
                // Retry query
                database.child("menu").get().addOnSuccessListener { snapshot ->
                    if (!isAdded) return@addOnSuccessListener
                    handleMenuSnapshot(snapshot)
                }.addOnFailureListener { exception ->
                    if (!isAdded) return@addOnFailureListener
                    Log.e("Chatbot", "Retry - Error fetching menu: ${exception.message}", exception)
                    messages.add(ChatMessage("Retry failed: Error fetching menu for $selectedBranch: ${exception.message}", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }
        handler.postDelayed(timeoutRunnable, 10000)

        database.child("menu").get().addOnSuccessListener { snapshot ->
            handler.removeCallbacks(timeoutRunnable) // Cancel timeout
            if (!isAdded) return@addOnSuccessListener
            handleMenuSnapshot(snapshot)
        }.addOnFailureListener { exception ->
            handler.removeCallbacks(timeoutRunnable) // Cancel timeout
            if (!isAdded) return@addOnFailureListener
            Log.e("Chatbot", "Error fetching menu: ${exception.message}", exception)
            messages.add(ChatMessage("Error fetching menu for $selectedBranch: ${exception.message}", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }
        return "Fetching menu for $selectedBranch..."
    }

    private fun handleMenuSnapshot(snapshot: DataSnapshot) {
        Log.d("Chatbot", "Snapshot exists: ${snapshot.exists()} | Child count: ${snapshot.childrenCount}")
        if (!snapshot.exists() || snapshot.childrenCount == 0L) {
            Log.w("Chatbot", "No menu items found in the database.")
            messages.add(ChatMessage("No menu items found in the database.", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
            return
        }

        var hasItems = false
        for (item in snapshot.children) {
            Log.d("Chatbot", "Item key: ${item.key} | Value: ${item.value}")
            val branches = item.child("branches").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
            Log.d("Chatbot", "Branches for ${item.key}: $branches | Selected branch: $selectedBranch")
            if (branches.contains(selectedBranch)) {
                hasItems = true
                val name = item.child("name").getValue(String::class.java) ?: "Unknown Item"
                val priceValue = item.child("price").value
                val price = when (priceValue) {
                    is Double -> priceValue
                    is Long -> priceValue.toDouble()
                    else -> 0.0
                }
                val description = item.child("description").getValue(String::class.java) ?: "No description"
                val menuID = item.child("menuID").getValue(String::class.java) ?: ""
                val imageURL = item.child("imageURL").getValue(String::class.java)
                Log.d("Chatbot", "Parsed item - Name: $name | Price: $price | Description: $description | MenuID: $menuID")
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
    }

    private fun selectCategory(message: String): String {
        val category = if (message.contains("vegetarian")) "Vegetarian" else "Non-Vegetarian"
        var response = "Showing $category items:\n"
        if (selectedBranch == null) {
            messages.add(ChatMessage("Please allow location access or try 'show menu' to use the nearest branch.", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
            return "Please allow location access to view the menu."
        }
        database.child("menu").orderByChild("category").equalTo(category).get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
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
                    val priceValue = item.child("price").value
                    val price = when (priceValue) {
                        is Double -> priceValue
                        is Long -> priceValue.toDouble()
                        else -> 0.0
                    }
                    response += "$name - LKR $price\n"
                }
            }
            messages.add(ChatMessage(response, false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }.addOnFailureListener { exception ->
            if (!isAdded) return@addOnFailureListener
            Log.e("Chatbot", "Error fetching $category items: ${exception.message}", exception)
            messages.add(ChatMessage("Error fetching $category items: ${exception.message}", false))
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
        if (currentUserID == null) {
            return "Please log in to add to cart."
        }
        if (currentPizza == null) {
            return "Please select a menu item first."
        }
        if (selectedBranch == null) {
            messages.add(ChatMessage("Please allow location access or try 'show menu' to use the nearest branch.", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
            return "Please allow location access to add to cart."
        }

        database.child("menu").orderByChild("name").equalTo(currentPizza).get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
            Log.d("Chatbot", "Add to cart - Snapshot exists: ${snapshot.exists()} | Count: ${snapshot.childrenCount}")
            val item = snapshot.children.firstOrNull()
            if (item == null) {
                messages.add(ChatMessage("Error: $currentPizza not found in menu.", false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                recyclerView.scrollToPosition(messages.size - 1)
                return@addOnSuccessListener
            }

            val menuID = item.child("menuID").getValue(String::class.java) ?: ""
            val name = item.child("name").getValue(String::class.java) ?: ""
            val priceValue = item.child("price").value
            val price = when (priceValue) {
                is Double -> priceValue
                is Long -> priceValue.toDouble()
                else -> 0.0
            }
            val imageURL = item.child("imageURL").getValue(String::class.java)

            // Load or create cart
            val cartID = "c_$currentUserID"
            database.child("carts").child(cartID).get().addOnSuccessListener { cartSnapshot ->
                if (!isAdded) return@addOnSuccessListener
                var cart = cartSnapshot.getValue(CustomerHomeActivity.Cart::class.java)
                if (cart == null) {
                    cart = CustomerHomeActivity.Cart(cartID, selectedBranch!!, currentUserID!!)
                }
                if (cart.items == null) {
                    cart.items = mutableListOf()
                }

                // Check if item exists in cart
                var found = false
                for (cartItem in cart.items!!) {
                    if (cartItem.menuID == menuID) {
                        cartItem.quantity += 1
                        found = true
                        break
                    }
                }
                if (!found) {
                    cart.items!!.add(CustomerHomeActivity.CartItem(menuID, name, price, 1, imageURL))
                }

                // Update totals
                var totalItems = 0
                var totalPrice = 0.0
                for (cartItem in cart.items!!) {
                    totalItems += cartItem.quantity
                    totalPrice += cartItem.quantity * cartItem.price
                }
                cart.totalItems = totalItems
                cart.totalPrice = totalPrice

                // Save to Firebase
                database.child("carts").child(cartID).setValue(cart).addOnSuccessListener {
                    if (!isAdded) return@addOnSuccessListener
                    messages.add(ChatMessage("$name added to cart! Say 'show cart' or 'pay'.", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)

                    // Notify CustomerHomeActivity to update cart badge
                    if (activity is CustomerHomeActivity) {
                        (activity as CustomerHomeActivity).updateCartBadge()
                    }
                }.addOnFailureListener { exception ->
                    if (!isAdded) return@addOnFailureListener
                    Log.e("Chatbot", "Error saving cart: ${exception.message}", exception)
                    messages.add(ChatMessage("Error adding to cart: ${exception.message}", false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                }
            }.addOnFailureListener { exception ->
                if (!isAdded) return@addOnFailureListener
                Log.e("Chatbot", "Error loading cart: ${exception.message}", exception)
                messages.add(ChatMessage("Error loading cart: ${exception.message}", false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                recyclerView.scrollToPosition(messages.size - 1)
            }
        }.addOnFailureListener { exception ->
            if (!isAdded) return@addOnFailureListener
            Log.e("Chatbot", "Error fetching menu item: ${exception.message}", exception)
            messages.add(ChatMessage("Error adding to cart: ${exception.message}", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }
        return "Adding $currentPizza to cart..."
    }

    private fun viewCart(): String {
        if (currentUserID == null) {
            return "Please log in to view cart."
        }
        val cartID = "c_$currentUserID"
        var response = "Your Cart:\n"
        database.child("carts").child(cartID).get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
            Log.d("Chatbot", "View cart - Snapshot exists: ${snapshot.exists()} | Count: ${snapshot.childrenCount}")
            val cart = snapshot.getValue(CustomerHomeActivity.Cart::class.java)
            if (cart == null || cart.items.isNullOrEmpty()) {
                response = "Your cart is empty."
            } else {
                for (item in cart.items!!) {
                    response += "${item.name} - LKR ${item.price} x ${item.quantity}\n"
                }
                response += "Total: LKR ${cart.totalPrice}"
            }
            messages.add(ChatMessage(response, false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }.addOnFailureListener { exception ->
            if (!isAdded) return@addOnFailureListener
            Log.e("Chatbot", "Error fetching cart: ${exception.message}", exception)
            messages.add(ChatMessage("Error fetching cart: ${exception.message}", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }
        return "Fetching cart..."
    }

    private fun initiatePayment(message: String): String {
        if (currentUserID == null) {
            return "Please log in to pay."
        }
        if (currentPizza == null) {
            return "Please add items to cart first."
        }
        return when {
            message.contains("card online") -> {
                "Redirecting to online card payment... [Stripe integration required]"
            }
            message.contains("card on delivery") -> {
                database.child("orders").child(currentUserID!!).push().setValue(
                    mapOf("name" to currentPizza, "payment" to "Card on Delivery", "status" to "Pending")
                )
                "Order placed with card on delivery. You'll provide card details to the delivery agent."
            }
            message.contains("cash") || message.contains("cod") -> {
                database.child("orders").child(currentUserID!!).push().setValue(
                    mapOf("name" to currentPizza, "payment" to "Cash on Delivery", "status" to "Pending")
                )
                "Order placed with cash on delivery."
            }
            else -> "Please specify payment method: 'pay card online', 'pay card on delivery', or 'pay cash'."
        }
    }

    private fun trackOrder(): String {
        if (currentUserID == null) {
            return "Please log in to track orders."
        }
        var response = "Order Status:\n"
        database.child("orders").child(currentUserID!!).get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
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
            if (!isAdded) return@addOnFailureListener
            Log.e("Chatbot", "Error tracking order: ${exception.message}", exception)
            messages.add(ChatMessage("Error tracking order: ${exception.message}", false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }
        return "Checking order status..."
    }

    private fun showOrderHistory(): String {
        if (currentUserID == null) {
            return "Please log in to view order history."
        }
        var response = "Order History:\n"
        database.child("orders").child(currentUserID!!).get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
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
            if (!isAdded) return@addOnFailureListener
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
        if (currentUserID == null) {
            return "Please log in to update profile."
        }
        return when {
            message.contains("address") -> {
                val address = message.substringAfter("address").trim()
                database.child("users").child(currentUserID!!).child("address").setValue(address)
                "Address updated to: $address"
            }
            message.contains("name") -> {
                val name = message.substringAfter("name").trim()
                database.child("users").child(currentUserID!!).child("name").setValue(name)
                "Name updated to: $name"
            }
            else -> "Please specify what to update (e.g., 'update address to 123 Main St' or 'update name to John')."
        }
    }

    private fun submitFeedback(): String {
        return "Thanks for your feedback! Please share your comments, and I'll save them."
    }

    private fun contactSupport(): String {
        return "Contact support at: evanonmail@gmail.com or call: +94752260132\n\n" +
                "Type:\n" +
                "👉 'show menu' - To view the menu for the nearest branch\n" +
                "👉 'add to cart' - To add items to the cart\n" +
                "👉 'show cart' - To view your cart\n" +
                "👉 'add topping' - To add extra toppings\n" +
                "👉 'pay' - To pay for your order\n" +
                "Feel free to reach out for more information."
    }

    private fun manageAccount(message: String): String {
        return when {
            message.contains("log out") -> {
                auth.signOut()
                sharedPreferences.edit().clear().apply()
                "Logged out successfully."
            }
            message.contains("password") -> "To update password, please use the main app's account settings."
            else -> "Account options: Say 'log out' or 'update password'."
        }
    }
}