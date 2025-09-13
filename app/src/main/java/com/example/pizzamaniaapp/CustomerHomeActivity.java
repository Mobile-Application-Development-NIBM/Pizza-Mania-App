// ========== Activity Summary ==========
// CustomerHomeActivity
// - Main customer-facing screen for viewing menus, managing a shopping cart, and placing orders.
// - Features:
//     * Automatically detects user's location to find the nearest branch.
//     * Loads and displays menu items specifically for that branch.
//     * Provides a real-time synchronized shopping cart tied to the user's ID.
//     * Allows users to view menu details, adjust item quantities, and add items to the cart.
//     * Includes a search bar with auto-suggestions for menu items.
//     * Navigates to a chatbot for customer support.
//     * Uses custom UI components for loading states, toasts, and dialogs.
// ======================================


// onCreate()
// - Initializes Firebase, Google location services, and UI components.
// - Sets up the RecyclerView to display menus.
// - Sets up a real-time listener for the user's cart to keep the UI in sync.
// - Configures the chatbot button and cart button click handlers.
// - Checks for location permissions and initiates the location-based branch selection.


// checkPermissionsAndLoad()
// - Checks if the app has `ACCESS_FINE_LOCATION` permission.
// - If not granted, requests it from the user.
// - If granted, proceeds to check if GPS is enabled.


// checkGpsAndFetchLocation()
// - Verifies if the device's GPS provider is enabled.
// - If GPS is off, displays a dialog asking the user to enable it.
// - If GPS is on, calls `fetchCurrentLocation()` to get the user's coordinates.


// fetchCurrentLocation()
// - Uses the `FusedLocationProviderClient` to get the device's current location with high accuracy.
// - Once a location is obtained, it calls `findNearestBranchAndLoadMenus()`.
// - Handles cases where the location is null (e.g., no GPS signal).


// findNearestBranchAndLoadMenus(userLocation)
// - Queries Firebase for a list of all branches.
// - Calculates the distance from the user's location to each branch.
// - Determines the nearest branch and stores its ID.
// - After finding the nearest branch, calls `loadMenusForBranch()` and `loadCart()`.


// loadMenusForBranch(branchID)
// - Fetches all menu items from Firebase.
// - Filters the list to include only those menu items that are available at the specified `branchID`.
// - Updates the RecyclerView adapter with this filtered list and refreshes search suggestions.


// loadCart(branchID, customerID)
// - Fetches the user's cart data from Firebase based on their unique ID.
// - If the cart exists, it loads the data; otherwise, it creates a new empty cart object.
// - Updates the cart badge on the screen.


// setupCartListener(customerID)
// - Establishes a real-time Firebase listener on the user's cart.
// - Any changes to the cart in the database will automatically trigger an update to the UI, keeping the cart badge and data synchronized across sessions.


// showMenuPopup(item)
// - Displays a pop-up dialog with details of a specific menu item.
// - Allows the user to view the item's name, price, and description.
// - Includes controls to adjust the quantity of the item.
// - The "Add to Cart" button handles updating the cart data locally and in Firebase.
// ======================================


// Defines the package name for this class (helps organize your project structure)
package com.example.pizzamaniaapp;

// Android framework imports for permissions, intents, UI, logging, etc.
import android.Manifest; // Used for requesting dangerous permissions (e.g., location)
import android.content.Intent; // To navigate between activities or open system settings
import android.content.SharedPreferences; // To store/retrieve small persistent data (userID, settings, etc.)
import android.content.pm.PackageManager; // To check if permissions are granted
import android.graphics.Color; // To set custom colors
import android.graphics.drawable.ColorDrawable; // To set transparent/custom backgrounds for dialogs
import android.location.Location; // Represents a geographic location (lat, lng, accuracy, etc.)
import android.location.LocationManager; // For checking if GPS is enabled
import android.os.Bundle; // Holds saved state data when activity is created/recreated
import android.os.CountDownTimer; // For countdown functionality (e.g., dismiss toast after X seconds)
import android.os.Parcel; // Used for writing data to a parcel (serialization)
import android.os.Parcelable; // Interface for passing objects between activities
import android.provider.Settings; // For opening device settings (e.g., GPS settings)
import android.util.Log; // For logging debug/info messages
import android.view.Gravity; // For positioning dialogs/toasts (e.g., top, bottom, center)
import android.view.LayoutInflater; // To inflate XML layouts into Java objects (Views)
import android.view.View; // Base class for UI components
import android.view.ViewGroup; // Used for layouts and View hierarchy
import android.view.WindowManager; // To adjust window properties like size, dim, etc.
import android.widget.ArrayAdapter; // Adapter for displaying a list of suggestions (e.g., search box)
import android.widget.AutoCompleteTextView; // Text field with auto-complete suggestions
import android.widget.ImageButton; // Button with an image (used for cart/search buttons)
import android.widget.ImageView; // To display images
import android.widget.ProgressBar; // To show progress (used in custom toast + loading)
import android.widget.TextView; // Basic text UI component

// AndroidX imports for backward compatibility and extra functionality
import androidx.annotation.NonNull; // Annotation to indicate parameter/return value cannot be null
import androidx.appcompat.app.AlertDialog; // For creating popup dialogs
import androidx.appcompat.app.AppCompatActivity; // Base class for activities with support libraries
import androidx.core.app.ActivityCompat; // Helper for handling runtime permissions
import androidx.recyclerview.widget.DividerItemDecoration; // Adds dividers between RecyclerView items
import androidx.recyclerview.widget.LinearLayoutManager; // Layout manager for RecyclerView (vertical list)
import androidx.recyclerview.widget.RecyclerView; // Flexible list/grid UI component

