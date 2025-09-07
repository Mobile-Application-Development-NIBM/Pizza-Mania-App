// ========== Activity Summary ==========
// AdminDeliverymanManagement
// - Admin screen for managing Deliverymen per Branch
// - Features:
//     * Load and display all Branches and their Deliverymen
//     * Add new Deliverymen with auto-generated IDs (deliverymanID, userID)
//     * Edit existing Deliverymen (update name, email, password, contact, address)
//     * Delete Deliverymen (and linked User account) with confirmation
//     * Always ensures Deliveryman has a "status" column (default = "Available")
//     * Custom Loading Dialog with fixed size (300dp x 180dp)
//     * Custom Top Toast with progress bar and close button
//     * Navigation:
//         - Back to AdminHomeActivity
//         - To Branch/Employee Management
//
// - Main Components:
//     * Firebase Realtime Database Reference (db)
//     * LinearLayout branchList → dynamically loads branches and deliverymen
//     * Custom popup dialogs (Add/Edit/Delete Deliverymen)
//     * Custom Loading Dialog + Toast
//
// - Model Classes:
//     * Branch: branchID, name
//     * Deliveryman: delID, branchID, name, email, contact, address, userID, password, status
//     * User: userID, name, email, address, contact, role ("Deliveryman")
//
// - Key Methods:
//     * onCreate()
//         - Initializes UI and navigation buttons
//         - Loads branches and their deliverymen
//     * loadBranches()
//         - Loads all branches from Firebase
//         - Inflates rows with deliverymen list per branch
//         - Calls loadDeliverymen() for each branch
//     * showAddDeliverymanPopup(branchId)
//         - Generates new deliverymanID and userID
//         - Shows popup form to enter details
//         - Validates input, creates Deliveryman + User objects
//         - Saves to Firebase
//     * loadDeliverymen(branchId, container, onLoaded)
//         - Loads deliverymen of a branch
//         - Populates list with edit/delete buttons
//     * showEditDeliverymanPopup(deliveryman)
//         - Opens popup pre-filled with existing values
//         - Updates Firebase if changes detected
//     * showDeleteConfirmation(type, key)
//         - Shows confirmation dialog
//         - Deletes deliveryman and linked user from Firebase
//     * showLoadingDialog(message) / hideLoadingDialog()
//         - Shows/Hides custom loading popup
//     * showCustomToast(message)
//         - Shows custom top toast with progress countdown
//
// - Notes:
//     * Ensures status is always present in Deliveryman records
//     * Prevents saving if no changes detected when editing
//     * Deletes linked user entry when deleting a deliveryman
// ======================================


package com.example.pizzamaniaapp; // Package name of the app

import android.content.Intent; // For navigating between activities
import android.graphics.Color; // For setting colors programmatically
import android.graphics.drawable.ColorDrawable; // For transparent/colored backgrounds
import android.os.Bundle; // For passing data between activities and saving instance state
import android.os.CountDownTimer; // For countdown timers (used in custom toast)
import android.view.LayoutInflater; // For inflating custom layouts
import android.view.View; // Base class for all UI components
import android.view.Window; // For controlling dialog/activity window properties
import android.view.WindowManager; // For setting window attributes (size, gravity, etc.)
import android.widget.Button; // Standard button UI element
import android.widget.EditText; // Text input field
import android.widget.ImageButton; // Button with an image (used for navigation/edit/delete)
import android.widget.LinearLayout; // Layout that arranges children in a single column/row
import android.widget.ProgressBar; // Progress indicator (used in custom toast countdown)
import android.widget.TextView; // Text display element
import android.widget.ImageView; // Image display element

import androidx.appcompat.app.AlertDialog; // For creating custom dialogs
import androidx.appcompat.app.AppCompatActivity; // Base class for activities with support features

import androidx.activity.EdgeToEdge; // For edge-to-edge screen display
import androidx.core.graphics.Insets; // For handling system bar insets
import androidx.core.view.ViewCompat; // For applying insets to views
import androidx.core.view.WindowInsetsCompat; // For managing window insets (status/navigation bar)

