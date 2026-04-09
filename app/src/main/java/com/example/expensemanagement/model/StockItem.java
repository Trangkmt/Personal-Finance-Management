package com.example.expensemanagement.model;

import java.util.List;

public class StockItem {
    public String symbol;
    public String fullName;
    public double price;
    public double change;      // giá trị thay đổi
    public double changePct;   // % thay đổi
    public boolean isPositive;
    public String currency;
    public List<Float> chartPoints; // dữ liệu mini chart

    public StockItem(String symbol, String fullName, double price,
                     double change, double changePct, String currency,
                     List<Float> chartPoints) {
        this.symbol      = symbol;
        this.fullName    = fullName;
        this.price       = price;
        this.change      = change;
        this.changePct   = changePct;
        this.isPositive  = change >= 0;
        this.currency    = currency;
        this.chartPoints = chartPoints;
    }
}