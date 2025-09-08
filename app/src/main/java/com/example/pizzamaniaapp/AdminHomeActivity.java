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
        loadMenuItems(); // Load all menu items from Firebase
        loadBranches(); // Load branch list (for assigning menu items)

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
                    branchAdapter = new BranchAdapter( // Create new adapter with menu list
                            AdminHomeActivity.this, // Context
                            new ArrayList<>(allMenuItems), // Copy of menu items
                            item -> showMenuPopup(item, null) // Click listener → open popup to edit menu item
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

    private void loadBranches() { // Method to load all branch names from Firebase
        db.child("branches").addListenerForSingleValueEvent(new ValueEventListener() { // Attach one-time listener to "branches" node
            @Override
            public void onDataChange(DataSnapshot snapshot) { // Called when data is successfully fetched
                allBranches.clear(); // Clear the existing branch list before reloading
                for (DataSnapshot snap : snapshot.getChildren()) { // Loop through all branch nodes
                    String branchName = snap.child("name").getValue(String.class); // Get branch name from snapshot
                    if (branchName != null) branchName = branchName.trim(); // Trim spaces if not null
                    if (branchName != null && branchName.length() > 0 && !allBranches.contains(branchName)) {
                        // ✅ Add branch if name is valid (not null/empty) and not already in the list
                        allBranches.add(branchName); // Add branch to list
                        Log.d(TAG, "Loaded branch: " + branchName); // Debug log for each branch loaded
                    }
                }
                Log.d(TAG, "✅ Total branches loaded: " + allBranches.size()); // Log total branch count
            }

            @Override
            public void onCancelled(DatabaseError error) { // Called if data fetch fails
                showCustomToast("Failed to load branches: " + error.getMessage()); // Show error message to user
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

    private void showMenuPopup(MenuItem existingItem, String preloadedID) { // Method to show popup for adding/editing a menu item
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_add_menu, null); // Inflate popup layout
        AlertDialog dialog = new AlertDialog.Builder(this).setView(popupView).create(); // Create dialog with popup view
        dialog.show(); // Show dialog

        Window window = dialog.getWindow(); // Get dialog window for customization
        if (window != null) {
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.9), // Set width = 90% of screen
                    WindowManager.LayoutParams.WRAP_CONTENT); // Height = wrap content
            window.setBackgroundDrawableResource(android.R.color.transparent); // Transparent background
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE); // Adjust UI when keyboard shows
        }

        // 🔹 Initialize popup input fields
        TextView title = popupView.findViewById(R.id.popupTitleMenu); // Popup title
        EditText idInput = popupView.findViewById(R.id.menuIdInput); // Menu ID field
        EditText nameInput = popupView.findViewById(R.id.menuNameInput); // Menu name field
        EditText categoryInput = popupView.findViewById(R.id.menuCategoryInput); // Menu category field
        EditText descInput = popupView.findViewById(R.id.menuDescriptionInput); // Menu description field
        EditText priceInput = popupView.findViewById(R.id.menuPriceInput); // Menu price field
        EditText imageInput = popupView.findViewById(R.id.menuImageInput); // Menu image URL field
        AutoCompleteTextView branchesDropdown = popupView.findViewById(R.id.branchesDropdown); // Dropdown to select branches
        ChipGroup selectedBranchesGroup = popupView.findViewById(R.id.selectedBranchesGroup); // Group to hold branch chips
        Button saveBtn = popupView.findViewById(R.id.saveMenuBtn); // Save button
        Button cancelBtn = popupView.findViewById(R.id.cancelMenuBtn); // Cancel button

        // 🔹 Setup branch dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<>( // Adapter for branches dropdown
                this, android.R.layout.simple_dropdown_item_1line, allBranches
        );
        branchesDropdown.setAdapter(adapter); // Attach adapter to dropdown
        branchesDropdown.setThreshold(1); // Start suggesting after 1 character

        List<String> selectedBranches = new ArrayList<>(); // List of selected branches
        branchesDropdown.setOnItemClickListener((parent, view, position, id) -> { // Handle branch selection
            String selected = adapter.getItem(position); // Get selected branch
            if (selected != null && !selectedBranches.contains(selected)) { // If valid and not already added
                selectedBranches.add(selected); // Add to selected list
                Chip chip = new Chip(this); // Create chip for branch
                chip.setText(selected); // Set chip text
                chip.setCloseIconVisible(true); // Show close (X) icon
                chip.setOnCloseIconClickListener(v -> { // Handle chip remove
                    selectedBranches.remove(selected);
                    selectedBranchesGroup.removeView(chip);
                });
                selectedBranchesGroup.addView(chip); // Add chip to group
                branchesDropdown.setText(""); // Clear dropdown text
            }
        });

        boolean isEditing = existingItem != null; // Check if editing an existing item
        if (isEditing) { // If editing existing menu item
            title.setText("Editing Menu Item"); // Set popup title
            idInput.setText(existingItem.menuID); // Show existing ID
            idInput.setEnabled(false); // Disable editing ID
            nameInput.setText(existingItem.name); // Load existing name
            categoryInput.setText(existingItem.category); // Load existing category
            descInput.setText(existingItem.description); // Load existing description
            priceInput.setText(String.valueOf(existingItem.price)); // Load existing price
            imageInput.setText(existingItem.imageURL); // Load existing image URL

            if (existingItem.branches != null) { // If item has assigned branches
                for (String b : existingItem.branches) { // Loop through branches
                    if (b == null) continue;
                    selectedBranches.add(b); // Add to selected list
                    Chip chip = new Chip(this); // Create chip
                    chip.setText(b); // Set branch name
                    chip.setCloseIconVisible(true); // Show close icon
                    chip.setOnCloseIconClickListener(v -> { // Handle remove
                        selectedBranches.remove(b);
                        selectedBranchesGroup.removeView(chip);
                    });
                    selectedBranchesGroup.addView(chip); // Add chip to group
                }
            }
        } else { // If adding a new menu item
            title.setText("Add New Menu Item"); // Set popup title
            idInput.setText(preloadedID); // Preload generated menu ID
            idInput.setEnabled(false); // Disable editing ID
        }

        cancelBtn.setOnClickListener(v -> dialog.dismiss()); // Close popup when cancel is clicked
        saveBtn.setOnClickListener(v -> saveMenuItem( // Save menu item when save is clicked
                existingItem, idInput, nameInput, categoryInput,
                descInput, priceInput, imageInput, selectedBranches, dialog
        ));
    }

    private void saveMenuItem(MenuItem existingItem, EditText idInput, EditText nameInput, EditText categoryInput,
                              EditText descInput, EditText priceInput, EditText imageInput,
                              List<String> selectedBranches, AlertDialog dialog) { // Method to save or update a menu item

        // 🔹 Read user input from fields
        String id = idInput.getText().toString().trim(); // Menu ID
        String name = nameInput.getText().toString().trim(); // Menu name
        String category = categoryInput.getText().toString().trim(); // Category
        String desc = descInput.getText().toString().trim(); // Description
        String priceStr = priceInput.getText().toString().trim(); // Price string (needs parsing)
        String imageUrl = imageInput.getText().toString().trim(); // Image URL

        // 🔹 Validate required fields
        if (id.isEmpty() || name.isEmpty() || category.isEmpty() || priceStr.isEmpty()) {
            showCustomToast("Please fill all required fields"); // Show error if required fields missing
            return; // Stop execution
        }

        // 🔹 Parse price
        double price;
        try {
            price = Double.parseDouble(priceStr); // Convert string to double
        } catch (NumberFormatException e) { // Handle invalid input
            showCustomToast("Invalid price"); // Show error if not a valid number
            return; // Stop execution
        }

        // 🔹 If editing, check if there are changes
        if (existingItem != null) {
            boolean noChanges =
                    existingItem.name.equals(name) &&
                            existingItem.category.equals(category) &&
                            ((existingItem.description == null && desc.isEmpty()) ||
                                    (existingItem.description != null && existingItem.description.equals(desc))) &&
                            existingItem.price == price &&
                            ((existingItem.imageURL == null && imageUrl.isEmpty()) ||
                                    (existingItem.imageURL != null && existingItem.imageURL.equals(imageUrl))) &&
                            existingItem.branches != null &&
                            existingItem.branches.size() == selectedBranches.size() &&
                            existingItem.branches.containsAll(selectedBranches);

            if (noChanges) {
                showCustomToast("No changes detected"); // ✅ Custom message if no updates
                return;
            }
        }

        // 🔹 Create new MenuItem object and set values
        MenuItem menuItem = new MenuItem();
        menuItem.menuID = id; // Set ID
        menuItem.name = name; // Set name
        menuItem.category = category; // Set category
        menuItem.description = desc; // Set description
        menuItem.price = price; // Set price
        menuItem.imageURL = imageUrl; // Set image URL
        menuItem.branches = new ArrayList<>(selectedBranches); // Save selected branches

        // 🔹 Save menu item in Firebase
        db.child("menu").child(id).setValue(menuItem) // Save item under "menu/{id}"
                .addOnSuccessListener(aVoid -> { // If save succeeds
                    String msg = (existingItem != null) ? "Menu item updated" : "Menu item saved";
                    showCustomToast(msg); // Notify user
                    dialog.dismiss(); // Close popup
                    loadMenuItems(); // Refresh menu list
                })
                .addOnFailureListener(e -> { // If save fails
                    Log.e(TAG, "Save failed: " + e.getMessage(), e); // Log error
                    showCustomToast("Failed: " + e.getMessage()); // Notify user
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
