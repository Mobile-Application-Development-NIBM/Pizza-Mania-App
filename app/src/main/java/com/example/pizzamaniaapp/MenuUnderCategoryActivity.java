// ========== Activity Summary ==========
// MenuUnderCategoryActivity
// - Displays all menu items under a selected category for a branch
// - Features:
//     * Load and display menu items from Firebase
//     * View menu details in a popup (image, description, price)
//     * Adjust quantity and add items to cart
//     * Search menus with autocomplete suggestions
//     * Chatbot button for support
//     * Custom top toast for messages
//     * Custom loading dialog for Firebase operations
// ======================================


// onCreate()
// - Sets layout and adjusts padding for system bars
// - Initializes Firebase DB reference and location services
// - Loads menu items for the selected category and branch
// - Loads cart data for current user
// - Configures search box, button, and autocomplete suggestions
// - Handles menu item click to open detail popup
// - Handles chatbot button click


// loadMenusForBranch(branchID, categoryFilter)
// - Shows loading dialog
// - Fetches menu items for the branch and category from Firebase
// - Parses data into MenuItem objects
// - Updates RecyclerView adapter
// - Handles Firebase errors with custom toast


// loadCart(branchID, customerID)
// - Loads or creates the user's cart from Firebase
// - Keeps track of menu items already added


// showMenuPopup(item)
// - Opens popup for selected menu item
// - Displays image, description, price, and quantity controls
// - Allows adding/updating item in cart
// - Handles Save/Cancel actions


// setupSearch()
// - Initializes search input and button
// - Configures autocomplete suggestions from all menus


// performSearch(query)
// - Filters menu items by name containing query (case-insensitive)
// - Updates adapter with filtered list
// - If no matches, shows toast and resets to full menu list


// updateSearchSuggestions()
// - Updates autocomplete suggestions based on current menu list


// showLoadingDialog(message)
// - Shows custom loading popup with given message
// - Blocks user interaction while visible


// hideLoadingDialog()
// - Dismisses loading dialog if visible


// showCustomToast(message)
// - Shows custom top toast with message
// - Includes close button and progress bar
// - Auto-dismisses after 3 seconds


// MenuItem class
// - Data model for menu items
// - Fields: menuID, name, category, description, price, imageURL, branches


package com.example.pizzamaniaapp; // 📦 Defines the package name for this file, used to organize the app's code.

import android.content.SharedPreferences; // 🗝️ Used to save and get small pieces of data (like user settings).
import android.graphics.Color; // 🎨 Lets you work with colors in your app.
import android.graphics.drawable.ColorDrawable; // 🖌️ Used to create a colored background for UI elements or dialogs.
import android.os.Bundle; // 📦 Holds data passed to an activity when it starts.
import android.os.CountDownTimer; // ⏱️ Creates timers that count down and trigger events.
import android.view.Gravity; // 📐 Helps position elements (e.g., center, top) on the screen.
import android.view.LayoutInflater; // 🏗️ Turns XML layouts into View objects you can use in code.
import android.view.View; // 🧩 The base class for all UI elements.
import android.view.ViewGroup; // 📁 A container that holds other views (like layouts).
import android.view.WindowManager; // 🖥️ Manages window-level properties like size and brightness.
import android.widget.ArrayAdapter; // 📋 Connects a data list to UI components like dropdowns or lists.
import android.widget.AutoCompleteTextView; // ✍️ A text field that shows suggestions as you type.
import android.widget.ImageButton; // 🖼️ A button that shows an image instead of text.
import android.widget.ImageView; // 🖼️ Used to display images in the app.
import android.widget.ProgressBar; // ⏳ Shows loading progress or activity.
import android.widget.TextView; // 📝 Displays text on the screen.

import androidx.annotation.NonNull; // ✅ Marks variables, parameters, or return values as not allowed to be null.
import androidx.appcompat.app.AlertDialog; // 💬 Creates pop-up dialogs with buttons and custom layouts.
import androidx.appcompat.app.AppCompatActivity; // 🏠 Base class for modern Android activities with compatibility support.
import androidx.core.graphics.Insets; // 📏 Represents system bar insets (like status and navigation bars).
import androidx.core.view.ViewCompat; // 🔧 Adds backward-compatible features for Views.
import androidx.core.view.WindowInsetsCompat; // 📐 Helps handle screen areas covered by system UI across versions.
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager; // 📜 Arranges RecyclerView items in a vertical or horizontal list.
import androidx.recyclerview.widget.RecyclerView; // 📚 A powerful view for displaying scrollable lists of items.

