package com.example.pizzamaniaapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeliverymanHomeActivity extends AppCompatActivity {

    private static final String TAG = "DeliverymanHome";
    private DatabaseReference db;

    private RecyclerView pendingRecyclerView, acceptedRecyclerView;
    private DeliveryAdapter pendingAdapter, acceptedAdapter;

    private ImageButton homeButton, deliveryHistoryButton, profileButton;

    private TextView pendingTitle, acceptedTitle;
    private View noDeliveriesLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity started ✅");
        setContentView(R.layout.activity_deliveryman_home);

        View mainLayout = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseDatabase.getInstance().getReference();

        // RecyclerViews for Pending & Accepted
        pendingRecyclerView = findViewById(R.id.pendingRecyclerView);
        acceptedRecyclerView = findViewById(R.id.acceptedRecyclerView);

        noDeliveriesLayout = findViewById(R.id.noDeliveriesLayout);

        pendingRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        acceptedRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Adapters
        pendingAdapter = new DeliveryAdapter(new ArrayList<>());
        acceptedAdapter = new DeliveryAdapter(new ArrayList<>());

        pendingRecyclerView.setAdapter(pendingAdapter);
        acceptedRecyclerView.setAdapter(acceptedAdapter);

        // Buttons
        homeButton = findViewById(R.id.homeButton);
        deliveryHistoryButton = findViewById(R.id.deliveryHistoryButton);
        profileButton = findViewById(R.id.profileButton);

        homeButton.setOnClickListener(v -> loadDeliveries());
        deliveryHistoryButton.setOnClickListener(v ->
                startActivity(new Intent(this, DeliverymanDeliveryHistoryActivity.class)));
        profileButton.setOnClickListener(v ->
                startActivity(new Intent(this, AccountActivity.class)));

        pendingTitle = findViewById(R.id.pendingTitle);
        acceptedTitle = findViewById(R.id.acceptedTitle);

        // Hide accepted section initially
        acceptedTitle.setVisibility(View.GONE);
        acceptedRecyclerView.setVisibility(View.GONE);

        setupAdapters(); // Setup click listeners
        loadDeliveries(); // Load both lists
    }

    // Returns the branch ID of the currently logged-in deliveryman
    private String getCurrentDeliverymanBranch() {
        String branchID = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                .getString("branchID", null);

        if (branchID == null || branchID.isEmpty()) {
            showCustomToast("❌ Branch not set! Cannot load orders.");
            Log.e(TAG, "Current deliveryman's branch is not set.");
            return null; // indicate error
        }

        return branchID;
    }

    // Returns the deliveryman ID of the currently logged-in deliveryman
    private String getCurrentDeliverymanID() {
        String delID = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                .getString("userID", null);

        if (delID == null || delID.isEmpty()) {
            showCustomToast("❌ Deliveryman ID not set! Cannot accept orders.");
            Log.e(TAG, "Current deliveryman's ID is not set.");
            return null;
        }

        Log.d(TAG, "Current deliveryman ID: " + delID);
        return delID;
    }


    private void loadDeliveries() {
        showLoadingDialog("Loading deliveries...");

        acceptedTitle.setVisibility(View.GONE);
        acceptedRecyclerView.setVisibility(View.GONE);

        String currentDeliveryman = getCurrentDeliverymanID();
        String currentBranch = getCurrentDeliverymanBranch();
        if (currentBranch == null) return; // stop loading if branch not set

        db.child("orders").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hideLoadingDialog();

                List<DeliveryItem> pendingList = new ArrayList<>();
                List<DeliveryItem> acceptedList = new ArrayList<>();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    DeliveryItem order = snap.getValue(DeliveryItem.class);
                    if (order == null) continue;

                    if ("Completed".equalsIgnoreCase(order.status)) continue; // skip completed

                    // Only orders from same branch
                    if (!currentBranch.equals(order.branchID)) continue;

                    if ("Delivering".equalsIgnoreCase(order.status) && currentDeliveryman.equals(order.assignedDeliverymanID)) {
                        acceptedList.add(order); // accepted by current deliveryman
                    } else if ("Delivery Pending".equalsIgnoreCase(order.status) && (order.assignedDeliverymanID == null || order.assignedDeliverymanID.isEmpty())) {
                        pendingList.add(order);
                    }
                }

                // Update adapters
                pendingAdapter.updateList(pendingList);
                acceptedAdapter.updateList(acceptedList);

                boolean hasAccepted = !acceptedList.isEmpty();
                boolean hasPending = !pendingList.isEmpty();

                boolean showPlaceholder = !hasAccepted && !hasPending;
                noDeliveriesLayout.setVisibility(showPlaceholder ? View.VISIBLE : View.GONE);

                // hide the lists when showing placeholder
                acceptedTitle.setVisibility(hasAccepted ? View.VISIBLE : View.GONE);
                acceptedRecyclerView.setVisibility(hasAccepted ? View.VISIBLE : View.GONE);
                pendingTitle.setVisibility(hasPending ? View.VISIBLE : View.GONE);
                pendingRecyclerView.setVisibility(hasPending ? View.VISIBLE : View.GONE);


                pendingRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                        if (layoutManager != null) {
                            int lastVisible = layoutManager.findLastCompletelyVisibleItemPosition();
                            if (lastVisible == pendingAdapter.getItemCount() - 1) {
                                if (!acceptedList.isEmpty()) {
                                    acceptedTitle.setVisibility(View.VISIBLE);
                                    acceptedRecyclerView.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoadingDialog();
            }
        });
    }

    private void setupAdapters() {
        // Pending adapter: Accept order
        pendingAdapter.setOnAcceptClickListener(position -> {
            DeliveryItem order = pendingAdapter.getItem(position);
            Log.d(TAG, "Accept clicked for order: " + (order != null ? order.orderID : "null"));

            if (acceptedAdapter.getItemCount() > 0) {
                showCustomToast("⚠ You can accept only 1 order at a time");
                Log.d(TAG, "Cannot accept: another order is already active. Returning.");
                return;
            }

            showLoadingDialog("Accepting delivery...");
            Map<String, Object> updates = new HashMap<>();
            updates.put("assignedDeliverymanID", getCurrentDeliverymanID());
            updates.put("status", "Delivering");
            Log.d(TAG, "Attempting to update order status to 'Delivering' for order: " + order.orderID);

            db.child("orders").child(order.orderID).updateChildren(updates)
                    .addOnSuccessListener(a -> {
                        hideLoadingDialog();
                        Log.d(TAG, "Order successfully accepted. Status updated to 'Delivering' for order: " + order.orderID);
                        loadDeliveries();
                        Log.d(TAG, "loadDeliveries() called after successful acceptance.");
                    })
                    .addOnFailureListener(e -> {
                        hideLoadingDialog();
                        showCustomToast("Failed to accept delivery");
                        Log.e(TAG, "Failed to accept order: " + order.orderID, e);
                    });
        });

        pendingAdapter.setOnViewMapClickListener(position -> {
            DeliveryItem order = pendingAdapter.getItem(position);
            Log.d(TAG, "View map clicked for order: " + (order != null ? order.orderID : "null"));
            if (order != null) {
                openMap(order.customerLat, order.customerLng);
            }
        });

        // Accepted adapter: Complete/revert order
        acceptedAdapter.setOnCompleteClickListener(position -> {
            DeliveryItem order = acceptedAdapter.getItem(position);
            if (order == null) {
                Log.w(TAG, "Complete clicked but order item is null. Aborting.");
                return;
            }

            Log.d(TAG, "Complete clicked for order: " + order.orderID + " | paymentMethod: " + order.paymentMethod + " | paymentStatus: " + order.paymentStatus);

            // --- ADDED LOGGING HERE ---
            Log.d(TAG, "Order details from adapter: orderID=" + order.orderID +
                    ", paymentMethod=" + order.paymentMethod +
                    ", paymentStatus=" + order.paymentStatus);
            // --------------------------

            db.child("orders").child(order.orderID).child("status")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String currentStatus = snapshot.getValue(String.class);
                            Log.d(TAG, "Current status from DB: " + currentStatus);
                            if (currentStatus == null) {
                                Log.w(TAG, "Current status from DB is null. Aborting.");
                                return;
                            }

                            Map<String, Object> updates = new HashMap<>();

                            if ("Completed".equalsIgnoreCase(currentStatus)) {
                                Log.d(TAG, "Order status is already 'Completed'. Reverting to 'Delivering' immediately.");
                                updates.put("status", "Delivering");
                                db.child("orders").child(order.orderID).updateChildren(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            showCustomToast("↩ Delivery reverted to Delivering");
                                            Log.d(TAG, "Order reverted to 'Delivering' successfully: " + order.orderID);
                                            loadDeliveries();
                                            Log.d(TAG, "loadDeliveries() called after reverting status.");
                                        })
                                        .addOnFailureListener(e -> {
                                            showCustomToast("Failed to revert delivery");
                                            Log.e(TAG, "Failed to revert order: " + order.orderID, e);
                                        });
                            } else {
                                Log.d(TAG, "Order status is not 'Completed'. Marking as 'Completed'.");
                                updates.put("status", "Completed");
                                db.child("orders").child(order.orderID).updateChildren(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            showCustomToast("✓ Delivery Completed");
                                            Log.d(TAG, "Order status updated to 'Completed' successfully: " + order.orderID);

                                            // Start 15-second timer
                                            Log.d(TAG, "Starting 15-second timer for order: " + order.orderID);
                                            new CountDownTimer(15000, 15000) {
                                                @Override
                                                public void onTick(long millisUntilFinished) {
                                                    // Not needed, but keeping for clarity
                                                }

                                                @Override
                                                public void onFinish() {
                                                    Log.d(TAG, "15-second timer finished for order: " + order.orderID);
                                                    // Read status once before updating payment
                                                    db.child("orders").child(order.orderID).child("status")
                                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot snap) {
                                                                    String finalStatus = snap.getValue(String.class);
                                                                    Log.d(TAG, "Final status read from DB before payment update: " + finalStatus);
                                                                    if (!"Completed".equalsIgnoreCase(finalStatus)) {
                                                                        Log.d(TAG, "Order status changed from 'Completed', skipping payment update logic.");
                                                                        return;
                                                                    }

                                                                    long deliveredTime = System.currentTimeMillis();
                                                                    Map<String, Object> finalUpdates = new HashMap<>();
                                                                    finalUpdates.put("deliveredTimestamp", deliveredTime);
                                                                    Log.d(TAG, "Attempting to update deliveredTimestamp: " + deliveredTime);

                                                                    db.child("orders").child(order.orderID).updateChildren(finalUpdates)
                                                                            .addOnSuccessListener(aVoid1 -> Log.d(TAG, "Delivered timestamp updated successfully."))
                                                                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update delivered timestamp", e));

                                                                    // --- MODIFIED CONDITION HERE ---
                                                                    Log.d(TAG, "New payment condition check: Is paymentStatus 'Pending'? " + "Pending".equalsIgnoreCase(order.paymentStatus));
                                                                    if ("Pending".equalsIgnoreCase(order.paymentStatus)) {
                                                                        // -----------------------------
                                                                        Log.d(TAG, "Payment conditions met. Attempting to update paymentStatus to 'Paid' and create new payment record.");

                                                                        // Update payment status
                                                                        db.child("orders").child(order.orderID)
                                                                                .child("paymentStatus").setValue("Paid")
                                                                                .addOnSuccessListener(aVoid12 -> Log.d(TAG, "Payment status updated to 'Paid' successfully."))
                                                                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update payment status to 'Paid'", e));

                                                                        // Add new payment record
                                                                        Map<String, Object> paymentData = new HashMap<>();
                                                                        paymentData.put("amount", order.totalPrice);
                                                                        paymentData.put("customerID", order.customerID);
                                                                        paymentData.put("customerName", order.customerName);
                                                                        paymentData.put("paymentID", order.orderID);
                                                                        paymentData.put("paymentMethod", "Cash");
                                                                        paymentData.put("timestamp", deliveredTime);
                                                                        Log.d(TAG, "Payment record data to be added: " + paymentData.toString());

                                                                        db.child("payments").child(order.orderID).setValue(paymentData)
                                                                                .addOnSuccessListener(aVoid13 -> Log.d(TAG, "New payment record added successfully for order: " + order.orderID))
                                                                                .addOnFailureListener(e -> Log.e(TAG, "Failed to add new payment record for order: " + order.orderID, e));
                                                                    } else {
                                                                        Log.d(TAG, "Payment conditions not met. Skipping payment update and record creation.");
                                                                    }

                                                                    showLoadingDialog("Refreshing orders...");
                                                                    loadDeliveries();
                                                                    Log.d(TAG, "loadDeliveries() called after payment/timestamp updates.");
                                                                }

                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError error) {
                                                                    Log.e(TAG, "Failed to read order status before payment update: " + error.getMessage(), error.toException());
                                                                }
                                                            });
                                                }
                                            }.start();
                                        })
                                        .addOnFailureListener(e -> {
                                            showCustomToast("Failed to complete delivery");
                                            Log.e(TAG, "Failed to mark order Completed: " + order.orderID, e);
                                        });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Failed to read current order status: " + error.getMessage(), error.toException());
                        }
                    });
        });

        acceptedAdapter.setOnViewMapClickListener(position -> {
            DeliveryItem order = acceptedAdapter.getItem(position);
            Log.d(TAG, "View map clicked for accepted order: " + (order != null ? order.orderID : "null"));
            if (order != null) {
                openMap(order.customerLat, order.customerLng);
            }
        });
    }

    private void openMap(double lat, double lng) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("lat", lat);
        intent.putExtra("lng", lng);
        startActivity(intent);
    }

    public static class DeliveryItem {
        public String orderID;
        public String customerName;
        public String customerAddress;
        public String status;
        public String assignedDeliverymanID;
        public double customerLat;
        public double customerLng;
        public String branchID;
        public long deliveredTimestamp;
        public String paymentStatus;
        public String paymentMethod;
        public double totalPrice; // CHANGE THIS LINE
        public String customerID;

        public DeliveryItem() {}
    }


    private AlertDialog loadingDialog;

    private void showLoadingDialog(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
        TextView textView = view.findViewById(R.id.loadingText);
        textView.setText(message);

        loadingDialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        loadingDialog.show();

        if (loadingDialog.getWindow() != null) {
            int width = (int) (300 * getResources().getDisplayMetrics().density);
            int height = (int) (180 * getResources().getDisplayMetrics().density);
            loadingDialog.getWindow().setLayout(width, height);
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
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
            params.gravity = android.view.Gravity.TOP;
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