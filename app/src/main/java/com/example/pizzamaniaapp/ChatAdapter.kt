package com.example.pizzamaniaapp

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.example.pizzamaniaapp.ChatMessage // Adjust import based on package

class ChatAdapter(private val messages: MutableList<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val USER_MESSAGE = 1
    private val BOT_MESSAGE = 2
    private val database = FirebaseDatabase.getInstance().reference

    override fun getItemViewType(position: Int): Int = if (messages[position].isUser) USER_MESSAGE else BOT_MESSAGE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_item_layout, parent, false)
        return if (viewType == USER_MESSAGE) UserViewHolder(view) else BotViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserViewHolder -> {
                holder.textView.text = message.text
                holder.textView.gravity = Gravity.END
                holder.textView.setTextColor(Color.BLACK)
                holder.imageView.visibility = View.GONE // No image for user messages
            }
            is BotViewHolder -> {
                holder.textView.text = message.text
                holder.textView.gravity = Gravity.START
                holder.textView.setTextColor(Color.BLUE)
                holder.imageView.visibility = View.GONE // Temporarily disable image loading
                // In onBindViewHolder for BotViewHolder
                if (!message.isUser && !message.text.startsWith("Fetching menu...") && !message.text.startsWith("Error fetching menu:")) {
                    holder.imageView.visibility = View.VISIBLE
                    val itemName = message.text.substringAfter("• ").substringBefore(" .......")
                    database.child("menu").orderByChild("name").equalTo(itemName.trim()).get().addOnSuccessListener { snapshot ->
                        val imageUrl = snapshot.children.firstOrNull()?.child("imageURL")?.getValue(String::class.java)
                        if (imageUrl != null) {
                            Glide.with(holder.imageView.context).load(imageUrl).into(holder.imageView)
                        } else {
                            holder.imageView.visibility = View.GONE
                        }
                    }.addOnFailureListener {
                        holder.imageView.visibility = View.GONE
                    }
                } else {
                    holder.imageView.visibility = View.GONE
                }
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.chatText)
        val imageView: ImageView = itemView.findViewById(R.id.chatImage)
    }

    class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.chatText)
        val imageView: ImageView = itemView.findViewById(R.id.chatImage)
    }
}