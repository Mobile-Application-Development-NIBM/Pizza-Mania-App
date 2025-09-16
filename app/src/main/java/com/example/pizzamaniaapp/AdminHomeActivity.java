// ========== Activity Summary ==========
// AdminHomeActivity
// - Admin home screen for managing Menu Items and navigating to Branch/Employee management
// - Features:
//     * Load and display all menu items
//     * Add/Edit/Delete menu items
//     * Assign menu items to branches
//     * Load list of branches for menu assignment
//     * Search menu items by name
//     * Navigate to Branch/Employee Management screen
//     * Custom top toast for messages
//     * Custom loading dialog for Firebase operations
// ======================================


// onCreate()
// - Sets layout and adjusts padding for system bars
// - Initializes Firebase DB reference
// - Loads menu items and branch list
// - Handles "Add Menu" button click
// - Configures search box and button (filter menu items by name)
// - Adds button to navigate to AdminBranchEmployeeManagementActivity


// loadMenuItems()
// - Shows loading dialog
// - Fetches all menu items from Firebase
// - Parses into MenuItem objects and sorts by menuID
// - Initializes or updates RecyclerView adapter
// - Handles Firebase errors with custom toast


// loadBranches()
// - Fetches all branch names from Firebase
// - Stores unique branch names in allBranches list
// - Used for assigning menu items to specific branches


// filterMenu(query)
// - Filters allMenuItems by name containing query (case-insensitive)
// - Updates adapter with results
// - If no matches, shows toast and resets to full menu list


// prepareNewMenuPopup()
// - Loads branch list first
// - Then fetches total menu items to generate next menuID (m001, m002…)
// - Calls showMenuPopup() with new ID
// - Fallback to m001 if error


// showMenuPopup(existingItem, preloadedID)
// - Opens popup for Add/Edit menu item
// - Inflates form fields (id, name, category, desc, price, image, branches)
// - Preloads data if editing existing item
// - Allows selecting multiple branches with removable chips
// - Handles Save/Cancel actions
// - Save action delegates to saveMenuItem()


// saveMenuItem(existingItem, ...)
// - Validates required fields (id, name, category, price)
// - Parses price input safely
// - Creates/updates MenuItem object
// - Saves item to Firebase
// - On success: toast + reload menu list
// - On failure: toast error


// showDeletePopup(item, parentDialog)
// - Opens confirmation popup to delete a menu item
// - On confirm: removes item from Firebase
// - On success: shows toast, closes popup, reloads menu list
// - On failure: shows error toast


// showCustomToast(message)
// - Shows custom top toast with message
// - Includes close button + progress bar
// - Auto-dismisses after 3 seconds with countdown


// showLoadingDialog(message)
// - Shows custom loading dialog with fixed size (300dp x 180dp)
// - Displays given message
// - Blocks user interaction while showing


// hideLoadingDialog()
// - Dismisses loading dialog if currently shown


// MenuItem class
// - Data model for menu items
// - Fields: menuID, name, category, description, price, imageURL, branches


package com.example.pizzamaniaapp; // Package name of the app

// -------- Android Core Imports --------
import android.content.Intent; // Used to switch between activities
import android.content.SharedPreferences;
import android.os.Bundle; // Holds saved instance state for activities
import android.os.CountDownTimer; // Provides countdown functionality (used for toast auto-dismiss)
import android.util.Log; // Logging for debugging
import android.view.LayoutInflater; // Used to inflate XML layouts into View objects
import android.view.View; // Base class for all UI components
import android.view.ViewGroup; // Container for views
import android.view.Window; // For customizing popup/dialog windows
import android.view.WindowManager; // Manage window display attributes
import android.widget.ArrayAdapter; // Adapter for dropdown lists
import android.widget.AutoCompleteTextView; // Editable dropdown text field
import android.widget.Button; // Button UI element
import android.widget.EditText; // Input text field
import android.widget.ImageButton; // Button with image icon
import android.widget.TextView; // Display text
import android.widget.ImageView; // Display image
import android.widget.ProgressBar; // Progress indicator
import android.graphics.drawable.ColorDrawable; // Drawable for solid colors (used for dialog background)
import android.graphics.Color; // Color constants/utilities

