package com.example.expensemanagement.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagement.AppDatabase;
import com.example.expensemanagement.R;
import com.example.expensemanagement.TransactionViewModel;
import com.example.expensemanagement.Adapters.ChartAdapter;
import com.example.expensemanagement.model.CategoryTotal;
import com.example.expensemanagement.model.DailyTotal;
import com.example.expensemanagement.model.MonthlyTotal;
import com.example.expensemanagement.model.TransactionEntity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.*;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.highlight.Highlight;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.DecimalFormat;
import java.util.*;

public class HomeFragment extends Fragment {

    private TextView tvName, tvBalance, tvIncome, tvExpense, tvSelectedMonth;
    private RecyclerView rvCharts;
    private BarChart barChartDaily;

    private TransactionViewModel viewModel;
    private ChartAdapter chartAdapter;

    private final DecimalFormat format = new DecimalFormat("#,###");

    private List<TransactionEntity> cachedList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvName = view.findViewById(R.id.tvName);
        tvBalance = view.findViewById(R.id.tvBalance);
        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        tvSelectedMonth = view.findViewById(R.id.tvSelectedMonth);
        rvCharts = view.findViewById(R.id.rvCharts);
        barChartDaily = view.findViewById(R.id.barChartDaily);

        viewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

        rvCharts.setLayoutManager(new LinearLayoutManager(requireContext()));
        barChartDaily.setVisibility(View.GONE);

        loadUserInfo();
        observeData();
        loadCharts();

