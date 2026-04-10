package com.example.expensemanagement.Adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagement.R;
import com.example.expensemanagement.model.StockItem;
import com.example.expensemanagement.widget.MiniChartView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.ViewHolder> {

    private List<StockItem> items = new ArrayList<>();
    private final DecimalFormat fmtVND = new DecimalFormat("#,###");
    private final DecimalFormat fmtUSD = new DecimalFormat("#,##0.00");

    public void setItems(List<StockItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        StockItem s = items.get(position);

        h.tvSymbol.setText(s.symbol);
        h.tvFullName.setText(s.fullName);

        // Giá
        if ("VND".equals(s.currency)) {
            h.tvPrice.setText(fmtVND.format(s.price));
        } else {
            h.tvPrice.setText(fmtUSD.format(s.price));
        }

        // Thay đổi
        String sign = s.isPositive ? "+" : "";
        String changeStr;
        if ("VND".equals(s.currency)) {
            changeStr = sign + fmtVND.format(s.change);
        } else {
            changeStr = sign + fmtUSD.format(s.change);
        }
        h.tvChange.setText(changeStr);
        h.tvChange.setBackgroundColor(s.isPositive ? Color.parseColor("#30D158") : Color.parseColor("#FF453A"));
        h.tvChange.setTextColor(Color.WHITE);

        // Mini chart
        h.miniChart.setData(s.chartPoints, s.isPositive);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSymbol, tvFullName, tvPrice, tvChange;
        MiniChartView miniChart;

        ViewHolder(View v) {
            super(v);
            tvSymbol   = v.findViewById(R.id.tvSymbol);
            tvFullName = v.findViewById(R.id.tvFullName);
            tvPrice    = v.findViewById(R.id.tvPrice);
            tvChange   = v.findViewById(R.id.tvChange);
            miniChart  = v.findViewById(R.id.miniChart);
        }
    }
}