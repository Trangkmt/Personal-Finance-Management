package com.example.expensemanagement.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagement.R;
import com.example.expensemanagement.model.WalletEntity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.ViewHolder> {

    private List<WalletEntity> wallets = new ArrayList<>();
    private OnWalletClickListener listener;

    public interface OnWalletClickListener {
        void onDelete(WalletEntity wallet);
    }

    public void setOnWalletClickListener(OnWalletClickListener listener) {
        this.listener = listener;
    }

    public void setWallets(List<WalletEntity> wallets) {
        this.wallets = wallets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WalletEntity wallet = wallets.get(position);
        holder.tvIcon.setText(wallet.getIcon());
        holder.tvName.setText(wallet.getName());
        
        String typeStr = "Tiền mặt";
        if ("bank".equals(wallet.getType())) typeStr = "Ngân hàng";
        else if ("e-wallet".equals(wallet.getType())) typeStr = "Ví điện tử";
        holder.tvType.setText(typeStr);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvBalance.setText(formatter.format(wallet.getBalance()));

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(wallet);
        });
    }

    @Override
    public int getItemCount() {
        return wallets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvType, tvBalance;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvWalletIcon);
            tvName = itemView.findViewById(R.id.tvWalletName);
            tvType = itemView.findViewById(R.id.tvWalletType);
            tvBalance = itemView.findViewById(R.id.tvWalletBalance);
            btnDelete = itemView.findViewById(R.id.btnDeleteWallet);
        }
    }
}
