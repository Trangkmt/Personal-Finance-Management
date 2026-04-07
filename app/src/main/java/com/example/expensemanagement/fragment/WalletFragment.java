package com.example.expensemanagement.fragment;

import android.app.AlertDialog;
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
import com.example.expensemanagement.R;
import com.example.expensemanagement.Adapters.WalletAdapter;
import com.example.expensemanagement.model.WalletEntity;
import com.google.firebase.auth.FirebaseAuth;
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
    private List<WalletEntity> walletList = new ArrayList<>();

    public static WalletFragment newInstance() {
        return new WalletFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = AppDatabase.getInstance(getContext());
        syncHelper = new FirestoreSyncHelper(db.appDao());

        initViews(view);
        setupUser();
        setupRecyclerView();
        observeWallets();

        return view;
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
                            // Xóa Room + push delete lên Firestore
                            AppDatabase.executor.execute(() -> db.appDao().deleteWallet(wallet));
                            syncHelper.pushDeleteWallet(wallet.getId());
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
            if (layoutTransfer != null) {
                layoutTransfer.setVisibility(wallets.size() >= 2 ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void updateTotalBalance(List<WalletEntity> wallets) {
        double total = 0;
        for (WalletEntity w : wallets) total += w.getBalance();
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvTotal.setText(formatter.format(total));
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
                .setTitle("Thêm ví mới")
                .setView(dialogView)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String balanceStr = etBalance.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập tên ví", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double balance = balanceStr.isEmpty() ? 0 : Double.parseDouble(balanceStr);
                    String type   = types[spnType.getSelectedItemPosition()];
                    String icon   = icons[spnIcon.getSelectedItemPosition()];
                    String userId = mAuth.getCurrentUser().getUid();

                    WalletEntity wallet = new WalletEntity(
                            UUID.randomUUID().toString(), userId, name, type, balance, icon);

                    // Lưu Room + push Firestore
                    AppDatabase.executor.execute(() -> db.appDao().insertWallet(wallet));
                    syncHelper.pushWallet(wallet);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showTransferDialog() {
        if (walletList.size() < 2) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_transfer, null);
        Spinner spnFrom  = dialogView.findViewById(R.id.spnFromWallet);
        Spinner spnTo    = dialogView.findViewById(R.id.spnToWallet);
        EditText etAmount = dialogView.findViewById(R.id.etTransferAmount);

        List<String> walletNames = new ArrayList<>();
        for (WalletEntity w : walletList) walletNames.add(w.getIcon() + " " + w.getName());

        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, walletNames);
        walletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnFrom.setAdapter(walletAdapter);
        spnTo.setAdapter(walletAdapter);

        new AlertDialog.Builder(getContext())
                .setTitle("Chuyển tiền")
                .setView(dialogView)
                .setPositiveButton("Chuyển", (dialog, which) -> {
                    int fromIdx = spnFrom.getSelectedItemPosition();
                    int toIdx   = spnTo.getSelectedItemPosition();
                    String amountStr = etAmount.getText().toString().trim();

                    if (fromIdx == toIdx) {
                        Toast.makeText(getContext(), "Không thể chuyển vào cùng một ví", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (amountStr.isEmpty()) return;

                    double amount = Double.parseDouble(amountStr);
                    WalletEntity fromWallet = walletList.get(fromIdx);
                    WalletEntity toWallet   = walletList.get(toIdx);

                    if (fromWallet.getBalance() < amount) {
                        Toast.makeText(getContext(), "Số dư không đủ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AppDatabase.executor.execute(() -> {
                        db.appDao().transferMoney(fromWallet.getId(), toWallet.getId(), amount);

                        // Push cả 2 ví lên Firestore sau khi transfer xong
                        WalletEntity updatedFrom = db.appDao().getWalletById(fromWallet.getId());
                        WalletEntity updatedTo   = db.appDao().getWalletById(toWallet.getId());
                        if (updatedFrom != null) syncHelper.pushWallet(updatedFrom);
                        if (updatedTo   != null) syncHelper.pushWallet(updatedTo);
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}