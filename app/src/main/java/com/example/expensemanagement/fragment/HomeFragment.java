package com.example.expensemanagement.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.expensemanagement.R;
import com.example.expensemanagement.TransactionViewModel;
import com.example.expensemanagement.model.TransactionEntity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvName, tvBalance, tvIncome, tvExpense;
    private BarChart barChart;
    private TransactionViewModel viewModel;
    private final DecimalFormat format = new DecimalFormat("#,###");

    public HomeFragment() {}

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvName = view.findViewById(R.id.tvName);
        tvBalance = view.findViewById(R.id.tvBalance);
        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        barChart = view.findViewById(R.id.barChart);

        viewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

        loadUserInfo();
        observeData();

        return view;
    }

    private void loadUserInfo() {
        tvName.setText("nguyen van a");
    }

    private void observeData() {
        viewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), entities -> {

            int income = 0;
            int expense = 0;

            if (entities != null) {
                for (TransactionEntity entity : entities) {
                    if ("income".equalsIgnoreCase(entity.type)) {
                        income += entity.amount;
                    } else {
                        expense += entity.amount;
                    }
                }

                setupChart(entities); // 🔥 update chart realtime
            }

            tvIncome.setText("+ " + format.format(income) + " đ");
            tvExpense.setText("- " + format.format(expense) + " đ");
            tvBalance.setText(format.format(income - expense) + " đ");
        });
    }

    // CHART
    private void setupChart(List<TransactionEntity> entities) {

        barChart.clear(); // FIX BUG mất dữ liệu khi quay lại fragment

        float[] incomeByDay = new float[7];
        float[] expenseByDay = new float[7];

        for (TransactionEntity entity : entities) {
            int index = getDayIndex(entity.transactionDate);

            if (index >= 0 && index < 7) {
                if ("income".equalsIgnoreCase(entity.type)) {
                    incomeByDay[index] += entity.amount;
                } else {
                    expenseByDay[index] += entity.amount;
                }
            }
        }

        ArrayList<BarEntry> incomeEntries = new ArrayList<>();
        ArrayList<BarEntry> expenseEntries = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            incomeEntries.add(new BarEntry(i, incomeByDay[i]));
            expenseEntries.add(new BarEntry(i, expenseByDay[i]));
        }

        BarDataSet incomeSet = new BarDataSet(incomeEntries, "Thu");
        incomeSet.setColor(Color.parseColor("#4CAF50"));
        incomeSet.setValueTextSize(10f);

        BarDataSet expenseSet = new BarDataSet(expenseEntries, "Chi");
        expenseSet.setColor(Color.parseColor("#F44336"));
        expenseSet.setValueTextSize(10f);

        BarData barData = new BarData(incomeSet, expenseSet);

        float groupSpace = 0.2f;
        float barSpace = 0.05f;
        float barWidth = 0.35f;

        barData.setBarWidth(barWidth);

        barChart.setData(barData);

        // FIX LỆCH / MẤT CỘT
        barChart.getXAxis().setAxisMinimum(0f);
        barChart.getXAxis().setAxisMaximum(
                0f + barData.getGroupWidth(groupSpace, barSpace) * 7
        );

        barChart.groupBars(0f, groupSpace, barSpace);

        // STYLE
        barChart.setFitBars(true);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);

        barChart.getDescription().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);

        barChart.getAxisLeft().setTextColor(Color.GRAY);
        barChart.getAxisLeft().setGridColor(Color.LTGRAY);

        // Thứ
        String[] days = {"T2","T3","T4","T5","T6","T7","CN"};
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        // Animation
        barChart.animateY(1200);

        barChart.notifyDataSetChanged();
        barChart.invalidate();
    }

    // TÍNH NGÀY
    private int getDayIndex(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(dateStr);

            Calendar cal = Calendar.getInstance();
            Calendar today = Calendar.getInstance();

            cal.setTime(date);

            long diff = today.getTimeInMillis() - cal.getTimeInMillis();
            int days = (int) (diff / (1000 * 60 * 60 * 24));

            return 6 - days;
        } catch (Exception e) {
            return -1;
        }
    }
}