import com.bumptech.glide.Glide; // 🖼️ A popular library for loading and caching images.
import com.example.pizzamaniaapp.CustomerHomeActivity.Cart; // 🛒 Represents a shopping cart object.
import com.example.pizzamaniaapp.CustomerHomeActivity.CartItem; // 🛍️ Represents an item inside the shopping cart.
import com.example.pizzamaniaapp.CustomerHomeActivity.MenuItem; // 📋 Represents a menu item in the app.
import com.google.android.gms.location.FusedLocationProviderClient; // 📡 Used to get the device’s current location.
import com.google.android.gms.location.LocationServices; // 🌍 Provides location-related services.
import com.google.android.material.button.MaterialButton; // 🔘 A button styled with Google’s Material Design.
import com.google.android.material.floatingactionbutton.FloatingActionButton; // ➕ A round action button for primary actions.
import com.google.firebase.database.DataSnapshot; // 📤 Holds a snapshot of data from Firebase Realtime Database.
import com.google.firebase.database.DatabaseError; // ⚠️ Represents an error from Firebase Database operations.
import com.google.firebase.database.DatabaseReference; // 📌 Points to a specific path or node in Firebase Database.
import com.google.firebase.database.FirebaseDatabase; // 🌐 Main entry point for using Firebase Realtime Database.
import com.google.firebase.database.ValueEventListener; // 👂 Listens for changes in Firebase Database data.

import java.util.ArrayList; // 📑 A resizable list to store multiple items.
import java.util.List; // 📜 A general interface for working with lists.


public class MenuUnderCategoryActivity extends AppCompatActivity { // 🏠 Defines the activity class for showing menus under a selected category. It extends AppCompatActivity for Android UI features.

    private RecyclerView recyclerView; // 📜 RecyclerView to display the list of menu items.
    private MenuUnderCategoryAdapter adapter; // 🔗 Adapter to bind menu data to the RecyclerView.
    private List<MenuItem> categoryMenus = new ArrayList<>(); // 📋 Stores menu items for the selected category.

    private DatabaseReference dbRef; // 🌐 Reference to Firebase Database for reading/writing data.
    private Cart currentCart; // 🛒 Holds the user's current shopping cart.
    private AlertDialog loadingDialog; // ⏳ Dialog to show a loading indicator while fetching data.

    private FusedLocationProviderClient fusedLocationClient; // 📡 Used to get the user's current location.

    private String currentUserID; // 🆔 Stores the current user's ID.
    private String currentUserName; // 🙍 Stores the current user's name.
    private String branchID; // 🏪 Stores the selected branch ID.

    // ---------------- SEARCH VARIABLES ----------------
    private List<String> searchSuggestions = new ArrayList<>(); // 🔎 Holds text suggestions for the search box.
    private ArrayAdapter<String> suggestionsAdapter; // 🧩 Adapter for showing suggestions in AutoCompleteTextView.
    private List<MenuItem> allMenus = new ArrayList<>(); // 📚 Stores all menus to filter for searching.

