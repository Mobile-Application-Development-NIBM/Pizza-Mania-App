package com.example.pizzamaniaapp;

import android.os.Bundle;
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

public class DeliverymanHomeActivity extends AppCompatActivity {

    private RecyclerView deliveryRecyclerView;
    private List<Order> pendingOrders;
    private OrderAdapter adapter;
    private DatabaseReference ordersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deliveryman_home);

        deliveryRecyclerView = findViewById(R.id.deliveryRecyclerView);
        deliveryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        pendingOrders = new ArrayList<>();
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        // Adapter (UI-only) with callback for status updates
        adapter = new OrderAdapter(this, pendingOrders, (order, newStatus) -> {
            updateOrderStatus(order, newStatus);
        });
        deliveryRecyclerView.setAdapter(adapter);

        loadPendingOrders();
    }

    private void loadPendingOrders() {
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingOrders.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Order order = dataSnapshot.getValue(Order.class);
                    if (order != null) {
                        order.setOrderId(dataSnapshot.getKey());

                        // ✅ Only show orders if the first item is "Delivery Pending"
                        if (order.getItems() != null &&
                                !order.getItems().isEmpty() &&
                                "Delivery Pending".equals(order.getItems().get(0).getStatus())) {
                            pendingOrders.add(order);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DeliverymanHomeActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Update order status in Firebase
    private void updateOrderStatus(Order order, String newStatus) {
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            // Update the status of the first item for now
            ordersRef.child(order.getOrderId())
                    .child("items")
                    .child("0") // first item index
                    .child("status")
                    .setValue(newStatus)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Status Updated", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()
                    );
        }
    }
}
