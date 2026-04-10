package com.example.expensemanagement.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "stocks")
public class StockEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "symbol")
    private String symbol;

    @NonNull
    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "price")
    private double price;

    @ColumnInfo(name = "change")
    private double change;

    @ColumnInfo(name = "change_percent")
    private String changePercent;

    public StockEntity(@NonNull String symbol, @NonNull String userId, String name, double price, double change, String changePercent) {
        this.symbol = symbol;
        this.userId = userId;
        this.name = name;
        this.price = price;
        this.change = change;
        this.changePercent = changePercent;
    }

    @NonNull
    public String getSymbol() { return symbol; }
    public void setSymbol(@NonNull String symbol) { this.symbol = symbol; }

    @NonNull
    public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getChange() { return change; }
    public void setChange(double change) { this.change = change; }

    public String getChangePercent() { return changePercent; }
    public void setChangePercent(String changePercent) { this.changePercent = changePercent; }
}
