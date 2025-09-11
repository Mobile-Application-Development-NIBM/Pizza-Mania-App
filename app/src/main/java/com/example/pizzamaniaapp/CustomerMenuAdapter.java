package com.example.pizzamaniaapp;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CustomerMenuAdapter extends RecyclerView.Adapter<CustomerMenuAdapter.CustomerMenuViewHolder> {

    private static final String TAG = "CustomerMenuAdapter";

    private final List<CustomerHomeActivity.MenuItem> menuList;
    private final OnViewClickListener viewListener;

    public interface OnViewClickListener {
        void onViewClick(CustomerHomeActivity.MenuItem item);
    }

    public CustomerMenuAdapter(List<CustomerHomeActivity.MenuItem> menuList,
                               OnViewClickListener viewListener) {
        this.menuList = menuList;
        this.viewListener = viewListener;
        Log.d(TAG, "Adapter initialized with menu list size: " + (menuList != null ? menuList.size() : "0"));
    }

    @NonNull
    @Override
    public CustomerMenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder called for viewType: " + viewType);
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.customer_menu_item_layout, parent, false);
        return new CustomerMenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerMenuViewHolder holder, int position) {
        CustomerHomeActivity.MenuItem item = menuList.get(position);
        Log.d(TAG, "Binding menu item: " + item.name + " at position: " + position);

        holder.name.setText(item.name);
        holder.price.setText("Rs. " + item.price);

        Glide.with(holder.itemView.getContext())
                .load(item.imageURL)
                .placeholder(R.drawable.sample_pizza)
                .into(holder.image);

        // Just call the activity's popup
        holder.viewButton.setOnClickListener(v -> viewListener.onViewClick(item));
    }

    @Override
    public int getItemCount() {
        return menuList != null ? menuList.size() : 0;
    }

    static class CustomerMenuViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price;
        MaterialButton viewButton;

        public CustomerMenuViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.menuImage);
            name = itemView.findViewById(R.id.menuName);
            price = itemView.findViewById(R.id.menuPrice);
            viewButton = itemView.findViewById(R.id.viewMenuButton);
        }
    }
}
