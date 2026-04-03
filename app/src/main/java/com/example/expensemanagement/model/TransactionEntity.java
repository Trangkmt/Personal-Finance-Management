package com.example.expensemanagement.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class TransactionEntity {

    @PrimaryKey @NonNull @ColumnInfo(name = "transaction_id") public String transactionId;
    @NonNull @ColumnInfo(name = "user_id")                    public String userId;
    @NonNull @ColumnInfo(name = "category_id")                public String categoryId; // Lưu tên danh mục trực tiếp
    @ColumnInfo(name = "amount")                              public double amount;
    @NonNull @ColumnInfo(name = "type")                       public String type; // "income" or "expense"
    @Nullable @ColumnInfo(name = "note")                      public String note;
    @NonNull @ColumnInfo(name = "transaction_date")           public String transactionDate; // yyyy-MM-dd
    @NonNull @ColumnInfo(name = "created_at")                 public String createdAt;
    @NonNull @ColumnInfo(name = "updated_at")                 public String updatedAt;

    public TransactionEntity(@NonNull String transactionId, @NonNull String userId,
                             @NonNull String categoryId, double amount,
                             @NonNull String type, @Nullable String note,
                             @NonNull String transactionDate, @NonNull String createdAt,
                             @NonNull String updatedAt) {
        this.transactionId   = transactionId;
        this.userId          = userId;
        this.categoryId      = categoryId;
        this.amount          = amount;
        this.type            = type;
        this.note            = note;
        this.transactionDate = transactionDate;
        this.createdAt       = createdAt;
        this.updatedAt       = updatedAt;
    }
}