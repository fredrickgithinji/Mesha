package com.dzovah.mesha.Activities.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.dzovah.mesha.Database.Entities.Transaction;
import com.dzovah.mesha.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private Context context;
    private List<Transaction> transactions = new ArrayList<>();

    public TransactionAdapter(Context context) {
        this.context = context;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        
        holder.tvDescription.setText(transaction.getTransactionDescription());
        holder.tvAmount.setText(String.format("%s$%.2f", 
            transaction.getTransactionType().equals("CREDIT") ? "+" : "-",
            transaction.getTransactionAmount()));
        
        // Format date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(transaction.getEntryTime())));
        
        // Set color based on transaction type
        holder.tvAmount.setTextColor(ContextCompat.getColor(context,
            transaction.getTransactionType().equals("CREDIT") ? R.color.green : R.color.red));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvAmount, tvDate;

        TransactionViewHolder(View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvTransactionDescription);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
            tvDate = itemView.findViewById(R.id.tvTransactionDate);
        }
    }
} 