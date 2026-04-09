package com.example.expensemanagement;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Lưu trữ danh sách các ID cần sync lên Firestore khi có mạng.
 * Dùng SharedPreferences — không cần thay đổi DB schema.
 *
 * Format key:
 *   pending_transactions  → Set<String> transactionId
 *   pending_budgets       → Set<String> budgetId
 *   deleted_transactions  → Set<String> transactionId
 *   deleted_budgets       → Set<String> budgetId
 */
public class PendingSyncStore {

    private static final String PREFS_NAME         = "pending_sync";
    private static final String KEY_TRANSACTIONS   = "pending_transactions";
    private static final String KEY_BUDGETS        = "pending_budgets";
    private static final String KEY_DEL_TRANS      = "deleted_transactions";
    private static final String KEY_DEL_BUDGETS    = "deleted_budgets";

    private final SharedPreferences prefs;

    public PendingSyncStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Thêm vào hàng chờ ────────────────────────────────────────────

    public void addTransaction(String id)        { addToSet(KEY_TRANSACTIONS, id); }
    public void addBudget(String id)             { addToSet(KEY_BUDGETS, id); }
    public void addDeletedTransaction(String id) { addToSet(KEY_DEL_TRANS, id); }
    public void addDeletedBudget(String id)      { addToSet(KEY_DEL_BUDGETS, id); }

    // ── Lấy danh sách chờ ───────────────────────────────────────────

    public Set<String> getPendingTransactions()   { return getSet(KEY_TRANSACTIONS); }
    public Set<String> getPendingBudgets()        { return getSet(KEY_BUDGETS); }
    public Set<String> getDeletedTransactions()   { return getSet(KEY_DEL_TRANS); }
    public Set<String> getDeletedBudgets()        { return getSet(KEY_DEL_BUDGETS); }

    // ── Xóa sau khi sync xong ───────────────────────────────────────

    public void clearAll() {
        prefs.edit().clear().apply();
    }

    public void clearTransactions()        { prefs.edit().remove(KEY_TRANSACTIONS).apply(); }
    public void clearBudgets()             { prefs.edit().remove(KEY_BUDGETS).apply(); }
    public void clearDeletedTransactions() { prefs.edit().remove(KEY_DEL_TRANS).apply(); }
    public void clearDeletedBudgets()      { prefs.edit().remove(KEY_DEL_BUDGETS).apply(); }

    public boolean hasPending() {
        return !getPendingTransactions().isEmpty()
                || !getPendingBudgets().isEmpty()
                || !getDeletedTransactions().isEmpty()
                || !getDeletedBudgets().isEmpty();
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void addToSet(String key, String value) {
        Set<String> set = new HashSet<>(getSet(key));
        set.add(value);
        prefs.edit().putStringSet(key, set).apply();
    }

    private Set<String> getSet(String key) {
        return prefs.getStringSet(key, new HashSet<>());
    }
}