// -------- AndroidX (Support Libraries) --------
import androidx.appcompat.app.AlertDialog; // Custom dialogs
import androidx.appcompat.app.AppCompatActivity; // Base class for activities using AppCompat features
import androidx.core.graphics.Insets; // Insets for system bars (status bar, nav bar)
import androidx.core.view.ViewCompat; // Helper for backward-compatible view features
import androidx.core.view.WindowInsetsCompat; // Handle system window insets
import androidx.recyclerview.widget.LinearLayoutManager; // Layout manager for RecyclerView (vertical/horizontal lists)
import androidx.recyclerview.widget.RecyclerView; // Advanced list view for large datasets

// -------- Material Design --------
import com.google.android.material.chip.Chip; // UI chip element (small tag-like UI)
import com.google.android.material.chip.ChipGroup; // Container for chips

// -------- Firebase --------
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot; // Snapshot of Firebase data
import com.google.firebase.database.DatabaseError; // Error handling for Firebase DB
import com.google.firebase.database.DatabaseReference; // Reference to a location in Firebase DB
import com.google.firebase.database.FirebaseDatabase; // Firebase database instance
import com.google.firebase.database.ValueEventListener; // Listener for data changes in Firebase

// -------- Java Utilities --------
import java.util.ArrayList; // Resizable array implementation
import java.util.Collections; // Utility class for sorting/collection operations
import java.util.List; // Interface for ordered collections


public class AdminHomeActivity extends AppCompatActivity { // Main admin home activity, extends AppCompat for modern features

    private static final String TAG = "AdminHomeActivity"; // Tag for logging/debugging
    private DatabaseReference db; // Firebase Realtime Database reference
    private final List<MenuItem> allMenuItems = new ArrayList<>(); // Stores all menu items loaded from Firebase
    private BranchAdapter branchAdapter; // RecyclerView adapter to display menu items

    private RecyclerView branchRecyclerView; // RecyclerView to list menu items
    private ImageButton addButton; // Button to open popup for adding new menu item

    private final List<String> allBranches = new ArrayList<>(); // Stores branch names for assigning menu items

    EditText searchBox; // Input field for searching menu items
    ImageButton searchButton; // Button to trigger search

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Called when activity is created
        super.onCreate(savedInstanceState); // Call parent implementation
        setContentView(R.layout.activity_admin_home); // Set layout for this activity

