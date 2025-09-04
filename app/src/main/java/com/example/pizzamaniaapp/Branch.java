package com.example.pizzamaniaapp;

public class Branch {
    public String branchID;
    public String name;

    // Empty constructor needed for Firebase
    public Branch() {}

    public Branch(String branchID, String name) {
        this.branchID = branchID;
        this.name = name;
    }
}
