package com.example.pizzamaniaapp;

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

import java.util.ArrayList;
import java.util.List;

public class DeliveryHistoryActivity extends AppCompatActivity {

    private static final String TAG = "DeliveryHistory";
    private RecyclerView recyclerView;
    private DeliveryHistoryAdapter adapter;
    private List<Order> deliveredOrders = new ArrayList<>();
    private DatabaseReference dbRef;
    private String currentDeliverymanID;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_history);

        recyclerView = findViewById(R.id.recyclerDeliveryHistory);
        emptyText = findViewById(R.id.tvNoOrders);
        ImageButton backBtn = findViewById(R.id.BackBtn);

        // Back button
        backBtn.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeliveryHistoryAdapter(this, deliveredOrders);
        recyclerView.setAdapter(adapter);

        // Load current deliveryman ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        // Read deliverymanID if exists, fallback to userID
        currentDeliverymanID = prefs.getString("deliverymanID",
                               prefs.getString("userID", "d001"));

        Log.d(TAG, "Loading delivered orders for deliveryman: " + currentDeliverymanID);


        // Firebase reference
        dbRef = FirebaseDatabase.getInstance().getReference("orders");
        loadDeliveredOrders();
    }

    private void loadDeliveredOrders() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                deliveredOrders.clear();

                for (DataSnapshot orderSnap : snapshot.getChildren()) {
                    Order order = orderSnap.getValue(Order.class);
                    if (order == null) continue;

                    // Only Completed orders assigned to this deliveryman
                    if ("Completed".equalsIgnoreCase(order.getStatus())
                            && currentDeliverymanID.equals(order.getAssignedDeliverymanID())) {
                        deliveredOrders.add(order);
                    }
                }

                if (deliveredOrders.isEmpty()) {
                    emptyText.setText("No delivered orders yet");
                    emptyText.setVisibility(TextView.VISIBLE);
                    recyclerView.setVisibility(RecyclerView.GONE);
                } else {
                    emptyText.setVisibility(TextView.GONE);
                    recyclerView.setVisibility(RecyclerView.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                emptyText.setText("Failed to load delivered orders");
                emptyText.setVisibility(TextView.VISIBLE);
                recyclerView.setVisibility(RecyclerView.GONE);
                Log.e(TAG, "Firebase load error: " + error.getMessage());
            }
        });
    }
}
