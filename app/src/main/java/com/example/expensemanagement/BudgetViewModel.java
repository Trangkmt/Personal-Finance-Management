package com.example.expensemanagement;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.expensemanagement.model.BudgetEntity;

import java.util.List;

public class BudgetViewModel extends AndroidViewModel {
    private final AppDao appDao;
    private final FirestoreSyncHelper syncHelper;

    public BudgetViewModel(@NonNull Application application) {
        super(application);
        appDao = AppDatabase.getInstance(application).appDao();
        syncHelper = new FirestoreSyncHelper(appDao);
    }

    public void insert(BudgetEntity budget) {
        AppDatabase.executor.execute(() -> {
            appDao.insertBudget(budget);
            syncHelper.pushBudget(budget);
        });
    }

    public void update(BudgetEntity budget) {
        AppDatabase.executor.execute(() -> {
            appDao.updateBudget(budget);
            syncHelper.pushBudget(budget);
        });
    }

    public void delete(BudgetEntity budget) {
        AppDatabase.executor.execute(() -> {
            appDao.deleteBudget(budget);
            syncHelper.pushDeleteBudget(budget.budgetId);
        });
    }

    public LiveData<List<BudgetEntity>> getBudgets(String userId) {
        return appDao.getBudgets(userId);
    }

    public double getSpentAmount(String userId, String categoryId, String startDate, String endDate) {
        if (categoryId == null) {
            return appDao.getTotalSpent(userId, startDate, endDate);
        } else {
            return appDao.getSpentByCategory(userId, categoryId, startDate, endDate);
        }
    }
}