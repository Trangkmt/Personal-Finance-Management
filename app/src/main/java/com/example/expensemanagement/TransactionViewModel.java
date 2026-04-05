package com.example.expensemanagement;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.expensemanagement.model.BudgetEntity;
import com.example.expensemanagement.model.TransactionEntity;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionViewModel extends AndroidViewModel {
    private final AppDao appDao;
    private final Application application;
    
    private final MutableLiveData<FilterConfig> filterConfig = new MutableLiveData<>(new FilterConfig(FilterType.ALL));

    public enum FilterType { ALL, DAY, WEEK, MONTH, CUSTOM }

    public static class FilterConfig {
        public FilterType type;
        public String startDate;
        public String endDate;

        public FilterConfig(FilterType type) {
            this.type = type;
        }

        public FilterConfig(FilterType type, String startDate, String endDate) {
            this.type = type;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    public TransactionViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        appDao = AppDatabase.getInstance(application).appDao();
    }

    public void setFilter(FilterType type) {
        filterConfig.setValue(new FilterConfig(type));
    }

    public void setCustomFilter(String start, String end) {
        filterConfig.setValue(new FilterConfig(FilterType.CUSTOM, start, end));
    }

    private String getUserId() {
        return FirebaseAuth.getInstance().getUid();
    }

    public LiveData<List<TransactionEntity>> getFilteredTransactions() {
        return Transformations.switchMap(filterConfig, config -> {
            String userId = getUserId();
            if (userId == null) return new MutableLiveData<>();

            if (config.type == FilterType.ALL) {
                return appDao.getAllTransactions(userId);
            }
            
            String start, end;
            if (config.type == FilterType.CUSTOM) {
                start = config.startDate;
                end = config.endDate;
            } else {
                String[] dates = getStartEndDates(config.type);
                start = dates[0];
                end = dates[1];
            }
            return appDao.getTransactionsBetweenDates(userId, start, end);
        });
    }

    private String[] getStartEndDates(FilterType filter) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String end = df.format(cal.getTime());
        String start;

        if (filter == FilterType.DAY) {
            start = end;
        } else if (filter == FilterType.WEEK) {
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            start = df.format(cal.getTime());
        } else { // MONTH
            cal.set(Calendar.DAY_OF_MONTH, 1);
            start = df.format(cal.getTime());
        }
        return new String[]{start, end};
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
        // getActiveBudget đã có userId ở bước trước
        BudgetEntity budget = appDao.getActiveBudget(transaction.userId, transaction.categoryId, today);
        
        if (budget != null) {
            // Sửa: Thêm userId vào hàm getSpentByCategory
            double spent = appDao.getSpentByCategory(transaction.userId, budget.categoryId, budget.startDate, budget.endDate);
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
        String userId = getUserId();
        if (userId == null) return new MutableLiveData<>();
        return appDao.getAllTransactions(userId);
    }
}
