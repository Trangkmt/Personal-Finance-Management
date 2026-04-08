package com.example.expensemanagement;

import android.util.Log;

import com.example.expensemanagement.model.BudgetEntity;
import com.example.expensemanagement.model.CategoryEntity;
import com.example.expensemanagement.model.TransactionEntity;
import com.example.expensemanagement.model.UserSettingsEntity;
import com.example.expensemanagement.model.WalletEntity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper đồng bộ 2 chiều Room ↔ Firestore cho toàn bộ data của user:
 *   - transactions
 *   - wallets
 *   - budgets
 *   - categories (chỉ category tự tạo của user, không sync system category)
 *   - user_settings
 *
 * PUSH: gọi ngay sau mỗi insert/update/delete
 * PULL: gọi khi user login — kéo toàn bộ về Room (merge, không xóa local trừ soft-delete)
 */
public class FirestoreSyncHelper {

    private static final String TAG = "FirestoreSyncHelper";

    // Tên collections trên Firestore
    private static final String COL_TRANSACTIONS   = "transactions";
    private static final String COL_WALLETS        = "wallets";
    private static final String COL_BUDGETS        = "budgets";
    private static final String COL_CATEGORIES     = "categories";
    private static final String COL_USER_SETTINGS  = "user_settings";

    private final FirebaseFirestore db;
    private final AppDao dao;

    public FirestoreSyncHelper(AppDao dao) {
        this.db  = FirebaseFirestore.getInstance();
        this.dao = dao;
    }

    // ══════════════════════════════════════════════════════════════════
    // PUSH — Transaction
    // ══════════════════════════════════════════════════════════════════

    public void pushTransaction(TransactionEntity t) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("transaction_id",   t.transactionId);
        doc.put("user_id",          t.userId);
        doc.put("category_id",      t.categoryId);
        doc.put("amount",           t.amount);
        doc.put("type",             t.type);
        doc.put("note",             t.note);
        doc.put("transaction_date", t.transactionDate);
        doc.put("created_at",       t.createdAt);
        doc.put("updated_at",       t.updatedAt);
        doc.put("deleted",          false);

