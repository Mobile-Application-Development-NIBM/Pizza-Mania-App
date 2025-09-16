package com.example.pizzamaniaapp;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EmployeeHomeActivity extends AppCompatActivity {

    private RecyclerView recyclerOrders;
    private List<Order> orderList;
    private OrderAdapter adapter;
    private DatabaseReference ordersRef;
    private ValueEventListener ordersListener;
    private Handler handler = new Handler();
    private AlertDialog loadingDialog; // ⏳ Loading dialog
    private String currentBranchID; // 🌍 Moved to a class variable
    private Runnable pendingRefreshRunnable; // 🏃 Holds the pending refresh task

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_home);

        recyclerOrders = findViewById(R.id.recyclerOrders);
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));

        orderList = new ArrayList<>();
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        adapter = new OrderAdapter(this, orderList, (order, newStatus) -> {
            updateOrderStatus(order, newStatus);
        });
        recyclerOrders.setAdapter(adapter);

        // Get the branch ID once
        currentBranchID = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getString("branchID", null);

        // Show loading when opening
        showLoadingDialog("Loading orders...");
        loadOrders();
    }

    // Create a listener that can be attached/removed
    private void createOrdersListener() {
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Order order = dataSnapshot.getValue(Order.class);
                    if (order != null) {
                        order.setOrderID(dataSnapshot.getKey());
                        if (currentBranchID != null && currentBranchID.equals(order.getBranchID()) &&
                                ("confirm order".equalsIgnoreCase(order.getStatus()) ||
                                        "Preparing".equalsIgnoreCase(order.getStatus()) ||
                                        "order pending".equalsIgnoreCase(order.getStatus()))) {
                            orderList.add(order);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                hideLoadingDialog();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showCustomToast("Failed to load orders");
                hideLoadingDialog();
            }
        };
    }

    private void loadOrders() {
        if (currentBranchID == null || currentBranchID.isEmpty()) {
            showCustomToast("Branch not set! Cannot load orders.");
            hideLoadingDialog();
            return;
        }

        // Remove previous listener to prevent duplication
        if (ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }

        // Add the listener back to start listening for changes
        createOrdersListener();
        ordersRef.addValueEventListener(ordersListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Always remove the listener and pending callbacks to prevent memory leaks
        if (ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }
        if (pendingRefreshRunnable != null) {
            handler.removeCallbacks(pendingRefreshRunnable);
        }
    }

    private void updateOrderStatus(Order order, String newStatus) {
        if (order == null) return;

        // 🛑 Cancel any existing pending refresh before proceeding
        if (pendingRefreshRunnable != null) {
            handler.removeCallbacks(pendingRefreshRunnable);
            pendingRefreshRunnable = null;
        }

        // Temporarily remove the listener before the update
        if (ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }

        ordersRef.child(order.getOrderID())
                .child("status")
                .setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    if ("Delivery Pending".equalsIgnoreCase(newStatus)) {
                        // 1. Show custom message
                        showCustomToast("Order will disappear in 15 seconds...");

                        // 2. Define the runnable and post it
                        pendingRefreshRunnable = () -> {
                            showLoadingDialog("Refreshing orders...");
                            loadOrders();
                            pendingRefreshRunnable = null; // Clear the runnable after it's executed
                        };
                        handler.postDelayed(pendingRefreshRunnable, 15000); // ⏳ 15 seconds delay
                    } else {
                        // For all other status updates, re-attach the listener immediately
                        showCustomToast("Status Updated Successfully");
                        loadOrders();
                    }
                })
                .addOnFailureListener(e -> {
                    showCustomToast("Update Failed: " + e.getMessage());
                    // If update fails, re-attach the listener
                    loadOrders();
                });
    }

    // ---------------- LOADING & TOAST ----------------

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

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }

    private void showCustomToast(String message) {
        View layout = LayoutInflater.from(this).inflate(R.layout.custom_message, null);
        TextView toastMessage = layout.findViewById(R.id.toast_message);
        ImageView close = layout.findViewById(R.id.toast_close);
        ProgressBar progressBar = layout.findViewById(R.id.toast_progress);

        toastMessage.setText(message);
        progressBar.setProgress(100);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(layout).create();
        close.setOnClickListener(v -> dialog.dismiss());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0f);
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.TOP;
            params.y = 50;
            dialog.getWindow().setAttributes(params);
        }
        dialog.show();

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