package com.example.pizzamaniaapp;

import java.util.List;
import com.google.firebase.database.PropertyName;


public class Order {
    private String branchID;
    private String customerID;
    private double customerLat;
    private double customerLng;
    private String customerName;
    private long deliveredTimestamp;
    private double totalPrice;
    private String status;          //  order-level status
    private List<Item> items;

    // Firebase key (not stored in DB)
    private String orderID;
    private String assignedDeliverymanID;
    private String paymentStatus;
    private long timestamp;      // optional


    public Order() {}

    public String getBranchID() { return branchID; }
    public void setBranchID(String branchID) { this.branchID = branchID; }

    public String getCustomerID() { return customerID; }
    public void setCustomerID(String customerID) { this.customerID = customerID; }

    public double getCustomerLat() { return customerLat; }
    public void setCustomerLat(double customerLat) { this.customerLat = customerLat; }

    public double getCustomerLng() { return customerLng; }
    public void setCustomerLng(double customerLng) { this.customerLng = customerLng; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public long getDeliveredTimestamp() { return deliveredTimestamp; }
    public void setDeliveredTimestamp(long deliveredTimestamp) { this.deliveredTimestamp = deliveredTimestamp; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    @PropertyName("orderID")
    public String getOrderId() { return orderID; }
    @PropertyName("orderID")
    public void setOrderId(String orderId) { this.orderID = orderId; }

    public String getAssignedDeliverymanID() { return assignedDeliverymanID; }
    public void setAssignedDeliverymanID(String assignedDeliverymanID) { this.assignedDeliverymanID = assignedDeliverymanID; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
