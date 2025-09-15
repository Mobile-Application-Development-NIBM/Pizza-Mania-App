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
    private View noDeliveriesLayout; // wrapper for image + text

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
        pendingAdapter.setOnAcceptClickListener(position -> {
            DeliveryItem order = pendingAdapter.getItem(position);

            // Check if deliveryman already has an active order
            if (acceptedAdapter.getItemCount() > 0) {
                showCustomToast("⚠ You can accept only 1 order at a time");
                return;
            }

            showLoadingDialog("Accepting delivery...");
            Map<String, Object> updates = new HashMap<>();
            updates.put("assignedDeliverymanID", getCurrentDeliverymanID()); // match Firebase field
            updates.put("status", "Delivering");

            db.child("orders").child(order.orderID).updateChildren(updates)
                    .addOnSuccessListener(a -> {
                        hideLoadingDialog();
                        loadDeliveries();
                    })
                    .addOnFailureListener(e -> hideLoadingDialog());
        });

        pendingAdapter.setOnViewMapClickListener(position -> {
            DeliveryItem order = pendingAdapter.getItem(position);
            openMap(order.customerLat, order.customerLng);
        });

        acceptedAdapter.setOnCompleteClickListener(position -> {
            DeliveryItem order = acceptedAdapter.getItem(position);

            if ("Completed".equalsIgnoreCase(order.status)) {
                // Undo: set back to Delivery Pending
                order.status = "Delivery Pending";
                db.child("orders").child(order.orderID).child("status").setValue(order.status);
                showCustomToast("↩ Delivery set back to Pending");
                return;
            }

            // First click: mark Completed
            order.status = "Completed";
            db.child("orders").child(order.orderID).child("status").setValue(order.status);
            showCustomToast("✓ Delivery Completed");

            final boolean[] undone = {false};
            CountDownTimer timer = new CountDownTimer(15000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    DeliveryItem current = acceptedAdapter.getItem(position);
                    if (!"Completed".equalsIgnoreCase(current.status)) {
                        undone[0] = true;
                        cancel();
                    }
                }

                @Override
                public void onFinish() {
                    if (!undone[0]) {
                        // ⏱ Update deliveredTimestamp only after 15 sec passed
                        db.child("orders").child(order.orderID)
                                .child("deliveredTimestamp")
                                .setValue(System.currentTimeMillis());

                        showLoadingDialog("Refreshing orders...");
                        loadDeliveries();
                    }
                }
            };
            timer.start();
        });

        acceptedAdapter.setOnViewMapClickListener(position -> {
            DeliveryItem order = acceptedAdapter.getItem(position);
            openMap(order.customerLat, order.customerLng);
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
        public String assignedDeliverymanID; // ✅ match Firebase
        public double customerLat;
        public double customerLng;
        public String branchID;
        public long deliveredTimestamp; // ⏱ added for completed time

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