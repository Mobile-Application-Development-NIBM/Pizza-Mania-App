// ========== Activity Summary ==========
// AdminBranchEmployeeManagementActivity
// - Manage branches and employees via Firebase Realtime Database
// - Features:
//     * Load and display all branches
//     * Add/Edit/Delete branches
//     * Add/Edit/Delete employees per branch
//     * Auto-generate IDs for branches (b001...), employees (e001...), and users (u001...)
//     * Popups/dialogs for forms and confirmation
//     * Custom top toast for messages
// ======================================

// onCreate()
// - Sets layout
// - Initializes Firebase DB reference
// - Loads branch list
// - Handles "Add Branch" button click

// prepareNewBranchPopup()
// - Fetches total branches
// - Generates next branch ID
// - Opens popup for new branch

// showBranchPopup(existingBranch, preloadedBranchID)
// - Add/Edit branch popup
// - Pre-fills fields if editing
// - Validates input: name, contact, location
// - Saves branch to Firebase
// - Refreshes branch list

// loadBranches()
// - Fetches all branches from Firebase
// - Sorts by branch ID number
// - Creates branch row with:
//     * Edit branch button
//     * Delete branch button
//     * Add employee button
// - Loads employees for each branch

// showAddEmployeePopup(branchId)
// - Popup to add new employee
// - Auto-generate Employee ID and User ID
// - Validate inputs: name, email, contact, address
// - Create Employee and User objects in Firebase
// - Refresh branch list on save

// loadEmployees(branchId, container)
// - Loads employees linked to branch
// - Creates employee rows dynamically
// - Each row has Edit + Delete buttons

// showEditEmployeePopup(employee)
// - Popup to edit existing employee
// - Pre-fills current details
// - Updates both Employee + linked User in Firebase
// - Reloads branch list on update

// showDeleteConfirmation(type, key)
// - Confirm delete popup
// - If branch: delete branch
// - If employee: delete employee + linked user
// - Refreshes branch list

// Branch class
// - branchID, name, contact, latitude, longitude

// Employee class
// - employeeID, branchID, name, email, contact, address, userID

// User class
// - userID, name, email, address, phone, role

// showCustomToast(message)
// - Custom top toast popup
// - Auto-dismiss after 3 seconds with progress bar

// showLoadingDialog(message)
// - Shows a custom loading popup with message
// - Prevents user interaction while loading
// - Forces fixed size (300dp x 180dp) for consistency
// - Transparent background

// hideLoadingDialog()
// - Dismisses the loading popup if it's showing



package com.example.pizzamaniaapp; // package: identifies app/project

import android.content.Intent;
import android.os.Bundle; // Bundle: used to pass data between activities
import android.os.CountDownTimer;
import android.view.View; // View: base class for all UI elements
import android.view.Window; // Window: used to customize activity window
import android.view.WindowManager; // WindowManager: manages window properties
import android.graphics.Color; // Color: provides color constants/methods
import android.graphics.drawable.ColorDrawable; // ColorDrawable: colored background drawable
import android.widget.Button; // Button: clickable UI button
import android.widget.ImageButton; // ImageButton: button with an image
import android.widget.ImageView;
import android.widget.LinearLayout; // LinearLayout: layout arranging children vertically/horizontally
import android.widget.ProgressBar;
import android.widget.TextView; // TextView: displays text
import android.widget.EditText; // EditText: text input field
import android.widget.Toast; // Toast: short popup message
import android.view.LayoutInflater; // LayoutInflater: to inflate XML layouts into View objects

import androidx.appcompat.app.AlertDialog; // AlertDialog: popup dialog for messages/confirmation
import androidx.appcompat.app.AppCompatActivity; // AppCompatActivity: base class for activities with modern features

import com.google.firebase.database.DataSnapshot; // DataSnapshot: snapshot of data from Firebase
import com.google.firebase.database.DatabaseError; // DatabaseError: handles Firebase DB errors
import com.google.firebase.database.DatabaseReference; // DatabaseReference: reference to a Firebase node
import com.google.firebase.database.FirebaseDatabase; // FirebaseDatabase: entry point to Firebase real-time DB
import com.google.firebase.database.ValueEventListener; // ValueEventListener: listens for data changes in Firebase

