package com.example.expensemanagement;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.expensemanagement.model.BudgetEntity;
import com.example.expensemanagement.model.TransactionEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionViewModel extends AndroidViewModel {
    private final AppDao appDao;
    private final Application application;
    private final FirestoreSyncHelper syncHelper;
    private final PendingSyncStore pendingStore;
    private ListenerRegistration realtimeListener; // Lắng nghe thay đổi real-time từ Firestore

    private final MutableLiveData<FilterConfig> filterConfig = new MutableLiveData<>(new FilterConfig(FilterType.ALL));

    public enum FilterType { ALL, DAY, WEEK, MONTH, CUSTOM }

    public static class FilterConfig {
        public FilterType type;
        public String startDate;
        public String endDate;
        public FilterConfig(FilterType type) { this.type = type; }
        public FilterConfig(FilterType type, String startDate, String endDate) {
            this.type = type; this.startDate = startDate; this.endDate = endDate;
        }
    }

    public TransactionViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        appDao       = AppDatabase.getInstance(application).appDao();
        syncHelper   = new FirestoreSyncHelper(appDao);
        pendingStore = new PendingSyncStore(application);
    }

    public void setFilter(FilterType type) { filterConfig.setValue(new FilterConfig(type)); }

    public void setCustomFilter(String start, String end) {
        filterConfig.setValue(new FilterConfig(FilterType.CUSTOM, start, end));
    }

    private String getUserId() { return FirebaseAuth.getInstance().getUid(); }

    public LiveData<List<TransactionEntity>> getFilteredTransactions() {
        return Transformations.switchMap(filterConfig, config -> {
            String userId = getUserId();
            if (userId == null) return new MutableLiveData<>();
            if (config.type == FilterType.ALL) return appDao.getAllTransactions(userId);
            String start, end;
            if (config.type == FilterType.CUSTOM) {
                start = config.startDate; end = config.endDate;
            } else {
                String[] dates = getStartEndDates(config.type);
                start = dates[0]; end = dates[1];
            }
            return appDao.getTransactionsBetweenDates(userId, start, end);
        });
    }

    private String[] getStartEndDates(FilterType filter) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String end = df.format(cal.getTime()), start;
        if (filter == FilterType.DAY) { start = end; }
        else if (filter == FilterType.WEEK) { cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek()); start = df.format(cal.getTime()); }
        else { cal.set(Calendar.DAY_OF_MONTH, 1); start = df.format(cal.getTime()); }
        return new String[]{start, end};
    }

    /** Thêm transaction: lưu Room + push Firestore (hoặc lưu pending nếu offline) */
    public void insert(TransactionEntity transaction) {
        AppDatabase.executor.execute(() -> {
            appDao.insertTransaction(transaction);
            if ("expense".equalsIgnoreCase(transaction.type)) checkBudgetExceeded(transaction);
        });
        if (isNetworkAvailable()) {
            syncHelper.pushTransaction(transaction);
        } else {
            pendingStore.addTransaction(transaction.transactionId);
        }
    }

    /** Sửa transaction */
    public void update(TransactionEntity transaction) {
        AppDatabase.executor.execute(() -> appDao.updateTransaction(transaction));
        if (isNetworkAvailable()) {
            syncHelper.pushTransaction(transaction);
        } else {
            pendingStore.addTransaction(transaction.transactionId);
        }
    }

    /** Xóa transaction */
    public void delete(TransactionEntity transaction) {
        AppDatabase.executor.execute(() -> appDao.deleteTransaction(transaction));
        if (isNetworkAvailable()) {
            syncHelper.pushDeleteTransaction(transaction.transactionId);
        } else {
            pendingStore.addDeletedTransaction(transaction.transactionId);
        }
    }

    public LiveData<List<TransactionEntity>> getAllTransactions() {
        String userId = getUserId();
        if (userId == null) return new MutableLiveData<>();
        return appDao.getAllTransactions(userId);
    }

    /**
     * Bắt đầu lắng nghe thay đổi real-time từ Firestore.
     * Gọi trong Fragment/Activity khi start, dừng khi stop.
     */
    public void startRealtimeSync() {
        String userId = getUserId();
        if (userId == null) return;

        realtimeListener = FirebaseFirestore.getInstance()
                .collection("transactions")
                .whereEqualTo("user_id", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    AppDatabase.executor.execute(() -> {
                        for (var doc : snapshots.getDocuments()) {
                            Boolean deleted = doc.getBoolean("deleted");
                            if (Boolean.TRUE.equals(deleted)) {
                                String id = doc.getString("transaction_id");
                                if (id != null) {
                                    TransactionEntity local = appDao.getTransactionById(id);
                                    if (local != null) appDao.deleteTransaction(local);
                                }
                                continue;
                            }
                            // Parse và upsert vào Room
                            try {
                                String id      = doc.getString("transaction_id");
                                String uid     = doc.getString("user_id");
                                String catId   = doc.getString("category_id");
                                Double amount  = doc.getDouble("amount");
                                String type    = doc.getString("type");
                                String note    = doc.getString("note");
                                String date    = doc.getString("transaction_date");
                                String created = doc.getString("created_at");
                                String updated = doc.getString("updated_at");
                                if (id == null || uid == null || catId == null || amount == null
                                        || type == null || date == null || created == null || updated == null) continue;
                                appDao.insertTransaction(new TransactionEntity(id, uid, catId, amount, type, note, date, created, updated));
                            } catch (Exception ignored) {}
                        }
                    });
                });
    }

    public void stopRealtimeSync() {
        if (realtimeListener != null) {
            realtimeListener.remove();
            realtimeListener = null;
        }
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

    private void checkBudgetExceeded(TransactionEntity transaction) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        BudgetEntity budget = appDao.getActiveBudget(transaction.userId, transaction.categoryId, today);
        if (budget != null) {
            double spent = appDao.getSpentByCategory(transaction.userId, budget.categoryId, budget.startDate, budget.endDate);
            if (spent > budget.amount) {
                NotificationHelper.showNotification(application, "Cảnh báo vượt ngân sách!",
                        "Bạn đã chi tiêu " + String.format(Locale.getDefault(), "%,.0f", spent) +
                                " đ cho mục " + (budget.categoryId == null ? "Tổng thể" : budget.categoryId) +
                                ", vượt mức ngân sách " + String.format(Locale.getDefault(), "%,.0f", budget.amount) + " đ.");
            }
        }
    }
}