        return view;
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    private void loadUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = user.getEmail();
            }
            tvName.setText(name);
        } else {
            tvName.setText("Khách");
        }
    }

    private void observeData() {
        viewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), entities -> {

            cachedList.clear();

            double income = 0;
            double expense = 0;

            if (entities != null) {
                cachedList.addAll(entities);

                for (TransactionEntity entity : entities) {
                    if (entity == null) continue;

                    if ("income".equalsIgnoreCase(entity.type)) {
                        income += entity.amount;
                    } else {
                        expense += entity.amount;
                    }
                }
            }

            tvIncome.setText("+ " + format.format(income) + " đ");
            tvExpense.setText("- " + format.format(expense) + " đ");
            tvBalance.setText(format.format(income - expense) + " đ");

            tvSelectedMonth.setText("");
        });
    }

    private void loadCharts() {

        final android.content.Context context = getContext();
        if (context == null) return;

        AppDatabase.executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);

                List<MonthlyTotal> monthlyList = db.appDao().getMonthlyTotals();
                List<CategoryTotal> categoryList = db.appDao().getCategoryTotals();

                if (monthlyList == null) monthlyList = new ArrayList<>();
                if (categoryList == null) categoryList = new ArrayList<>();

                final List<MonthlyTotal> finalMonthlyList = monthlyList;
                final List<CategoryTotal> finalCategoryList = categoryList;

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {

                        chartAdapter = new ChartAdapter(
                                finalMonthlyList,
                                finalCategoryList,
                                month -> filterByMonth(month)
                        );

                        rvCharts.setAdapter(chartAdapter);
                    });
                }

            } catch (Exception e) {
                Log.e("HOME_CRASH", "loadCharts error", e);
            }
        });
    }

    private void filterByMonth(int month) {

        if (cachedList == null || cachedList.isEmpty()) return;

        List<TransactionEntity> filtered = new ArrayList<>();

        double income = 0;
        double expense = 0;

        for (TransactionEntity item : cachedList) {
            if (item == null) continue;

            try {
                int m = Integer.parseInt(item.transactionDate.substring(5, 7));

                if (m == month) {
                    filtered.add(item);

                    if ("income".equalsIgnoreCase(item.type)) {
                        income += item.amount;
                    } else {
                        expense += item.amount;
                    }
                }
            } catch (Exception ignored) {}
        }

        tvIncome.setText("+ " + format.format(income) + " đ");
        tvExpense.setText("- " + format.format(expense) + " đ");
        tvBalance.setText(format.format(income - expense) + " đ");

        tvSelectedMonth.setText("📊 Tháng " + month);

        // 🔥 FIX MÀU: KHÔNG set màu cứng nữa
        List<CategoryTotal> newCategory = buildCategoryFromList(filtered);
        if (chartAdapter != null) {
            chartAdapter.updateCategoryList(newCategory);
        }

        barChartDaily.setVisibility(View.VISIBLE);
        loadDailyChart(month);
    }

    private List<CategoryTotal> buildCategoryFromList(List<TransactionEntity> list) {

        Map<String, Double> map = new HashMap<>();

        for (TransactionEntity item : list) {
            if (item == null) continue;

            String category = item.categoryId;
            double amount = item.amount;

            map.put(category, map.getOrDefault(category, 0.0) + amount);
        }

        List<CategoryTotal> result = new ArrayList<>();

        for (String key : map.keySet()) {
            CategoryTotal ct = new CategoryTotal();
            ct.category = key;
            ct.total = map.get(key);

            // để null để ChartAdapter tự random màu
            ct.color = null;

            result.add(ct);
        }

        return result;
    }

    private void loadDailyChart(int month) {

        final android.content.Context context = getContext();
        if (context == null) return;

        AppDatabase.executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);

                String m = String.format("%02d", month);
                List<DailyTotal> dailyList = db.appDao().getDailyTotalsByMonth(m);

                if (dailyList == null) dailyList = new ArrayList<>();

                Map<String, Float> map = new HashMap<>();

                for (DailyTotal d : dailyList) {
                    map.put(d.day, (float) d.total);
                }

                List<BarEntry> entries = new ArrayList<>();
                List<String> labels = new ArrayList<>();

                float maxValue = 0;

                for (int i = 1; i <= 31; i++) {
                    String day = String.format("%02d", i);
                    float value = map.containsKey(day) ? map.get(day) : 0f;

                    entries.add(new BarEntry(i - 1, value));
                    labels.add(day);

                    if (value > maxValue) maxValue = value;
                }

                float finalMax = maxValue;

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {

                        BarDataSet dataSet = new BarDataSet(entries, "Theo ngày");
                        dataSet.setColor(Color.parseColor("#FF9800"));
                        dataSet.setDrawValues(false);

                        BarData data = new BarData(dataSet);
                        data.setBarWidth(0.9f);

                        barChartDaily.setData(data);

                        XAxis xAxis = barChartDaily.getXAxis();
                        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        xAxis.setGranularity(1f);
                        xAxis.setDrawGridLines(false);

                        YAxis yAxis = barChartDaily.getAxisLeft();
                        barChartDaily.getAxisRight().setEnabled(false);
                        yAxis.setDrawGridLines(false);

                        float axisMax = (float) (Math.ceil(finalMax / 10000) * 10000);
                        if (axisMax == 0) axisMax = 10000;

                        yAxis.setAxisMinimum(0f);
                        yAxis.setAxisMaximum(axisMax);

                        barChartDaily.setVisibleXRangeMaximum(7f);
                        barChartDaily.setDragEnabled(true);
                        barChartDaily.setScaleEnabled(false);

                        Calendar cal = Calendar.getInstance();
                        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
                        barChartDaily.moveViewToX(Math.max(currentDay - 3, 0));

                        barChartDaily.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                            @Override
                            public void onValueSelected(Entry e, Highlight h) {
                                int day = (int) e.getX() + 1;
                                showTransactionsByDay(month, day);
                            }

                            @Override
                            public void onNothingSelected() {}
                        });

                        barChartDaily.getDescription().setEnabled(false);
                        barChartDaily.getLegend().setEnabled(false);

                        barChartDaily.animateY(1200);
                        barChartDaily.invalidate();
                    });
                }

            } catch (Exception e) {
                Log.e("DAILY_CHART", "error", e);
            }
        });
    }

    private void showTransactionsByDay(int month, int day) {

        List<TransactionEntity> result = new ArrayList<>();

        for (TransactionEntity item : cachedList) {
            if (item == null) continue;

            try {
                int m = Integer.parseInt(item.transactionDate.substring(5, 7));
                int d = Integer.parseInt(item.transactionDate.substring(8, 10));

                if (m == month && d == day) {
                    result.add(item);
                }
            } catch (Exception ignored) {}
        }

        showTransactionDialog(day, result);
    }

    private void showTransactionDialog(int day, List<TransactionEntity> list) {

        StringBuilder builder = new StringBuilder();
        builder.append("📅 Ngày ").append(day).append("\n\n");

        for (TransactionEntity t : list) {
            builder.append("• ")
                    .append(t.categoryId)
                    .append(" - ")
                    .append(format.format(t.amount))
                    .append(" đ\n");
        }

        if (list.isEmpty()) {
            builder.append("Không có giao dịch");
        }

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Chi tiết giao dịch")
                .setMessage(builder.toString())
                .setPositiveButton("OK", null)
                .show();
    }
}