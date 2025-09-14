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

public class EmployeeHomeActivity extends AppCompatActivity {

    private RecyclerView recyclerOrders;
    private List<Order> orderList;
    private OrderAdapter adapter;
    private DatabaseReference ordersRef;

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

        loadOrders();
    }

    private void loadOrders() {
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Order order = dataSnapshot.getValue(Order.class);
                    if (order != null) {
                        order.setOrderId(dataSnapshot.getKey());
                        orderList.add(order);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EmployeeHomeActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ Update all items' status inside an order
    private void updateOrderStatus(Order order, String newStatus) {
        if (order.getItems() == null) return;

        for (int i = 0; i < order.getItems().size(); i++) {
            ordersRef.child(order.getOrderId())
                    .child("items")
                    .child(String.valueOf(i))
                    .child("status")
                    .setValue(newStatus);
        }

        Toast.makeText(this, "Status Updated", Toast.LENGTH_SHORT).show();
    }
}
