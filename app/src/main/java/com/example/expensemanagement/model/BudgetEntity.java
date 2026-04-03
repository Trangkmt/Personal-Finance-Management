package com.example.expensemanagement.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class BudgetEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "budget_id")
    public String budgetId;

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @Nullable
    @ColumnInfo(name = "category_id") // Null if it's an overall budget
    public String categoryId;

    @ColumnInfo(name = "amount")
    public double amount;

    @NonNull
    @ColumnInfo(name = "period") // "monthly", "weekly", etc.
    public String period;

    @NonNull
    @ColumnInfo(name = "start_date")
    public String startDate;

    @NonNull
    @ColumnInfo(name = "end_date")
    public String endDate;

    @NonNull
    @ColumnInfo(name = "created_at")
    public String createdAt;

    public BudgetEntity(@NonNull String budgetId, @NonNull String userId, @Nullable String categoryId,
                        double amount, @NonNull String period, @NonNull String startDate,
                        @NonNull String endDate, @NonNull String createdAt) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.period = period;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
    }
}