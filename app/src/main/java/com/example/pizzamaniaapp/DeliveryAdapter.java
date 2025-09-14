package com.example.pizzamaniaapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class DeliveryAdapter extends RecyclerView.Adapter<DeliveryAdapter.ViewHolder> {

    private List<DeliverymanHomeActivity.DeliveryItem> deliveryList;
    private OnAcceptClickListener acceptListener;
    private OnCompleteClickListener completeListener;
    private OnViewMapClickListener viewMapListener;

    public DeliveryAdapter(List<DeliverymanHomeActivity.DeliveryItem> deliveryList) {
        this.deliveryList = deliveryList != null ? deliveryList : new ArrayList<>();
    }

    // --- Click listener interfaces ---
    public interface OnAcceptClickListener { void onAcceptClick(int position); }
    public interface OnCompleteClickListener { void onCompleteClick(int position); }
    public interface OnViewMapClickListener { void onViewMapClick(int position); }

    // --- Set listeners ---
    public void setOnAcceptClickListener(OnAcceptClickListener listener) { this.acceptListener = listener; }
    public void setOnCompleteClickListener(OnCompleteClickListener listener) { this.completeListener = listener; }
    public void setOnViewMapClickListener(OnViewMapClickListener listener) { this.viewMapListener = listener; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.delivery_order_row, parent, false);
        return new ViewHolder(view, acceptListener, completeListener, viewMapListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeliverymanHomeActivity.DeliveryItem item = deliveryList.get(position);

        // Set texts
        holder.customerName.setText("Customer: " + item.customerName);
        holder.customerLocation.setText("Latitude: " + item.customerLat + ", Longitude: " + item.customerLng);

        boolean isAcceptedByCurrent = item.assignedDeliverymanID != null && !item.assignedDeliverymanID.isEmpty()
                && "Delivering".equalsIgnoreCase(item.status);

        // Show Accept button only if order is not accepted
        holder.acceptButton.setVisibility(isAcceptedByCurrent ? View.GONE : View.VISIBLE);

        // Show Completed checkbox only if accepted by current deliveryman
        holder.completedCheckbox.setVisibility(isAcceptedByCurrent ? View.VISIBLE : View.GONE);

        // Checkbox reflects status
        holder.completedCheckbox.setChecked("Completed".equalsIgnoreCase(item.status));
    }

    @Override
    public int getItemCount() { return deliveryList.size(); }

    // --- Helper to get item at position ---
    public DeliverymanHomeActivity.DeliveryItem getItem(int position) {
        return deliveryList.get(position);
    }


    // --- Update list dynamically ---
    public void updateList(List<DeliverymanHomeActivity.DeliveryItem> newList) {
        deliveryList.clear();
        if (newList != null) deliveryList.addAll(newList);
        notifyDataSetChanged();
    }

    // --- ViewHolder class ---
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView customerName, customerLocation; // add customerLocation
        MaterialButton acceptButton, viewMapButton;
        CheckBox completedCheckbox;

        public ViewHolder(@NonNull View itemView,
                          OnAcceptClickListener acceptListener,
                          OnCompleteClickListener completeListener,
                          OnViewMapClickListener viewMapListener) {
            super(itemView);

            customerName = itemView.findViewById(R.id.customerName);
            customerLocation = itemView.findViewById(R.id.customerLocation);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            viewMapButton = itemView.findViewById(R.id.viewMapButton);
            completedCheckbox = itemView.findViewById(R.id.completedCheckbox);

            acceptButton.setOnClickListener(v -> {
                if (acceptListener != null) acceptListener.onAcceptClick(getAdapterPosition());
            });

            completedCheckbox.setOnClickListener(v -> {
                if (completeListener != null) completeListener.onCompleteClick(getAdapterPosition());
            });

            viewMapButton.setOnClickListener(v -> {
                if (viewMapListener != null) viewMapListener.onViewMapClick(getAdapterPosition());
            });
        }
    }
}
