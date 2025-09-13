package com.example.pizzamaniaapp;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final List<CustomerHomeActivity.CartItem> cartItems;
    private final CustomerHomeActivity activity;

    public CartAdapter(List<CustomerHomeActivity.CartItem> cartItems, CustomerHomeActivity activity) {
        this.cartItems = cartItems;
        this.activity = activity;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cart_item, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CustomerHomeActivity.CartItem cartItem = cartItems.get(position); // renamed variable

        holder.name.setText(cartItem.name);
        holder.price.setText("Rs. " + cartItem.price + " x" + cartItem.quantity);

        Glide.with(activity)
                .load(cartItem.imageURL != null ? cartItem.imageURL : R.drawable.sample_pizza)
                .placeholder(R.drawable.sample_pizza)
                .into(holder.image);

        holder.removeBtn.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || pos >= cartItems.size()) return;

            CustomerHomeActivity.CartItem removedItem = cartItems.get(pos); // renamed variable
            cartItems.remove(pos);

            // Update totals in activity
            activity.updateCartAfterRemoval(removedItem);

            // Update cart popup UI
            if (activity.cartPopupTotalText != null) {
                activity.cartPopupTotalText.setText(cartItems.isEmpty()
                        ? "Cart is empty"
                        : "Total: Rs. " + activity.currentCart.totalPrice);

                View parentView = (View) activity.cartPopupTotalText.getParent();
                RecyclerView recyclerView = parentView.findViewById(R.id.cartRecyclerView);
                ImageView emptyImage = parentView.findViewById(R.id.emptyCartImage);

                if (cartItems.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyImage.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyImage.setVisibility(View.GONE);
                }
            }

            notifyItemRemoved(pos);
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    // ----------------- VIEW HOLDER -----------------
    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price;
        ImageButton removeBtn;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.cartItemImage);
            name = itemView.findViewById(R.id.cartItemName);
            price = itemView.findViewById(R.id.cartItemPrice);
            removeBtn = itemView.findViewById(R.id.removeCartItemButton);
        }
    }
}
