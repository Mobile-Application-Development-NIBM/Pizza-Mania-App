package com.example.pizzamaniaapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DeliveryHistoryAdapter extends RecyclerView.Adapter<DeliveryHistoryAdapter.DeliveryViewHolder> {

    private final Context context;
    private final List<Order> orderList;

    public DeliveryHistoryAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public DeliveryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_delivery_order, parent, false);
        return new DeliveryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeliveryViewHolder holder, int position) {
        Order order = orderList.get(position);

        holder.tvOrderId.setText("Order ID: " + order.getOrderId());
        holder.tvCustomerName.setText("Customer: " + order.getCustomerName());
        holder.tvTotalPrice.setText("Total: Rs. " + order.getTotalPrice());

        if (order.getDeliveredTimestamp() > 0) {
            Date date = new Date(order.getDeliveredTimestamp());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvDeliveredTime.setText("Delivered: " + sdf.format(date));
        } else {
            holder.tvDeliveredTime.setText("Delivered: N/A");
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class DeliveryViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvCustomerName, tvTotalPrice, tvDeliveredTime;

        public DeliveryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvDeliveredTime = itemView.findViewById(R.id.tvDeliveredTime);
        }
    }
}
