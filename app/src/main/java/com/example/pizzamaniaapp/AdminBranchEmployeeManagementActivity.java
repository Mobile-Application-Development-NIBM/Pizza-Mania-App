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


package com.example.pizzamaniaapp; // package: identifies app/project

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

    @Override
    protected void onCreate(Bundle savedInstanceState) { // called when activity is created
        super.onCreate(savedInstanceState); // call parent constructor
        setContentView(R.layout.activity_admin_branch_employee_management); // set layout for this activity

        branchList = findViewById(R.id.branchList); // connect branchList layout from XML
        addBranchBtn = findViewById(R.id.addBranchBtn); // connect addBranchBtn from XML
        db = FirebaseDatabase.getInstance().getReference(); // get Firebase database reference

        loadBranches(); // load all branches from database
        addBranchBtn.setOnClickListener(v -> prepareNewBranchPopup()); // show popup when add button is clicked
    }

    private void prepareNewBranchPopup() { // prepares popup for adding new branch
        db.child("branches").addListenerForSingleValueEvent(new ValueEventListener() { // get all branches once from Firebase
            @Override
            public void onDataChange(DataSnapshot snapshot) { // called when data is loaded
                int nextNumber = (int) snapshot.getChildrenCount() + 1; // calculate next branch number
                String branchID = "b" + String.format("%03d", nextNumber); // create branch ID like b001, b002
                showBranchPopup(null, branchID); // show popup with new branch ID
            }

            @Override
            public void onCancelled(DatabaseError error) { // called if Firebase query fails
                showBranchPopup(null, "b001"); // fallback: show popup with default ID b001
            }
        });
    }

    private void showBranchPopup(Branch existingBranch, String preloadedBranchID) { // show popup to add or edit a branch
        View popupView = getLayoutInflater().inflate(R.layout.add_branch_popup, null); // inflate popup layout
        AlertDialog dialog = new AlertDialog.Builder(this).setView(popupView).create(); // create AlertDialog
        dialog.show(); // show the popup
        Window window = dialog.getWindow(); // get window to set size/background
        if (window != null) {
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.8), // set width 80% of screen
                    WindowManager.LayoutParams.WRAP_CONTENT); // height wraps content
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // transparent background
        }

        // get input fields and buttons from popup
        EditText branchIdInput = popupView.findViewById(R.id.branchIdInput);
        EditText branchNameInput = popupView.findViewById(R.id.branchNameInput);
        EditText contactInput = popupView.findViewById(R.id.contactInput);
        EditText locationInput = popupView.findViewById(R.id.locationInput);
        Button addBtn = popupView.findViewById(R.id.addBtn);
        Button cancelBtn = popupView.findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(v -> dialog.dismiss()); // close popup on cancel

        // if editing existing branch, populate fields
        if (existingBranch != null) {
            branchIdInput.setText(existingBranch.branchID); branchIdInput.setEnabled(false); // ID cannot be edited
            branchNameInput.setText(existingBranch.name);
            contactInput.setText(String.valueOf(existingBranch.contact));
            locationInput.setText(existingBranch.latitude + ", " + existingBranch.longitude);
        } else if (preloadedBranchID != null) { // if new branch, set preloaded ID
            branchIdInput.setText(preloadedBranchID);
            branchIdInput.setEnabled(false);
        }

        addBtn.setOnClickListener(v -> { // handle save button click
            String id = branchIdInput.getText().toString().trim(); // get input values
            String name = branchNameInput.getText().toString().trim();
            String contactStr = contactInput.getText().toString().trim();
            String locationStr = locationInput.getText().toString().trim();

            // check if any field is empty
            if (id.isEmpty() || name.isEmpty() || contactStr.isEmpty() || locationStr.isEmpty()) {
                showCustomToast("Please fill all fields");
                return;
            }

            // parse contact number
            long contact;
            try { contact = Long.parseLong(contactStr); }
            catch (NumberFormatException e) { showCustomToast("Invalid contact number"); return; }

            // parse location values
            String[] locParts = locationStr.split(",");
            if (locParts.length != 2) { showCustomToast("Enter location as latitude,longitude"); return; }

            double latitude, longitude;
            try { latitude = Double.parseDouble(locParts[0].trim()); longitude = Double.parseDouble(locParts[1].trim()); }
            catch (NumberFormatException e) { showCustomToast("Invalid location values"); return; }

            Branch branch = new Branch(id, name, contact, latitude, longitude); // create branch object
            db.child("branches").child(id).setValue(branch) // save to Firebase
                    .addOnSuccessListener(aVoid -> { showCustomToast("Branch saved!"); loadBranches(); dialog.dismiss(); }) // success
                    .addOnFailureListener(e -> showCustomToast("Failed: " + e.getMessage())); // failure
        });
    }

    private void loadBranches() { // load all branches from Firebase and display
        branchList.removeAllViews(); // clear existing branch views
        db.child("branches").addListenerForSingleValueEvent(new ValueEventListener() { // read branches once
            @Override
            public void onDataChange(DataSnapshot snapshot) { // called when data is fetched
                List<Branch> branchListTemp = new ArrayList<>(); // temporary list to store branches
                for (DataSnapshot branchSnap : snapshot.getChildren()) { // loop through each branch
                    Branch branch = branchSnap.getValue(Branch.class); // convert snapshot to Branch object
                    if (branch != null) branchListTemp.add(branch); // add to temp list if not null
                }

                Collections.sort(branchListTemp, Comparator.comparingInt(a -> Integer.parseInt(a.branchID.substring(1)))); // sort branches by ID number

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

                    loadEmployees(branch.branchID, employeeContainer); // load employees for this branch
                    branchList.addView(branchView); // add branch row to main layout
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {} // handle database errors (empty)
        });
    }

    private void showAddEmployeePopup(String branchId) { // open popup to add new employee
        View popupView = getLayoutInflater().inflate(R.layout.add_employee_popup, null); // inflate employee popup layout
        AlertDialog dialog = new AlertDialog.Builder(this).setView(popupView).create(); // create dialog with popup view
        dialog.show(); // show popup
        Window window = dialog.getWindow(); // get window to customize size
        if (window != null) { // if window exists
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.8), WindowManager.LayoutParams.WRAP_CONTENT); // set width 80% of screen
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // transparent background
        }

        EditText empIdInput = popupView.findViewById(R.id.employeeIDInput); // input for employee ID
        EditText userIdInput = popupView.findViewById(R.id.userIDInput); // input for user ID
        EditText nameInput = popupView.findViewById(R.id.employeeNameInput); // input for name
        EditText emailInput = popupView.findViewById(R.id.employeeEmailInput); // input for email
        EditText contactInput = popupView.findViewById(R.id.employeeContactInput); // input for contact number
        EditText addressInput = popupView.findViewById(R.id.employeeAddressInput); // input for address

        Button cancelBtn = popupView.findViewById(R.id.cancelEmployeeBtn); // cancel button
        Button addBtn = popupView.findViewById(R.id.addEmployeeBtn); // add/save button
        cancelBtn.setOnClickListener(v -> dialog.dismiss()); // close popup on cancel

        db.child("employees").addListenerForSingleValueEvent(new ValueEventListener() { // fetch all employees once
            @Override
            public void onDataChange(DataSnapshot snapshot) { // on fetch success
                int nextEmpNum = (int) snapshot.getChildrenCount() + 1; // next employee number
                empIdInput.setText("e" + String.format("%03d", nextEmpNum)); // auto-generate employee ID
            }
            @Override public void onCancelled(DatabaseError error) {} // handle error (empty)
        });

        db.child("users").addListenerForSingleValueEvent(new ValueEventListener() { // fetch all users once
            @Override
            public void onDataChange(DataSnapshot snapshot) { // on fetch success
                int nextUserNum = (int) snapshot.getChildrenCount() + 1; // next user number
                userIdInput.setText("u" + String.format("%03d", nextUserNum)); // auto-generate user ID
            }
            @Override public void onCancelled(DatabaseError error) {} // handle error (empty)
        });

        addBtn.setOnClickListener(v -> { // save employee button clicked
            String empID = empIdInput.getText().toString().trim(); // get employee ID
            String userID = userIdInput.getText().toString().trim(); // get user ID
            String name = nameInput.getText().toString().trim(); // get name
            String email = emailInput.getText().toString().trim(); // get email
            String contactStr = contactInput.getText().toString().trim(); // get contact string
            String address = addressInput.getText().toString().trim(); // get address

            if(empID.isEmpty() || userID.isEmpty() || name.isEmpty() || email.isEmpty() || contactStr.isEmpty() || address.isEmpty()){ // check empty fields
                showCustomToast("Please fill all fields"); // show warning
                return; // stop execution
            }

            long contact;
            try { contact = Long.parseLong(contactStr); } // parse contact to number
            catch(NumberFormatException e){ showCustomToast("Invalid contact"); return; } // handle invalid number

            Employee employee = new Employee(empID, branchId, name, email, contact, address, userID); // create Employee object
            User user = new User(userID, name, email, address, contact, "Employee"); // create User object

            db.child("employees").child(empID).setValue(employee) // save employee to Firebase
                    .addOnSuccessListener(aVoid -> db.child("users").child(userID).setValue(user) // save user on success
                            .addOnSuccessListener(aVoid2 -> { showCustomToast("Employee added!"); loadBranches(); dialog.dismiss(); }) // success message
                            .addOnFailureListener(e -> showCustomToast("Failed to add user: "+e.getMessage())) // handle user save failure
                    )
                    .addOnFailureListener(e -> showCustomToast("Failed to add employee: "+e.getMessage())); // handle employee save failure
        });
    }

    private void loadEmployees(String branchId, LinearLayout container) { // load employees of a branch into container
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
                    }
                    @Override public void onCancelled(DatabaseError error) {} // handle fetch error (empty)
                });
    }

    private void showEditEmployeePopup(Employee employee) { // open popup to edit an existing employee
        View popupView = getLayoutInflater().inflate(R.layout.add_employee_popup, null); // inflate employee popup layout
        AlertDialog dialog = new AlertDialog.Builder(this).setView(popupView).create(); // create dialog with popup view
        dialog.show(); // show dialog
        Window window = dialog.getWindow(); // get dialog window
        if(window!=null){
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.8), WindowManager.LayoutParams.WRAP_CONTENT); // set width to 80% of screen
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // transparent background
        }

        EditText empIdInput = popupView.findViewById(R.id.employeeIDInput); // employee ID input field
        EditText userIdInput = popupView.findViewById(R.id.userIDInput); // user ID input field
        EditText nameInput = popupView.findViewById(R.id.employeeNameInput); // name input
        EditText emailInput = popupView.findViewById(R.id.employeeEmailInput); // email input
        EditText contactInput = popupView.findViewById(R.id.employeeContactInput); // contact input
        EditText addressInput = popupView.findViewById(R.id.employeeAddressInput); // address input

        Button cancelBtn = popupView.findViewById(R.id.cancelEmployeeBtn); // cancel button
        Button addBtn = popupView.findViewById(R.id.addEmployeeBtn); // update button (originally add)
        cancelBtn.setOnClickListener(v -> dialog.dismiss()); // dismiss popup on cancel

        empIdInput.setText(employee.employeeID); empIdInput.setEnabled(false); // set employee ID and disable editing
        userIdInput.setText(employee.userID); userIdInput.setEnabled(false); // set user ID and disable editing
        nameInput.setText(employee.name); // set current name
        emailInput.setText(employee.email); // set current email
        contactInput.setText(String.valueOf(employee.contact)); // set current contact
        addressInput.setText(employee.address); // set current address
        addBtn.setText("Update"); // change button text to Update

        addBtn.setOnClickListener(v -> { // update employee on click
            String name = nameInput.getText().toString().trim(); // get trimmed name
            String email = emailInput.getText().toString().trim(); // get trimmed email
            String contactStr = contactInput.getText().toString().trim(); // get trimmed contact
            String address = addressInput.getText().toString().trim(); // get trimmed address

            if(name.isEmpty() || email.isEmpty() || contactStr.isEmpty() || address.isEmpty()){ // check empty fields
                showCustomToast("Please fill all fields"); // show warning
                return;
            }

            long contact; // variable for contact number
            try { contact = Long.parseLong(contactStr); } // parse contact to long
            catch(NumberFormatException e){ showCustomToast("Invalid contact"); return; } // handle invalid number

            employee.name = name; // update employee name
            employee.email = email; // update email
            employee.contact = contact; // update contact
            employee.address = address; // update address

            db.child("employees").child(employee.employeeID).setValue(employee) // save updated employee to DB
                    .addOnSuccessListener(aVoid -> {
                        db.child("users").child(employee.userID).child("name").setValue(name); // update user name
                        db.child("users").child(employee.userID).child("email").setValue(email); // update user email
                        db.child("users").child(employee.userID).child("address").setValue(address); // update user address
                        db.child("users").child(employee.userID).child("phone").setValue(contact); // update user phone
                        showCustomToast("Employee updated!"); // success message
                        loadBranches(); dialog.dismiss(); // reload branches and dismiss dialog
                    })
                    .addOnFailureListener(e -> showCustomToast("Update failed: "+e.getMessage())); // failure message
        });
    }

    private void showDeleteConfirmation(String type, String key) { // open popup to confirm deletion
        View popupView = getLayoutInflater().inflate(R.layout.delete_confirmation_popup, null); // inflate delete confirmation layout
        AlertDialog dialog = new AlertDialog.Builder(this).setView(popupView).create(); // create dialog with popup view
        dialog.show(); // show dialog
        Window window = dialog.getWindow(); // get dialog window
        if(window!=null){
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.8), WindowManager.LayoutParams.WRAP_CONTENT); // set width 80% of screen
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // transparent background
        }

        Button cancelBtn = popupView.findViewById(R.id.cancelDeleteBtn); // cancel button
        Button confirmBtn = popupView.findViewById(R.id.confirmDeleteBtn); // confirm button
        cancelBtn.setOnClickListener(v -> dialog.dismiss()); // dismiss dialog on cancel

        confirmBtn.setOnClickListener(v -> { // handle confirm click
            if(type.equals("branch")) { // delete branch
                db.child("branches").child(key).removeValue()
                        .addOnSuccessListener(aVoid -> {
                            showCustomToast("Branch deleted!"); // show success message
                            loadBranches(); // reload branches
                        })
                        .addOnFailureListener(e -> showCustomToast("Failed to delete branch: " + e.getMessage())); // failure
            }
            else if(type.equals("employee")) { // delete employee
                db.child("employees").child(key).addListenerForSingleValueEvent(new ValueEventListener() { // get employee data
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String userID = snapshot.child("userID").getValue(String.class); // get linked user ID
                        db.child("employees").child(key).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    if(userID != null) db.child("users").child(userID).removeValue(); // delete linked user
                                    showCustomToast("Employee deleted!"); // show success message
                                    loadBranches(); // reload branches
                                })
                                .addOnFailureListener(e -> showCustomToast("Failed to delete employee: " + e.getMessage())); // failure
                    }
                    @Override public void onCancelled(DatabaseError error) {} // ignore cancel error
                });
            }
            dialog.dismiss(); // dismiss dialog after delete
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
        public String employeeID, branchID, name, email, address, userID; // employee details
        public long contact; // employee phone number

        public Employee() {} // default constructor for Firebase

        public Employee(String employeeID, String branchID, String name, String email, long contact, String address, String userID){ // constructor with data
            this.employeeID = employeeID; this.branchID = branchID; this.name = name; this.email=email; this.contact=contact; this.address=address; this.userID=userID;
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

}
