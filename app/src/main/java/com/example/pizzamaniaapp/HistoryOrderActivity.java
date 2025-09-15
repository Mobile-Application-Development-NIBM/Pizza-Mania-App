package com.example.pizzamaniaapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

public class HistoryOrderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryOrderAdapter adapter;
    private List<Item> allCompletedItems;

    private FirebaseAuth mAuth;
    private DatabaseReference ordersRef;

    private TextView tvNoOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_order);

        tvNoOrders = findViewById(R.id.tvNoOrders);

        // Use RecyclerView's id, NOT the LinearLayout
        recyclerView = findViewById(R.id.recyclerHistoryOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        allCompletedItems = new ArrayList<>();
        adapter = new HistoryOrderAdapter(this, allCompletedItems);
        recyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        loadCompletedOrders();
    }

    private void loadCompletedOrders() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();
        Log.d("HistoryOrder", "Loading orders for user: " + currentUserId);

        ordersRef.orderByChild("customerID").equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allCompletedItems.clear();

                        Log.d("HistoryOrder", "Orders snapshot count: " + snapshot.getChildrenCount());

                        for (DataSnapshot orderSnap : snapshot.getChildren()) {
                            Order order = orderSnap.getValue(Order.class);
                            if (order != null) {
                                Log.d("HistoryOrder", "Order status: " + order.getStatus());
                                if ("completed".equalsIgnoreCase(order.getStatus()) && order.getItems() != null) {
                                    allCompletedItems.addAll(order.getItems());
                                }
                            }
                        }

                        adapter.notifyDataSetChanged();

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

