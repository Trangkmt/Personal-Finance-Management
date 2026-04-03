package com.example.expensemanagement.model;

public class TransactionItem {
    private String id;
    private String title;
    private String category;
    private String date;
    private long amount;
    private boolean income;

    public TransactionItem(String id, String title, String category, String date, long amount, boolean income) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.date = date;
        this.amount = amount;
        this.income = income;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public long getAmount() { return amount; }
    public boolean isIncome() { return income; }

    public void setTitle(String title) { this.title = title; }
    public void setCategory(String category) { this.category = category; }
    public void setDate(String date) { this.date = date; }
    public void setAmount(long amount) { this.amount = amount; }
    public void setIncome(boolean income) { this.income = income; }
}