    @Override // ✅ Marks this method as overriding a parent class method (AppCompatActivity's onCreate).
    protected void onCreate(Bundle savedInstanceState) { // 🏁 Called when the activity starts; used to set up the UI and data.
        super.onCreate(savedInstanceState); // 🔄 Calls the parent onCreate to ensure proper setup.
        setContentView(R.layout.activity_menu_under_category); // 🖼️ Sets the layout for this screen.

        setupEdgeToEdge(); // 🖌️ Adjusts the layout to use the full screen, handling system bars.

        // Initialize Firebase and location
        dbRef = FirebaseDatabase.getInstance().getReference(); // 🌐 Gets reference to Firebase database root.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this); // 📡 Sets up location service client.

        recyclerView = findViewById(R.id.viewAllRecyclerView); // 📋 Finds RecyclerView from layout to display menus.

        // ====================================================================================
        // 🍕 UPDATED: Changed from LinearLayoutManager to GridLayoutManager for a 2-column grid
        // ====================================================================================
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 📜 Arranges items in a 2-column grid.

        TextView categoryText = findViewById(R.id.categoryText); // 🏷️ TextView to show selected category name.

        // ------------------- GET BRANCH ID -------------------
        branchID = getIntent().getStringExtra("branchID"); // 🏪 Gets branch ID passed from previous activity.
        if (branchID == null) branchID = "defaultBranch"; // 🛠️ Uses "defaultBranch" if no branch ID was passed.

        String categoryName = getIntent().getStringExtra("category_name"); // 🏷️ Gets category name passed from previous activity.
        if (categoryName != null) categoryText.setText(categoryName); // 🖋️ Displays category name if available.

        List<CustomerHomeActivity.MenuItem> menuList = // 📦 Gets a list of menu items passed from previous screen.
                getIntent().getParcelableArrayListExtra("menu_list");

        if (menuList != null) { // ✅ If menu list is provided
            categoryMenus.clear(); // 🧹 Clears old menu list for safety.
            categoryMenus.addAll(menuList); // ➕ Adds provided menus to display list.
            allMenus.clear(); // 🧹 Clears full menus list.
            allMenus.addAll(menuList); // ➕ Adds all menus for searching.

            // Setup search immediately
            setupSearch(); // 🔍 Prepares search box with suggestions.
            updateSearchSuggestions(); // 📜 Updates dropdown suggestions for search.
        } else { // ❌ If menu list was not provided
            loadMenusForBranch(branchID, categoryName); // 🌐 Loads menus from Firebase for the selected branch and category.
        }

        adapter = new MenuUnderCategoryAdapter(categoryMenus, this::showMenuPopup); // 🧩 Creates adapter with menus and click listener for popup.
        recyclerView.setAdapter(adapter); // 🔗 Connects adapter to RecyclerView.

        // ------------------- GET USER INFO -------------------
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE); // 📂 Opens shared preferences to get stored user data.
        currentUserID = prefs.getString("userID", "u001"); // 🆔 Gets stored user ID or uses "u001" as default.
        currentUserName = prefs.getString("name", "User"); // 🙍 Gets stored username or uses "User".

        // ------------------- LOAD CART -------------------
        loadCart(branchID, currentUserID); // 🛒 Loads user's shopping cart for this branch.

        // Back button
        ImageButton backBtn = findViewById(R.id.backButton); // ⬅️ Finds the back button in the layout.
        backBtn.setOnClickListener(v -> finish()); // 🚪 Closes the activity when back button is clicked.

