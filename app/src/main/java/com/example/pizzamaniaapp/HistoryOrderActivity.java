package com.example.pizzamaniaapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

public class HistoryOrderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryOrderAdapter adapter;
    private List<Item> allCompletedItems;

    private DatabaseReference ordersRef;
    private TextView tvNoOrders;

    private String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_order2);

        tvNoOrders = findViewById(R.id.tvNoOrders);
        recyclerView = findViewById(R.id.recyclerHistoryOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        allCompletedItems = new ArrayList<>();
        adapter = new HistoryOrderAdapter(this, allCompletedItems);
        recyclerView.setAdapter(adapter);

        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        // ✅ Fetch current logged-in user's ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        currentUserID = prefs.getString("userID", null);

        if (currentUserID == null) {
            // 🔹 User ID not found → show message and close activity
            Toast.makeText(this, "User ID not found. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 🔹 Load completed orders for this user
        loadCompletedOrders();
    }

    private void loadCompletedOrders() {
        Log.d("HistoryOrder", "Loading orders for user: " + currentUserID);

        // 🔹 Query orders by customerID = currentUserID
        ordersRef.orderByChild("customerID").equalTo(currentUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allCompletedItems.clear();

                        Log.d("HistoryOrder", "Orders snapshot count: " + snapshot.getChildrenCount());

                        for (DataSnapshot orderSnap : snapshot.getChildren()) {
                            // Convert snapshot to Order object
                            Order order = orderSnap.getValue(Order.class);

                            if (order != null) {
                                Log.d("HistoryOrder", "Order status: " + order.getStatus());
                                Log.d("HistoryOrder", "CustomerID: " + order.getCustomerID());

                                // 🔹 Only add items from orders with status = Completed
                                if ("Completed".equalsIgnoreCase(order.getStatus()) && order.getItems() != null) {
                                    Log.d("HistoryOrder", "Adding " + order.getItems().size() + " items");
                                    allCompletedItems.addAll(order.getItems());
                                }
                            }
                        }

                        // 🔹 Refresh RecyclerView
                        adapter.notifyDataSetChanged();

                        // 🔹 Show "No completed orders" text if list is empty
                        if (allCompletedItems.isEmpty()) {
                            tvNoOrders.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            tvNoOrders.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(HistoryOrderActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("HistoryOrder", "Firebase error: " + error.getMessage());
                    }
                });
    }
}
