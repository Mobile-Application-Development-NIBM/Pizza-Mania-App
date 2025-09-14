// package declaration
package com.example.pizzamaniaapp;

// imports for Android components
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

// imports for AndroidX RecyclerView
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// import for Java utilities
import java.util.List;

// main class declaration extending RecyclerView.Adapter
public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    // variables
    private Context context;
    private List<Order> orderList;
    private OnStatusUpdateListener listener;

    // interface for status update callback
    public interface OnStatusUpdateListener {
        void onStatusUpdate(Order order, String newStatus);
    }

    // constructor
    public OrderAdapter(Context context, List<Order> orderList, OnStatusUpdateListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
    }

    // create new ViewHolder
    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.employee_order_item, parent, false);
        return new OrderViewHolder(view);
    }

    // bind data to ViewHolder
    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.tvCustomerName.setText(order.getCustomerName());

        // check if order has items
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            StringBuilder itemsDetails = new StringBuilder();
            itemsDetails.append("Branch: ").append(order.getBranchID()).append("\n");   // ✅ show BranchID
            itemsDetails.append("Items: ").append(order.getItems().size()).append("\n");

            // loop through items
            for (Item item : order.getItems()) {
                itemsDetails.append("- ")
                        .append(item.getName())
                        .append(" x")
                        .append(item.getQuantity())
                        .append("\n");
            }

            // ✅ Use order-level status
            String status = order.getStatus();
            itemsDetails.append("Status: ").append(status != null ? status : "N/A").append("\n");

            itemsDetails.append("Total: Rs. ").append(order.getTotalPrice());

            // set text to items TextView
            holder.tvItems.setText(itemsDetails.toString());

            // Payment status (from first item)
            String paymentStatus = order.getItems().get(0).getPaymentStatus();
            if (paymentStatus != null) {
                holder.tvPaymentStatus.setText("Payment: " + paymentStatus);
                holder.tvPaymentStatus.setTextColor(paymentStatus.equalsIgnoreCase("Pending") ? Color.RED : Color.GREEN);
            }

            // Spinner setup
            String[] statuses = {"confirm order", "Preparing", "Delivery Pending", "Delivering", "Completed"};
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, statuses);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.spinnerStatus.setAdapter(spinnerAdapter);

            // set spinner selection based on status
            if (status != null) {
                int spinnerPosition = spinnerAdapter.getPosition(status);
                if (spinnerPosition >= 0) {
                    holder.spinnerStatus.setSelection(spinnerPosition);
                }
            }

            // button click listener for update
            holder.btnUpdate.setOnClickListener(v -> {
                if (listener != null) {
                    String newStatus = holder.spinnerStatus.getSelectedItem().toString();
                    listener.onStatusUpdate(order, newStatus);
                }
            });
        }
    }

    // return number of items
    @Override
    public int getItemCount() {
        return orderList.size();
    }

    // ViewHolder inner class
    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvItems, tvPaymentStatus;
        Spinner spinnerStatus;
        Button btnUpdate;

        // constructor for ViewHolder
        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvItems = itemView.findViewById(R.id.tvItems);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            spinnerStatus = itemView.findViewById(R.id.spinnerStatus);
            btnUpdate = itemView.findViewById(R.id.btnUpdate);
        }
    }
}
