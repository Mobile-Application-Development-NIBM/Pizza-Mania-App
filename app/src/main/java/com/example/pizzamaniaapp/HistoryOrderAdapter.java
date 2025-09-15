package com.example.pizzamaniaapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class HistoryOrderAdapter extends RecyclerView.Adapter<HistoryOrderAdapter.HistoryOrderViewHolder> {

    private Context context;
    private List<Item> items;

    public HistoryOrderAdapter(Context context, List<Item> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public HistoryOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new HistoryOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryOrderViewHolder holder, int position) {
        Item item = items.get(position);

        holder.itemName.setText(item.getName());
        holder.itemQuantity.setText("Qty: " + item.getQuantity());
        holder.itemPrice.setText("Price: " + item.getPrice());
        holder.itemTotalPrice.setText("Total: " + (item.getPrice() * item.getQuantity()));

        Glide.with(context)
                .load(item.getImageURL())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.itemImage);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class HistoryOrderViewHolder extends RecyclerView.ViewHolder {
        CircleImageView itemImage;
        TextView itemName, itemQuantity, itemPrice, itemTotalPrice;

        public HistoryOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemName = itemView.findViewById(R.id.itemName);
            itemQuantity = itemView.findViewById(R.id.itemQuantity);
            itemPrice = itemView.findViewById(R.id.itemPrice);
            itemTotalPrice = itemView.findViewById(R.id.itemTotalPrice);
        }
    }
}
