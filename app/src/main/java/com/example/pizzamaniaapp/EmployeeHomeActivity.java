// package name
package com.example.pizzamaniaapp;

// import required Android and Firebase libraries
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

// Activity class to display orders for employees
public class EmployeeHomeActivity extends AppCompatActivity {

    // RecyclerView to display orders
    private RecyclerView recyclerOrders;
    // List to hold orders retrieved from Firebase
    private List<Order> orderList;
    // Adapter to bind data to RecyclerView
    private OrderAdapter adapter;
    // Firebase database reference
    private DatabaseReference ordersRef;
    // Handler used for delayed actions (e.g., remove order after 5 sec)
    private Handler handler = new Handler();

    // onCreate() is called when activity starts
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set the UI layout for this activity
        setContentView(R.layout.activity_employee_home);

        // find RecyclerView in layout
        recyclerOrders = findViewById(R.id.recyclerOrders);
        // set layout manager to display items in vertical list
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));

        // initialize empty list for orders
        orderList = new ArrayList<>();
        // reference to "orders" node in Firebase
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        // initialize adapter and define callback for status update
        adapter = new OrderAdapter(this, orderList, (order, newStatus) -> {
            updateOrderStatus(order, newStatus); // method to update Firebase when status changes
        });
        // set adapter to RecyclerView
        recyclerOrders.setAdapter(adapter);

        // load orders from Firebase
        loadOrders();
    }

    // method to load orders from Firebase Realtime Database
    private void loadOrders() {
        // add listener to "orders" node
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // clear old list to avoid duplicates
                orderList.clear();
                // loop through each order in Firebase
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // convert snapshot into Order object
                    Order order = dataSnapshot.getValue(Order.class);
                    if (order != null) {
                        // set orderId from Firebase key
                        order.setOrderId(dataSnapshot.getKey());

                        // get status from order-level, fallback to first item’s status if null
                        String status = order.getStatus();
                        if (status == null && order.getItems() != null && !order.getItems().isEmpty()) {
                            status = order.getItems().get(0).getStatus();
                        }

                        // filter: only show orders with "Confirm Order" or "Preparing" status
                        if ("confirm order".equalsIgnoreCase(status) ||
                                "Preparing".equalsIgnoreCase(status)) {
                            orderList.add(order); // add to list
                        }
                    }
                }
                // notify adapter that data has changed → refresh UI
                adapter.notifyDataSetChanged();
            }

            // called if Firebase read fails
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EmployeeHomeActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // method to update order status in Firebase and handle UI updates
    private void updateOrderStatus(Order order, String newStatus) {
        // safety check: if order has no items, return
        if (order.getItems() == null) return;

        // loop through items of this order and update each item's status in Firebase
        for (int i = 0; i < order.getItems().size(); i++) {
            int finalI = i; // needed for lambda
            ordersRef.child(order.getOrderId()) // go to specific order
                    .child("items")
                    .child(String.valueOf(i)) // index of item
                    .child("status") // update status field
                    .setValue(newStatus) // set new status
                    .addOnSuccessListener(aVoid -> Log.d("EmployeeHome", "Item " + finalI + " updated")) // log success
                    .addOnFailureListener(e -> Log.e("EmployeeHome", "Update failed: " + e.getMessage())); // log error
        }

        // update order-level status in Firebase
        ordersRef.child(order.getOrderId())
                .child("status")
                .setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    // notify user with toast
                    Toast.makeText(this, "Status Updated Successfully", Toast.LENGTH_SHORT).show();

                    // if status changed to "Delivery Pending" → remove from UI after 5 seconds
                    if ("Delivery Pending".equalsIgnoreCase(newStatus)) {
                        Toast.makeText(this, "Order will disappear in 5 seconds", Toast.LENGTH_LONG).show();

                        // delay removal by 5 seconds
                        handler.postDelayed(() -> {
                            int indexToRemove = -1;
                            // find the index of this order in list
                            for (int i = 0; i < orderList.size(); i++) {
                                if (orderList.get(i).getOrderId().equals(order.getOrderId())) {
                                    indexToRemove = i;
                                    break;
                                }
                            }
                            // if found, remove from list and notify adapter
                            if (indexToRemove != -1) {
                                orderList.remove(indexToRemove);
                                adapter.notifyItemRemoved(indexToRemove);
                            }
                        }, 5000);
                    }
                })
                // if update fails, show error toast
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
