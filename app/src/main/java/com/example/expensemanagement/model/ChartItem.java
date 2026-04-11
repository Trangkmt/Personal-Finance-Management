package com.example.expensemanagement.model;

public class ChartItem {

    public static final int BAR = 0;
    public static final int PIE = 1;

    private int type;
    private String title;

    public ChartItem(int type, String title) {
        this.type = type;
        this.title = title;
    }

    public int getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }
}