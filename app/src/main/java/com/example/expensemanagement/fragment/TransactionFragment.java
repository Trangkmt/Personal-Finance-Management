package com.example.expensemanagement.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagement.R;
import com.example.expensemanagement.Adapters.TransactionAdapter;
import com.example.expensemanagement.TransactionViewModel;
import com.example.expensemanagement.model.TransactionEntity;
import com.example.expensemanagement.model.TransactionItem;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TransactionFragment extends Fragment implements TransactionAdapter.OnTransactionActionListener {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private List<TransactionItem> displayList = new ArrayList<>();
    private TransactionViewModel viewModel;
    private TextView tvBalanceValue, tvEmpty;
    private Button btnAddTransaction;
    private final DecimalFormat decimalFormat = new DecimalFormat("#,###");

    public static TransactionFragment newInstance() {
        return new TransactionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction, container, false);

        initViews(view);
        setupRecyclerView();
        setupViewModel();
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerTransactions);
        tvBalanceValue = view.findViewById(R.id.tvBalanceValue);
        tvEmpty = view.findViewById(R.id.tvEmptyState);
        btnAddTransaction = view.findViewById(R.id.fabAddTransaction);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(displayList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), entities -> {
            displayList.clear();
            long balance = 0;
            if (entities != null) {
                for (TransactionEntity entity : entities) {
                    boolean isIncome = "income".equalsIgnoreCase(entity.type);
                    // entities.note đóng vai trò là Title
                    displayList.add(new TransactionItem(entity.transactionId, entity.note, entity.categoryId, entity.transactionDate, (long)entity.amount, isIncome));
                    balance += isIncome ? entity.amount : -entity.amount;
                }
            }
            adapter.notifyDataSetChanged();
            tvBalanceValue.setText(decimalFormat.format(balance) + " đ");
            toggleEmptyState();
        });
    }

    private void setupListeners() {
        btnAddTransaction.setOnClickListener(v -> showTransactionDialog(false, -1));
    }

    private void showTransactionDialog(boolean isEdit, int position) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_transaction, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        RadioGroup rgType = dialogView.findViewById(R.id.rgType);
        RadioButton rbExpense = dialogView.findViewById(R.id.rbExpense);
        RadioButton rbIncome = dialogView.findViewById(R.id.rbIncome);
        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etCategory = dialogView.findViewById(R.id.etCategory);
        TextInputEditText etDate = dialogView.findViewById(R.id.etDate);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        if (isEdit) {
            tvDialogTitle.setText("Sửa giao dịch");
            TransactionItem item = displayList.get(position);
            etTitle.setText(item.getTitle());
            etCategory.setText(item.getCategory());
            etDate.setText(item.getDate());
            etAmount.setText(String.valueOf(item.getAmount()));
            if (item.isIncome()) rbIncome.setChecked(true); else rbExpense.setChecked(true);
        } else {
            rbExpense.setChecked(true);
            etDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = valueOf(etTitle);
            String category = valueOf(etCategory);
            String date = valueOf(etDate);
            String amountStr = valueOf(etAmount);
            String type = rbIncome.isChecked() ? "income" : "expense";

            if (title.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                String userId = FirebaseAuth.getInstance().getUid();
                if (userId == null) userId = "default_user";
                String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

                if (isEdit) {
                    TransactionItem item = displayList.get(position);
                    TransactionEntity updateEntity = new TransactionEntity(
                            item.getId(), userId, category, amount, type, title, date, now, now);
                    viewModel.update(updateEntity);
                    Toast.makeText(getContext(), "Đã cập nhật giao dịch", Toast.LENGTH_SHORT).show();
                } else {
                    TransactionEntity newEntity = new TransactionEntity(
                            UUID.randomUUID().toString(), userId, category, amount, type, title, date, now, now);
                    viewModel.insert(newEntity);
                    Toast.makeText(getContext(), "Đã thêm giao dịch", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Lỗi nhập liệu số tiền", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private String valueOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void toggleEmptyState() {
        if (displayList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEdit(int position) {
        showTransactionDialog(true, position);
    }

    @Override
    public void onDelete(int position) {
        if (position < 0 || position >= displayList.size()) return;
        TransactionItem item = displayList.get(position);
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa giao dịch")
                .setMessage("Bạn có chắc muốn xóa giao dịch này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    TransactionEntity proxy = new TransactionEntity(item.getId(), "", "", 0, "", "", "", "", "");
                    viewModel.delete(proxy);
                    Toast.makeText(getContext(), "Đã xóa giao dịch", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}