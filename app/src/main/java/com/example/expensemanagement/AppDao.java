package com.example.expensemanagement;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Transaction;

import com.example.expensemanagement.model.BudgetEntity;
import com.example.expensemanagement.model.CategoryEntity;
import com.example.expensemanagement.model.TransactionEntity;
import com.example.expensemanagement.model.UserEntity;
import com.example.expensemanagement.model.UserSettingsEntity;
import com.example.expensemanagement.model.WalletEntity;

import java.util.List;

@Dao
public interface AppDao {

    // Users
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(UserEntity user);

    @Update
    void updateUser(UserEntity user);

    @Query("SELECT * FROM users WHERE firebase_uid = :firebaseUid LIMIT 1")
    UserEntity getUserByFirebaseUid(String firebaseUid);

    @Query("UPDATE users SET last_login_at = :time, updated_at = :time WHERE firebase_uid = :firebaseUid")
    void updateLastLogin(String firebaseUid, String time);

    // User Settings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSettings(UserSettingsEntity settings);

    @Query("SELECT * FROM user_settings WHERE user_id = :userId LIMIT 1")
    LiveData<UserSettingsEntity> getSettingsByUserId(String userId);

    // Categories
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAllCategories(List<CategoryEntity> categories);

    @Query("SELECT * FROM categories WHERE type = :type AND (user_id IS NULL OR user_id = :userId)")
    List<CategoryEntity> getCategoriesByType(String type, String userId);

    // Transactions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTransaction(TransactionEntity transaction);

    @Update
    void updateTransaction(TransactionEntity transaction);

    @Delete
    void deleteTransaction(TransactionEntity transaction);

    @Query("SELECT * FROM transactions WHERE user_id = :userId ORDER BY transaction_date DESC, created_at DESC")
    LiveData<List<TransactionEntity>> getAllTransactions(String userId);

    @Query("SELECT * FROM transactions WHERE user_id = :userId AND transaction_date BETWEEN :startDate AND :endDate ORDER BY transaction_date DESC")
    LiveData<List<TransactionEntity>> getTransactionsBetweenDates(String userId, String startDate, String endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE user_id = :userId AND type = 'expense' AND transaction_date BETWEEN :startDate AND :endDate")
    double getTotalSpent(String userId, String startDate, String endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE user_id = :userId AND type = 'expense' AND category_id = :categoryId AND transaction_date BETWEEN :startDate AND :endDate")
    double getSpentByCategory(String userId, String categoryId, String startDate, String endDate);

    // Budgets
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBudget(BudgetEntity budget);

    @Update
    void updateBudget(BudgetEntity budget);

    @Delete
    void deleteBudget(BudgetEntity budget);

    @Query("SELECT * FROM budgets WHERE user_id = :userId")
    LiveData<List<BudgetEntity>> getBudgets(String userId);

    @Query("SELECT * FROM budgets WHERE user_id = :userId AND (category_id = :categoryId OR category_id IS NULL) AND :date BETWEEN start_date AND end_date LIMIT 1")
    BudgetEntity getActiveBudget(String userId, String categoryId, String date);

    // Wallets
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWallet(WalletEntity wallet);

    @Update
    void updateWallet(WalletEntity wallet);

    @Delete
    void deleteWallet(WalletEntity wallet);

    @Query("SELECT * FROM wallets WHERE user_id = :userId")
    LiveData<List<WalletEntity>> getWalletsByUserId(String userId);

    @Transaction
    default void transferMoney(String fromId, String toId, double amount) {
        // This is a simplified version for Room
        // In a real scenario, you'd fetch the wallets, update their balance, and then update them in the DB
    }
}