import com.google.firebase.database.DataSnapshot; // Snapshot of data at a database location
import com.google.firebase.database.DatabaseError; // Error callback for Firebase database operations
import com.google.firebase.database.DatabaseReference; // Reference to a specific Firebase DB node
import com.google.firebase.database.FirebaseDatabase; // Firebase Realtime Database entry point
import com.google.firebase.database.ValueEventListener; // Listener for data changes in Firebase

import java.util.ArrayList; // Resizable array implementation
import java.util.Collections; // Utility class for collection operations (sorting, etc.)
import java.util.Comparator; // For comparing objects when sorting
import java.util.List; // List interface for ordered collections


public class AdminDeliverymanManagement extends AppCompatActivity { // Activity class for managing deliverymen

    LinearLayout branchList; // Container to dynamically hold branch rows
    DatabaseReference db; // Firebase database reference
    AlertDialog loadingDialog; // Custom loading dialog
    ImageButton reloadBtn; // Reload button to refresh branches

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Lifecycle method called when activity is created
        super.onCreate(savedInstanceState); // Call parent constructor
        EdgeToEdge.enable(this); // Enable edge-to-edge display
        setContentView(R.layout.activity_admin_deliveryman_management); // Set layout for this activity
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> { // Handle system bar insets
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); // Get status/nav bar insets
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom); // Apply padding
            return insets; // Return modified insets
        });

        branchList = findViewById(R.id.branchList); // Find branch list container by ID
        db = FirebaseDatabase.getInstance().getReference(); // Get Firebase DB reference

        loadBranches(); // Load branches and deliverymen on startup

        reloadBtn = findViewById(R.id.reloadBranchBtn); // Find reload button by ID
        reloadBtn.setOnClickListener(v -> loadBranches()); // Reload branches when button clicked

        ImageButton btnBranch = findViewById(R.id.branchPageButton); // Find branch page button by ID

        btnBranch.setOnClickListener(new View.OnClickListener() { // Set click listener for branch page button
            @Override
            public void onClick(View v) { // When clicked
                Intent intent = new Intent(AdminDeliverymanManagement.this, AdminBranchEmployeeManagementActivity.class); // Create intent to open branch/employee management
                startActivity(intent); // Start new activity
            }
        });

        // --- Home button click ---
        ImageButton homeButton = findViewById(R.id.homeButton); // Find home button by ID
        homeButton.setOnClickListener(v -> { // Set click listener
            Intent intent = new Intent(AdminDeliverymanManagement.this, AdminHomeActivity.class); // Create intent to open AdminHomeActivity
            startActivity(intent); // Start new activity
        });
    }

    private void loadBranches() { // Method to load all branches from Firebase
        branchList.removeAllViews(); // Clear the branch list UI before reloading
        showLoadingDialog("Loading branches & deliverymen..."); // Show loading dialog while fetching data

        db.child("branches").addListenerForSingleValueEvent(new ValueEventListener() { // Read branches from Firebase once
            @Override
            public void onDataChange(DataSnapshot snapshot) { // Called when data is received
                List<Branch> branchListTemp = new ArrayList<>(); // Temporary list to hold branches
                for (DataSnapshot branchSnap : snapshot.getChildren()) { // Loop through each branch snapshot
                    Branch branch = branchSnap.getValue(Branch.class); // Convert snapshot to Branch object
                    if (branch != null) branchListTemp.add(branch); // Add branch if not null
                }

                // Sort branches by numeric part of branchID (b001, b002, ...)
                Collections.sort(branchListTemp, Comparator.comparingInt(a -> Integer.parseInt(a.branchID.substring(1))));

                if (branchListTemp.isEmpty()) { // If no branches found
                    hideLoadingDialog(); // Hide loading dialog
                    return; // Exit method
                }

                final int[] branchesLoaded = {0}; // Counter to track loaded branches

                for (Branch branch : branchListTemp) { // Loop through each branch
                    // ✅ Inflate the branch row layout for deliverymen
                    View branchView = getLayoutInflater().inflate(R.layout.deliveryman_branch_row, null);

                    TextView branchName = branchView.findViewById(R.id.branchName); // Branch name text
                    branchName.setText(branch.name); // Set branch name

                    // ✅ Add deliveryman button (Button type in XML)
                    Button addDeliveryBtn = branchView.findViewById(R.id.addDeliverymanBtn);

                    // ✅ Container for deliverymen under this branch
                    LinearLayout deliveryContainer = branchView.findViewById(R.id.deliverymanContainer);

                    addDeliveryBtn.setOnClickListener(v -> showAddDeliverymanPopup(branch.branchID)); // Open popup to add deliveryman

                    loadDeliverymen(branch.branchID, deliveryContainer, () -> { // Load deliverymen for this branch
                        branchesLoaded[0]++; // Increment loaded branch count
                        if (branchesLoaded[0] == branchListTemp.size()) hideLoadingDialog(); // Hide dialog after last branch is done
                    });

                    branchList.addView(branchView); // Add branch view to branch list
                }
            }

            @Override
            public void onCancelled(DatabaseError error) { // Called if Firebase query fails
                hideLoadingDialog(); // Hide loading dialog
            }
        });
    }

    private void showAddDeliverymanPopup(String branchId) { // Show popup dialog for adding a new deliveryman
        showLoadingDialog("Preparing new deliveryman..."); // Show loading dialog while preparing IDs

        // Step 1: Generate deliveryman ID
        db.child("deliverymen").addListenerForSingleValueEvent(new ValueEventListener() { // Read current deliverymen count
            @Override
            public void onDataChange(DataSnapshot snapshot) { // Called when deliverymen data is fetched
                int nextDelNum = (int) snapshot.getChildrenCount() + 1; // Next deliveryman number
                String deliverymanID = "d" + String.format("%03d", nextDelNum); // Format ID like d001, d002

                // Step 2: Generate user ID
                db.child("users").addListenerForSingleValueEvent(new ValueEventListener() { // Read current users count
                    @Override
                    public void onDataChange(DataSnapshot snapshot) { // Called when users data is fetched
                        int nextUserNum = (int) snapshot.getChildrenCount() + 1; // Next user number
                        String userID = "u" + String.format("%03d", nextUserNum); // Format ID like u001, u002

                        hideLoadingDialog(); // Hide loading dialog before showing popup

                        // --- Inflate popup layout ---
                        View popupView = getLayoutInflater().inflate(R.layout.add_deliveryman_popup, null); // Inflate custom popup
                        AlertDialog dialog = new AlertDialog.Builder(AdminDeliverymanManagement.this) // Create dialog
                                .setView(popupView) // Attach custom view
                                .create();
                        dialog.show(); // Show dialog

                        Window window = dialog.getWindow(); // Get dialog window
                        if (window != null) {
                            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.8), // Set width = 80% of screen
                                    WindowManager.LayoutParams.WRAP_CONTENT); // Height wraps content
                            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background
                        }

                        // --- Input fields ---
                        EditText deliverymanIdInput = popupView.findViewById(R.id.deliverymanIDInput); // Deliveryman ID field
                        EditText userIdInput = popupView.findViewById(R.id.userIDInput); // User ID field
                        EditText nameInput = popupView.findViewById(R.id.deliverymanNameInput); // Name input
                        EditText emailInput = popupView.findViewById(R.id.deliverymanEmailInput); // Email input
                        EditText passwordInput = popupView.findViewById(R.id.deliverymanPasswordInput); // Password input
                        EditText contactInput = popupView.findViewById(R.id.deliverymanContactInput); // Contact input
                        EditText addressInput = popupView.findViewById(R.id.deliverymanAddressInput); // Address input

                        deliverymanIdInput.setText(deliverymanID); // Pre-fill deliveryman ID
                        deliverymanIdInput.setEnabled(false); // Disable editing
                        userIdInput.setText(userID); // Pre-fill user ID
                        userIdInput.setEnabled(false); // Disable editing

                        // --- Buttons ---
                        Button cancelBtn = popupView.findViewById(R.id.cancelDeliverymanBtn); // Cancel button
                        Button saveBtn = popupView.findViewById(R.id.saveDeliverymanBtn); // Save button
                        cancelBtn.setOnClickListener(v -> dialog.dismiss()); // Dismiss popup when cancel is clicked

                        saveBtn.setOnClickListener(v -> { // Handle save button click
                            String name = nameInput.getText().toString().trim(); // Get name
                            String email = emailInput.getText().toString().trim(); // Get email
                            String password = passwordInput.getText().toString().trim(); // Get password
                            String contactStr = contactInput.getText().toString().trim(); // Get contact as string
                            String address = addressInput.getText().toString().trim(); // Get address

                            if (name.isEmpty() || email.isEmpty() || password.isEmpty() // Check if any field is empty
                                    || contactStr.isEmpty() || address.isEmpty()) {
                                showCustomToast("Please fill all fields"); // Show warning toast
                                return; // Stop saving
                            }

                            long contact;
                            try {
                                contact = Long.parseLong(contactStr); // Try parsing contact number
                            } catch (NumberFormatException e) { // If invalid number
                                showCustomToast("Invalid contact"); // Show error toast
                                return;
                            }

                            // --- Create objects ---
                            Deliveryman deliveryman = new Deliveryman( // Create new Deliveryman object
                                    deliverymanID, branchId, name, email, contact, address, userID, password
                            );
                            deliveryman.status = "Available"; // 🔹 Always set default status

                            User user = new User(userID, name, email, address, contact, "Deliveryman"); // Create linked User object

                            // --- Save to Firebase ---
                            db.child("deliverymen").child(deliverymanID).setValue(deliveryman) // Save deliveryman
                                    .addOnSuccessListener(aVoid -> db.child("users").child(userID).setValue(user) // On success, save user
                                            .addOnSuccessListener(aVoid2 -> { // Both saved successfully
                                                showCustomToast("Deliveryman added!"); // Show success toast
                                                loadBranches(); // Refresh deliveryman list
                                                dialog.dismiss(); // Close dialog
                                            })
                                            .addOnFailureListener(e -> showCustomToast("Failed to add user: " + e.getMessage()))) // Error while saving user
                                    .addOnFailureListener(e -> showCustomToast("Failed to add deliveryman: " + e.getMessage())); // Error while saving deliveryman
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError error) { // Called if Firebase fails
                        hideLoadingDialog(); // Hide loading dialog
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) { // Called if Firebase fails
                hideLoadingDialog(); // Hide loading dialog
            }
        });
    }

    private void loadDeliverymen(String branchId, LinearLayout container, Runnable onLoaded) { // Load deliverymen for a specific branch
        db.child("deliverymen").orderByChild("branchID").equalTo(branchId) // Query deliverymen where branchID matches
                .addListenerForSingleValueEvent(new ValueEventListener() { // Listen for data once
                    @Override
                    public void onDataChange(DataSnapshot snapshot) { // Called when data is fetched
                        container.removeAllViews(); // Clear old views before reloading
                        for (DataSnapshot delSnap : snapshot.getChildren()) { // Loop through deliverymen in this branch
                            Deliveryman del = delSnap.getValue(Deliveryman.class); // Convert snapshot to Deliveryman object
                            if (del == null) continue; // Skip if null

                            View delView = getLayoutInflater().inflate(R.layout.employee_row, null); // Inflate row layout
                            TextView delName = delView.findViewById(R.id.employeeName); // Find name field
                            delName.setText(del.name); // Set deliveryman name

                            ImageButton editBtn = delView.findViewById(R.id.editEmployeeBtn); // Edit button
                            ImageButton deleteBtn = delView.findViewById(R.id.deleteEmployeeBtn); // Delete button

                            editBtn.setOnClickListener(v -> showEditDeliverymanPopup(del)); // Open edit popup on click
                            deleteBtn.setOnClickListener(v -> showDeleteConfirmation("deliveryman", del.delID)); // Show delete confirmation

                            container.addView(delView); // Add deliveryman row to container
                        }
                        if (onLoaded != null) onLoaded.run(); // Run callback when done loading
                    }

                    @Override
                    public void onCancelled(DatabaseError error) { // Called if Firebase query fails
                        if (onLoaded != null) onLoaded.run(); // Still run callback to avoid hanging
                    }
                });
    }

    private void showEditDeliverymanPopup(Deliveryman del) { // Show popup dialog for editing an existing deliveryman
        View popupView = getLayoutInflater().inflate(R.layout.add_deliveryman_popup, null); // Inflate the same popup layout
        AlertDialog dialog = new AlertDialog.Builder(this).setView(popupView).create(); // Create dialog with custom view
        dialog.show(); // Show dialog

        Window window = dialog.getWindow(); // Get dialog window
        if (window != null) {
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.8), // Width = 80% of screen
                    WindowManager.LayoutParams.WRAP_CONTENT); // Height wraps content
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background
        }

        TextView popupTitle = popupView.findViewById(R.id.popupTitleDeliveryman); // Find popup title
        popupTitle.setText("Edit Deliveryman"); // Set title to "Edit Deliveryman"

        // --- Input fields ---
        EditText delIdInput = popupView.findViewById(R.id.deliverymanIDInput); // Deliveryman ID field
        EditText userIdInput = popupView.findViewById(R.id.userIDInput); // User ID field
        EditText nameInput = popupView.findViewById(R.id.deliverymanNameInput); // Name input
        EditText emailInput = popupView.findViewById(R.id.deliverymanEmailInput); // Email input
        EditText passwordInput = popupView.findViewById(R.id.deliverymanPasswordInput); // Password input
        EditText contactInput = popupView.findViewById(R.id.deliverymanContactInput); // Contact input
        EditText addressInput = popupView.findViewById(R.id.deliverymanAddressInput); // Address input

        Button cancelBtn = popupView.findViewById(R.id.cancelDeliverymanBtn); // Cancel button
        Button updateBtn = popupView.findViewById(R.id.saveDeliverymanBtn); // Save button (renamed to Update here)
        cancelBtn.setOnClickListener(v -> dialog.dismiss()); // Dismiss dialog when cancel is clicked

        // --- Pre-fill values from existing deliveryman ---
        delIdInput.setText(del.delID); // Set deliveryman ID
        delIdInput.setEnabled(false); // Disable editing
        userIdInput.setText(del.userID); // Set user ID
        userIdInput.setEnabled(false); // Disable editing
        nameInput.setText(del.name); // Set name
        emailInput.setText(del.email); // Set email
        passwordInput.setText(del.password); // Set password
        contactInput.setText(String.valueOf(del.contact)); // Set contact number
        addressInput.setText(del.address); // Set address
        updateBtn.setText("Update"); // Change button text to "Update"

        updateBtn.setOnClickListener(v -> { // Handle update button click
            String name = nameInput.getText().toString().trim(); // Get updated name
            String email = emailInput.getText().toString().trim(); // Get updated email
            String password = passwordInput.getText().toString().trim(); // Get updated password
            String contactStr = contactInput.getText().toString().trim(); // Get updated contact
            String address = addressInput.getText().toString().trim(); // Get updated address

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() // Validate empty fields
                    || contactStr.isEmpty() || address.isEmpty()) {
                showCustomToast("Please fill all fields"); // Show error toast
                return; // Stop update
            }

            long contact;
            try {
                contact = Long.parseLong(contactStr); // Try parsing contact number
            } catch (Exception e) { // If parsing fails
                showCustomToast("Invalid contact"); // Show error toast
                return;
            }

            // If no changes detected in any field
            if (del.name.equals(name) && del.email.equals(email) &&
                    del.password.equals(password) && del.contact == contact &&
                    del.address.equals(address)) {
                showCustomToast("No changes detected!"); // Show message
                return; // Stop update
            }

            // --- Update deliveryman object ---
            del.name = name; // Update name
            del.email = email; // Update email
            del.password = password; // Update password
            del.contact = contact; // Update contact
            del.address = address; // Update address
            if (del.status == null || del.status.isEmpty()) { // If status missing
                del.status = "Available"; // ✅ Ensure status column always exists
            }

            // --- Save updated deliveryman to Firebase ---
            db.child("deliverymen").child(del.delID).setValue(del) // Overwrite deliveryman record
                    .addOnSuccessListener(aVoid -> { // If successful
                        showCustomToast("Deliveryman updated!"); // Show success toast
                        loadBranches(); // Refresh branch list
                        dialog.dismiss(); // Close dialog
                    })
                    .addOnFailureListener(e -> showCustomToast("Update failed: " + e.getMessage())); // Show error if failed
        });
    }

    private void showDeleteConfirmation(String type, String key) { // Show confirmation popup before deleting a deliveryman
        View popupView = getLayoutInflater().inflate(R.layout.delete_confirmation_popup, null); // Inflate delete confirmation layout
        AlertDialog dialog = new AlertDialog.Builder(this).setView(popupView).create(); // Create dialog with custom view
        dialog.show(); // Show dialog

        Window window = dialog.getWindow(); // Get dialog window
        if (window != null) {
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.8), // Width = 80% of screen
                    WindowManager.LayoutParams.WRAP_CONTENT); // Height wraps content
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background
        }

        Button cancelBtn = popupView.findViewById(R.id.cancelDeleteBtn); // Cancel button
        Button confirmBtn = popupView.findViewById(R.id.confirmDeleteBtn); // Confirm button

        cancelBtn.setOnClickListener(v -> dialog.dismiss()); // Dismiss popup if cancel clicked
        confirmBtn.setOnClickListener(v -> { // Confirm delete button clicked
            dialog.dismiss(); // 🔹 Close popup immediately after confirming

            // 🔹 Check if deliveryman exists before deleting
            db.child("deliverymen").child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Deliveryman del = snapshot.getValue(Deliveryman.class); // Get deliveryman object
                    if (del == null) { // If not found
                        showCustomToast("Deliveryman not found!"); // Show error toast
                        return; // Stop further execution
                    }

                    String userId = del.userID; // Get linked userID

                    // --- Remove deliveryman from database ---
                    db.child("deliverymen").child(key).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                // --- Remove linked user from "users" node ---
                                db.child("users").child(userId).removeValue()
                                        .addOnSuccessListener(aVoid2 -> {
                                            showCustomToast("Deliveryman deleted!"); // Success toast
                                            loadBranches(); // Refresh branch & deliveryman list
                                        })
                                        .addOnFailureListener(e ->
                                                showCustomToast("User delete failed: " + e.getMessage())); // User removal failed
                            })
                            .addOnFailureListener(e ->
                                    showCustomToast("Delete failed: " + e.getMessage())); // Deliveryman removal failed
                }

                @Override
                public void onCancelled(DatabaseError error) { // Firebase query cancelled or failed
                    showCustomToast("Error: " + error.getMessage()); // Show error toast
                }
            });
        });
    }

    private void showLoadingDialog(String message) { // Show a custom loading dialog with message
        if (loadingDialog != null && loadingDialog.isShowing()) return; // If already showing, skip creating again

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null); // Inflate custom loading layout
        TextView textView = view.findViewById(R.id.loadingText); // Find TextView inside layout
        textView.setText(message); // Set provided message

        loadingDialog = new AlertDialog.Builder(this) // Create new AlertDialog
                .setView(view) // Set custom loading view
                .setCancelable(false) // Make it non-cancelable by outside touches
                .create();

        loadingDialog.show(); // Show dialog

        // --- Adjust dialog window size and background ---
        if (loadingDialog.getWindow() != null) {
            int width = (int) (300 * getResources().getDisplayMetrics().density);  // Convert 300dp → pixels
            int height = (int) (180 * getResources().getDisplayMetrics().density); // Convert 180dp → pixels
            loadingDialog.getWindow().setLayout(width, height); // Apply custom size
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent corners
        }
    }

    private void hideLoadingDialog() { // Hide the loading dialog if it’s showing
        if (loadingDialog != null && loadingDialog.isShowing()) { // Check dialog exists & is visible
            loadingDialog.dismiss(); // Close it
        }
    }

    private void showCustomToast(String message) { // Method to show custom toast dialog
        LayoutInflater inflater = getLayoutInflater(); // Get inflater to load custom layout
        View layout = inflater.inflate(R.layout.custom_message, null); // Inflate the custom toast layout

        TextView text = layout.findViewById(R.id.toast_message); // Get TextView for message
        ImageView close = layout.findViewById(R.id.toast_close); // Get close button ImageView
        ProgressBar progressBar = layout.findViewById(R.id.toast_progress); // Get ProgressBar

        text.setText(message); // Set the toast message text
        progressBar.setProgress(100); // Initialize progress bar to full (100%)

        AlertDialog dialog = new AlertDialog.Builder(this) // Create AlertDialog instance
                .setView(layout) // Attach custom layout to dialog
                .create(); // Build the dialog

        close.setOnClickListener(v -> dialog.dismiss()); // Dismiss dialog when close button clicked

        if (dialog.getWindow() != null) { // Ensure window exists before modifying
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background
            dialog.getWindow().setDimAmount(0f); // No dimming behind dialog

            WindowManager.LayoutParams params = dialog.getWindow().getAttributes(); // Get dialog attributes
            params.width = WindowManager.LayoutParams.MATCH_PARENT; // Set full width
            params.height = WindowManager.LayoutParams.WRAP_CONTENT; // Set height to wrap content
            params.gravity = android.view.Gravity.TOP; // Position at the top of screen
            params.y = 50; // Offset by 50px from the top
            dialog.getWindow().setAttributes(params); // Apply attributes
        }

        dialog.show(); // Show the dialog

        new CountDownTimer(3000, 50) { // 3-second countdown, ticks every 50ms
            public void onTick(long millisUntilFinished) { // Each tick event
                int progress = (int) ((millisUntilFinished / 3000.0) * 100); // Calculate % progress
                progressBar.setProgress(progress); // Update progress bar accordingly
            }
            public void onFinish() { // When countdown finishes
                if (dialog.isShowing()) dialog.dismiss(); // Dismiss the dialog if still showing
            }
        }.start(); // Start the countdown timer
    }

    // Model classes inside activity for simplicity
    public static class Branch { // Branch model class
        public String branchID, name; // Branch ID and name fields

        public Branch() {} // Default constructor (needed for Firebase)

        public Branch(String branchID, String name){ // Constructor with parameters
            this.branchID = branchID; // Assign branch ID
            this.name = name; // Assign branch name
        }
    }

    // ================= Deliveryman model =================
    public static class Deliveryman { // Deliveryman model class
        public String delID, branchID, name, email, address, userID, password, status; // Fields for deliveryman info (status added)
        public long contact; // Contact number

        public Deliveryman() {} // Default constructor (needed for Firebase)

        public Deliveryman(String delID, String branchID, String name, String email,
                           long contact, String address, String userID, String password) { // Constructor with parameters
            this.delID = delID; // Deliveryman ID
            this.branchID = branchID; // Associated branch ID
            this.name = name; // Deliveryman's name
            this.email = email; // Deliveryman's email
            this.contact = contact; // Contact number
            this.address = address; // Home address
            this.userID = userID; // Linked user account ID
            this.password = password; // Password for login
            this.status = "Available"; // 🔹 Default status set when created
        }
    }

    public static class User { // User model class
        public String userID, name, email, address, role; // User fields (role specifies type, e.g. Deliveryman)
        public long contact; // Contact number

        public User() {} // Default constructor (needed for Firebase)

        public User(String userID, String name, String email, String address, long contact, String role) { // Constructor with parameters
            this.userID = userID; // User ID
            this.name = name; // User's name
            this.email = email; // User's email
            this.address = address; // User's address
            this.contact = contact; // Contact number
            this.role = role; // Role (e.g. "Deliveryman")
        }
    }
}
