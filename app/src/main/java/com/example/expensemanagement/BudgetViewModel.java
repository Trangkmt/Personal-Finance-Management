package com.example.expensemanagement;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.expensemanagement.model.BudgetEntity;

import java.util.List;

public class BudgetViewModel extends AndroidViewModel {
    private final AppDao appDao;
    private final FirestoreSyncHelper syncHelper;
    private final PendingSyncStore pendingStore;
    private final Application application;

    public BudgetViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        appDao       = AppDatabase.getInstance(application).appDao();
        syncHelper   = new FirestoreSyncHelper(appDao);
        pendingStore = new PendingSyncStore(application);
    }

    public void insert(BudgetEntity budget) {
        AppDatabase.executor.execute(() -> appDao.insertBudget(budget));
        if (isNetworkAvailable()) {
            syncHelper.pushBudget(budget);
        } else {
            pendingStore.addBudget(budget.budgetId);
        }
    }

    public void update(BudgetEntity budget) {
        AppDatabase.executor.execute(() -> appDao.updateBudget(budget));
        if (isNetworkAvailable()) {
            syncHelper.pushBudget(budget);
        } else {
            pendingStore.addBudget(budget.budgetId);
        }
    }

    public void delete(BudgetEntity budget) {
        AppDatabase.executor.execute(() -> appDao.deleteBudget(budget));
        if (isNetworkAvailable()) {
            syncHelper.pushDeleteBudget(budget.budgetId);
        } else {
            pendingStore.addDeletedBudget(budget.budgetId);
        }
    }

    public LiveData<List<BudgetEntity>> getBudgets(String userId) {
        return appDao.getBudgets(userId);
    }

    public double getSpentAmount(String userId, String categoryId, String startDate, String endDate) {
        if (categoryId == null) return appDao.getTotalSpent(userId, startDate, endDate);
        else return appDao.getSpentByCategory(userId, categoryId, startDate, endDate);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }
}