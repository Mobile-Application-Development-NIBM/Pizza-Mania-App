package com.example.pizzamaniaapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryOrderAdapter extends RecyclerView.Adapter<HistoryOrderAdapter.HistoryOrderViewHolder> {

    private Context context;
    private List<Order> orderList;

    public HistoryOrderAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public HistoryOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new HistoryOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryOrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.tvCustomerName.setText(order.getCustomerName());

        StringBuilder details = new StringBuilder();
        details.append("Branch: ").append(order.getBranchID()).append("\n");
        details.append("Items: ").append(order.getItems().size()).append("\n");
        for (Item item : order.getItems()) {
            details.append("- ").append(item.getName())
                    .append(" x").append(item.getQuantity()).append("\n");
        }
        details.append("Status: ").append(order.getStatus()).append("\n");
        details.append("Total: Rs. ").append(order.getTotalPrice());

        holder.tvItems.setText(details.toString());


    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class HistoryOrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvItems;

        public HistoryOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvItems = itemView.findViewById(R.id.tvItems);
            ;
        }
    }
}