        // Chatbot button
        FloatingActionButton chatFab = findViewById(R.id.chatbotButton); // 💬 Finds floating chatbot button.
        chatFab.setOnClickListener(v -> { // ▶️ Opens chatbot dialog when clicked.
            ChatbotDialogFragment chatbotDialog = new ChatbotDialogFragment(); // 🧠 Creates chatbot dialog instance.
            chatbotDialog.show(getSupportFragmentManager(), "ChatbotDialog"); // 📺 Shows chatbot dialog on screen.
        });
    }

    // This method makes the layout fit the screen edges, handling status and navigation bars 🖼️✨
    private void setupEdgeToEdge() {
        // Set a listener for window insets (like status and navigation bars) 🖲️👀
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // Get the size of system bars (status bar, navigation bar) 📏📲
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Add padding to the view so content won't hide under system bars 🛡️📐
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            // Return the same insets after applying padding 🔁✅
            return insets;
        });
    }

    // Loads menu items from Firebase for a specific branch and optional category 🏪📋
    private void loadMenusForBranch(String branchID, String categoryFilter) {
        // Show a loading dialog to the user while fetching menus ⏳📡
        showLoadingDialog("Loading menus...");

        // Access the "menu" node in Firebase Database and listen once 🔗🗄️
        dbRef.child("menu").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Clear old data before adding new ones 🧹♻️
                allMenus.clear();
                categoryMenus.clear();

                // Loop through all menu items in the snapshot 🔄📑
                for (DataSnapshot menuSnap : snapshot.getChildren()) {
                    // Convert snapshot into a MenuItem object 🔄🍕
                    MenuItem menuItem = menuSnap.getValue(MenuItem.class);

                    // Check: item exists, belongs to branch, and matches category filter ✅🔍
                    if (menuItem != null
                            && menuItem.branches != null
                            && menuItem.branches.contains(branchID)
                            && (categoryFilter == null || menuItem.category.equals(categoryFilter))) {
                        // Add matching item to both lists 📝💾
                        allMenus.add(menuItem);
                        categoryMenus.add(menuItem);
                    }
                }

                // Notify adapter that data changed to refresh RecyclerView 🔄📲
                adapter.notifyDataSetChanged();
                // Hide loading dialog now that menus are loaded ✅🛑
                hideLoadingDialog();

                // Set up search functionality and update suggestions 🔎💡
                setupSearch();
                updateSearchSuggestions();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Hide loading dialog if Firebase call fails ❌🛑
                hideLoadingDialog();
                // Show error message to user ❗🍕
                showCustomToast("Failed to load menus");
            }
        });
    }

    // Loads the shopping cart for the current customer 🛒👤
    private void loadCart(String branchID, String customerID) {
        // Create a unique cart ID using the customer ID 🆔🧾
        String cartID = "c_" + customerID;

        // Reference the "carts" node in Firebase for this cart 🗄️🔗
        DatabaseReference cartRef = dbRef.child("carts").child(cartID);

        // Read the cart data once from Firebase 📡🔄
        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // If the cart already exists, get its data ✅📋
                if (snapshot.exists()) {
                    currentCart = snapshot.getValue(Cart.class);
                } else {
                    // If not found, create a new empty cart for this customer 🆕🛒
                    currentCart = new Cart(cartID, branchID, customerID);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Show a toast if there was an error fetching cart data ❌📢
                showCustomToast("Failed to load cart");
            }
        });
    }

    // Shows a popup dialog with menu item details 🍕📋
    private void showMenuPopup(MenuItem item) {
        // Inflate custom popup layout from XML 🖼️
        View popupView = LayoutInflater.from(this).inflate(R.layout.view_menu_popup, null);

        // Create an AlertDialog with the inflated view 🛑🪟
        AlertDialog popupDialog = new AlertDialog.Builder(this).setView(popupView).create();
        popupDialog.show(); // Show the dialog immediately 👀

        // Customize dialog window appearance ✨
        if (popupDialog.getWindow() != null) {
            int width = (int) (300 * getResources().getDisplayMetrics().density + 0.5f); // Set fixed width (dp → px) 📏
            popupDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background 🪟
            popupDialog.getWindow().getDecorView().post(() ->
                    popupDialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)); // Apply width dynamically ⚡
            popupDialog.getWindow().setGravity(Gravity.CENTER); // Center the popup 🎯
        }

        // Find UI elements inside popup layout 🔍
        TextView title = popupView.findViewById(R.id.popupMenuTitle);
        TextView price = popupView.findViewById(R.id.popupMenuPrice);
        TextView desc = popupView.findViewById(R.id.popupMenuDescription);
        ImageView image = popupView.findViewById(R.id.popupMenuImage);

        // Fill menu details into popup 📋
        title.setText(item.name);
        price.setText("Rs. " + item.price);
        desc.setText(item.description != null ? item.description : "No description");

        // Load menu image with Glide (with placeholder) 🖼️✨
        Glide.with(this).load(item.imageURL).placeholder(R.drawable.sample_pizza).into(image);

        // Quantity UI controls 🔢
        TextView quantityText = popupView.findViewById(R.id.quantityText);
        ImageButton plusBtn = popupView.findViewById(R.id.plusButton);
        ImageButton minusBtn = popupView.findViewById(R.id.minusButton);

        // Default quantity is 1, check if item already exists in cart 🛒
        int existingQuantity = 1;
        if (currentCart != null && currentCart.items != null) {
            for (CartItem ci : currentCart.items) {
                if (ci.menuID.equals(item.menuID)) {
                    existingQuantity = ci.quantity; // If already in cart, use saved quantity
                    break;
                }
            }
        }
        quantityText.setText(String.valueOf(existingQuantity)); // Show initial quantity

        // Increase quantity when + is pressed ➕
        plusBtn.setOnClickListener(v -> {
            int q = Integer.parseInt(quantityText.getText().toString());
            quantityText.setText(String.valueOf(q + 1));
        });

        // Decrease quantity when - is pressed ➖ (minimum = 1)
        minusBtn.setOnClickListener(v -> {
            int q = Integer.parseInt(quantityText.getText().toString());
            if (q > 1) quantityText.setText(String.valueOf(q - 1));
        });

        // Add to cart button 🛒
        MaterialButton addToCartBtn = popupView.findViewById(R.id.addToCartButton);
        addToCartBtn.setOnClickListener(v -> {
            int selectedQuantity = Integer.parseInt(quantityText.getText().toString());
            if (currentCart == null) return; // No cart → exit 🚪

            // Initialize cart items list if empty 📦
            if (currentCart.items == null) currentCart.items = new ArrayList<>();

            boolean found = false; // Flag to check if item already exists in cart
            int initialQuantity = 0;

            // Search for existing item in cart 🔍
            for (CartItem ci : currentCart.items) {
                if (ci.menuID.equals(item.menuID)) {
                    initialQuantity = ci.quantity;
                    if (ci.quantity != selectedQuantity) {
                        ci.quantity = selectedQuantity; // Update quantity if changed
                    }
                    found = true;
                    break;
                }
            }

            // If item not found, add it as a new cart item 🆕🍕
            if (!found) {
                currentCart.items.add(new CartItem(item.menuID, item.name, item.price, selectedQuantity, item.imageURL));
            }

            // If found but quantity unchanged → show message ⚠️
            if (found && initialQuantity == selectedQuantity) {
                showCustomToast("Quantity not changed");
                return;
            }

            // Recalculate total items & total price in cart 🔄💰
            int totalItems = 0;
            double totalPrice = 0;
            for (CartItem ci : currentCart.items) {
                totalItems += ci.quantity;
                totalPrice += ci.quantity * ci.price;
            }
            currentCart.totalItems = totalItems;
            currentCart.totalPrice = totalPrice;

            // Save updated cart to Firebase ✅
            dbRef.child("carts").child(currentCart.cartID).setValue(currentCart)
                    .addOnSuccessListener(aVoid -> showCustomToast("Cart updated")) // Success msg 🎉
                    .addOnFailureListener(e -> showCustomToast("Failed to update cart")); // Error msg ❌

            popupDialog.dismiss(); // Close popup after saving 🚪
        });

        // Close button (X) to dismiss popup ❌
        ImageButton closeBtn = popupView.findViewById(R.id.closeButton);
        closeBtn.setOnClickListener(v -> popupDialog.dismiss());
    }

    // ---------------- UPDATE SEARCH SUGGESTIONS ----------------
