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
import java.util.Locale;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.ViewHolder> {

    private List<StockItem> items = new ArrayList<>();
    private final DecimalFormat fmtVND = new DecimalFormat("#,###");
    private final DecimalFormat fmtUSD = new DecimalFormat("#,##0.00");
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(StockItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

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

        h.tvStockSymbol.setText(s.symbol);
        h.tvStockName.setText(s.fullName);

        // Giá
        if ("VND".equals(s.currency)) {
            h.tvStockPrice.setText(fmtVND.format(s.price) + " đ");
        } else {
            h.tvStockPrice.setText("$" + fmtUSD.format(s.price));
        }

        // Thay đổi
        String sign = s.isPositive ? "+" : "";
        String changeStr;
        if ("VND".equals(s.currency)) {
            changeStr = sign + fmtVND.format(s.change) + " (" + String.format(Locale.US, "%.2f", s.changePct) + "%)";
        } else {
            changeStr = sign + fmtUSD.format(s.change) + " (" + String.format(Locale.US, "%.2f", s.changePct) + "%)";
        }
        h.tvStockChange.setText(changeStr);
        h.tvStockChange.setTextColor(s.isPositive ? Color.parseColor("#30D158") : Color.parseColor("#FF453A"));

        // Mini chart
        if (h.miniChart != null) {
            h.miniChart.setData(s.chartPoints, s.isPositive);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(s);
            }
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStockSymbol, tvStockName, tvStockPrice, tvStockChange;
        MiniChartView miniChart;

        ViewHolder(View v) {
            super(v);
            tvStockSymbol = v.findViewById(R.id.tvSymbol);
            tvStockName   = v.findViewById(R.id.tvStockName);
            tvStockPrice  = v.findViewById(R.id.tvPrice);
            tvStockChange = v.findViewById(R.id.tvChange);
            miniChart     = v.findViewById(R.id.miniChart);
        }
    }
}