import java.util.ArrayList; // ArrayList: dynamic array to store objects
import java.util.Collections; // Collections: utility for sorting, reversing, etc.
import java.util.Comparator; // Comparator: defines rules to compare objects for sorting
import java.util.List; // List: ordered collection of objects


public class AdminBranchEmployeeManagementActivity extends AppCompatActivity { // main activity class for managing branches & employees

    LinearLayout branchList; // layout to hold branch items dynamically
    DatabaseReference db; // reference to Firebase database
    ImageButton addBranchBtn; // button to add new branch
    AlertDialog loadingDialog; //button for loading dialog
    ImageButton reloadBtn;    // button to reload branches



    @Override
    protected void onCreate(Bundle savedInstanceState) { // called when activity is created
        super.onCreate(savedInstanceState); // call parent constructor
        setContentView(R.layout.activity_admin_branch_employee_management); // set layout for this activity

        branchList = findViewById(R.id.branchList); // connect branchList layout from XML
        addBranchBtn = findViewById(R.id.addBranchBtn); // connect addBranchBtn from XML
        db = FirebaseDatabase.getInstance().getReference(); // get Firebase database reference

        loadBranches(); // load all branches from database
        addBranchBtn.setOnClickListener(v -> prepareNewBranchPopup()); // show popup when add button is clicked


        // --- Home button click ---
        ImageButton homeButton = findViewById(R.id.homeButton); // get home button by ID
        homeButton.setOnClickListener(v -> {
            // open AdminHomeActivity
            Intent intent = new Intent(AdminBranchEmployeeManagementActivity.this, AdminHomeActivity.class);
            startActivity(intent);
        });

        // --- Deliveryman button click ---
        ImageButton deliveryman = findViewById(R.id.deliverymanPageButton); // get deliveryman button by ID
        deliveryman.setOnClickListener(v -> {
            // open AdminDeliverymanManagement
            Intent intent = new Intent(AdminBranchEmployeeManagementActivity.this, AdminDeliverymanManagement.class);
            startActivity(intent);
        });


        reloadBtn = findViewById(R.id.reloadBranchBtn); // connect reloadBtn from XML
        reloadBtn.setOnClickListener(v -> loadBranches()); // reload branches when clicked
    }

    private void prepareNewBranchPopup() { // method to prepare and open the branch popup
        showLoadingDialog("Preparing new branch..."); // show loading dialog while generating branch ID

        db.child("branches").addListenerForSingleValueEvent(new ValueEventListener() { // fetch all branches from Firebase (one-time read)
            @Override
            public void onDataChange(DataSnapshot snapshot) { // called when data is successfully fetched
                hideLoadingDialog(); // hide loading dialog since data is ready

                int nextNumber = (int) snapshot.getChildrenCount() + 1; // count existing branches and add 1 for the new branch
                String branchID = "b" + String.format("%03d", nextNumber); // create branch ID in format b001, b002, ...

                // Now open the branch popup with the generated branch ID
                showBranchPopup(null, branchID); // pass null (no existing branch) and the new ID
            }

            @Override
            public void onCancelled(DatabaseError error) { // called if Firebase read fails
                hideLoadingDialog(); // hide loading since request failed
                showCustomToast("Failed to prepare branch: " + error.getMessage()); // show error message
            }
        });
    }

