package com.example.expensemanagement.Adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagement.R;
import com.example.expensemanagement.model.MonthlyTotal;
import com.example.expensemanagement.model.CategoryTotal;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.*;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.*;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.highlight.Highlight;

import java.util.*;

public class ChartAdapter extends RecyclerView.Adapter<ChartAdapter.ChartViewHolder> {

    public interface OnMonthClickListener {
        void onMonthClick(int month);
    }

    private List<MonthlyTotal> monthlyList;
    private List<CategoryTotal> categoryList;
    private OnMonthClickListener listener;

    // 🎨 PALETTE MÀU ĐẸP
    private final int[] MATERIAL_COLORS = new int[]{
            Color.parseColor("#F44336"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#673AB7"),
            Color.parseColor("#3F51B5"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#03A9F4"),
            Color.parseColor("#00BCD4"),
            Color.parseColor("#009688"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#8BC34A"),
            Color.parseColor("#CDDC39"),
            Color.parseColor("#FFC107"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#FF5722")
    };

    public ChartAdapter(List<MonthlyTotal> monthlyList,
                        List<CategoryTotal> categoryList,
                        OnMonthClickListener listener) {
        this.monthlyList = monthlyList != null ? monthlyList : new ArrayList<>();
        this.categoryList = categoryList != null ? categoryList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chart, parent, false);
        return new ChartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChartViewHolder holder, int position) {

        // ================= BAR CHART =================
        if (position == 0) {
            holder.tvTitle.setText("Chi tiêu theo tháng");

            holder.barChart.setVisibility(View.VISIBLE);
            holder.pieChart.setVisibility(View.GONE);

            Map<String, Float> map = new HashMap<>();
            for (MonthlyTotal item : monthlyList) {
                map.put(item.month, (float) item.total);
            }

            List<BarEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            float maxValue = 0;

            for (int i = 1; i <= 12; i++) {
                String m = String.format("%02d", i);
                float value = map.containsKey(m) ? map.get(m) : 0f;

                entries.add(new BarEntry(i - 1, value));
                labels.add("T" + i);

                if (value > maxValue) maxValue = value;
            }

            BarDataSet dataSet = new BarDataSet(entries, "");
            dataSet.setGradientColor(
                    Color.parseColor("#66BB6A"),
                    Color.parseColor("#2E7D32")
            );
            dataSet.setDrawValues(false);

            BarData data = new BarData(dataSet);
            data.setBarWidth(0.5f);

            holder.barChart.setData(data);

            XAxis xAxis = holder.barChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setDrawGridLines(false);

            YAxis yAxis = holder.barChart.getAxisLeft();
            holder.barChart.getAxisRight().setEnabled(false);

            float axisMax = (float) (Math.ceil(maxValue / 10000) * 10000);
            if (axisMax == 0) axisMax = 10000;

            yAxis.setAxisMinimum(0f);
            yAxis.setAxisMaximum(axisMax);
            yAxis.setGranularity(axisMax / 5);

            yAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.format("%,d đ", (int) value);
                }
            });

            holder.barChart.setVisibleXRangeMaximum(6f);
            holder.barChart.setDragEnabled(true);
            holder.barChart.setScaleEnabled(false);

            Calendar cal = Calendar.getInstance();
            int currentMonth = cal.get(Calendar.MONTH);
            holder.barChart.moveViewToX(Math.max(currentMonth - 2, 0));

            holder.barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
                    int month = (int) e.getX() + 1;
                    if (listener != null) {
                        listener.onMonthClick(month);
                    }
                }

                @Override
                public void onNothingSelected() {}
            });

            holder.barChart.getDescription().setEnabled(false);
            holder.barChart.getLegend().setEnabled(false);

            holder.barChart.animateY(1200);
            holder.barChart.invalidate();
        }

        // ================= PIE CHART =================
        else {
            holder.tvTitle.setText("Tỷ lệ danh mục");

            holder.barChart.setVisibility(View.GONE);
            holder.pieChart.setVisibility(View.VISIBLE);

            if (categoryList == null || categoryList.isEmpty()) {
                holder.pieChart.setNoDataText("Không có dữ liệu");
                holder.pieChart.clear();
                return;
            }

            List<PieEntry> entries = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();

            int colorIndex = 0;

            for (CategoryTotal item : categoryList) {
                entries.add(new PieEntry((float) item.total, item.category));

                int color;

                try {
                    color = Color.parseColor(item.color);
                } catch (Exception e) {
                    color = MATERIAL_COLORS[colorIndex % MATERIAL_COLORS.length];
                }

                colors.add(color);
                colorIndex++;
            }

            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(colors);
            dataSet.setSliceSpace(4f);
            dataSet.setSelectionShift(8f);

            PieData data = new PieData(dataSet);
            data.setValueFormatter(new PercentFormatter(holder.pieChart));
            data.setValueTextSize(13f);
            data.setValueTextColor(Color.WHITE);

            holder.pieChart.setUsePercentValues(true);
            holder.pieChart.setDrawHoleEnabled(true);
            holder.pieChart.setHoleRadius(60f);

            holder.pieChart.setCenterText("Chi tiêu");
            holder.pieChart.setCenterTextSize(14f);

            holder.pieChart.getDescription().setEnabled(false);
            holder.pieChart.getLegend().setEnabled(true);

            holder.pieChart.setData(data);
            holder.pieChart.animateY(1200);
            holder.pieChart.invalidate();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public static class ChartViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle;
        BarChart barChart;
        PieChart pieChart;

        public ChartViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvChartTitle);
            barChart = itemView.findViewById(R.id.barChart);
            pieChart = itemView.findViewById(R.id.pieChart);
        }
    }

    public void updateCategoryList(List<CategoryTotal> newList) {
        this.categoryList = newList != null ? newList : new ArrayList<>();
        notifyItemChanged(1);
    }
}