package com.dzovah.mesha.Activities.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.dzovah.mesha.Database.Entities.Transaction;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;
import com.dzovah.mesha.R;
import com.dzovah.mesha.Database.Utils.TransactionType;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private final Context context;
    private List<Transaction> transactions;
    private String betaAccountIcon; // Add this field

    public TransactionAdapter(Context context) {
        this.context = context;
        this.transactions = new ArrayList<>();
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    public void setBetaAccountIcon(String iconPath) {
        this.betaAccountIcon = iconPath;
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
        

            holder.tvAmount.setText(CurrencyFormatter.format(Math.abs(transaction.getTransactionAmount())));

        // Format date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(transaction.getEntryTime())));
        
        // Set color based on TransactionType enum
        holder.tvAmount.setTextColor(ContextCompat.getColor(context,
            transaction.getTransactionType() == TransactionType.CREDIT ? 
            R.color.green : R.color.red));

        // Load beta account icon
        try {
            String iconPath = betaAccountIcon.replace("Assets/", "");
            InputStream is = context.getAssets().open(iconPath);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            holder.transaction_icon.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvAmount, tvDate;
        ImageView transaction_icon;

        TransactionViewHolder(View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvTransactionDescription);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
            tvDate = itemView.findViewById(R.id.tvTransactionDate);
            transaction_icon = itemView.findViewById(R.id.transaction_icon);
        }
    }
} 