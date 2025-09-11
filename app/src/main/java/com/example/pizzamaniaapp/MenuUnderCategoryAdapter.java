package com.example.pizzamaniaapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MenuUnderCategoryAdapter extends RecyclerView.Adapter<MenuUnderCategoryAdapter.MenuViewHolder> {

    private final List<CustomerHomeActivity.MenuItem> menuList;
    private final OnMenuClickListener listener;

    public interface OnMenuClickListener {
        void onMenuClick(CustomerHomeActivity.MenuItem item);
    }

    public MenuUnderCategoryAdapter(List<CustomerHomeActivity.MenuItem> menuList,
                                    OnMenuClickListener listener) {
        this.menuList = menuList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.customer_menu_item_layout, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        CustomerHomeActivity.MenuItem item = menuList.get(position);

        holder.name.setText(item.name);
        holder.price.setText("Rs. " + item.price);

        Glide.with(holder.itemView.getContext())
                .load(item.imageURL)
                .placeholder(R.drawable.sample_pizza)
                .into(holder.image);

        // 🔹 View button triggers the popup
        holder.viewButton.setOnClickListener(v -> {
            if (listener != null) listener.onMenuClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return menuList != null ? menuList.size() : 0;
    }

    static class MenuViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price;
        MaterialButton viewButton;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.menuImage);
            name = itemView.findViewById(R.id.menuName);
            price = itemView.findViewById(R.id.menuPrice);
            viewButton = itemView.findViewById(R.id.viewMenuButton);
        }
    }
}
