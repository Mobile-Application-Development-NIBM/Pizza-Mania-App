package com.example.pizzamaniaapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

    private DatabaseReference db;
    private RecyclerView orderRecyclerView;
    private OrderAdapter orderAdapter;
    private List<Order> allOrders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_order);

        db = FirebaseDatabase.getInstance().getReference("orders");

        orderRecyclerView = findViewById(R.id.orderRecyclerView);
        orderRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        orderAdapter = new OrderAdapter(allOrders);
        orderRecyclerView.setAdapter(orderAdapter);

        loadOrders();
    }

    private void loadOrders() {
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                allOrders.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Order order = snap.getValue(Order.class);
                    if (order != null) {
                        allOrders.add(order);
                    }
                }
                orderAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(EmployeeOrderActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                Log.e("Firebase", "Error: " + error.getMessage());
            }
        });
    }

    // ------------------- Order Adapter -------------------
    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
        private List<Order> orders;

        public OrderAdapter(List<Order> orders) {
            this.orders = orders;
        }

        @Override
        public OrderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // Create item layout programmatically
            LinearLayout itemLayout = new LinearLayout(parent.getContext());
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(12, 12, 12, 12);
            itemLayout.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 8, 0, 8);
            itemLayout.setLayoutParams(params);

            // Order ID TextView
            TextView orderID = new TextView(parent.getContext());
            orderID.setId(View.generateViewId());
            orderID.setTextSize(18f);
            orderID.setTypeface(null, Typeface.BOLD);
            itemLayout.addView(orderID);

            // Status Spinner
            Spinner statusSpinner = new Spinner(parent.getContext());
            statusSpinner.setId(View.generateViewId());
            itemLayout.addView(statusSpinner);

            // Update Button
            Button updateBtn = new Button(parent.getContext());
            updateBtn.setId(View.generateViewId());
            updateBtn.setText("Update Status");
            updateBtn.setBackgroundColor(Color.parseColor("#FFDE21"));
            updateBtn.setTextColor(Color.BLACK);
            itemLayout.addView(updateBtn);

            return new OrderViewHolder(itemLayout, orderID, statusSpinner, updateBtn);
        }

        @Override
        public void onBindViewHolder(OrderViewHolder holder, int position) {
            Order order = orders.get(position);

            holder.orderID.setText("Order: " + order.orderID);

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(EmployeeOrderActivity.this,
                    android.R.layout.simple_spinner_item,
                    new String[]{"Pending", "Preparing", "Completed"});
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.statusSpinner.setAdapter(spinnerAdapter);

            if (order.orderStatus != null) {
                int spinnerPos = spinnerAdapter.getPosition(order.orderStatus);
                holder.statusSpinner.setSelection(spinnerPos);
            }

            holder.updateBtn.setOnClickListener(v -> {
                String newStatus = holder.statusSpinner.getSelectedItem().toString();
                db.child(order.orderID).child("orderStatus").setValue(newStatus)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(EmployeeOrderActivity.this, "Order updated", Toast.LENGTH_SHORT).show()
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(EmployeeOrderActivity.this, "Update failed", Toast.LENGTH_SHORT).show()
                        );
            });
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView orderID;
            Spinner statusSpinner;
            Button updateBtn;

            OrderViewHolder(View itemView, TextView orderID, Spinner statusSpinner, Button updateBtn) {
                super(itemView);
                this.orderID = orderID;
                this.statusSpinner = statusSpinner;
                this.updateBtn = updateBtn;
            }
        }
    }

    // ------------------- Order Model -------------------
    public static class Order {
        public String orderID;
        public String orderStatus;
        public List<String> items;

        public Order() {} // required for Firebase

        public Order(String orderID, String orderStatus, List<String> items) {
            this.orderID = orderID;
            this.orderStatus = orderStatus;
            this.items = items;
        }
    }
}
