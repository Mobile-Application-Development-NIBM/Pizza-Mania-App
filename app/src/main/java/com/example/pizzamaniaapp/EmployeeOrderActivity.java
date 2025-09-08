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

public class EmployeeOrderActivity extends AppCompatActivity {

    private RecyclerView recyclerOrders;
    private List<Order> orderList;
    private OrderAdapter adapter;
    private DatabaseReference ordersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_order);

        recyclerOrders = findViewById(R.id.recyclerOrders);
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));

        orderList = new ArrayList<>();
        //  Pass "employee" role to adapter
        adapter = new OrderAdapter(this, orderList, "employee");
        recyclerOrders.setAdapter(adapter);

        ordersRef = FirebaseDatabase.getInstance().getReference("order");

        // Load orders from Firebase
        ordersRef.addValueEventListener(new ValueEventListener() {    // Syncs data in real-time
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Order order = dataSnapshot.getValue(Order.class);
                    if (order != null) {
                        order.setOrderId(dataSnapshot.getKey()); // Save key for updates
                        orderList.add(order);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EmployeeOrderActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
