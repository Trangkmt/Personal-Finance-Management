package com.example.expensemanagement.Adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagement.AppDatabase;
import com.example.expensemanagement.BudgetViewModel;
import com.example.expensemanagement.R;
import com.example.expensemanagement.model.BudgetEntity;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.ViewHolder> {

    private List<BudgetEntity> budgets = new ArrayList<>();
    private final BudgetViewModel viewModel;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public BudgetAdapter(BudgetViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public void setBudgets(List<BudgetEntity> budgets) {
        this.budgets = budgets;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BudgetEntity budget = budgets.get(position);
        holder.bind(budget, viewModel, formatter);
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvPercent, tvDates, tvSpent, tvTotal, tvWarning;
        LinearProgressIndicator progress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvBudgetCategory);
            tvPercent = itemView.findViewById(R.id.tvBudgetPercent);
            tvDates = itemView.findViewById(R.id.tvBudgetDates);
            tvSpent = itemView.findViewById(R.id.tvSpentAmount);
            tvTotal = itemView.findViewById(R.id.tvTotalBudget);
            tvWarning = itemView.findViewById(R.id.tvBudgetWarning);
            progress = itemView.findViewById(R.id.progressBudget);
        }

        public void bind(BudgetEntity budget, BudgetViewModel viewModel, DecimalFormat formatter) {
            tvCategory.setText(budget.categoryId == null ? "Tổng thể" : budget.categoryId);
            tvDates.setText(budget.startDate + " - " + budget.endDate);
            tvTotal.setText("Ngân sách: " + formatter.format(budget.amount) + " đ");

            AppDatabase.executor.execute(() -> {
                double spent = viewModel.getSpentAmount(budget.categoryId, budget.startDate, budget.endDate);
                int percent = (int) ((spent / budget.amount) * 100);

                itemView.post(() -> {
                    tvSpent.setText("Đã chi: " + formatter.format(spent) + " đ");
                    tvPercent.setText(percent + "%");
                    progress.setProgress(Math.min(percent, 100));

                    if (percent >= 100) {
                        progress.setIndicatorColor(Color.parseColor("#DC2626"));
                        tvWarning.setVisibility(View.VISIBLE);
                        tvWarning.setText("Cảnh báo: Đã vượt ngân sách!");
                    } else if (percent >= 80) {
                        progress.setIndicatorColor(Color.parseColor("#F59E0B"));
                        tvWarning.setVisibility(View.VISIBLE);
                        tvWarning.setText("Cảnh báo: Sắp đạt giới hạn!");
                    } else {
                        progress.setIndicatorColor(Color.parseColor("#16A34A"));
                        tvWarning.setVisibility(View.GONE);
                    }
                });
            });
        }
    }
}