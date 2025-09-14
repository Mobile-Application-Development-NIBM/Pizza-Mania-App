package com.example.pizzamaniaapp;

import android.os.Bundle;
import android.os.Handler;
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

public class EmployeeHomeActivity extends AppCompatActivity {

    private RecyclerView recyclerOrders;
    private List<Order> orderList;
    private OrderAdapter adapter;
    private DatabaseReference ordersRef;
    private Handler handler = new Handler();

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

                        // 🔹 Use order-level status, fallback to first item's status
                        String status = order.getStatus();
                        if (status == null && order.getItems() != null && !order.getItems().isEmpty()) {
                            status = order.getItems().get(0).getStatus();
                        }

                        // ✅ Filter: only show Confirm Order & Preparing
                        if ("confirm order".equalsIgnoreCase(status) ||
                                "Preparing".equalsIgnoreCase(status)) {
                            orderList.add(order);
                        }
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

    // ✅ Update all items + order-level status
    private void updateOrderStatus(Order order, String newStatus) {
        if (order.getItems() == null) return;

        // update every item in Firebase
        for (int i = 0; i < order.getItems().size(); i++) {
            int finalI = i;
            ordersRef.child(order.getOrderId())
                    .child("items")
                    .child(String.valueOf(i))
                    .child("status")
                    .setValue(newStatus)
                    .addOnSuccessListener(aVoid -> Log.d("EmployeeHome", "Item " + finalI + " updated"))
                    .addOnFailureListener(e -> Log.e("EmployeeHome", "Update failed: " + e.getMessage()));
        }

        // update order-level status in Firebase
        ordersRef.child(order.getOrderId())
                .child("status")
                .setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Status Updated Successfully", Toast.LENGTH_SHORT).show();

                    // ✅ If changed to Delivery Pending → wait 5 sec before removing locally
                    if ("Delivery Pending".equalsIgnoreCase(newStatus)) {
                        Toast.makeText(this, "Order will disappear in 5 seconds", Toast.LENGTH_LONG).show();

                        handler.postDelayed(() -> {
                            int indexToRemove = -1;
                            for (int i = 0; i < orderList.size(); i++) {
                                if (orderList.get(i).getOrderId().equals(order.getOrderId())) {
                                    indexToRemove = i;
                                    break;
                                }
                            }
                            if (indexToRemove != -1) {
                                orderList.remove(indexToRemove);
                                adapter.notifyItemRemoved(indexToRemove);
                            }
                        }, 5000);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
