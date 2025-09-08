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

        // 🔹 Link RecyclerView from XML
        deliveryRecyclerView = findViewById(R.id.deliveryRecyclerView);
        deliveryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        pendingOrders = new ArrayList<>();
        // 🔹 Delivery role: can update to "Completed"
        adapter = new OrderAdapter(this, pendingOrders, "delivery");
        deliveryRecyclerView.setAdapter(adapter);

        ordersRef = FirebaseDatabase.getInstance().getReference("order");

        // 🔹 Load only Delivery Pending orders
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingOrders.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Order order = dataSnapshot.getValue(Order.class);
                    if (order != null) {
                        order.setOrderId(dataSnapshot.getKey());

                        // ✅ Show only Delivery Pending orders
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
}
