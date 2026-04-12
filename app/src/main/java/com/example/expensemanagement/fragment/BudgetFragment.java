package com.example.expensemanagement.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagement.Adapters.BudgetAdapter;
import com.example.expensemanagement.AppDatabase;
import com.example.expensemanagement.BudgetViewModel;
import com.example.expensemanagement.R;
import com.example.expensemanagement.model.BudgetEntity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DecimalFormat;
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
    
    // Overview UI
    private MaterialCardView cardOverview;
    private TextView tvTotalRemaining, tvTotalStatus;
    private LinearProgressIndicator progressTotal;
    private View layoutEmpty;
    private Button btnCreateFirst;

    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public static BudgetFragment newInstance() {
        return new BudgetFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        initViews(view);
        setupRecyclerView();
        setupViewModel();

        btnAddBudget.setOnClickListener(v -> showAddBudgetDialog());
        btnCreateFirst.setOnClickListener(v -> showAddBudgetDialog());

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerBudgets);
        btnAddBudget = view.findViewById(R.id.btnAddBudget);
        cardOverview = view.findViewById(R.id.cardBudgetOverview);
        tvTotalRemaining = view.findViewById(R.id.tvTotalRemaining);
        tvTotalStatus = view.findViewById(R.id.tvTotalBudgetStatus);
        progressTotal = view.findViewById(R.id.progressTotalBudget);
        layoutEmpty = view.findViewById(R.id.layoutEmptyBudget);
        btnCreateFirst = view.findViewById(R.id.btnCreateFirstBudget);
    }

    private void setupRecyclerView() {
        adapter = new BudgetAdapter(viewModel);
        adapter.setOnItemClickListener(this::showEditBudgetDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(BudgetViewModel.class);
        adapter = new BudgetAdapter(viewModel);
        adapter.setOnItemClickListener(this::showEditBudgetDialog);
        recyclerView.setAdapter(adapter);

        String userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            viewModel.getBudgets(userId).observe(getViewLifecycleOwner(), budgets -> {
                if (budgets == null || budgets.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    cardOverview.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    cardOverview.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.setBudgets(budgets);
                    updateOverview(userId, budgets);
                }
            });
        }
    }

    private void updateOverview(String userId, List<BudgetEntity> budgets) {
        AppDatabase.executor.execute(() -> {
            double totalBudgetAmount = 0;
            double totalSpentAmount = 0;

            for (BudgetEntity b : budgets) {
                totalBudgetAmount += b.amount;
                totalSpentAmount += viewModel.getSpentAmount(userId, b.categoryId, b.startDate, b.endDate);
            }

            final double finalTotalBudget = totalBudgetAmount;
            final double finalTotalSpent = totalSpentAmount;
            final double remaining = Math.max(0, finalTotalBudget - finalTotalSpent);
            final int percent = finalTotalBudget > 0 ? (int) ((finalTotalSpent / finalTotalBudget) * 100) : 0;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvTotalRemaining.setText(formatter.format(remaining) + " đ");
                    progressTotal.setProgress(Math.min(percent, 100));
                    tvTotalStatus.setText("Đã chi " + percent + "% tổng ngân sách");
                });
            }
        });
    }

    private void showAddBudgetDialog() {
        showBudgetDialog(null);
    }

    private void showEditBudgetDialog(BudgetEntity budget) {
        showBudgetDialog(budget);
    }

    private void showBudgetDialog(@Nullable BudgetEntity existingBudget) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_budget, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText etAmount = dialogView.findViewById(R.id.etBudgetAmount);
        Spinner spCategory = dialogView.findViewById(R.id.spBudgetCategory);
        Spinner spPeriod = dialogView.findViewById(R.id.spBudgetPeriod);
        CheckBox cbAutoRepeat = dialogView.findViewById(R.id.cbAutoRepeat);
        Button btnSave = dialogView.findViewById(R.id.btnSaveBudget);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelBudget);

        if (existingBudget != null) {
            tvTitle.setText("Chỉnh sửa ngân sách");
            etAmount.setText(String.valueOf(existingBudget.amount));
            btnSave.setText("Cập nhật");
        }

        // Setup Category Spinner
        List<String> categories = new ArrayList<>();
        categories.add("Tổng thể");
        categories.add("Ăn uống");
        categories.add("Di chuyển");
        categories.add("Giải trí");
        categories.add("Y tế");
        categories.add("Mua sắm");
        categories.add("Khác");
        
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);

        if (existingBudget != null) {
            String cat = existingBudget.categoryId == null ? "Tổng thể" : existingBudget.categoryId;
            int pos = categories.indexOf(cat);
            if (pos >= 0) spCategory.setSelection(pos);
        }

        // Setup Period Spinner
        List<String> periods = new ArrayList<>();
        periods.add("Hàng tuần");
        periods.add("Hàng tháng");
        periods.add("Hàng quý");
        periods.add("Hàng năm");
        
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPeriod.setAdapter(periodAdapter);

        if (existingBudget != null) {
            int pos = 1;
            switch (existingBudget.period) {
                case "weekly": pos = 0; break;
                case "monthly": pos = 1; break;
                case "quarterly": pos = 2; break;
                case "yearly": pos = 3; break;
            }
            spPeriod.setSelection(pos);
        } else {
            spPeriod.setSelection(1);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String userId = FirebaseAuth.getInstance().getUid();
            if (userId == null) return;

            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            String selectedCategory = spCategory.getSelectedItem().toString();
            String categoryId = selectedCategory.equals("Tổng thể") ? null : selectedCategory;
            
            String selectedPeriod = spPeriod.getSelectedItem().toString();
            String periodKey = "monthly";
            if (selectedPeriod.equals("Hàng tuần")) periodKey = "weekly";
            else if (selectedPeriod.equals("Hàng quý")) periodKey = "quarterly";
            else if (selectedPeriod.equals("Hàng năm")) periodKey = "yearly";

            Calendar calStart = Calendar.getInstance();
            Calendar calEnd = Calendar.getInstance();
            
            if (periodKey.equals("weekly")) {
                calStart.set(Calendar.DAY_OF_WEEK, calStart.getFirstDayOfWeek());
                calEnd.set(Calendar.DAY_OF_WEEK, calStart.getFirstDayOfWeek());
                calEnd.add(Calendar.DAY_OF_YEAR, 6);
            } else if (periodKey.equals("quarterly")) {
                int month = calStart.get(Calendar.MONTH);
                int quarterStartMonth = (month / 3) * 3;
                calStart.set(Calendar.MONTH, quarterStartMonth);
                calStart.set(Calendar.DAY_OF_MONTH, 1);
                calEnd.set(Calendar.MONTH, quarterStartMonth + 2);
                calEnd.set(Calendar.DAY_OF_MONTH, calEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
            } else if (periodKey.equals("yearly")) {
                calStart.set(Calendar.DAY_OF_YEAR, 1);
                calEnd.set(Calendar.MONTH, Calendar.DECEMBER);
                calEnd.set(Calendar.DAY_OF_MONTH, 31);
            } else {
                calStart.set(Calendar.DAY_OF_MONTH, 1);
                calEnd.set(Calendar.DAY_OF_MONTH, calEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
            }

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String startDate = df.format(calStart.getTime());
            String endDate = df.format(calEnd.getTime());
            String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

            if (existingBudget == null) {
                BudgetEntity budget = new BudgetEntity(
                        UUID.randomUUID().toString(),
                        userId, categoryId, amount, periodKey, startDate, endDate, now
                );
                viewModel.insert(budget);
            } else {
                existingBudget.amount = amount;
                existingBudget.categoryId = categoryId;
                existingBudget.period = periodKey;
                existingBudget.startDate = startDate;
                existingBudget.endDate = endDate;
                viewModel.update(existingBudget);
            }
            
            dialog.dismiss();
        });

        dialog.show();
    }
}