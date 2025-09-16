package com.example.pizzamaniaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.pizzamaniaapp.HistoryOrderAdapter;


import java.util.ArrayList;
import java.util.List;

public class HistoryOrderActivity extends AppCompatActivity {

    private static final String TAG = "HistoryOrder";
    private RecyclerView recyclerView;
    private HistoryOrderAdapter orderAdapter;  //  same type as the object

    private List<Order> completedOrders = new ArrayList<>();

    private DatabaseReference dbRef;
    private String currentUserID;

    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_order2);

        recyclerView = findViewById(R.id.recyclerHistoryOrders);
        emptyText = findViewById(R.id.tvNoOrders);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new HistoryOrderAdapter(this, completedOrders);
        recyclerView.setAdapter(orderAdapter);


        // Load current user ID from SharedPreferences (saved during login)
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        currentUserID = prefs.getString("userID", "u001");
        Log.d(TAG, "Loading orders for user: " + currentUserID);

        dbRef = FirebaseDatabase.getInstance().getReference("orders");

        loadCompletedOrders();

        // -------------------- Order History --------------------
        ImageButton Backbtn = findViewById(R.id.Backbtn);
        Backbtn.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryOrderActivity.this, CustomerHomeActivity.class);
            startActivity(intent);
        });
    }

    private void loadCompletedOrders() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                completedOrders.clear();

                Log.d(TAG, "Orders snapshot count: " + snapshot.getChildrenCount());

                for (DataSnapshot orderSnap : snapshot.getChildren()) {
                    try {
                        Order order = orderSnap.getValue(Order.class);

                        if (order == null) {
                            Log.w(TAG, "Skipped null order for key: " + orderSnap.getKey());
                            continue;
                        }

                        Log.d(TAG, "Order status: " + order.getStatus() + ", CustomerID: " + order.getCustomerID());

                        // Check if this order belongs to current user AND is completed
                        if ("Completed".equalsIgnoreCase(order.getStatus())
                                && currentUserID.equals(order.getCustomerID())) {
                            completedOrders.add(order);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse order: " + orderSnap.getKey(), e);
                    }
                }

                if (completedOrders.isEmpty()) {
                    emptyText.setText("No completed orders yet");
                    emptyText.setVisibility(TextView.VISIBLE);
                    recyclerView.setVisibility(RecyclerView.GONE);
                } else {
                    emptyText.setVisibility(TextView.GONE);
                    recyclerView.setVisibility(RecyclerView.VISIBLE);
                    orderAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load orders", error.toException());
                emptyText.setText("Failed to load orders");
                emptyText.setVisibility(TextView.VISIBLE);
                recyclerView.setVisibility(RecyclerView.GONE);
            }
        });
    }
}
