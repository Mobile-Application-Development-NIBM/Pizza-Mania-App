package com.example.pizzamaniaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EmployeeHomeActivity extends AppCompatActivity {

    private RecyclerView recyclerOrders;        // RecyclerView to show the list of orders
    private List<Order> orderList;              // Stores all orders for this branch
    private OrderAdapter adapter;               // Adapter to bind data to RecyclerView
    private DatabaseReference ordersRef;        // Firebase reference to "orders" node
    private ValueEventListener ordersListener;  // Listener to update UI when data changes
    private Handler handler = new Handler();    // Used for delayed actions
    private AlertDialog loadingDialog;          // Custom loading dialog
    private String currentBranchID;             // Current logged-in employee’s branch ID
    private Runnable pendingRefreshRunnable;    // Holds delayed refresh task

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_home);

        recyclerOrders = findViewById(R.id.recyclerOrders);  // Link RecyclerView from layout
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this)); // Vertical list

        orderList = new ArrayList<>();
        ordersRef = FirebaseDatabase.getInstance().getReference("orders"); // Connect to Firebase node "orders"

        // Set up adapter with order list and a callback for updating status
        adapter = new OrderAdapter(this, orderList, (order, newStatus) -> {
            updateOrderStatus(order, newStatus);  // Call update method when status changes
        });
        recyclerOrders.setAdapter(adapter);

        // Get branch ID saved in SharedPreferences
        currentBranchID = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                .getString("branchID", null);

        // Show loading dialog initially
        showLoadingDialog("Loading orders...");
        loadOrders();  // Start loading orders from Firebase

        // -------------------- Order History --------------------
        ImageButton orderButton = findViewById(R.id.orderButton);
        orderButton.setOnClickListener(v -> {
            Intent intent = new Intent(EmployeeHomeActivity.this, EmployeeOrderActivity.class);
            startActivity(intent);
        });


        // -------------------- Log Out Button --------------------
        ImageButton LogoutButton = findViewById(R.id.LogoutButton);
        LogoutButton.setOnClickListener(v -> {
            // 1. Try sign out from FirebaseAuth (only works if current user is FirebaseAuth user)
            FirebaseAuth.getInstance().signOut();

            // 2. Clear session data in SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            // 3. Redirect user back to LoginActivity
            Intent intent = new Intent(EmployeeHomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // Create Firebase listener to load and filter orders
    private void createOrdersListener() {
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();  // Clear old list before refreshing
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Order order = dataSnapshot.getValue(Order.class);
                    if (order != null) {
                        order.setOrderId(dataSnapshot.getKey());  // Attach Firebase key as ID
                        // Show only orders for this branch and only in certain statuses
                        if (currentBranchID != null && currentBranchID.equals(order.getBranchID()) &&
                                ("confirm order".equalsIgnoreCase(order.getStatus()) ||
                                        "Preparing".equalsIgnoreCase(order.getStatus()) ||
                                        "order pending".equalsIgnoreCase(order.getStatus()))) {
                            orderList.add(order);
                        }
                    }
                }
                adapter.notifyDataSetChanged(); // Update RecyclerView
                hideLoadingDialog();            // Hide loading after done
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showCustomToast("Failed to load orders"); // Error handling
                hideLoadingDialog();
            }
        };
    }

    // Attach listener to Firebase and start listening
    private void loadOrders() {
        if (currentBranchID == null || currentBranchID.isEmpty()) {
            showCustomToast("Branch not set! Cannot load orders.");
            hideLoadingDialog();
            return;
        }

        // Remove old listener if already attached
        if (ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }

        // Create and attach new listener
        createOrdersListener();
        ordersRef.addValueEventListener(ordersListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firebase listener and pending tasks to avoid memory leaks
        if (ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }
        if (pendingRefreshRunnable != null) {
            handler.removeCallbacks(pendingRefreshRunnable);
        }
    }

    // Update order status in Firebase
    private void updateOrderStatus(Order order, String newStatus) {
        if (order == null) return;

        // Cancel any delayed refresh if exists
        if (pendingRefreshRunnable != null) {
            handler.removeCallbacks(pendingRefreshRunnable);
            pendingRefreshRunnable = null;
        }

        // Temporarily remove listener to prevent duplicate events
        if (ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }

        // Update the status field in Firebase
        ordersRef.child(order.getOrderId())
                .child("status")
                .setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    if ("Delivery Pending".equalsIgnoreCase(newStatus)) {
                        // If status = Delivery Pending, show message and refresh after 15s
                        showCustomToast("Order will disappear in 15 seconds...");

                        pendingRefreshRunnable = () -> {
                            showLoadingDialog("Refreshing orders...");
                            loadOrders();
                            pendingRefreshRunnable = null; // Clear task
                        };
                        handler.postDelayed(pendingRefreshRunnable, 15000); // Delay refresh
                    } else {
                        // For other statuses, refresh immediately
                        showCustomToast("Status Updated Successfully");
                        loadOrders();
                    }
                })
                .addOnFailureListener(e -> {
                    showCustomToast("Update Failed: " + e.getMessage());
                    loadOrders(); // Re-attach listener if failed
                });
    }

    // LOADING & TOAST

    // Show custom loading dialog
    private void showLoadingDialog(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
        TextView textView = view.findViewById(R.id.loadingText);
        textView.setText(message);
        loadingDialog = new AlertDialog.Builder(this).setView(view).setCancelable(false).create();
        loadingDialog.show();
        if (loadingDialog.getWindow() != null) {
            int width = (int) (300 * getResources().getDisplayMetrics().density);
            int height = (int) (180 * getResources().getDisplayMetrics().density);
            loadingDialog.getWindow().setLayout(width, height);
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    // Hide loading dialog
    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }

    // Show custom toast-like message at top with progress bar
    private void showCustomToast(String message) {
        View layout = LayoutInflater.from(this).inflate(R.layout.custom_message, null);
        TextView toastMessage = layout.findViewById(R.id.toast_message);
        ImageView close = layout.findViewById(R.id.toast_close);
        ProgressBar progressBar = layout.findViewById(R.id.toast_progress);

        toastMessage.setText(message);
        progressBar.setProgress(100);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(layout).create();
        close.setOnClickListener(v -> dialog.dismiss()); // Close button
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0f); // No background dim
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.TOP; // Appear at top of screen
            params.y = 50;
            dialog.getWindow().setAttributes(params);
        }
        dialog.show();

        // Auto dismiss after 3 seconds with progress animation
        new CountDownTimer(3000, 50) {
            public void onTick(long millisUntilFinished) {
                int progress = (int) Math.max(0, Math.round((millisUntilFinished / 3000.0) * 100));
                progressBar.setProgress(progress);
            }
            public void onFinish() {
                if (dialog.isShowing()) dialog.dismiss();
            }
        }.start();
    }
}