// Third-party libraries
import com.bumptech.glide.Glide; // Library for loading images from URLs efficiently
import com.example.pizzamaniaapp.CartAdapter; // Custom adapter for displaying cart items in RecyclerView

// Google Play Services (for location)
import com.google.android.gms.location.FusedLocationProviderClient; // Provides location services
import com.google.android.gms.location.LocationServices; // Entry point to get FusedLocationProviderClient
import com.google.android.gms.location.Priority; // Defines location request priority (accuracy vs. battery)

// Material Design components
import com.google.android.material.button.MaterialButton; // Styled button with Material design
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Round button (used for chatbot)

// Firebase imports
import com.google.firebase.database.DataSnapshot; // Snapshot of Firebase database node
import com.google.firebase.database.DatabaseError; // Error handling for Firebase
import com.google.firebase.database.DatabaseReference; // Reference to a Firebase DB location
import com.google.firebase.database.FirebaseDatabase; // Main entry point for Firebase Realtime Database
import com.google.firebase.database.ValueEventListener; // Listener for DB value changes

// Java utility imports
import java.io.Serializable; // Marker interface for serializable classes (not used often with Firebase)
import java.util.ArrayList; // Resizable list implementation
import java.util.HashMap;
import java.util.List; // List interface
import java.util.Map;


// Main activity for the customer’s home screen
// Extends AppCompatActivity so it has full Activity lifecycle support + compatibility features
public class CustomerHomeActivity extends AppCompatActivity {

    // Request code constant for asking location permission at runtime
    private static final int LOCATION_PERMISSION_REQUEST = 100;

    // Tag used for logging (helps identify log messages for this Activity)
    private static final String TAG = "CustomerHome";

    // Holds the branch ID of the nearest branch (once determined by GPS)
    private String currentBranchID;

    // Google Play Services client for accessing device’s location
    private FusedLocationProviderClient fusedLocationClient;

    // Firebase Realtime Database reference (root of DB)
    private DatabaseReference dbRef;

    // RecyclerView to display categories/menus
    private RecyclerView recyclerView;

    // List to store all menus fetched from Firebase for the current branch
    private List<MenuItem> menuList = new ArrayList<>();

    // Adapter for binding menuList data to RecyclerView
    private CustomerCategoryAdapter categoryAdapter;

    // ---------------- CART VARIABLES ----------------

    // Holds the current user’s cart (retrieved/created in Firebase)
    Cart currentCart;

    // TextView that shows the number of items in the cart (as a badge on cart icon)
    private TextView cartBadge;

    // TextView inside the cart popup dialog showing total price
    public TextView cartPopupTotalText;

    // ---------------- LOADING DIALOG ----------------

    // Reference to a loading dialog (used to block UI and show progress while fetching data)
    private AlertDialog loadingDialog;

    // ---------------- USER INFO ----------------

    // Current user’s name (fetched from Firebase "users" table)
    private String currentUsername; // will be initialized from SharedPreferences

    // Current user’s unique ID (fetched from SharedPreferences)
    private String currentUserID;

    // ---------------- SEARCH ----------------

    // Stores search suggestions (menu names and categories)
    private List<String> searchSuggestions = new ArrayList<>();

    // Adapter to bind suggestions list to the AutoCompleteTextView (search box)
    private ArrayAdapter<String> suggestionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Debug log so you can see in Logcat when this Activity starts
        Log.d(TAG, "onCreate: Activity started");

        // Inflate and display the activity layout (UI defined in res/layout/activity_customer_home.xml)
        setContentView(R.layout.activity_customer_home);

        // -------------------- RECYCLER VIEW SETUP --------------------

        // Find the RecyclerView in the layout (used to display categories/menus)
        recyclerView = findViewById(R.id.branchRecyclerView);

        // Set the layout manager so items appear in a vertical scrolling list
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create the adapter for menu categories
        //   - this: context (Activity)
        //   - menuList: the list of menus (initially empty, will be filled later)
        //   - this::showMenuPopup: callback function when an item is clicked (opens menu popup)
        //   - "initialBranch": branch placeholder (will later be replaced with actual nearest branch)
        categoryAdapter = new CustomerCategoryAdapter(this, menuList, this::showMenuPopup, "initialBranch");

        // Attach adapter to RecyclerView
        recyclerView.setAdapter(categoryAdapter);

        // -------------------- LOCATION + DATABASE --------------------

        // Initialize Google Play Services fused location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get root Firebase database reference
        dbRef = FirebaseDatabase.getInstance().getReference();

        // -------------------- CHATBOT BUTTON --------------------

        // Floating Action Button for chatbot (small round button in bottom corner)
        FloatingActionButton chatFab = findViewById(R.id.chatbotButton);

        // Set click listener -> open chatbot dialog fragment
        chatFab.setOnClickListener(v -> {
            ChatbotDialogFragment chatbotDialog = new ChatbotDialogFragment();
            chatbotDialog.show(getSupportFragmentManager(), "ChatbotDialog");
        });

        // -------------------- CART BADGE --------------------

