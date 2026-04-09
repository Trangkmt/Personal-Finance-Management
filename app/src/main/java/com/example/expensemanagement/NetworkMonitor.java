package com.example.expensemanagement;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.expensemanagement.model.BudgetEntity;
import com.example.expensemanagement.model.TransactionEntity;
import com.example.expensemanagement.model.WalletEntity;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Set;

/**
 * Lắng nghe trạng thái mạng.
 * Khi mạng bật lên → tự động đẩy tất cả data đang chờ trong PendingSyncStore lên Firestore.
 *
 * Dùng trong MainActivity: gọi register() ở onResume, unregister() ở onPause.
 */
public class NetworkMonitor {

    private static final String TAG = "NetworkMonitor";

    private final ConnectivityManager cm;
    private final ConnectivityManager.NetworkCallback callback;
    private boolean registered = false;

    public NetworkMonitor(Context context) {
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        AppDatabase db               = AppDatabase.getInstance(context);
        AppDao dao                   = db.appDao();
        FirestoreSyncHelper sync     = new FirestoreSyncHelper(dao);
        PendingSyncStore store       = new PendingSyncStore(context);

        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                // Mạng vừa có — flush hàng chờ
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid == null || !store.hasPending()) return;

                Log.d(TAG, "Mạng có lại — bắt đầu flush pending sync...");
                flushPending(dao, sync, store, uid);
            }
        };
    }

    public void register() {
        if (registered || cm == null) return;
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        cm.registerNetworkCallback(request, callback);
        registered = true;
    }

    public void unregister() {
        if (!registered || cm == null) return;
        cm.unregisterNetworkCallback(callback);
        registered = false;
    }

    // ── Flush toàn bộ hàng chờ ───────────────────────────────────────

    private void flushPending(AppDao dao, FirestoreSyncHelper sync,
                              PendingSyncStore store, String uid) {
        AppDatabase.executor.execute(() -> {

            // 1. Transactions chưa sync
            Set<String> pendingTrans = store.getPendingTransactions();
            for (String id : pendingTrans) {
                TransactionEntity t = dao.getTransactionById(id);
                if (t != null) sync.pushTransaction(t);
            }
            if (!pendingTrans.isEmpty()) store.clearTransactions();

            // 2. Wallets chưa sync
            Set<String> pendingWallets = store.getPendingWallets();
            for (String id : pendingWallets) {
                WalletEntity w = dao.getWalletById(id);
                if (w != null) sync.pushWallet(w);
            }
            if (!pendingWallets.isEmpty()) store.clearWallets();

            // 3. Budgets chưa sync
            Set<String> pendingBudgets = store.getPendingBudgets();
            for (String id : pendingBudgets) {
                BudgetEntity b = dao.getBudgetById(id);
                if (b != null) sync.pushBudget(b);
            }
            if (!pendingBudgets.isEmpty()) store.clearBudgets();

            // 4. Deleted transactions
            Set<String> delTrans = store.getDeletedTransactions();
            for (String id : delTrans) sync.pushDeleteTransaction(id);
            if (!delTrans.isEmpty()) store.clearDeletedTransactions();

            // 5. Deleted wallets
            Set<String> delWallets = store.getDeletedWallets();
            for (String id : delWallets) sync.pushDeleteWallet(id);
            if (!delWallets.isEmpty()) store.clearDeletedWallets();

            // 6. Deleted budgets
            Set<String> delBudgets = store.getDeletedBudgets();
            for (String id : delBudgets) sync.pushDeleteBudget(id);
            if (!delBudgets.isEmpty()) store.clearDeletedBudgets();

            Log.d(TAG, "Flush pending sync hoàn tất");
        });
    }
}