// Updates the autocomplete suggestions based on all menu items 🔎✨
    private void updateSearchSuggestions() {
        searchSuggestions.clear(); // Clear previous suggestions 🧹
        for (MenuItem menu : allMenus) {
            if (!searchSuggestions.contains(menu.name)) {
                searchSuggestions.add(menu.name); // Add unique menu names to suggestions 📝
            }
        }
        if (suggestionsAdapter != null) {
            suggestionsAdapter.notifyDataSetChanged(); // Refresh the dropdown with new suggestions 🔄
        }
    }

    // ---------------- SETUP SEARCH ----------------
// Initializes the search box and search button functionality 🛠️
    private void setupSearch() {
        AutoCompleteTextView searchBox = findViewById(R.id.searchBox); // Input field with autocomplete 🔤
        ImageButton searchButton = findViewById(R.id.searchButton); // Button to trigger search 🔍

        // Connect search suggestions to the AutoCompleteTextView 🧩
        suggestionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, searchSuggestions);
        searchBox.setAdapter(suggestionsAdapter);
        searchBox.setThreshold(1); // Start showing suggestions after 1 character ✨

        // Trigger search when button is clicked 🖱️
        searchButton.setOnClickListener(v -> performSearch(searchBox.getText().toString().trim()));
        // Trigger search when user presses enter on keyboard ⌨️
        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            performSearch(searchBox.getText().toString().trim());
            return true;
        });
    }

    // ---------------- PERFORM SEARCH ----------------
