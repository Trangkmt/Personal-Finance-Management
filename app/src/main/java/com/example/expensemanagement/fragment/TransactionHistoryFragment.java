package com.example.expensemanagement.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagement.R;
import com.example.expensemanagement.Adapters.TransactionAdapter;
import com.example.expensemanagement.TransactionViewModel;
import com.example.expensemanagement.model.TransactionEntity;
import com.example.expensemanagement.model.TransactionItem;
import com.google.android.material.chip.ChipGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionHistoryFragment extends Fragment implements TransactionAdapter.OnTransactionActionListener {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private TransactionViewModel viewModel;
    private ChipGroup chipGroupFilter;
    private LinearLayout layoutRangePicker, layoutSearch;
    private TextView tvStartDate, tvEndDate;
    private EditText etSearchCategory;
    
    private List<TransactionEntity> allEntities = new ArrayList<>();
    private List<TransactionItem> displayItems = new ArrayList<>();
    
    private String startDateStr = ""; // yyyy-MM-dd
    private String endDateStr = "";   // yyyy-MM-dd
    private String searchCategoryQuery = "";

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static TransactionHistoryFragment newInstance() {
        return new TransactionHistoryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_history, container, false);

        initViews(view);
        setupListeners();

        try {
            viewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);
            observeTransactions();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi nạp dữ liệu", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void initViews(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbarHistory);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        }

        recyclerView = view.findViewById(R.id.rvHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter(displayItems, this);
        recyclerView.setAdapter(adapter);

        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        layoutRangePicker = view.findViewById(R.id.layoutRangePicker);
        layoutSearch = view.findViewById(R.id.layoutSearch);
        tvStartDate = view.findViewById(R.id.tvStartDate);
        tvEndDate = view.findViewById(R.id.tvEndDate);
        etSearchCategory = view.findViewById(R.id.etSearchCategory);

        // Mặc định chọn khoảng thời gian là tháng hiện tại
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        startDateStr = df.format(c.getTime());
        tvStartDate.setText(displayDf.format(c.getTime()));

        c = Calendar.getInstance();
        endDateStr = df.format(c.getTime());
        tvEndDate.setText(displayDf.format(c.getTime()));
    }

    private void setupListeners() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = group.getCheckedChipId();
            layoutRangePicker.setVisibility(checkedId == R.id.chipRange ? View.VISIBLE : View.GONE);
            layoutSearch.setVisibility(checkedId == R.id.chipCategory ? View.VISIBLE : View.GONE);
            applyFilter();
        });

        tvStartDate.setOnClickListener(v -> showDatePicker(true));
        tvEndDate.setOnClickListener(v -> showDatePicker(false));

        etSearchCategory.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                searchCategoryQuery = s.toString().trim().toLowerCase();
                applyFilter();
            }
        });
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar c = Calendar.getInstance();
        try {
            Date current = df.parse(isStartDate ? startDateStr : endDateStr);
            if (current != null) c.setTime(current);
        } catch (ParseException ignored) {}

        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            String formatted = df.format(selected.getTime());
            String display = displayDf.format(selected.getTime());

            if (isStartDate) {
                startDateStr = formatted;
                tvStartDate.setText(display);
            } else {
                endDateStr = formatted;
                tvEndDate.setText(display);
            }
            applyFilter();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void observeTransactions() {
        if (viewModel == null) return;
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), entities -> {
            if (entities != null) {
                allEntities = new ArrayList<>(entities);
                Collections.sort(allEntities, (o1, o2) -> o2.transactionDate.compareTo(o1.transactionDate));
                applyFilter();
            }
        });
    }

    private void applyFilter() {
        if (allEntities == null) return;

        int checkedId = chipGroupFilter.getCheckedChipId();
        displayItems.clear();
        List<TransactionEntity> filteredEntities = new ArrayList<>();

        if (checkedId == R.id.chipRange) {
            for (TransactionEntity e : allEntities) {
                if (e.transactionDate.compareTo(startDateStr) >= 0 && e.transactionDate.compareTo(endDateStr) <= 0) {
                    filteredEntities.add(e);
                }
            }
        } else if (checkedId == R.id.chipCategory) {
            for (TransactionEntity e : allEntities) {
                if (searchCategoryQuery.isEmpty() || e.categoryId.toLowerCase().contains(searchCategoryQuery)) {
                    filteredEntities.add(e);
                }
            }
            Collections.sort(filteredEntities, (o1, o2) -> o1.categoryId.compareTo(o2.categoryId));
        } else {
            // Mặc định: Tất cả
            filteredEntities.addAll(allEntities);
        }

        for (TransactionEntity entity : filteredEntities) {
            boolean isIncome = "income".equalsIgnoreCase(entity.type);
            displayItems.add(new TransactionItem(
                    entity.transactionId,
                    entity.note,
                    entity.categoryId,
                    entity.transactionDate,
                    (long) entity.amount,
                    isIncome
            ));
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override public void onEdit(int position) { }
    @Override public void onDelete(int position) {
        if (position < 0 || position >= displayItems.size()) return;
        TransactionItem item = displayItems.get(position);
        TransactionEntity entity = new TransactionEntity(item.getId(), "", "", 0, "", "", "", "", "");
        viewModel.delete(entity);
        Toast.makeText(getContext(), "Đã xóa giao dịch thành công", Toast.LENGTH_SHORT).show();
    }
}