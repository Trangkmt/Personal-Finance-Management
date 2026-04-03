package com.example.expensemanagement.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagement.Adapters.BudgetAdapter;
import com.example.expensemanagement.BudgetViewModel;
import com.example.expensemanagement.R;
import com.example.expensemanagement.model.BudgetEntity;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BudgetFragment extends Fragment {

    private RecyclerView recyclerView;
    private BudgetAdapter adapter;
    private BudgetViewModel viewModel;
    private Button btnAddBudget;

    public static BudgetFragment newInstance() {
        return new BudgetFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        recyclerView = view.findViewById(R.id.recyclerBudgets);
        btnAddBudget = view.findViewById(R.id.btnAddBudget);

        viewModel = new ViewModelProvider(this).get(BudgetViewModel.class);
        adapter = new BudgetAdapter(viewModel);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        String userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            viewModel.getBudgets(userId).observe(getViewLifecycleOwner(), budgets -> {
                adapter.setBudgets(budgets);
            });
        }

        btnAddBudget.setOnClickListener(v -> showAddBudgetDialog());

        return view;
    }

    private void showAddBudgetDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_budget, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        EditText etAmount = dialogView.findViewById(R.id.etBudgetAmount);
        Spinner spCategory = dialogView.findViewById(R.id.spBudgetCategory);
        Button btnSave = dialogView.findViewById(R.id.btnSaveBudget);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelBudget);

        // Setup Spinner with categories (including "Overall")
        List<String> categories = new ArrayList<>();
        categories.add("Tổng thể");
        categories.add("Ăn uống");
        categories.add("Di chuyển");
        categories.add("Giải trí");
        categories.add("Y tế");
        
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(spinnerAdapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            String selectedCategory = spCategory.getSelectedItem().toString();
            String categoryId = selectedCategory.equals("Tổng thể") ? null : selectedCategory;

            String userId = FirebaseAuth.getInstance().getUid();
            if (userId == null) userId = "default_user";

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            String startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            String endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

            String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

            BudgetEntity budget = new BudgetEntity(
                    UUID.randomUUID().toString(),
                    userId,
                    categoryId,
                    amount,
                    "monthly",
                    startDate,
                    endDate,
                    now
            );

            viewModel.insert(budget);
            dialog.dismiss();
            Toast.makeText(getContext(), "Đã thiết lập ngân sách", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}