    private void showBranchPopup(Branch existingBranch, String preloadedBranchID) { // show add/edit branch popup
        // 1️⃣ Inflate the popup layout
        View popupView = getLayoutInflater().inflate(R.layout.add_branch_popup, null);

        // 2️⃣ Create the AlertDialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(popupView)
                .create();

        // 3️⃣ Show the dialog
        dialog.show();

        // 4️⃣ Customize window size & background
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.8), // width = 80% of screen
                    WindowManager.LayoutParams.WRAP_CONTENT); // height = wrap content
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // transparent bg for rounded corners
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        // 5️⃣ Get all views from popup
        TextView popupTitle = popupView.findViewById(R.id.popupTitleBranch); // popup title
        EditText branchIdInput = popupView.findViewById(R.id.branchIdInput); // branch ID input
        EditText branchNameInput = popupView.findViewById(R.id.branchNameInput); // branch name input
        EditText contactInput = popupView.findViewById(R.id.contactInput); // branch contact input
        EditText locationInput = popupView.findViewById(R.id.locationInput); // branch location input
        Button addBtn = popupView.findViewById(R.id.addBtn); // save/update button
        Button cancelBtn = popupView.findViewById(R.id.cancelBtn); // cancel button

        // 6️⃣ Cancel button closes the dialog
        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        // 7️⃣ Set title & button text depending on Add or Edit mode
        if (existingBranch != null) {
            popupTitle.setText("Edit Branch"); // editing mode
            addBtn.setText("Update");
        } else {
            popupTitle.setText("Add New Branch"); // adding mode
            addBtn.setText("Save");
        }

        // 8️⃣ Populate fields if editing, or preload ID if adding
        if (existingBranch != null) {
            // Fill existing branch details
            branchIdInput.setText(existingBranch.branchID);
            branchIdInput.setEnabled(false); // ID cannot be changed
            branchNameInput.setText(existingBranch.name);
            contactInput.setText(String.valueOf(existingBranch.contact));
            locationInput.setText(existingBranch.latitude + ", " + existingBranch.longitude);
        } else if (preloadedBranchID != null) {
            // Show loading only when preparing a new branch
            showLoadingDialog("Preparing branch...");
            branchIdInput.setText(preloadedBranchID); // preload branch ID
            branchIdInput.setEnabled(false); // disable editing ID
            hideLoadingDialog(); // ✅ hide loading once ID is set
        }

        // 9️⃣ Handle Save/Update button click
        addBtn.setOnClickListener(v -> {
            String id = branchIdInput.getText().toString().trim();
            String name = branchNameInput.getText().toString().trim();
            String contactStr = contactInput.getText().toString().trim();
            String locationStr = locationInput.getText().toString().trim();

            // 🔹 Validate required fields
            if (id.isEmpty() || name.isEmpty() || contactStr.isEmpty() || locationStr.isEmpty()) {
                showCustomToast("Please fill all fields");
                return;
            }

            // 🔹 Parse contact number
            long contact;
            try {
                contact = Long.parseLong(contactStr);
            } catch (NumberFormatException e) {
                showCustomToast("Invalid contact number");
                return;
            }

            // 🔹 Parse location (latitude,longitude)
            String[] locParts = locationStr.split(",");
            if (locParts.length != 2) {
                showCustomToast("Enter location as latitude,longitude");
                return;
            }

            double latitude, longitude;
            try {
                latitude = Double.parseDouble(locParts[0].trim());
                longitude = Double.parseDouble(locParts[1].trim());
            } catch (NumberFormatException e) {
                showCustomToast("Invalid location values");
                return;
            }

            // 🔹 Skip saving if nothing changed in edit mode
            if (existingBranch != null) {
                if (existingBranch.name.equals(name) &&
                        existingBranch.contact == contact &&
                        existingBranch.latitude == latitude &&
                        existingBranch.longitude == longitude) {
                    showCustomToast("No changes detected!");
                    return;
                }
            }

            // 🔹 Create branch object
            Branch branch = new Branch(id, name, contact, latitude, longitude);

            // ✅ No loading dialog here — only toast feedback
            db.child("branches").child(id).setValue(branch)
                    .addOnSuccessListener(aVoid -> {
                        showCustomToast(existingBranch != null ? "Branch updated!" : "Branch added!");
                        loadBranches(); // refresh list
                        dialog.dismiss(); // close popup
                    })
                    .addOnFailureListener(e -> {
                        showCustomToast("Failed: " + e.getMessage());
                    });
        });
    }

    private void loadBranches() { // load all branches from Firebase and display
        branchList.removeAllViews(); // clear existing branch views
        showLoadingDialog("Loading branches & employees..."); // show loading dialog

        db.child("branches").addListenerForSingleValueEvent(new ValueEventListener() { // read branches once
            @Override
            public void onDataChange(DataSnapshot snapshot) { // called when data is fetched
                List<Branch> branchListTemp = new ArrayList<>(); // temporary list to store branches
                for (DataSnapshot branchSnap : snapshot.getChildren()) { // loop through each branch
                    Branch branch = branchSnap.getValue(Branch.class); // convert snapshot to Branch object
                    if (branch != null) branchListTemp.add(branch); // add to temp list if not null
                }

                Collections.sort(branchListTemp, Comparator.comparingInt(a -> Integer.parseInt(a.branchID.substring(1)))); // sort branches by ID number

                if (branchListTemp.isEmpty()) { hideLoadingDialog(); return; } // hide loading if no branches

                final int[] branchesLoaded = {0}; // counter to track loaded branches

                for (Branch branch : branchListTemp) { // loop sorted branches
                    View branchView = getLayoutInflater().inflate(R.layout.branch_row, null); // inflate branch row layout
                    TextView branchName = branchView.findViewById(R.id.branchName); // get branch name TextView
                    branchName.setText(branch.name); // set branch name

                    ImageButton editBranch = branchView.findViewById(R.id.editBranchBtn); // edit button
                    ImageButton deleteBranch = branchView.findViewById(R.id.deleteBranchBtn); // delete button
                    Button addEmployeeBtn = branchView.findViewById(R.id.addEmployeeBtn); // add employee button
                    LinearLayout employeeContainer = branchView.findViewById(R.id.employeeContainer); // container for employees

                    editBranch.setOnClickListener(v -> showBranchPopup(branch, null)); // open edit popup
                    deleteBranch.setOnClickListener(v -> showDeleteConfirmation("branch", branch.branchID)); // open delete confirmation
                    addEmployeeBtn.setOnClickListener(v -> showAddEmployeePopup(branch.branchID)); // open add employee popup

                    // load employees for this branch, hide loading only after all are loaded
                    loadEmployees(branch.branchID, employeeContainer, () -> {
                        branchesLoaded[0]++;
                        if (branchesLoaded[0] == branchListTemp.size()) hideLoadingDialog(); // hide loading when all branches processed
                    });

                    branchList.addView(branchView); // add branch row to main layout
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                hideLoadingDialog(); // hide loading on error
            }
        });
    }

    private void showAddEmployeePopup(String branchId) {
        showLoadingDialog("Preparing new employee..."); // Show loading while fetching IDs

        // Step 1: Fetch all employees from Firebase
        db.child("employees").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // 🔎 Collect existing employee numbers to find smallest available
                List<Integer> empNums = new ArrayList<>();
                for (DataSnapshot s : snapshot.getChildren()) {
                    String id = s.getKey(); // Get employee ID key
                    if (id != null && id.startsWith("e")) {
                        try {
                            empNums.add(Integer.parseInt(id.substring(1))); // Extract numeric part
                        } catch (NumberFormatException ignored) {}
                    }
                }
                Collections.sort(empNums); // Sort ascending
                int nextEmp = 1;
                for (int num : empNums) { // Find first missing number
                    if (num == nextEmp) nextEmp++;
                    else if (num > nextEmp) break;
                }
                String empID = "e" + String.format("%03d", nextEmp); // Format ID with leading zeros

                // Step 2: Fetch all users to assign new user ID
                db.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // 🔎 Collect existing user numbers to find smallest available
                        List<Integer> userNums = new ArrayList<>();
                        for (DataSnapshot s : snapshot.getChildren()) {
                            String id = s.getKey(); // Get user ID key
                            if (id != null && id.startsWith("u")) {
                                try {
                                    userNums.add(Integer.parseInt(id.substring(1))); // Extract numeric part
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                        Collections.sort(userNums); // Sort ascending
                        int nextUser = 1;
                        for (int num : userNums) { // Find first missing number
                            if (num == nextUser) nextUser++;
                            else if (num > nextUser) break;
                        }
                        String userID = "u" + String.format("%03d", nextUser); // Format ID with leading zeros

                        hideLoadingDialog(); // Hide loading dialog before showing popup

                        // Inflate employee popup layout
                        View popupView = getLayoutInflater().inflate(R.layout.add_employee_popup, null);
                        AlertDialog dialog = new AlertDialog.Builder(AdminBranchEmployeeManagementActivity.this)
                                .setView(popupView)
                                .create();
                        dialog.show();

                        // Configure popup window size and background
                        Window window = dialog.getWindow();
                        if (window != null) {
                            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.8);
                            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
                            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                        }

                        // Bind input fields
                        EditText empIdInput = popupView.findViewById(R.id.employeeIDInput);
                        EditText userIdInput = popupView.findViewById(R.id.userIDInput);
                        EditText nameInput = popupView.findViewById(R.id.employeeNameInput);
                        EditText emailInput = popupView.findViewById(R.id.employeeEmailInput);
                        EditText passwordInput = popupView.findViewById(R.id.employeePasswordInput);
                        EditText contactInput = popupView.findViewById(R.id.employeeContactInput);
                        EditText addressInput = popupView.findViewById(R.id.employeeAddressInput);

                        // Pre-fill IDs and disable editing
                        empIdInput.setText(empID);
                        empIdInput.setEnabled(false);
                        userIdInput.setText(userID);
                        userIdInput.setEnabled(false);

                        // Bind buttons
                        Button cancelBtn = popupView.findViewById(R.id.cancelEmployeeBtn);
                        Button addBtn = popupView.findViewById(R.id.addEmployeeBtn);
                        cancelBtn.setOnClickListener(v -> dialog.dismiss()); // Close popup

                        // --- Handle add employee button ---
                        addBtn.setOnClickListener(v -> {
                            // Read input values
                            String name = nameInput.getText().toString().trim();
                            String email = emailInput.getText().toString().trim();
                            String password = passwordInput.getText().toString().trim();
                            String contactStr = contactInput.getText().toString().trim();
                            String address = addressInput.getText().toString().trim();

                            // Validate inputs
                            if (name.isEmpty() || email.isEmpty() || password.isEmpty()
                                    || contactStr.isEmpty() || address.isEmpty()) {
                                showCustomToast("Please fill all fields");
                                return;
                            }

                            // Parse contact number
                            long contact;
                            try {
                                contact = Long.parseLong(contactStr);
                            } catch (NumberFormatException e) {
                                showCustomToast("Invalid contact number");
                                return;
                            }

                            // Create employee and user objects
                            Employee employee = new Employee(empID, branchId, name, email, contact, address, userID, password);
                            User user = new User(userID, name, email, address, contact, "Employee");

                            // Save employee to Firebase
                            db.child("employees").child(empID).setValue(employee)
                                    .addOnSuccessListener(aVoid ->
                                            // Save linked user to Firebase
                                            db.child("users").child(userID).setValue(user)
                                                    .addOnSuccessListener(aVoid2 -> {
                                                        showCustomToast("Employee added successfully!");
                                                        loadBranches(); // Refresh branch/employee list
                                                        dialog.dismiss(); // Close popup
                                                    })
                                                    .addOnFailureListener(e -> { // Handle user save error
                                                        if (e.getMessage() != null && e.getMessage().contains("Permission denied")) {
                                                            showCustomToast("Permission denied while saving user!");
                                                        } else {
                                                            showCustomToast("Failed to add user: " + e.getMessage());
                                                        }
                                                    }))
                                    .addOnFailureListener(e -> { // Handle employee save error
                                        if (e.getMessage() != null && e.getMessage().contains("Permission denied")) {
                                            showCustomToast("Permission denied while saving employee!");
                                        } else {
                                            showCustomToast("Failed to add employee: " + e.getMessage());
                                        }
                                    });
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        hideLoadingDialog(); // Hide loading if cancelled
                        if (error.getMessage().contains("Permission denied")) {
                            showCustomToast("Permission denied while reading users!");
                        } else {
                            showCustomToast("Failed to load users: " + error.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                hideLoadingDialog(); // Hide loading if cancelled
                if (error.getMessage().contains("Permission denied")) {
                    showCustomToast("Permission denied while reading employees!");
                } else {
                    showCustomToast("Failed to load employees: " + error.getMessage());
                }
            }
        });
    }

    private void loadEmployees(String branchId, LinearLayout container, Runnable onLoaded) { // load employees with callback
        db.child("employees").orderByChild("branchID").equalTo(branchId) // query employees with this branch ID
                .addListenerForSingleValueEvent(new ValueEventListener() { // fetch data once
                    @Override
                    public void onDataChange(DataSnapshot snapshot) { // on successful fetch
                        container.removeAllViews(); // clear existing employee views
                        for (DataSnapshot empSnap : snapshot.getChildren()) { // loop through each employee
                            Employee employee = empSnap.getValue(Employee.class); // convert snapshot to Employee object
                            if(employee==null) continue; // skip null entries

                            View empView = getLayoutInflater().inflate(R.layout.employee_row, null); // inflate employee row layout
                            TextView empName = empView.findViewById(R.id.employeeName); // get employee name TextView
                            empName.setText(employee.name); // set employee name

                            ImageButton editEmp = empView.findViewById(R.id.editEmployeeBtn); // edit button
                            ImageButton deleteEmp = empView.findViewById(R.id.deleteEmployeeBtn); // delete button

                            editEmp.setOnClickListener(v -> showEditEmployeePopup(employee)); // edit employee on click
                            deleteEmp.setOnClickListener(v -> showDeleteConfirmation("employee", employee.employeeID)); // delete employee on click

                            container.addView(empView); // add employee row to container
                        }

                        if(onLoaded != null) onLoaded.run(); // notify branch that employees are loaded
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        if(onLoaded != null) onLoaded.run(); // notify on error
                    }
                });
    }

    private void showEditEmployeePopup(Employee employee) { // open popup to edit an existing employee
        View popupView = getLayoutInflater().inflate(R.layout.add_employee_popup, null); // reuse same popup layout
        AlertDialog dialog = new AlertDialog.Builder(this).setView(popupView).create(); // create dialog
        dialog.show(); // display dialog

        Window window = dialog.getWindow(); // get window for customization
        if(window != null){
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.8), WindowManager.LayoutParams.WRAP_CONTENT); // 80% width
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // rounded/transparent bg
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        // --- Popup title ---
        TextView popupTitle = popupView.findViewById(R.id.popupTitleEmployee); // bind title text view
        popupTitle.setText("Edit Employee"); // set popup title

        // --- Input fields ---
        EditText empIdInput = popupView.findViewById(R.id.employeeIDInput); // employee ID field
        EditText userIdInput = popupView.findViewById(R.id.userIDInput); // user ID field
        EditText nameInput = popupView.findViewById(R.id.employeeNameInput); // employee name
        EditText emailInput = popupView.findViewById(R.id.employeeEmailInput); // email
        EditText contactInput = popupView.findViewById(R.id.employeeContactInput); // contact number
        EditText addressInput = popupView.findViewById(R.id.employeeAddressInput); // address
        EditText passwordInput = popupView.findViewById(R.id.employeePasswordInput); // password (optional update)

        // --- Buttons ---
        Button cancelBtn = popupView.findViewById(R.id.cancelEmployeeBtn); // cancel button
        Button updateBtn = popupView.findViewById(R.id.addEmployeeBtn); // reuse add button as update
        cancelBtn.setOnClickListener(v -> dialog.dismiss()); // close popup on cancel

        // --- Pre-fill existing data ---
        empIdInput.setText(employee.employeeID); empIdInput.setEnabled(false); // ID fixed
        userIdInput.setText(employee.userID); userIdInput.setEnabled(false); // user ID fixed
        nameInput.setText(employee.name); // prefill name
        emailInput.setText(employee.email); // prefill email
        contactInput.setText(String.valueOf(employee.contact)); // prefill contact
        addressInput.setText(employee.address); // prefill address
        passwordInput.setText(employee.password); // prefill password
        updateBtn.setText("Update"); // relabel button

        // --- Update button logic ---
        updateBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim(); // read name
            String email = emailInput.getText().toString().trim(); // read email
            String contactStr = contactInput.getText().toString().trim(); // read contact
            String address = addressInput.getText().toString().trim(); // read address
            String newPassword = passwordInput.getText().toString().trim(); // read new password

            // Validation: required fields
            if(name.isEmpty() || email.isEmpty() || contactStr.isEmpty() || address.isEmpty()){
                showCustomToast("Please fill all fields"); // show error
                return;
            }

            // Parse contact number
            long contact;
            try { contact = Long.parseLong(contactStr); } // convert string to long
            catch(NumberFormatException e){ showCustomToast("Invalid contact"); return; }

            // Detect if no changes at all (including password)
            boolean noPasswordChange = newPassword.isEmpty() || newPassword.equals(employee.password); // check password
            if (employee.name.equals(name) &&
                    employee.email.equals(email) &&
                    employee.contact == contact &&
                    employee.address.equals(address) &&
                    noPasswordChange) {
                showCustomToast("No changes detected!"); // nothing to update
                return;
            }

            // --- Update employee object ---
            employee.name = name; // update name
            employee.email = email; // update email
            employee.contact = contact; // update contact
            employee.address = address; // update address
            if(!newPassword.isEmpty()) {
                employee.password = newPassword; // update password only if provided
            }

            // --- Save updates in Firebase ---
            db.child("employees").child(employee.employeeID).setValue(employee) // update employees table
                    .addOnSuccessListener(aVoid -> {
                        // Update linked user record except password
                        db.child("users").child(employee.userID).child("name").setValue(name);
                        db.child("users").child(employee.userID).child("email").setValue(email);
                        db.child("users").child(employee.userID).child("address").setValue(address);
                        db.child("users").child(employee.userID).child("phone").setValue(contact);
                        db.child("users").child(employee.userID).child("role").setValue("Employee");

                        showCustomToast("Employee updated!"); // success message
                        loadBranches(); // refresh UI
                        dialog.dismiss(); // close popup
                    })
                    .addOnFailureListener(e -> showCustomToast("Update failed: "+e.getMessage())); // handle errors
        });
    }

    private void showDeleteConfirmation(String type, String key) {
        View popupView = getLayoutInflater().inflate(R.layout.delete_confirmation_popup, null); // Inflate popup layout
        AlertDialog dialog = new AlertDialog.Builder(this).setView(popupView).create(); // Create dialog with custom view
        dialog.show(); // Show the dialog on screen

        Window window = dialog.getWindow(); // Get dialog window to customize size
        if(window != null){
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.8),
                    WindowManager.LayoutParams.WRAP_CONTENT); // Set width to 80% screen, height wraps content
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Make background transparent
        }

        Button cancelBtn = popupView.findViewById(R.id.cancelDeleteBtn); // Get cancel button from popup
        Button confirmBtn = popupView.findViewById(R.id.confirmDeleteBtn); // Get confirm button from popup
        cancelBtn.setOnClickListener(v -> dialog.dismiss()); // Close dialog if cancel clicked

        confirmBtn.setOnClickListener(v -> { // Handle confirm button click
            if(type.equals("branch")) { // If deleting a branch
                db.child("employees").orderByChild("branchID").equalTo(key) // Query employees under this branch
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if(snapshot.exists()) { // If any employee exists
                                    showCustomToast("Cannot delete: employees exist in this branch!"); // Show warning
                                } else { // No employees exist
                                    db.child("branches").child(key).removeValue() // Delete branch
                                            .addOnSuccessListener(aVoid -> {
                                                showCustomToast("Branch deleted!"); // Show success message
                                                loadBranches(); // Reload branches
                                            })
                                            .addOnFailureListener(e ->
                                                    showCustomToast("Failed to delete branch: " + e.getMessage())); // Show failure message
                                }
                            }
                            @Override public void onCancelled(DatabaseError error) {} // Ignore cancel/error
                        });
            }
            else if(type.equals("employee")) { // If deleting an employee
                db.child("employees").child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String userID = snapshot.child("userID").getValue(String.class); // Get linked user ID
                        db.child("employees").child(key).removeValue() // Delete employee
                                .addOnSuccessListener(aVoid -> {
                                    if(userID != null) db.child("users").child(userID).removeValue(); // Delete linked user if exists
                                    showCustomToast("Employee deleted!"); // Show success message
                                    loadBranches(); // Reload branches
                                })
                                .addOnFailureListener(e ->
                                        showCustomToast("Failed to delete employee: " + e.getMessage())); // Show failure message
                    }
                    @Override public void onCancelled(DatabaseError error) {} // Ignore cancel/error
                });
            }

            dialog.dismiss(); // Close confirmation dialog
        });
    }

    public static class Branch { // Branch data model
        public String branchID, name; // branch ID and name
        public long contact; // branch contact number
        public double latitude, longitude; // branch location coordinates

        public Branch() {} // default constructor for Firebase

        public Branch(String branchID, String name, long contact, double latitude, double longitude){ // constructor with data
            this.branchID = branchID; this.name = name; this.contact = contact; this.latitude = latitude; this.longitude = longitude;
        }
    }

    public static class Employee { // Employee data model
        public String employeeID, branchID, name, email, address, userID, password; // ✅ added password
        public long contact; // employee phone number

        public Employee() {} // default constructor for Firebase

        // ✅ constructor including password
        public Employee(String employeeID, String branchID, String name, String email, long contact, String address, String userID, String password) {
            this.employeeID = employeeID;
            this.branchID = branchID;
            this.name = name;
            this.email = email;
            this.contact = contact;
            this.address = address;
            this.userID = userID;
            this.password = password;
        }

        // ✅ constructor without password (for compatibility if needed)
        public Employee(String employeeID, String branchID, String name, String email, long contact, String address, String userID) {
            this(employeeID, branchID, name, email, contact, address, userID, ""); // default password empty
        }
    }

    public static class User { // User data model for authentication/role
        public String userID, name, email, address, role; // user details
        public long phone; // user phone number

        public User() {} // default constructor for Firebase

        public User(String userID, String name, String email, String address, long phone, String role){ // constructor with data
            this.userID=userID; this.name=name; this.email=email; this.address=address; this.phone=phone; this.role=role;
        }
    }

    private void showCustomToast(String message) { // Method to show custom toast dialog
        LayoutInflater inflater = getLayoutInflater(); // Get inflater to load custom layout
        View layout = inflater.inflate(R.layout.custom_message, null); // Inflate the custom toast layout

        TextView text = layout.findViewById(R.id.toast_message); // Get TextView for message
        ImageView close = layout.findViewById(R.id.toast_close); // Get close button ImageView
        ProgressBar progressBar = layout.findViewById(R.id.toast_progress); // Get ProgressBar

        text.setText(message); // Set the toast message text
        progressBar.setProgress(100); // Initialize progress bar to full

        AlertDialog dialog = new AlertDialog.Builder(this) // Create AlertDialog
                .setView(layout) // Set custom layout
                .create(); // Create dialog instance

        close.setOnClickListener(v -> dialog.dismiss()); // Close dialog on button click

        if (dialog.getWindow() != null) { // Check if window exists
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Make background transparent
            dialog.getWindow().setDimAmount(0f); // Remove dim behind dialog

            WindowManager.LayoutParams params = dialog.getWindow().getAttributes(); // Get window attributes
            params.width = WindowManager.LayoutParams.MATCH_PARENT; // Set width to match parent
            params.height = WindowManager.LayoutParams.WRAP_CONTENT; // Set height to wrap content
            params.gravity = android.view.Gravity.TOP; // Position at top
            params.y = 50; // Offset from top in pixels
            dialog.getWindow().setAttributes(params); // Apply attributes
        }

        dialog.show(); // Show the dialog

        new CountDownTimer(3000, 50) { // Timer for auto-dismiss: 3s, tick every 50ms
            public void onTick(long millisUntilFinished) { // Called on every tick
                int progress = (int) ((millisUntilFinished / 3000.0) * 100); // Calculate progress
                progressBar.setProgress(progress); // Update progress bar
            }
            public void onFinish() { // Called when timer finishes
                if (dialog.isShowing()) dialog.dismiss(); // Dismiss dialog
            }
        }.start(); // Start timer
    }

    private void showLoadingDialog(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) return; // If dialog already showing, do nothing

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null); // Inflate custom loading layout
        TextView textView = view.findViewById(R.id.loadingText); // Get TextView for loading message
        textView.setText(message); // Set the message text

        loadingDialog = new AlertDialog.Builder(this) // Create AlertDialog with custom view
                .setView(view)
                .setCancelable(false) // Prevent user from dismissing by tapping outside
                .create();

        loadingDialog.show(); // Show the dialog

        // 🔹 Force the dialog to match a fixed size (300dp x 180dp)
        if (loadingDialog.getWindow() != null) {
            int width = (int) (300 * getResources().getDisplayMetrics().density);  // Convert 300dp to pixels
            int height = (int) (180 * getResources().getDisplayMetrics().density); // Convert 180dp to pixels
            loadingDialog.getWindow().setLayout(width, height); // Set dialog width and height
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background
        }
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) { // Check if dialog exists and is showing
            loadingDialog.dismiss(); // Dismiss the dialog
        }
    }
}
