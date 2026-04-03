package com.example.expensemanagement;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.expensemanagement.model.BudgetEntity;
import com.example.expensemanagement.model.TransactionEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionViewModel extends AndroidViewModel {
    private final AppDao appDao;
    private final Application application;

    public TransactionViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        appDao = AppDatabase.getInstance(application).appDao();
    }

    public void insert(TransactionEntity transaction) {
        AppDatabase.executor.execute(() -> {
            appDao.insertTransaction(transaction);
            if ("expense".equalsIgnoreCase(transaction.type)) {
                checkBudgetExceeded(transaction);
            }
        });
    }

    private void checkBudgetExceeded(TransactionEntity transaction) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        BudgetEntity budget = appDao.getActiveBudget(transaction.userId, transaction.categoryId, today);
        
        if (budget != null) {
            double spent = appDao.getSpentByCategory(budget.categoryId, budget.startDate, budget.endDate);
            if (spent > budget.amount) {
                NotificationHelper.showNotification(
                        application,
                        "Cảnh báo vượt ngân sách!",
                        "Bạn đã chi tiêu " + String.format(Locale.getDefault(), "%,.0f", spent) + 
                        " đ cho mục " + (budget.categoryId == null ? "Tổng thể" : budget.categoryId) + 
                        ", vượt mức ngân sách " + String.format(Locale.getDefault(), "%,.0f", budget.amount) + " đ."
                );
            }
        }
    }

    public void update(TransactionEntity transaction) {
        AppDatabase.executor.execute(() -> appDao.updateTransaction(transaction));
    }

    public void delete(TransactionEntity transaction) {
        AppDatabase.executor.execute(() -> appDao.deleteTransaction(transaction));
    }

    public LiveData<List<TransactionEntity>> getAllTransactions() {
        return appDao.getAllTransactions();
    }
}