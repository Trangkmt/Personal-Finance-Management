package com.example.expensemanagement.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagement.AppDatabase;
import com.example.expensemanagement.FirestoreSyncHelper;
import com.example.expensemanagement.PendingSyncStore;
import com.example.expensemanagement.R;
import com.example.expensemanagement.Adapters.WalletAdapter;
import com.example.expensemanagement.model.WalletEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class WalletFragment extends Fragment {

    private TextView tvName, tvEmail, tvTotal;
    private Button btnTransfer;
    private ImageButton btnAdd;
    private RecyclerView recyclerWallet;
    private WalletAdapter adapter;
    private FirebaseAuth mAuth;
    private AppDatabase db;
    private FirestoreSyncHelper syncHelper;
    private PendingSyncStore pendingStore;
    private List<WalletEntity> walletList = new ArrayList<>();
    private ListenerRegistration walletListener;

    public static WalletFragment newInstance() { return new WalletFragment(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        mAuth        = FirebaseAuth.getInstance();
        db           = AppDatabase.getInstance(getContext());
        syncHelper   = new FirestoreSyncHelper(db.appDao());
        pendingStore = new PendingSyncStore(requireContext());

        initViews(view);
        setupUser();
        setupRecyclerView();
        observeWallets();
        startWalletRealtimeSync();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (walletListener != null) walletListener.remove();
    }

    private void initViews(View view) {
        tvName  = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvTotal = view.findViewById(R.id.tvTotal);
        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) btnLogout.setVisibility(View.GONE);
        btnAdd      = view.findViewById(R.id.btnAdd);
        btnTransfer = view.findViewById(R.id.btnTransfer);
        recyclerWallet = view.findViewById(R.id.recyclerWallet);
        btnAdd.setOnClickListener(v -> showAddWalletDialog());
        btnTransfer.setOnClickListener(v -> showTransferDialog());
    }

    private void setupUser() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvName.setText(user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                    ? user.getDisplayName() : "Người dùng");
            tvEmail.setText(user.getEmail());
        }
    }

    private void setupRecyclerView() {
        adapter = new WalletAdapter();
        recyclerWallet.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerWallet.setAdapter(adapter);
        adapter.setOnWalletClickListener(wallet ->
                new AlertDialog.Builder(getContext())
                        .setTitle("Xóa ví")
                        .setMessage("Bạn có chắc muốn xóa ví '" + wallet.getName() + "'?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            AppDatabase.executor.execute(() -> db.appDao().deleteWallet(wallet));
                            if (isNetworkAvailable()) {
                                syncHelper.pushDeleteWallet(wallet.getId());
                            } else {
                                pendingStore.addDeletedWallet(wallet.getId());
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show()
        );
    }

    private void observeWallets() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        db.appDao().getWalletsByUserId(user.getUid()).observe(getViewLifecycleOwner(), wallets -> {
            walletList = wallets;
            adapter.setWallets(wallets);
            updateTotalBalance(wallets);
            View layoutTransfer = getView() != null ? getView().findViewById(R.id.layoutTransfer) : null;
            if (layoutTransfer != null)
                layoutTransfer.setVisibility(wallets.size() >= 2 ? View.VISIBLE : View.GONE);
        });
    }

    private void updateTotalBalance(List<WalletEntity> wallets) {
        double total = 0;
        for (WalletEntity w : wallets) total += w.getBalance();
        tvTotal.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(total));
    }

    private void showAddWalletDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_wallet, null);
        EditText etName    = dialogView.findViewById(R.id.etWalletName);
        EditText etBalance = dialogView.findViewById(R.id.etInitialBalance);
        Spinner spnType    = dialogView.findViewById(R.id.spnWalletType);
        Spinner spnIcon    = dialogView.findViewById(R.id.spnWalletIcon);
        String[] types = {"cash", "bank", "e-wallet"};
        String[] icons = {"💵", "🏦", "📱", "💳", "🏪", "💰"};

        new AlertDialog.Builder(getContext())
                .setTitle("Thêm ví mới").setView(dialogView)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String balanceStr = etBalance.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập tên ví", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double balance = balanceStr.isEmpty() ? 0 : Double.parseDouble(balanceStr);
                    String type = types[spnType.getSelectedItemPosition()];
                    String icon = icons[spnIcon.getSelectedItemPosition()];
                    WalletEntity wallet = new WalletEntity(
                            UUID.randomUUID().toString(), mAuth.getCurrentUser().getUid(), name, type, balance, icon);

                    AppDatabase.executor.execute(() -> db.appDao().insertWallet(wallet));
                    if (isNetworkAvailable()) {
                        syncHelper.pushWallet(wallet);
                    } else {
                        pendingStore.addWallet(wallet.getId());
                    }
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void showTransferDialog() {
        if (walletList.size() < 2) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_transfer, null);
        Spinner spnFrom   = dialogView.findViewById(R.id.spnFromWallet);
        Spinner spnTo     = dialogView.findViewById(R.id.spnToWallet);
        EditText etAmount = dialogView.findViewById(R.id.etTransferAmount);
        List<String> names = new ArrayList<>();
        for (WalletEntity w : walletList) names.add(w.getIcon() + " " + w.getName());
        ArrayAdapter<String> wa = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, names);
        wa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnFrom.setAdapter(wa); spnTo.setAdapter(wa);

        new AlertDialog.Builder(getContext())
                .setTitle("Chuyển tiền").setView(dialogView)
                .setPositiveButton("Chuyển", (dialog, which) -> {
                    int fi = spnFrom.getSelectedItemPosition(), ti = spnTo.getSelectedItemPosition();
                    String amountStr = etAmount.getText().toString().trim();
                    if (fi == ti) { Toast.makeText(getContext(), "Không thể chuyển vào cùng một ví", Toast.LENGTH_SHORT).show(); return; }
                    if (amountStr.isEmpty()) return;
                    double amount = Double.parseDouble(amountStr);
                    WalletEntity from = walletList.get(fi), to = walletList.get(ti);
                    if (from.getBalance() < amount) { Toast.makeText(getContext(), "Số dư không đủ", Toast.LENGTH_SHORT).show(); return; }

                    AppDatabase.executor.execute(() -> {
                        db.appDao().transferMoney(from.getId(), to.getId(), amount);
                        WalletEntity updatedFrom = db.appDao().getWalletById(from.getId());
                        WalletEntity updatedTo   = db.appDao().getWalletById(to.getId());
                        if (isNetworkAvailable()) {
                            if (updatedFrom != null) syncHelper.pushWallet(updatedFrom);
                            if (updatedTo   != null) syncHelper.pushWallet(updatedTo);
                        } else {
                            if (updatedFrom != null) pendingStore.addWallet(updatedFrom.getId());
                            if (updatedTo   != null) pendingStore.addWallet(updatedTo.getId());
                        }
                    });
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void startWalletRealtimeSync() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        walletListener = FirebaseFirestore.getInstance()
                .collection("wallets")
                .whereEqualTo("user_id", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    AppDatabase.executor.execute(() -> {
                        for (var doc : snapshots.getDocuments()) {
                            Boolean deleted = doc.getBoolean("deleted");
                            if (Boolean.TRUE.equals(deleted)) {
                                String id = doc.getString("wallet_id");
                                if (id != null) {
                                    WalletEntity local = db.appDao().getWalletById(id);
                                    if (local != null) db.appDao().deleteWallet(local);
                                }
                                continue;
                            }
                            try {
                                String id      = doc.getString("wallet_id");
                                String uid     = doc.getString("user_id");
                                String name    = doc.getString("name");
                                String type    = doc.getString("type");
                                Double balance = doc.getDouble("balance");
                                String icon    = doc.getString("icon");
                                if (id == null || uid == null || name == null || type == null || balance == null) continue;
                                db.appDao().insertWallet(new WalletEntity(id, uid, name, type, balance, icon));
                            } catch (Exception ignored) {}
                        }
                    });
                });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }
}