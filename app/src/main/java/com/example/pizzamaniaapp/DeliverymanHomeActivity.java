package com.example.pizzamaniaapp;

import android.os.Bundle;
import android.util.Log;
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

                        if ("Delivery Pending".equals(order.getStatus())) {
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

    // ✅ Update all items + order-level status
    private void updateOrderStatus(Order order, String newStatus) {
        if (order.getItems() == null) return;

        for (int i = 0; i < order.getItems().size(); i++) {
            int finalI = i;
            ordersRef.child(order.getOrderId())
                    .child("items")
                    .child(String.valueOf(i))
                    .child("status")
                    .setValue(newStatus)
                    .addOnSuccessListener(aVoid -> Log.d("DeliveryHome", "Item " + finalI + " updated"))
                    .addOnFailureListener(e -> Log.e("DeliveryHome", "Update failed: " + e.getMessage()));
        }

        ordersRef.child(order.getOrderId())
                .child("status")
                .setValue(newStatus)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Status Updated Successfully", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