        // Find the badge TextView (small number bubble on cart icon)
        cartBadge = findViewById(R.id.cartBadge);

        // Update cart badge count (initially from Firebase or empty)
        updateCartBadge();

        // -------------------- LOCATION PERMISSIONS --------------------

        // Check location permission and if granted -> load nearest branch + menu
        checkPermissionsAndLoad();

        // -------------------- CART BUTTON --------------------

        // Find the cart button (shopping cart icon)
        ImageButton cartButton = findViewById(R.id.cartButton);

        // When clicked -> show popup with cart items
        cartButton.setOnClickListener(v -> showCartPopup());

        // -------------------- CART LISTENER --------------------

        // Setup Firebase listener for current user’s cart (keeps cart in sync in real-time)
        setupCartListener(currentUserID);

        // -------------------- SEARCH --------------------

        // Initialize search box + suggestion system
        setupSearch();

        // -------------------- USER INFO --------------------

        // Get SharedPreferences (persistent storage for small data)
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        // Load currentUserID from prefs (fallback to "u001" if not found)
        currentUserID = prefs.getString("userID", "u001");

        // -------------------- FETCH USERNAME FROM FIREBASE --------------------

        // Reference to "users" table in Firebase
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Get current user’s data once from Firebase
        usersRef.child(currentUserID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                // Fetch the "name" field
                currentUsername = task.getResult().child("name").getValue(String.class);

                // If no name found, fallback to generic "User"
                if (currentUsername == null) currentUsername = "User";
            } else {
                // If user not found in DB -> use "User"
                currentUsername = "User";
            }
        });
    }

    // -------------------- UPDATE CART BADGE --------------------
    public void updateCartBadge() {
        // Check if currentCart exists AND has at least one item
        if (currentCart != null && currentCart.totalItems > 0) {
            // Show the number of items as text inside the badge
            cartBadge.setText(String.valueOf(currentCart.totalItems));

            // Make the badge visible (since items exist)
            cartBadge.setVisibility(View.VISIBLE);
        } else {
            // If cart is empty or null -> hide the badge completely
            cartBadge.setVisibility(View.GONE);
        }
    }

    // -------------------- CHECK PERMISSIONS --------------------
    private void checkPermissionsAndLoad() {
        // Check if the app has permission to access fine location (GPS-level accuracy)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // If NOT granted -> request permission from the user
            // LOCATION_PERMISSION_REQUEST is the request code (used in onRequestPermissionsResult)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            // If permission is already granted -> proceed to check GPS and fetch location
            checkGpsAndFetchLocation();
        }
    }


    // Dialog reference used to prompt the user to enable GPS
    private AlertDialog gpsDialog;

    private void checkGpsAndFetchLocation() {
        // Get the system service for managing location providers (GPS, Network, etc.)
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        // -------------------- CHECK IF GPS IS ENABLED --------------------
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // If GPS is OFF...

            // Prevent multiple dialogs from showing at the same time
            if (gpsDialog != null && gpsDialog.isShowing()) return;

            // Build an AlertDialog asking user to enable GPS
            gpsDialog = new AlertDialog.Builder(this)
                    .setTitle("Enable GPS") // Dialog title
                    .setMessage("GPS is required to find the nearest branch. Enable it?") // Explanation
                    // "Yes" button -> open device location settings so user can enable GPS
                    .setPositiveButton("Yes", (dialog, which) -> {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    })
                    // "No" button -> show custom toast and dismiss dialog
                    .setNegativeButton("No", (dialog, which) -> {
                        showCustomToast("Cannot proceed without GPS");
                        dialog.dismiss();
                    })
                    // Make dialog non-cancelable (user must pick Yes/No, can’t just tap outside)
                    .setCancelable(false)
                    .create();

            // Show the dialog
            gpsDialog.show();

        } else {
            // -------------------- GPS IS ENABLED --------------------

            // If dialog is still showing from earlier, dismiss it
            if (gpsDialog != null && gpsDialog.isShowing()) gpsDialog.dismiss();

            // Proceed to actually fetch the user’s current location
            fetchCurrentLocation();
        }
    }

    // Global variable at the top of your activity
    private Location lastUserLocation; // ✅ stores the last fetched user location

    // -------------------- FETCH CURRENT LOCATION --------------------
    private void fetchCurrentLocation() {
        // Check again if location permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        // Show loading dialog while location is being fetched
        showLoadingDialog("Fetching location...");

        // Request current location with HIGH accuracy (GPS)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    // Hide loading dialog once we get a response
                    hideLoadingDialog();

                    if (location != null) {
                        // ✅ Store the location globally for later use in placeOrder()
                        lastUserLocation = location;

                        // If location is valid -> find nearest branch
                        findNearestBranchAndLoadMenus(location);
                    } else {
                        // If location is null (happens indoors sometimes) -> show message
                        showCustomToast("Location is null, move outside for GPS fix");
                    }
                });
    }

    // Map to store branches by ID for quick lookup
    private Map<String, Branch> branchMap = new HashMap<>();

    // -------------------- FIND NEAREST BRANCH + LOAD MENUS --------------------
    private void findNearestBranchAndLoadMenus(Location userLocation) {
        // Show loading dialog while scanning branches
        showLoadingDialog("Finding nearest branch...");

        // Query the "branches" node in Firebase once
        dbRef.child("branches").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nearestBranchID = null;
                double nearestDistance = Double.MAX_VALUE; // Start with infinity

                branchMap.clear(); // Clear any previous entries

                // Loop through all branches in Firebase
                for (DataSnapshot branchSnap : snapshot.getChildren()) {
                    Branch branch = branchSnap.getValue(Branch.class);
                    if (branch != null) {
                        // Store branch in map for later use
                        branchMap.put(branch.branchID, branch);

                        // Create a Location object for the branch
                        Location branchLocation = new Location("");
                        branchLocation.setLatitude(branch.latitude);
                        branchLocation.setLongitude(branch.longitude);

                        // Calculate distance between user and branch
                        float distance = userLocation.distanceTo(branchLocation);

                        // Keep track of the nearest branch
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestBranchID = branch.branchID;
                        }
                    }
                }

                // Done scanning branches
                hideLoadingDialog();

                if (nearestBranchID != null) {
                    // Save the nearest branch globally
                    currentBranchID = nearestBranchID;

                    // Mark that we successfully fetched a location
                    locationFetched = true;

                    // Load current user’s cart from this branch
                    loadCart(nearestBranchID, currentUserID);

                    // Load menus for this branch
                    loadMenusForBranch(nearestBranchID);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // If Firebase query fails -> hide dialog
                hideLoadingDialog();
            }
        });
    }

    // -------------------- HELPER METHOD --------------------
    private Branch getBranchByID(String branchID) {
        return branchMap.get(branchID); // Returns cached branch or null
    }

    // Flag to ensure location is only fetched once per session
    private boolean locationFetched = false;

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: re-checking permissions and GPS");

        if (!locationFetched) {
            // If we haven’t fetched location yet -> check permissions & fetch location
            checkPermissionsAndLoad(); // ✅ runs GPS + nearest branch detection
        } else {
            // If location already fetched -> no need to re-fetch GPS again
            // Instead, just refresh cart & menus (in case things changed)

            // Reload the user’s cart for the already-determined branch
            loadCart(currentBranchID, currentUserID);

            // Refresh adapter -> makes sure quantities in menu popup update properly
            categoryAdapter.notifyDataSetChanged();

            // Refresh cart badge (in case items were added/removed)
            updateCartBadge();
        }

        // Extra safety: if GPS dialog is still open but user enabled GPS manually, close the dialog
        if (gpsDialog != null && gpsDialog.isShowing()) {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gpsDialog.dismiss();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);

        // Check if the callback is for our location permission request
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            // If user granted location permission
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted");

                // Continue -> check GPS and fetch location
                checkGpsAndFetchLocation();
            } else {
                // User denied location permission
                Log.d(TAG, "Location permission denied");

                // Show toast telling user why it’s needed
                showCustomToast("Location permission denied. App needs location to find nearest branch.");

                // (Optional) You could also open app settings here so they can enable permission manually
            }
        }
    }


    // -------------------- LOAD MENUS FOR BRANCH --------------------
    private void loadMenusForBranch(String branchID) {
        // Show loading dialog while fetching menu items
        showLoadingDialog("Loading menus...");

        // Access the "menu" node from Firebase (contains all menu items)
        dbRef.child("menu").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Clear the old list before adding fresh data
                menuList.clear();

                // Loop through all menu items in Firebase
                for (DataSnapshot menuSnap : snapshot.getChildren()) {
                    // Convert snapshot into MenuItem object
                    MenuItem menuItem = menuSnap.getValue(MenuItem.class);

                    // Only add menu items that:
                    // 1. Are not null
                    // 2. Have a "branches" list
                    // 3. Include this branchID (means this menu item is available at the current branch)
                    if (menuItem != null && menuItem.branches != null && menuItem.branches.contains(branchID)) {
                        menuList.add(menuItem);
                    }
                }

                // Update the adapter with the new filtered menu list
                // Also pass branchID (so menu popups know which branch we’re working with)
                categoryAdapter.updateList(menuList, branchID);

                // Refresh search suggestions (so autocomplete includes the new menu names)
                updateSearchSuggestions();

                // Done -> hide the loading dialog
                hideLoadingDialog();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // If Firebase fails -> hide dialog
                hideLoadingDialog();
            }
        });
    }

    // ---------------- CART METHODS ----------------

    // Load the cart for a specific branch and customer
    private void loadCart(String branchID, String customerID) {
        // Construct a cart ID (unique per customer)
        String cartID = "c_" + customerID;

        // Reference to this cart in Firebase
        DatabaseReference cartRef = dbRef.child("carts").child(cartID);

        // Single-time read from Firebase
        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Cart exists in Firebase -> load it
                    currentCart = snapshot.getValue(Cart.class);
                } else {
                    // Cart doesn't exist -> create a new empty cart
                    currentCart = new Cart(cartID, branchID, customerID);
                }

                // Update the cart badge (number of items) in UI
                updateCartBadge();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Firebase read failed
                showCustomToast("Failed to load cart");
            }
        });
    }

    // Setup a real-time listener to track cart changes for this customer
    private void setupCartListener(String customerID) {
        // Construct cart ID
        String cartID = "c_" + customerID;

        // Reference to cart in Firebase
        DatabaseReference cartRef = dbRef.child("carts").child(cartID);

        // Attach a value listener -> fires whenever cart changes
        cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Cart exists -> update currentCart
                    currentCart = snapshot.getValue(Cart.class);
                } else {
                    // Cart doesn't exist -> initialize a new empty cart
                    currentCart = new Cart(cartID, "initialBranch", customerID);
                }

                // Update cart badge in UI to reflect changes
                updateCartBadge();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Firebase failed to fetch cart data
                showCustomToast("Failed to load cart");
            }
        });
    }

    // ---------------- MENU POPUP ----------------
    private void showMenuPopup(MenuItem item) {
        // Inflate custom layout for menu popup
        View popupView = LayoutInflater.from(this).inflate(R.layout.view_menu_popup, null);

        // Create an AlertDialog using the inflated view
        AlertDialog popupDialog = new AlertDialog.Builder(this).setView(popupView).create();
        popupDialog.show();

        // Adjust dialog window size & appearance
        if (popupDialog.getWindow() != null) {
            int width = (int) (300 * getResources().getDisplayMetrics().density + 0.5f); // Convert dp to pixels
            popupDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background
            // Ensure layout is applied after view is drawn
            popupDialog.getWindow().getDecorView().post(() ->
                    popupDialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT));
            popupDialog.getWindow().setGravity(Gravity.CENTER); // Center dialog on screen
        }

        // ---------------- BIND DATA TO UI ----------------
        TextView title = popupView.findViewById(R.id.popupMenuTitle);
        TextView price = popupView.findViewById(R.id.popupMenuPrice);
        TextView desc = popupView.findViewById(R.id.popupMenuDescription);
        ImageView image = popupView.findViewById(R.id.popupMenuImage);

        title.setText(item.name); // Menu name
        price.setText("Rs. " + item.price); // Price
        desc.setText(item.description != null ? item.description : "No description"); // Description fallback
        // Load image with Glide, fallback to sample image
        Glide.with(this).load(item.imageURL).placeholder(R.drawable.sample_pizza).into(image);

        // ---------------- QUANTITY HANDLING ----------------
        TextView quantityText = popupView.findViewById(R.id.quantityText);
        ImageButton plusBtn = popupView.findViewById(R.id.plusButton);
        ImageButton minusBtn = popupView.findViewById(R.id.minusButton);

        // Set initial quantity based on existing cart, default to 1
        int initialQuantity = 1;
        if (currentCart != null && currentCart.items != null) {
            for (CartItem ci : currentCart.items) {
                if (ci.menuID.equals(item.menuID)) {
                    initialQuantity = ci.quantity; // Use existing quantity
                    break;
                }
            }
        }
        quantityText.setText(String.valueOf(initialQuantity));

        // Increment quantity button
        plusBtn.setOnClickListener(v ->
                quantityText.setText(String.valueOf(Integer.parseInt(quantityText.getText().toString()) + 1))
        );
        // Decrement quantity button (minimum 1)
        minusBtn.setOnClickListener(v -> {
            int q = Integer.parseInt(quantityText.getText().toString());
            if (q > 1) quantityText.setText(String.valueOf(q - 1));
        });

        // ---------------- ADD TO CART ----------------
        MaterialButton addToCartBtn = popupView.findViewById(R.id.addToCartButton);
        int finalInitialQuantity = initialQuantity; // Needed for lambda

        addToCartBtn.setOnClickListener(v -> {
            int selectedQuantity = Integer.parseInt(quantityText.getText().toString());

            if (currentCart == null) return; // Safety check

            if (currentCart.items == null) currentCart.items = new ArrayList<>(); // Initialize list if null

            boolean found = false;
            // Check if item already exists in cart
            for (CartItem ci : currentCart.items) {
                if (ci.menuID.equals(item.menuID)) {
                    found = true;
                    if (ci.quantity != selectedQuantity) ci.quantity = selectedQuantity; // Update quantity if changed
                    break;
                }
            }

            // Add new item if it was not in cart
            if (!found) {
                currentCart.items.add(new CartItem(item.menuID, item.name, item.price, selectedQuantity, item.imageURL));
            }

            // If quantity was unchanged, show message
            if (found && finalInitialQuantity == selectedQuantity) {
                showCustomToast("Quantity not changed");
                return;
            }

            // Update total items and price in cart
            int totalItems = 0;
            double totalPrice = 0;
            for (CartItem ci : currentCart.items) {
                totalItems += ci.quantity;
                totalPrice += ci.quantity * ci.price;
            }
            currentCart.totalItems = totalItems;
            currentCart.totalPrice = totalPrice;

            // Save updated cart to Firebase
            dbRef.child("carts").child(currentCart.cartID).setValue(currentCart)
                    .addOnSuccessListener(aVoid -> {
                        showCustomToast("Cart updated"); // Feedback to user
                        updateCartBadge(); // Update UI badge
                    })
                    .addOnFailureListener(e -> showCustomToast("Failed to update cart"));

            popupDialog.dismiss(); // Close the popup
        });

        // ---------------- CLOSE BUTTON ----------------
        ImageButton closeBtn = popupView.findViewById(R.id.closeButton);
        closeBtn.setOnClickListener(v -> popupDialog.dismiss()); // Close popup
    }

    // ---------------- CART POPUP ----------------
    private void showCartPopup() {
        if (currentCart == null) return; // Safety: no cart, no popup

        // Inflate custom layout for cart popup
        View popupView = LayoutInflater.from(this).inflate(R.layout.cart_popup, null);

        // Create AlertDialog with inflated view
        AlertDialog popupDialog = new AlertDialog.Builder(this).setView(popupView).create();
        popupDialog.show();

        // Configure window appearance
        if (popupDialog.getWindow() != null) {
            int width = (int) (330 * getResources().getDisplayMetrics().density + 0.5f); // Convert dp to pixels
            popupDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupDialog.getWindow().getDecorView().post(() ->
                    popupDialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT));
            popupDialog.getWindow().setGravity(Gravity.CENTER);
        }

        // Close button
        ImageButton closeBtn = popupView.findViewById(R.id.closeCartButton);
        closeBtn.setOnClickListener(v -> popupDialog.dismiss());

        // RecyclerView for cart items
        RecyclerView cartRecyclerView = popupView.findViewById(R.id.cartRecyclerView);
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Views for empty cart or total price
        ImageView emptyImage = popupView.findViewById(R.id.emptyCartImage);
        cartPopupTotalText = popupView.findViewById(R.id.cartTotalAmount);

        // Runnable to update cart UI whenever cart changes
        Runnable updateCartUI = () -> {
            if (currentCart.items == null || currentCart.items.isEmpty()) {
                cartRecyclerView.setVisibility(View.GONE);  // hide list
                emptyImage.setVisibility(View.VISIBLE);     // show empty image
                cartPopupTotalText.setText("Cart is empty"); // show message
            } else {
                cartRecyclerView.setVisibility(View.VISIBLE);
                emptyImage.setVisibility(View.GONE);
                cartPopupTotalText.setText("Total: Rs. " + currentCart.totalPrice); // show total
            }
        };
        updateCartUI.run(); // initial UI setup

        // Adapter for RecyclerView
        CartAdapter cartAdapter = new CartAdapter(currentCart.items, this);
        cartRecyclerView.setAdapter(cartAdapter);

        // Place order button
        MaterialButton placeOrderBtn = popupView.findViewById(R.id.placeOrderButton);
        placeOrderBtn.setOnClickListener(v -> {
            if (currentCart.items == null || currentCart.items.isEmpty()) {
                showCustomToast("Cart is empty");
                return;
            }
            placeOrder();         // call your order processing method
            popupDialog.dismiss(); // close popup
        });

        // Observe changes in adapter and update UI accordingly
        cartAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() { updateCartUI.run(); }
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) { updateCartUI.run(); }
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) { updateCartUI.run(); }
        });
    }

    // Update the cart after removing menu items
    public void updateCartAfterRemoval(CartItem removedItem) {
        // Check if the cart or cart items list is null, exit if so
        if (currentCart == null || currentCart.items == null) return;

        // Initialize counters for total items and total price
        int totalItems = 0;
        double totalPrice = 0;

        // Loop through all items in the cart to recalculate totals
        for (CartItem ci : currentCart.items) {
            totalItems += ci.quantity;          // Add each item's quantity to totalItems
            totalPrice += ci.quantity * ci.price; // Add item's total price (quantity * price) to totalPrice
        }

        // Update cart object with new totals
        currentCart.totalItems = totalItems;
        currentCart.totalPrice = totalPrice;

        // Update the cart badge in the UI to reflect new totals
        updateCartBadge();

        // Save the updated cart back to Firebase under its cartID
        dbRef.child("carts").child(currentCart.cartID).setValue(currentCart);
    }


    private void placeOrder() {
        // -------------------------------
        // Step 1: Validate Cart
        // -------------------------------
        // Ensure that the cart exists and has items
        if (currentCart == null || currentCart.items == null || currentCart.items.isEmpty()) return;

        // -------------------------------
        // Step 2: Show Loading
        // -------------------------------
        showLoadingDialog("Placing order...");

        // -------------------------------
        // Step 3: Get Last Order ID from Firebase
        // -------------------------------
        dbRef.child("orders").orderByKey().limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Default order ID if no previous orders exist
                        String newOrderID = "o001";

                        // Generate sequential order ID based on last order
                        if (snapshot.exists()) {
                            for (DataSnapshot lastOrderSnap : snapshot.getChildren()) {
                                String lastID = lastOrderSnap.getKey();
                                if (lastID != null && lastID.startsWith("o")) {
                                    int num = Integer.parseInt(lastID.substring(1)) + 1;
                                    newOrderID = String.format("o%03d", num);
                                }
                            }
                        }

                        // -------------------------------
                        // Step 4: Determine Customer Location
                        // -------------------------------
                        // Use the last fetched user location from GPS
                        double userLat = 0, userLng = 0;
                        if (locationFetched && fusedLocationClient != null) {
                            // Note: You may want to cache the last Location object globally
                            // For example, keep a variable `lastUserLocation` in the activity
                            // Here we assume you have a global Location object named `lastUserLocation`
                            if (lastUserLocation != null) {
                                userLat = lastUserLocation.getLatitude();
                                userLng = lastUserLocation.getLongitude();
                            } else {
                                // Fallback if somehow location is null
                                showCustomToast("Location unavailable, using branch location instead");
                                Branch branch = getBranchByID(currentCart.branchID);
                                if (branch != null) {
                                    userLat = branch.latitude;
                                    userLng = branch.longitude;
                                }
                            }
                        }

                        long timestamp = System.currentTimeMillis();

                        // -------------------------------
                        // Step 5: Create Order Object
                        // -------------------------------
                        Order order = new Order(
                                newOrderID,
                                currentCart.branchID,
                                currentUserID,
                                currentUsername,
                                userLat,          // use user's current latitude
                                userLng,          // use user's current longitude
                                "",               // assignedDeliverymanID
                                "Order Pending",  // order status
                                currentCart.totalPrice,
                                timestamp,
                                0,                // delivered timestamp
                                "Pending",        // payment status
                                new ArrayList<>(currentCart.items)
                        );

                        // -------------------------------
                        // Step 6: Save Order to Firebase
                        // -------------------------------
                        dbRef.child("orders").child(newOrderID).setValue(order)
                                .addOnSuccessListener(aVoid -> {
                                    showCustomToast("Order placed successfully!");

                                    // -------------------------------
                                    // Step 7: Reset Cart
                                    // -------------------------------
                                    currentCart.items.clear();
                                    currentCart.totalItems = 0;
                                    currentCart.totalPrice = 0;
                                    dbRef.child("carts").child(currentCart.cartID).setValue(currentCart);

                                    updateCartBadge();
                                    hideLoadingDialog();
                                })
                                .addOnFailureListener(e -> {
                                    showCustomToast("Failed to place order");
                                    hideLoadingDialog();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showCustomToast("Failed to fetch last order ID");
                        hideLoadingDialog();
                    }
                });
    }

    // -------------------- UPDATE SEARCH SUGGESTIONS --------------------
    private void updateSearchSuggestions() {
        // Clear the old suggestions so we start fresh
        searchSuggestions.clear();

        // Loop through all menus currently loaded for this branch
        for (MenuItem menu : menuList) {
            // Add the menu name as a suggestion
            searchSuggestions.add(menu.name);

            // Also add the category name if it exists and isn't already in suggestions
            // Prevents duplicate entries
            if (menu.category != null && !searchSuggestions.contains(menu.category)) {
                searchSuggestions.add(menu.category);
            }
        }

        // Create a new ArrayAdapter to bind the suggestions list to the search box
        // android.R.layout.simple_dropdown_item_1line: default Android dropdown layout
        suggestionsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                searchSuggestions
        );

        // Find the AutoCompleteTextView in the layout
        AutoCompleteTextView searchBox = findViewById(R.id.searchBox);

        // Attach the adapter to the search box
        searchBox.setAdapter(suggestionsAdapter);

        // Set minimum number of characters to trigger suggestions
        searchBox.setThreshold(1); // Start showing suggestions after 1 character
    }

    // -------------------- SETUP SEARCH FUNCTIONALITY --------------------
    private void setupSearch() {
        // Find the search box and search button in the layout
        AutoCompleteTextView searchBox = findViewById(R.id.searchBox);
        ImageButton searchButton = findViewById(R.id.searchButton);

        // When search button is clicked, perform search using the text in the search box
        searchButton.setOnClickListener(v ->
                performSearch(searchBox.getText().toString().trim()) // Remove extra spaces
        );

        // Also trigger search when user presses "Enter" / "Done" on keyboard
        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            performSearch(searchBox.getText().toString().trim());
            return true; // Consume the event so keyboard doesn't perform default action
        });
    }

    // -------------------- PERFORM SEARCH --------------------
    private void performSearch(String query) {
        // If search query is empty, reset to show all menus
        if (query.isEmpty()) {
            categoryAdapter.updateList(menuList, currentBranchID); // Show full list
            return;
        }

        // List to hold menus matching the search query
        List<MenuItem> filtered = new ArrayList<>();

        // Loop through all menus
        for (MenuItem menu : menuList) {
            // Check if the menu name or category contains the search query (case-insensitive)
            if (menu.name.toLowerCase().contains(query.toLowerCase()) ||
                    (menu.category != null && menu.category.toLowerCase().contains(query.toLowerCase()))) {
                filtered.add(menu); // Add matching menu to filtered list
            }
        }

        // If no menus matched the query
        if (filtered.isEmpty()) {
            showCustomToast("No menus found"); // Inform the user
            categoryAdapter.updateList(menuList, currentBranchID); // Reset to show all menus
        } else {
            // Otherwise, update the adapter with only the filtered menus
            categoryAdapter.updateList(filtered, currentBranchID);
        }
    }

    // -------------------- MODELS --------------------

    // Represents a branch of the restaurant
    public static class Branch {
        public String branchID;    // Unique branch identifier
        public String name;        // Branch name (e.g., "Galle")
        public double latitude;    // Latitude for GPS/location
        public double longitude;   // Longitude for GPS/location
        public long contact;       // Contact phone number
        public Branch() {}         // Default constructor required for Firebase
    }

    // Represents a menu item in the app
    public static class MenuItem implements Parcelable {
        public String menuID, name, category, description, imageURL; // Basic info
        public double price;                                          // Menu price
        public List<String> branches;                                 // Branches where this item is available

        public MenuItem() {}  // Default constructor required for Firebase

        // Constructor for recreating from a Parcel (used for passing between activities)
        protected MenuItem(Parcel in) {
            menuID = in.readString();
            name = in.readString();
            category = in.readString();
            description = in.readString();
            imageURL = in.readString();
            price = in.readDouble();
            branches = in.createStringArrayList();
        }

        // Parcelable implementation to allow sending MenuItem objects via Intent
        public static final Parcelable.Creator<MenuItem> CREATOR = new Creator<MenuItem>() {
            @Override
            public MenuItem createFromParcel(Parcel in) {
                return new MenuItem(in);
            }

            @Override
            public MenuItem[] newArray(int size) {
                return new MenuItem[size];
            }
        };

        @Override
        public int describeContents() {
            return 0; // Usually returns 0 unless special objects exist
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(menuID);
            parcel.writeString(name);
            parcel.writeString(category);
            parcel.writeString(description);
            parcel.writeString(imageURL);
            parcel.writeDouble(price);
            parcel.writeStringList(branches); // Store the list of branch IDs
        }
    }

    // Represents a single item in the cart
    public static class CartItem {
        public String menuID, name, imageURL; // Info about the menu
        public double price;                   // Price at time of adding
        public int quantity;                   // Quantity added

        public CartItem() {} // Default constructor

        public CartItem(String menuID, String name, double price, int quantity, String imageURL) {
            this.menuID = menuID;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
            this.imageURL = imageURL;
        }
    }

    // Represents a user's shopping cart
    public static class Cart {
        public String cartID, branchID, customerID; // IDs for cart, branch, customer
        public List<CartItem> items;                // List of items in cart
        public int totalItems;                      // Total quantity of items
        public double totalPrice;                   // Total price of cart

        public Cart() {} // Default constructor

        // Constructor for creating a new cart
        public Cart(String cartID, String branchID, String customerID) {
            this.cartID = cartID;
            this.branchID = branchID;
            this.customerID = customerID;
            this.items = new ArrayList<>();
            this.totalItems = 0;
            this.totalPrice = 0;
        }
    }

    // Represents a customer's order
    public static class Order {
        public String orderID, branchID, customerID, customerName;
        public String assignedDeliverymanID, status, paymentStatus;
        public double customerLat, customerLng, totalPrice; // Location & total
        public long timestamp, deliveredTimestamp;          // Order timestamps
        public List<CartItem> items;                        // ✅ Stores ordered menus

        public Order() {} // Default constructor

        // Full constructor for creating order with all details
        public Order(String orderID, String branchID, String customerID,
                     String customerName,
                     double customerLat, double customerLng,
                     String assignedDeliverymanID, String status,
                     double totalPrice, long timestamp, long deliveredTimestamp,
                     String paymentStatus, List<CartItem> items) {

            this.orderID = orderID;
            this.branchID = branchID;
            this.customerID = customerID;
            this.customerName = customerName;
            this.customerLat = customerLat;
            this.customerLng = customerLng;
            this.assignedDeliverymanID = assignedDeliverymanID;
            this.status = status;
            this.totalPrice = totalPrice;
            this.timestamp = timestamp;
            this.deliveredTimestamp = deliveredTimestamp;
            this.paymentStatus = paymentStatus;
            this.items = items; // ✅ Stores all the items the customer ordered
        }
    }

    // -------------------- LOADING DIALOG --------------------

    // Shows a custom loading dialog with a message
    private void showLoadingDialog(String message) {
        // If a dialog is already showing, do nothing
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        // Inflate the custom layout for the loading dialog
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);

        // Set the message text in the dialog
        TextView textView = view.findViewById(R.id.loadingText);
        textView.setText(message);

        // Create an AlertDialog with the custom view, make it non-cancelable
        loadingDialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        // Show the dialog
        loadingDialog.show();

        // Optional: Adjust the dialog window size and make background transparent
        if (loadingDialog.getWindow() != null) {
            // Convert dp to pixels for width and height
            int width = (int) (300 * getResources().getDisplayMetrics().density);
            int height = (int) (180 * getResources().getDisplayMetrics().density);

            // Set the dialog size
            loadingDialog.getWindow().setLayout(width, height);

            // Make background transparent to match custom layout
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    // Hides the loading dialog if it is currently shown
    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    // -------------------- CUSTOM TOAST --------------------

    // Shows a custom toast-like message using an AlertDialog
    private void showCustomToast(String message) {
        // Inflate the custom layout for the toast
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_message, null);

        // Get references to views in the custom layout
        TextView toastMessage = layout.findViewById(R.id.toast_message); // Message text
        ImageView close = layout.findViewById(R.id.toast_close);         // Close button
        ProgressBar progressBar = layout.findViewById(R.id.toast_progress); // Countdown bar

        // Set the toast message
        toastMessage.setText(message);
        progressBar.setProgress(100); // Start at full progress

        // Create an AlertDialog to act as the toast
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(layout)
                .create();

        // Close button dismisses the dialog
        close.setOnClickListener(v -> dialog.dismiss());

        // Customize dialog window appearance
        if (dialog.getWindow() != null) {
            // Make background transparent
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Disable dimming behind the dialog
            dialog.getWindow().setDimAmount(0f);

            // Set width, height, and position
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT; // Full width
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.TOP; // Show at top of screen
            params.y = 50;                // Offset from top
            dialog.getWindow().setAttributes(params);
        }

        // Show the dialog
        dialog.show();

        // Use a CountDownTimer to animate the progress bar and auto-dismiss the dialog
        new CountDownTimer(3000, 50) { // 3 seconds total, ticks every 50ms
            public void onTick(long millisUntilFinished) {
                // Update progress bar (from 100% down to 0%)
                int progress = (int) Math.max(0, Math.round((millisUntilFinished / 3000.0) * 100));
                progressBar.setProgress(progress);
            }

            public void onFinish() {
                // Dismiss dialog when timer finishes
                if (dialog.isShowing()) dialog.dismiss();
            }
        }.start();
    }
}