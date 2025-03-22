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

import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.Entities.Transaction;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;
import com.dzovah.mesha.Methods.Dialogs.EditTransactionDialog;
import com.dzovah.mesha.R;
import com.dzovah.mesha.Database.Utils.TransactionType;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private final Context context;
    private List<Transaction> transactions;
    private String betaAccountIcon;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat timeFormat;
    private BetaAccount betaAccount;

    public TransactionAdapter(Context context) {
        this.context = context;
        this.transactions = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
         this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    public void setBetaAccountIcon(String icon) {
        this.betaAccountIcon = icon;
        notifyDataSetChanged();
    }

    public void setBetaAccount(BetaAccount account) {
        this.betaAccount = account;
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
        holder.tvDate.setText(dateFormat.format(new Date(transaction.getEntryTime())));
        holder.tvTime.setText(timeFormat.format(new Time(transaction.getEntryTime())));
        holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.green));

        // Load beta account icon
        try {
            String iconPath = betaAccountIcon.replace("Assets/", "");
            InputStream is = context.getAssets().open(iconPath);
            holder.transaction_icon.setImageBitmap(BitmapFactory.decodeStream(is));
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Set long click listener
        holder.itemView.setOnLongClickListener(v -> {
            if (betaAccount != null) {
                EditTransactionDialog dialog = new EditTransactionDialog(context, 
                    MeshaDatabase.Get_database(context), transaction, betaAccount);
                dialog.setOnTransactionEditedListener(new EditTransactionDialog.OnTransactionEditedListener() {
                    @Override
                    public void onTransactionEdited() {
                        // Refresh the list after edit
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onTransactionDeleted() {
                        // Remove the item and refresh
                        int pos = transactions.indexOf(transaction);
                        if (pos != -1) {
                            transactions.remove(pos);
                            notifyItemRemoved(pos);
                        }
                    }
                });
                dialog.show();
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvAmount, tvDate, tvTime;
        ImageView transaction_icon;

        TransactionViewHolder(View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvTransactionDescription);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
            tvDate = itemView.findViewById(R.id.tvTransactionDate);
            tvTime = itemView.findViewById(R.id.tvTransactionTime);
            transaction_icon = itemView.findViewById(R.id.transaction_icon);
        }
    }
} 