package com.example.expensemanagement.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagement.R;
import com.example.expensemanagement.model.TransactionItem;

import java.text.DecimalFormat;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    public interface OnTransactionActionListener {
        void onEdit(int position);
        void onDelete(int position);
    }

    private final List<TransactionItem> items;
    private final OnTransactionActionListener listener;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public TransactionAdapter(List<TransactionItem> items, OnTransactionActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvCategory.setText(item.getCategory());
        holder.tvDate.setText(item.getDate());

        String amountText = (item.isIncome() ? "+ " : "- ") + formatter.format(item.getAmount()) + " đ";
        holder.tvAmount.setText(amountText);

        int colorRes = item.isIncome() ? R.color.income_green : R.color.expense_red;
        holder.tvAmount.setTextColor(holder.itemView.getContext().getColor(colorRes));

        holder.ivEdit.setOnClickListener(v -> listener.onEdit(position));
        holder.ivDelete.setOnClickListener(v -> listener.onDelete(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvTitle, tvCategory, tvDate, tvAmount;
        public ImageView ivEdit, ivDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTransactionTitle);
            tvCategory = itemView.findViewById(R.id.tvTransactionCategory);
            tvDate = itemView.findViewById(R.id.tvTransactionDate);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
            ivEdit = itemView.findViewById(R.id.ivEdit);
            ivDelete = itemView.findViewById(R.id.ivDelete); // Sửa ID: ivDeleteBudget -> ivDelete
        }
    }
}