// Filters the menu list based on the search query 🔎🍕
    private void performSearch(String query) {
        if (query.isEmpty()) {
            // If search is empty, show all menus 🆓
            categoryMenus.clear();
            categoryMenus.addAll(allMenus);
            adapter.notifyDataSetChanged();
            return;
        }

        List<MenuItem> filtered = new ArrayList<>();
        for (MenuItem menu : allMenus) {
            if (menu.name.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(menu); // Add menus that match query ✅
            }
        }

        categoryMenus.clear();
        if (filtered.isEmpty()) {
            showCustomToast("No menus found"); // Show message if no results ❌
            categoryMenus.addAll(allMenus); // Restore full menu list to avoid blank page 🛡️
        } else {
            categoryMenus.addAll(filtered); // Show only filtered menus 📝
        }

        adapter.notifyDataSetChanged(); // Refresh RecyclerView with search results 🔄
    }

    // ---------------- LOADING & TOAST ----------------

    // Shows a custom loading dialog with a message ⏳🖼️
    private void showLoadingDialog(String message) {
        // If dialog is already visible, do nothing 🚫
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        // Inflate custom loading layout from XML 🖼️
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
        TextView textView = view.findViewById(R.id.loadingText);
        textView.setText(message); // Set the message to show 📝

        // Create AlertDialog with custom view, not cancelable 🔒
        loadingDialog = new AlertDialog.Builder(this).setView(view).setCancelable(false).create();
        loadingDialog.show(); // Show the dialog 👀

        // Customize the dialog window appearance ✨
        if (loadingDialog.getWindow() != null) {
            int width = (int) (300 * getResources().getDisplayMetrics().density); // Width in pixels 📏
            int height = (int) (180 * getResources().getDisplayMetrics().density); // Height in pixels 📐
            loadingDialog.getWindow().setLayout(width, height); // Apply size 🖥️
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent bg 🎨
        }
    }

    // Hides the loading dialog if it is showing ❌⏳
    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }

    // Shows a custom toast message with progress bar and close button 🍞✨
    private void showCustomToast(String message) {
        // Inflate custom toast layout from XML 🖼️
        View layout = LayoutInflater.from(this).inflate(R.layout.custom_message, null);
        TextView toastMessage = layout.findViewById(R.id.toast_message); // Text of toast 📝
        ImageView close = layout.findViewById(R.id.toast_close); // Close button ❌
        ProgressBar progressBar = layout.findViewById(R.id.toast_progress); // Progress bar ⏳

        toastMessage.setText(message); // Set message text 📋
        progressBar.setProgress(100); // Initial progress 100% ✅

        // Create AlertDialog to show custom toast 🛑
        AlertDialog dialog = new AlertDialog.Builder(this).setView(layout).create();
        close.setOnClickListener(v -> dialog.dismiss()); // Close dialog when X clicked 🚪

        // Customize dialog window appearance ✨
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent bg 🎨
            dialog.getWindow().setDimAmount(0f); // No background dim 🌟
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT; // Full width 🖥️
            params.height = WindowManager.LayoutParams.WRAP_CONTENT; // Wrap content height 📐
            params.gravity = Gravity.TOP; // Show at top of screen ⬆️
            params.y = 50; // Offset from top 📏
            dialog.getWindow().setAttributes(params);
        }

        dialog.show(); // Show the custom toast 👀

        // Countdown timer to auto-dismiss toast after 3 seconds ⏱️
        new CountDownTimer(3000, 50) {
            public void onTick(long millisUntilFinished) {
                // Update progress bar based on remaining time ⏳
                int progress = (int) Math.max(0, Math.round((millisUntilFinished / 3000.0) * 100));
                progressBar.setProgress(progress);
            }

            public void onFinish() {
                if (dialog.isShowing()) dialog.dismiss(); // Dismiss toast when timer ends 🚪
            }
        }.start();
    }
}