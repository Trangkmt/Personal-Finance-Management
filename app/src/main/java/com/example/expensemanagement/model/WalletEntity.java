package com.example.expensemanagement.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "wallets")
public class WalletEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "wallet_id")
    private String id;

    @NonNull
    @ColumnInfo(name = "user_id")
    private String userId;

    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    @NonNull
    @ColumnInfo(name = "type")
    private String type; // cash, bank, e-wallet

    @ColumnInfo(name = "balance")
    private double balance;

    @ColumnInfo(name = "icon")
    private String icon;

    public WalletEntity(@NonNull String id, @NonNull String userId, @NonNull String name, @NonNull String type, double balance, String icon) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.balance = balance;
        this.icon = icon;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull
    public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    @NonNull
    public String getType() { return type; }
    public void setType(@NonNull String type) { this.type = type; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
