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

    /** Thêm budget: lưu Room (background) + push Firestore (main thread) */
    public void insert(BudgetEntity budget) {
        AppDatabase.executor.execute(() -> appDao.insertBudget(budget)); // Room: background
        syncHelper.pushBudget(budget);                                   // Firestore: main thread ✅
    }

    /** Sửa budget: cập nhật Room (background) + push Firestore (main thread) */
    public void update(BudgetEntity budget) {
        AppDatabase.executor.execute(() -> appDao.updateBudget(budget));
        syncHelper.pushBudget(budget);
    }

    /** Xóa budget: xóa Room (background) + soft-delete Firestore (main thread) */
    public void delete(BudgetEntity budget) {
        AppDatabase.executor.execute(() -> appDao.deleteBudget(budget));
        syncHelper.pushDeleteBudget(budget.budgetId);
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