        db.collection(COL_TRANSACTIONS).document(t.transactionId)
                .set(doc, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "pushTransaction FAIL: " + t.transactionId, e));
    }

    public void pushDeleteTransaction(String transactionId) {
        softDelete(COL_TRANSACTIONS, transactionId);
    }

    // ══════════════════════════════════════════════════════════════════
    // PUSH — Wallet
    // ══════════════════════════════════════════════════════════════════

    public void pushWallet(WalletEntity w) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("wallet_id", w.getId());
        doc.put("user_id",   w.getUserId());
        doc.put("name",      w.getName());
        doc.put("type",      w.getType());
        doc.put("balance",   w.getBalance());
        doc.put("icon",      w.getIcon());
        doc.put("deleted",   false);

        db.collection(COL_WALLETS).document(w.getId())
                .set(doc, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "pushWallet FAIL: " + w.getId(), e));
    }

    public void pushDeleteWallet(String walletId) {
        softDelete(COL_WALLETS, walletId);
    }

    // ══════════════════════════════════════════════════════════════════
    // PUSH — Budget
    // ══════════════════════════════════════════════════════════════════

    public void pushBudget(BudgetEntity b) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("budget_id",   b.budgetId);
        doc.put("user_id",     b.userId);
        doc.put("category_id", b.categoryId);
        doc.put("amount",      b.amount);
        doc.put("period",      b.period);
        doc.put("start_date",  b.startDate);
        doc.put("end_date",    b.endDate);
        doc.put("created_at",  b.createdAt);
        doc.put("deleted",     false);

        db.collection(COL_BUDGETS).document(b.budgetId)
                .set(doc, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "pushBudget FAIL: " + b.budgetId, e));
    }

    public void pushDeleteBudget(String budgetId) {
        softDelete(COL_BUDGETS, budgetId);
    }

    // ══════════════════════════════════════════════════════════════════
    // PUSH — Category (chỉ category do user tự tạo, is_system = 0)
    // ══════════════════════════════════════════════════════════════════

    public void pushCategory(CategoryEntity c) {
        if (c.isSystem == 1) return; // Không sync category hệ thống

        Map<String, Object> doc = new HashMap<>();
        doc.put("category_id", c.categoryId);
        doc.put("user_id",     c.userId);
        doc.put("parent_id",   c.parentId);
        doc.put("name",        c.name);
        doc.put("type",        c.type);
        doc.put("icon",        c.icon);
        doc.put("color",       c.color);
        doc.put("is_system",   c.isSystem);
        doc.put("sort_order",  c.sortOrder);
        doc.put("created_at",  c.createdAt);
        doc.put("deleted",     false);

        db.collection(COL_CATEGORIES).document(c.categoryId)
                .set(doc, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "pushCategory FAIL: " + c.categoryId, e));
    }

    public void pushDeleteCategory(String categoryId) {
        softDelete(COL_CATEGORIES, categoryId);
    }

    // ══════════════════════════════════════════════════════════════════
    // PUSH — UserSettings
    // ══════════════════════════════════════════════════════════════════

    public void pushUserSettings(UserSettingsEntity s) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("setting_id",             s.settingId);
        doc.put("user_id",                s.userId);
        doc.put("theme",                  s.theme);
        doc.put("daily_reminder_enabled", s.dailyReminderEnabled);
        doc.put("daily_reminder_time",    s.dailyReminderTime);
        doc.put("budget_alert_enabled",   s.budgetAlertEnabled);
        doc.put("sms_reading_enabled",    s.smsReadingEnabled);
        doc.put("biometric_enabled",      s.biometricEnabled);
        doc.put("app_lock_enabled",       s.appLockEnabled);
        doc.put("app_lock_timeout",       s.appLockTimeout);
        doc.put("gps_suggestion_enabled", s.gpsSuggestionEnabled);
        doc.put("first_day_of_week",      s.firstDayOfWeek);
        doc.put("date_format",            s.dateFormat);
        doc.put("updated_at",             s.updatedAt);

        // Document id = userId vì mỗi user chỉ có 1 settings
        db.collection(COL_USER_SETTINGS).document(s.userId)
                .set(doc, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "pushUserSettings FAIL: " + s.userId, e));
    }

    // ══════════════════════════════════════════════════════════════════
    // PULL — Kéo toàn bộ data của user từ Firestore về Room (khi login)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Kéo toàn bộ 5 collection về Room.
     * Callback onDone() được gọi trên main thread sau khi tất cả xong.
     */
    public void pullAllForUser(String userId, OnSyncDoneListener listener) {
        // Dùng AtomicInteger đếm số collection đã xong (5 tổng cộng)
        AtomicInteger remaining = new AtomicInteger(5);

        pullTransactions(userId, remaining, listener);
        pullWallets(userId, remaining, listener);
        pullBudgets(userId, remaining, listener);
        pullCategories(userId, remaining, listener);
        pullUserSettings(userId, remaining, listener);
    }

    // ── Pull Transactions ────────────────────────────────────────────

    private void pullTransactions(String userId, AtomicInteger remaining, OnSyncDoneListener listener) {
        db.collection(COL_TRANSACTIONS)
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    AppDatabase.executor.execute(() -> {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            if (isDeleted(doc)) {
                                String id = doc.getString("transaction_id");
                                if (id != null) {
                                    TransactionEntity local = dao.getTransactionById(id);
                                    if (local != null) dao.deleteTransaction(local);
                                }
                                continue;
                            }
                            TransactionEntity e = parseTransaction(doc);
                            if (e != null) dao.insertTransaction(e);
                        }
                        Log.d(TAG, "Pull transactions done: " + snap.size());
                        checkDone(remaining, listener);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Pull transactions FAIL", e);
                    checkDone(remaining, listener);
                });
    }

    // ── Pull Wallets ─────────────────────────────────────────────────

    private void pullWallets(String userId, AtomicInteger remaining, OnSyncDoneListener listener) {
        db.collection(COL_WALLETS)
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    AppDatabase.executor.execute(() -> {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            if (isDeleted(doc)) {
                                String id = doc.getString("wallet_id");
                                if (id != null) {
                                    WalletEntity local = dao.getWalletById(id);
                                    if (local != null) dao.deleteWallet(local);
                                }
                                continue;
                            }
                            WalletEntity w = parseWallet(doc);
                            if (w != null) dao.insertWallet(w);
                        }
                        Log.d(TAG, "Pull wallets done: " + snap.size());
                        checkDone(remaining, listener);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Pull wallets FAIL", e);
                    checkDone(remaining, listener);
                });
    }

    // ── Pull Budgets ─────────────────────────────────────────────────

    private void pullBudgets(String userId, AtomicInteger remaining, OnSyncDoneListener listener) {
        db.collection(COL_BUDGETS)
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    AppDatabase.executor.execute(() -> {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            if (isDeleted(doc)) {
                                String id = doc.getString("budget_id");
                                if (id != null) {
                                    BudgetEntity local = dao.getBudgetById(id);
                                    if (local != null) dao.deleteBudget(local);
                                }
                                continue;
                            }
                            BudgetEntity b = parseBudget(doc);
                            if (b != null) dao.insertBudget(b);
                        }
                        Log.d(TAG, "Pull budgets done: " + snap.size());
                        checkDone(remaining, listener);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Pull budgets FAIL", e);
                    checkDone(remaining, listener);
                });
    }

    // ── Pull Categories (chỉ category user tự tạo) ──────────────────

    private void pullCategories(String userId, AtomicInteger remaining, OnSyncDoneListener listener) {
        db.collection(COL_CATEGORIES)
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    AppDatabase.executor.execute(() -> {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            if (isDeleted(doc)) continue; // Bỏ qua, không xóa category
                            CategoryEntity c = parseCategory(doc);
                            if (c != null) dao.insertCategory(c);
                        }
                        Log.d(TAG, "Pull categories done: " + snap.size());
                        checkDone(remaining, listener);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Pull categories FAIL", e);
                    checkDone(remaining, listener);
                });
    }

    // ── Pull UserSettings ────────────────────────────────────────────

    private void pullUserSettings(String userId, AtomicInteger remaining, OnSyncDoneListener listener) {
        db.collection(COL_USER_SETTINGS).document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        AppDatabase.executor.execute(() -> {
                            UserSettingsEntity s = parseSettings(doc);
                            if (s != null) dao.insertSettings(s);
                            Log.d(TAG, "Pull user_settings done");
                            checkDone(remaining, listener);
                        });
                    } else {
                        checkDone(remaining, listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Pull user_settings FAIL", e);
                    checkDone(remaining, listener);
                });
    }

    // ══════════════════════════════════════════════════════════════════
    // Parse helpers
    // ══════════════════════════════════════════════════════════════════

    private TransactionEntity parseTransaction(DocumentSnapshot doc) {
        try {
            String id      = doc.getString("transaction_id");
            String userId  = doc.getString("user_id");
            String catId   = doc.getString("category_id");
            Double amount  = doc.getDouble("amount");
            String type    = doc.getString("type");
            String note    = doc.getString("note");
            String date    = doc.getString("transaction_date");
            String created = doc.getString("created_at");
            String updated = doc.getString("updated_at");
            if (id == null || userId == null || catId == null || amount == null
                    || type == null || date == null || created == null || updated == null) return null;
            return new TransactionEntity(id, userId, catId, amount, type, note, date, created, updated);
        } catch (Exception e) { Log.e(TAG, "parseTransaction error", e); return null; }
    }

    private WalletEntity parseWallet(DocumentSnapshot doc) {
        try {
            String id     = doc.getString("wallet_id");
            String userId = doc.getString("user_id");
            String name   = doc.getString("name");
            String type   = doc.getString("type");
            Double balance = doc.getDouble("balance");
            String icon   = doc.getString("icon");
            if (id == null || userId == null || name == null || type == null || balance == null) return null;
            return new WalletEntity(id, userId, name, type, balance, icon);
        } catch (Exception e) { Log.e(TAG, "parseWallet error", e); return null; }
    }

    private BudgetEntity parseBudget(DocumentSnapshot doc) {
        try {
            String id        = doc.getString("budget_id");
            String userId    = doc.getString("user_id");
            String catId     = doc.getString("category_id");
            Double amount    = doc.getDouble("amount");
            String period    = doc.getString("period");
            String startDate = doc.getString("start_date");
            String endDate   = doc.getString("end_date");
            String created   = doc.getString("created_at");
            if (id == null || userId == null || amount == null
                    || period == null || startDate == null || endDate == null || created == null) return null;
            return new BudgetEntity(id, userId, catId, amount, period, startDate, endDate, created);
        } catch (Exception e) { Log.e(TAG, "parseBudget error", e); return null; }
    }

    private CategoryEntity parseCategory(DocumentSnapshot doc) {
        try {
            String id       = doc.getString("category_id");
            String userId   = doc.getString("user_id");
            String parentId = doc.getString("parent_id");
            String name     = doc.getString("name");
            String type     = doc.getString("type");
            String icon     = doc.getString("icon");
            String color    = doc.getString("color");
            Long isSystem   = doc.getLong("is_system");
            Long sortOrder  = doc.getLong("sort_order");
            String created  = doc.getString("created_at");
            if (id == null || name == null || type == null || created == null) return null;
            return new CategoryEntity(id, userId, parentId, name, type, icon, color,
                    isSystem != null ? isSystem.intValue() : 0,
                    sortOrder != null ? sortOrder.intValue() : 0,
                    created);
        } catch (Exception e) { Log.e(TAG, "parseCategory error", e); return null; }
    }

    private UserSettingsEntity parseSettings(DocumentSnapshot doc) {
        try {
            String settingId = doc.getString("setting_id");
            String userId    = doc.getString("user_id");
            String updatedAt = doc.getString("updated_at");
            if (settingId == null || userId == null || updatedAt == null) return null;

            UserSettingsEntity s = new UserSettingsEntity(settingId, userId, updatedAt);
            if (doc.getString("theme") != null)               s.theme = doc.getString("theme");
            if (doc.getLong("daily_reminder_enabled") != null) s.dailyReminderEnabled = doc.getLong("daily_reminder_enabled").intValue();
            if (doc.getString("daily_reminder_time") != null)  s.dailyReminderTime = doc.getString("daily_reminder_time");
            if (doc.getLong("budget_alert_enabled") != null)   s.budgetAlertEnabled = doc.getLong("budget_alert_enabled").intValue();
            if (doc.getLong("sms_reading_enabled") != null)    s.smsReadingEnabled = doc.getLong("sms_reading_enabled").intValue();
            if (doc.getLong("biometric_enabled") != null)      s.biometricEnabled = doc.getLong("biometric_enabled").intValue();
            if (doc.getLong("app_lock_enabled") != null)       s.appLockEnabled = doc.getLong("app_lock_enabled").intValue();
            if (doc.getLong("app_lock_timeout") != null)       s.appLockTimeout = doc.getLong("app_lock_timeout").intValue();
            if (doc.getLong("gps_suggestion_enabled") != null) s.gpsSuggestionEnabled = doc.getLong("gps_suggestion_enabled").intValue();
            if (doc.getLong("first_day_of_week") != null)      s.firstDayOfWeek = doc.getLong("first_day_of_week").intValue();
            if (doc.getString("date_format") != null)          s.dateFormat = doc.getString("date_format");
            return s;
        } catch (Exception e) { Log.e(TAG, "parseSettings error", e); return null; }
    }

    // ══════════════════════════════════════════════════════════════════
    // Utilities
    // ══════════════════════════════════════════════════════════════════

    /** Soft-delete: đánh dấu deleted=true thay vì xóa hẳn document */
    private void softDelete(String collection, String docId) {
        Map<String, Object> update = new HashMap<>();
        update.put("deleted",    true);
        update.put("updated_at", now());
        db.collection(collection).document(docId)
                .set(update, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "softDelete FAIL: " + collection + "/" + docId, e));
    }

    private boolean isDeleted(DocumentSnapshot doc) {
        Boolean deleted = doc.getBoolean("deleted");
        return Boolean.TRUE.equals(deleted);
    }

    /** Đếm ngược — khi tất cả 5 collection xong thì gọi callback */
    private void checkDone(AtomicInteger remaining, OnSyncDoneListener listener) {
        if (remaining.decrementAndGet() == 0 && listener != null) {
            listener.onDone();
        }
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    // ══════════════════════════════════════════════════════════════════
    // Callback
    // ══════════════════════════════════════════════════════════════════

    public interface OnSyncDoneListener {
        void onDone();
    }
}