        View mainLayout = findViewById(R.id.main); // Get reference to root layout
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> { // Handle system bar insets (status/nav bars)
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); // Get system bar sizes
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom); // Apply padding so content doesn’t overlap
            return insets; // Return insets after handling
        });

        db = FirebaseDatabase.getInstance().getReference(); // Initialize Firebase DB reference
        branchRecyclerView = findViewById(R.id.branchRecyclerView); // RecyclerView for menu list
        addButton = findViewById(R.id.addButton); // "Add menu item" button

        branchRecyclerView.setLayoutManager(new LinearLayoutManager(this)); // Set RecyclerView layout (vertical list)

        loadBranches(); // Load all menu items from Firebase

        addButton.setOnClickListener(v -> prepareNewMenuPopup()); // When add button is clicked, open popup for new menu

        // search views
        searchBox = findViewById(R.id.searchBox); // Input for search text
        searchButton = findViewById(R.id.searchButton); // Search button

        searchButton.setOnClickListener(v -> { // Handle search button click
            String query = searchBox.getText().toString().trim(); // Get search text
            if (query.isEmpty()) {
                branchAdapter.updateList(new ArrayList<>(allMenuItems)); // reset to all menus if empty
            } else {
                filterMenu(query); // filter menu list by query
            }
        });

        searchBox.setOnEditorActionListener((v, actionId, event) -> { // Handle Enter key press in search box
            String query = searchBox.getText().toString().trim(); // Get search text
            if (query.isEmpty()) {
                branchAdapter.updateList(new ArrayList<>(allMenuItems)); // reset all menus if empty
            } else {
                filterMenu(query); // filter menu list by query
            }
            return true; // ✅ mark event as handled
        });

        ImageButton btnBranch = findViewById(R.id.branchPageButton); // Button to navigate to Branch/Employee management

        btnBranch.setOnClickListener(new View.OnClickListener() { // Handle branch page button click
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminHomeActivity.this, AdminBranchEmployeeManagementActivity.class); // Navigate to Branch/Employee activity
                startActivity(intent); // Start new activity
            }
        });

        // --- Deliveryman button click ---
        ImageButton deliveryman = findViewById(R.id.deliverymanPageButton); // get deliveryman button by ID
        deliveryman.setOnClickListener(v -> {
            // open AdminDeliverymanManagement
            Intent intent = new Intent(AdminHomeActivity.this, AdminDeliverymanManagement.class);
            startActivity(intent);
        });

        // -------------------- Log out button --------------------
        ImageButton LogoutButtonButton = findViewById(R.id.LogoutButtonButton);
        LogoutButtonButton.setOnClickListener(v -> {
            // 1. Try sign out from FirebaseAuth (only works if current user is FirebaseAuth user)
            FirebaseAuth.getInstance().signOut();

            // 2. Clear session data in SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            // 3. Redirect user back to LoginActivity
            Intent intent = new Intent(AdminHomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

    }

    private void loadMenuItems() { // Method to load all menu items from Firebase
        showLoadingDialog("Loading menus..."); // 🔹 Show custom loading popup while fetching data

        db.child("menu").addListenerForSingleValueEvent(new ValueEventListener() { // Attach one-time listener to "menu" node in Firebase
            @Override
            public void onDataChange(DataSnapshot snapshot) { // Called when data is successfully fetched
                hideLoadingDialog(); // 🔹 Hide loading popup after data is loaded
                allMenuItems.clear(); // Clear current menu list before reloading
                for (DataSnapshot itemSnap : snapshot.getChildren()) { // Loop through all menu items in Firebase
                    try {
                        MenuItem item = itemSnap.getValue(MenuItem.class); // Convert snapshot to MenuItem object
                        if (item != null) allMenuItems.add(item); // Add valid menu items to list
                    } catch (Exception e) { // Catch parsing errors
                        Log.e(TAG, "Menu item parse error for key " + itemSnap.getKey(), e); // Log error for debugging
                    }
                }

                // 🔹 Sort menu items by menuID (alphabetically, e.g., m001, m002...)
                Collections.sort(allMenuItems, (a, b) -> {
                    String idA = (a == null || a.menuID == null) ? "" : a.menuID; // Handle null safety
                    String idB = (b == null || b.menuID == null) ? "" : b.menuID; // Handle null safety
                    return idA.compareTo(idB); // Compare menu IDs
                });

                if (branchAdapter == null) { // If adapter not initialized yet
                    branchAdapter = new BranchAdapter(
                            AdminHomeActivity.this,
                            new ArrayList<>(allMenuItems),
                            item -> showMenuPopup(item, null),
                            branchIdToName   // ✅ pass ID→Name map here
                    );
                    branchRecyclerView.setAdapter(branchAdapter); // Attach adapter to RecyclerView
                } else {
                    branchAdapter.updateList(new ArrayList<>(allMenuItems)); // If adapter exists, update its list
                }
            }

            @Override
            public void onCancelled(DatabaseError error) { // Called if data fetch fails
                hideLoadingDialog(); // 🔹 Hide loading even if error
                showCustomToast("Failed to load menu items"); // Show error message to user
                Log.e(TAG, "Failed to load menu items: " + error.getMessage()); // Log error for debugging
            }
        });
    }

    private final List<String> allBranchNames = new ArrayList<>(); // Stores branch names for UI
    private final List<String> allBranchIDs = new ArrayList<>();   // Stores branch IDs for saving
    private final java.util.Map<String, String> branchIdToName = new java.util.HashMap<>(); // Map branchID → branchName
    private final java.util.Map<String, String> branchNameToId = new java.util.HashMap<>(); // Map branchName → branchID

    private void loadBranches() {
        // 🔹 Query the "branches" node once from Firebase Realtime Database
        db.child("branches").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Clear old data before reloading
                allBranchNames.clear();   // Holds branch names for dropdown
                allBranchIDs.clear();     // Holds branch IDs for mapping
                branchIdToName.clear();   // Map: ID → Name
                branchNameToId.clear();   // Map: Name → ID

                // Loop through all branches in the DB
                for (DataSnapshot snap : snapshot.getChildren()) {
                    // Read branchID as String
                    String branchID = snap.child("branchID").getValue(String.class);
                    // Read branch name
                    String branchName = snap.child("name").getValue(String.class);

                    if (branchID != null && branchName != null) {
                        branchID = branchID.trim();     // Clean spaces
                        branchName = branchName.trim(); // Clean spaces

                        // Store into lists and maps
                        allBranchIDs.add(branchID);          // Add ID to list
                        allBranchNames.add(branchName);      // Add name to list
                        branchIdToName.put(branchID, branchName); // ID → Name
                        branchNameToId.put(branchName, branchID); // Name → ID

                        Log.d(TAG, "Loaded branch: " + branchID + " (" + branchName + ")");
                    }
                }
                // Log summary count
                Log.d(TAG, "✅ Total branches loaded: " + allBranchIDs.size());

                // 🔹 Now that branches are loaded, fetch menu items
                loadMenuItems();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle DB error
                showCustomToast("Failed to load branches: " + error.getMessage());
            }
        });
    }

    private void filterMenu(String query) { // Method to filter menu items by name
        List<MenuItem> filtered = new ArrayList<>(); // Temporary list to store filtered items
        for (MenuItem item : allMenuItems) { // Loop through all menu items
            if (item.name != null && item.name.toLowerCase().contains(query.toLowerCase())) {
                // ✅ If item name exists and contains the search query (case-insensitive)
                filtered.add(item); // Add to filtered list
            }
        }

        if (filtered.isEmpty()) { // If no matching items found
            showCustomToast("No menus found with that name"); // Show message to user
            // 👇 keep showing all menus instead of empty
            branchAdapter.updateList(new ArrayList<>(allMenuItems)); // Reset list to all items
        } else {
            branchAdapter.updateList(filtered); // Update adapter with filtered results
        }
    }

    private void prepareNewMenuPopup() { // Method to prepare popup for adding a new menu item
        showLoadingDialog("Loading..."); // 🔹 Show loading popup while fetching data

        db.child("branches").addListenerForSingleValueEvent(new ValueEventListener() { // Fetch all branches first
            @Override
            public void onDataChange(DataSnapshot snapshot) { // Called when branches are fetched
                allBranches.clear(); // Clear old list
                for (DataSnapshot snap : snapshot.getChildren()) { // Loop through branches
                    String branchName = snap.child("name").getValue(String.class); // Get branch name
                    if (branchName != null) branchName = branchName.trim(); // Trim spaces
                    if (branchName != null && branchName.length() > 0 && !allBranches.contains(branchName)) {
                        // ✅ Add only if valid and not already in list
                        allBranches.add(branchName);
                    }
                }

                db.child("menu").addListenerForSingleValueEvent(new ValueEventListener() { // Fetch menu list to calculate next ID
                    @Override
                    public void onDataChange(DataSnapshot snapshot) { // Called when menus are fetched
                        hideLoadingDialog(); // 🔹 Hide loading before showing popup
                        int nextNumber = (int) snapshot.getChildrenCount() + 1; // Calculate next menu number
                        String menuID = "m" + String.format(java.util.Locale.US, "%03d", nextNumber);
                        // Format menu ID as m001, m002, etc.
                        showMenuPopup(null, menuID); // Open popup for adding a new menu
                    }

                    @Override
                    public void onCancelled(DatabaseError error) { // If menu fetch fails
                        hideLoadingDialog(); // Hide loading
                        showMenuPopup(null, "m001"); // Default to m001 if error
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) { // If branch fetch fails
                hideLoadingDialog(); // Hide loading
                showMenuPopup(null, "m001"); // Default to m001
            }
        });
    }

    private void showMenuPopup(MenuItem existingItem, String preloadedID) {
        // Inflate the popup layout for adding/editing menu items
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_add_menu, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(popupView).create();
        dialog.show();

        // Configure popup window size, background, and keyboard behavior
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.9), // 90% of screen width
                    WindowManager.LayoutParams.WRAP_CONTENT); // Height wraps content
            window.setBackgroundDrawableResource(android.R.color.transparent); // Transparent corners
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE); // Resize when keyboard appears
        }

        // 🔹 UI elements inside popup
        TextView title = popupView.findViewById(R.id.popupTitleMenu);         // Popup title
        EditText idInput = popupView.findViewById(R.id.menuIdInput);          // Menu ID input
        EditText nameInput = popupView.findViewById(R.id.menuNameInput);      // Menu name input
        EditText categoryInput = popupView.findViewById(R.id.menuCategoryInput); // Menu category input
        EditText descInput = popupView.findViewById(R.id.menuDescriptionInput);  // Menu description input
        EditText priceInput = popupView.findViewById(R.id.menuPriceInput);    // Menu price input
        EditText imageInput = popupView.findViewById(R.id.menuImageInput);    // Menu image URL input
        AutoCompleteTextView branchesDropdown = popupView.findViewById(R.id.branchesDropdown); // Dropdown for selecting branch
        ChipGroup selectedBranchesGroup = popupView.findViewById(R.id.selectedBranchesGroup); // Shows selected branches as chips
        Button saveBtn = popupView.findViewById(R.id.saveMenuBtn);            // Save button
        Button cancelBtn = popupView.findViewById(R.id.cancelMenuBtn);        // Cancel button

        // 🔹 Branch dropdown shows NAMES to the user, but internally we store IDs
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, allBranchNames // Provide all branch names
        );
        branchesDropdown.setAdapter(adapter); // Attach adapter
        branchesDropdown.setThreshold(1);     // Start suggesting after typing 1 character

        List<String> selectedBranchIDs = new ArrayList<>(); // Stores chosen branch IDs

        // When a branch is selected from dropdown
        branchesDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = adapter.getItem(position);   // Get selected branch name
            String branchID = branchNameToId.get(selectedName); // Convert name → ID

            if (branchID != null && !selectedBranchIDs.contains(branchID)) { // Avoid duplicates
                selectedBranchIDs.add(branchID); // Add branch ID

                Chip chip = new Chip(this);       // Create a chip for UI
                chip.setText(selectedName);       // Show branch NAME on chip
                chip.setCloseIconVisible(true);   // Add "X" to remove
                chip.setOnCloseIconClickListener(v -> {
                    selectedBranchIDs.remove(branchID);        // Remove ID from list
                    selectedBranchesGroup.removeView(chip);    // Remove chip from group
                });
                selectedBranchesGroup.addView(chip); // Add chip to group
                branchesDropdown.setText("");       // Clear dropdown input after selection
            }
        });

        boolean isEditing = existingItem != null; // Check if editing an existing menu
        if (isEditing) {
            title.setText("Editing Menu Item");    // Change popup title
            idInput.setText(existingItem.menuID);  // Fill ID
            idInput.setEnabled(false);             // ID cannot be changed
            nameInput.setText(existingItem.name);  // Fill name
            categoryInput.setText(existingItem.category); // Fill category
            descInput.setText(existingItem.description);  // Fill description
            priceInput.setText(String.valueOf(existingItem.price)); // Fill price
            imageInput.setText(existingItem.imageURL);   // Fill image URL

            // 🔹 Restore branches
            if (existingItem.branches != null) {
                for (String branchID : existingItem.branches) {
                    if (branchID == null) continue; // Skip nulls
                    String branchName = branchIdToName.get(branchID); // Convert ID → name
                    if (branchName == null) branchName = branchID; // Fallback if not found

                    selectedBranchIDs.add(branchID); // Add to selected list

                    Chip chip = new Chip(this);     // Create chip
                    chip.setText(branchName);       // Show branch NAME
                    chip.setCloseIconVisible(true); // Add close icon
                    String finalBranchID = branchID;
                    chip.setOnCloseIconClickListener(v -> {
                        selectedBranchIDs.remove(finalBranchID);  // Remove branch from list
                        selectedBranchesGroup.removeView(chip);   // Remove chip from UI
                    });
                    selectedBranchesGroup.addView(chip); // Add chip to group
                }
            }
        } else {
            // Adding a new menu item
            title.setText("Add New Menu Item");
            idInput.setText(preloadedID);  // Auto-generated/preloaded ID
            idInput.setEnabled(false);     // Cannot edit ID
        }

        // Cancel button → close popup
        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        // Save button → call saveMenuItem()
        saveBtn.setOnClickListener(v -> saveMenuItem(
                existingItem, idInput, nameInput, categoryInput,
                descInput, priceInput, imageInput, selectedBranchIDs, dialog
        ));
    }

    private void saveMenuItem(MenuItem existingItem, EditText idInput, EditText nameInput,
                              EditText categoryInput, EditText descInput, EditText priceInput,
                              EditText imageInput, List<String> selectedBranchIDs, AlertDialog dialog) {

        String id = idInput.getText().toString().trim();           // Get menu ID from input field (trim spaces)
        String name = nameInput.getText().toString().trim();       // Get menu name
        String category = categoryInput.getText().toString().trim(); // Get category
        String desc = descInput.getText().toString().trim();       // Get description
        String priceStr = priceInput.getText().toString().trim();  // Get price as string
        String imageUrl = imageInput.getText().toString().trim();  // Get image URL

        if (id.isEmpty() || name.isEmpty() || category.isEmpty() || priceStr.isEmpty()) {
            showCustomToast("Please fill all required fields");
            return;
        }

        if (selectedBranchIDs == null || selectedBranchIDs.isEmpty()) {
            showCustomToast("Please select at least one branch");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);                  // Convert price string to double
        } catch (NumberFormatException e) {
            showCustomToast("Invalid price");                      // Show error if price is not a valid number
            return;                                                // Stop saving
        }

        // 🔹 Normalize branch references → handle old menus stored with names instead of IDs
        List<String> normalizedBranchIDs = new ArrayList<>();       // New list for cleaned branch IDs
        for (String b : selectedBranchIDs) {                        // Loop through selected branches
            if (branchIdToName.containsKey(b)) {                    // If already an ID (exists in ID→Name map)
                normalizedBranchIDs.add(b);                         // Keep as ID
            } else if (branchNameToId.containsKey(b)) {             // If it’s actually a branch NAME
                normalizedBranchIDs.add(branchNameToId.get(b));     // Convert name → ID
            } else {
                normalizedBranchIDs.add(b);                         // Fallback: keep original value
            }
        }

        if (existingItem != null) {                                 // If editing an existing menu
            boolean noChanges =
                    existingItem.name.equals(name) &&               // Check if name is unchanged
                            existingItem.category.equals(category) && // Check if category is unchanged
                            ((existingItem.description == null && desc.isEmpty()) || // Handle null description
                                    (existingItem.description != null && existingItem.description.equals(desc))) &&
                            existingItem.price == price &&          // Check if price unchanged
                            ((existingItem.imageURL == null && imageUrl.isEmpty()) || // Handle null imageURL
                                    (existingItem.imageURL != null && existingItem.imageURL.equals(imageUrl))) &&
                            existingItem.branches != null &&        // Check branch list
                            existingItem.branches.size() == normalizedBranchIDs.size() && // Same size
                            existingItem.branches.containsAll(normalizedBranchIDs); // Same items

            if (noChanges) {                                        // If nothing changed
                showCustomToast("No changes detected");             // Show toast
                return;                                             // Stop saving
            }
        }

        // ✅ Always save with branch IDs (cleaned version)
        MenuItem menuItem = new MenuItem();                         // Create new MenuItem object
        menuItem.menuID = id;                                       // Set ID
        menuItem.name = name;                                       // Set name
        menuItem.category = category;                               // Set category
        menuItem.description = desc;                                // Set description
        menuItem.price = price;                                     // Set price
        menuItem.imageURL = imageUrl;                               // Set image URL
        menuItem.branches = normalizedBranchIDs;                    // Save branch IDs only

        db.child("menu").child(id).setValue(menuItem)               // Save to Firebase under "menu/{id}"
                .addOnSuccessListener(aVoid -> {                    // On success
                    String msg = (existingItem != null) ? "Menu item updated" : "Menu item saved"; // Message based on add/edit
                    showCustomToast(msg);                           // Show success toast
                    dialog.dismiss();                               // Close popup dialog
                    loadMenuItems();                                // Refresh menu list
                })
                .addOnFailureListener(e -> {                        // On failure
                    Log.e(TAG, "Save failed: " + e.getMessage(), e); // Log error
                    showCustomToast("Failed: " + e.getMessage());   // Show failure toast
                });
    }

    public void showDeletePopup(MenuItem item, AlertDialog parentDialog) { // Method to confirm deletion of a menu item
        if (item == null) return; // ✅ Safety check → if no item, do nothing

        // 🔹 Inflate delete confirmation popup layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.delete_confirmation_popup, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(popupView).create(); // Create dialog
        dialog.show(); // Show dialog

        Window window = dialog.getWindow(); // Get dialog window
        if (window != null) {
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.85), // Set width = 85% of screen
                    WindowManager.LayoutParams.WRAP_CONTENT); // Height = wrap content
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background
        }

        // 🔹 Get cancel and confirm buttons from popup
        Button cancelBtn = popupView.findViewById(R.id.cancelDeleteBtn);
        Button confirmBtn = popupView.findViewById(R.id.confirmDeleteBtn);

        // ❌ Cancel → just close popup
        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        // ✅ Confirm → delete menu item from Firebase
        confirmBtn.setOnClickListener(v -> {
            db.child("menu").child(item.menuID).removeValue() // Remove "menu/{id}" from Firebase
                    .addOnSuccessListener(aVoid -> { // If deletion succeeds
                        showCustomToast("Menu item deleted"); // Notify user
                        dialog.dismiss(); // Close confirmation popup
                        if (parentDialog != null && parentDialog.isShowing()) {
                            parentDialog.dismiss(); // Also close parent popup if still open
                        }
                        loadMenuItems(); // Refresh menu list
                    })
                    .addOnFailureListener(e -> { // If deletion fails
                        Log.e(TAG, "Delete failed: " + e.getMessage(), e); // Log error
                        showCustomToast("Failed to delete"); // Show error to user
                    });
        });
    }

    private void showCustomToast(String message) { // Method to show a custom toast-like message popup
        LayoutInflater inflater = getLayoutInflater(); // Get layout inflater for inflating XML
        View layout = inflater.inflate(R.layout.custom_message, null); // Inflate custom toast layout

        // 🔹 Initialize views inside custom layout
        TextView toastMessage = layout.findViewById(R.id.toast_message); // Text field for message
        ImageView close = layout.findViewById(R.id.toast_close); // Close (X) button
        ProgressBar progressBar = layout.findViewById(R.id.toast_progress); // Progress bar for auto-dismiss timer

        toastMessage.setText(message); // Set message text
        progressBar.setProgress(100); // Start with full progress (100%)

        // 🔹 Create dialog to show custom toast
        AlertDialog dialog = new AlertDialog.Builder(this).setView(layout).create();
        close.setOnClickListener(v -> dialog.dismiss()); // Close button → dismiss toast

        if (dialog.getWindow() != null) { // Customize dialog window
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background
            dialog.getWindow().setDimAmount(0f); // No dim background

            WindowManager.LayoutParams params = dialog.getWindow().getAttributes(); // Get window attributes
            params.width = WindowManager.LayoutParams.MATCH_PARENT; // Full width
            params.height = WindowManager.LayoutParams.WRAP_CONTENT; // Wrap height
            params.gravity = android.view.Gravity.TOP; // Show at top of screen
            params.y = 50; // Add top margin (distance from status bar)
            dialog.getWindow().setAttributes(params); // Apply attributes
        }

        dialog.show(); // Show the custom toast dialog

        // 🔹 Countdown timer for auto-dismiss with progress bar
        new CountDownTimer(3000, 50) { // 3 seconds total, tick every 50ms
            public void onTick(long millisUntilFinished) { // Update progress on each tick
                int progress = (int) Math.max(0, Math.round((millisUntilFinished / 3000.0) * 100));
                // Convert remaining time to percentage
                progressBar.setProgress(progress); // Update progress bar
            }

            public void onFinish() { // Called after 3 seconds
                if (dialog.isShowing()) dialog.dismiss(); // Auto dismiss toast if still showing
            }
        }.start(); // Start countdown
    }

    private AlertDialog loadingDialog; // Global variable to hold reference to loading dialog

    private void showLoadingDialog(String message) { // Show custom loading popup with text
        if (loadingDialog != null && loadingDialog.isShowing()) return; // If already showing, do nothing

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null); // Inflate custom loading layout
        TextView textView = view.findViewById(R.id.loadingText); // Reference to loading message TextView
        textView.setText(message); // Set loading message text

        loadingDialog = new AlertDialog.Builder(this) // Create AlertDialog with custom loading view
                .setView(view) // Use custom view
                .setCancelable(false) // Disable dismissing by tapping outside
                .create();

        loadingDialog.show(); // Show the dialog

        // 🔹 Adjust dialog size and style
        if (loadingDialog.getWindow() != null) {
            int width = (int) (300 * getResources().getDisplayMetrics().density);  // Convert 300dp to px
            int height = (int) (180 * getResources().getDisplayMetrics().density); // Convert 180dp to px
            loadingDialog.getWindow().setLayout(width, height); // Set fixed width & height
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background
        }
    }

    private void hideLoadingDialog() { // Hide the loading popup
        if (loadingDialog != null && loadingDialog.isShowing()) { // Ensure dialog exists and is visible
            loadingDialog.dismiss(); // Close the dialog
        }
    }

    // 🔹 Data model class for Menu items
    public static class MenuItem {
        public String menuID, name, category, description, imageURL; // Menu details
        public double price; // Price of menu item
        public List<String> branches; // Branches where this item is available

        public MenuItem() { } // Empty constructor (required for Firebase)
    }
}
