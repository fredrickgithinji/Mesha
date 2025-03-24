package com.dzovah.mesha.Activities.Adapters;

import static com.dzovah.mesha.R.drawable.icon_mesha;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;
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
import com.dzovah.mesha.R;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnalysisTransactionAdapter extends RecyclerView.Adapter<AnalysisTransactionAdapter.ViewHolder> {
    private final Context context;
    private List<Transaction> transactions;
    private final SimpleDateFormat dateFormat;
    private OnTransactionClickListener listener;
    private final MeshaDatabase database;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public AnalysisTransactionAdapter(Context context) {
        this.context = context;
        this.transactions = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        this.database = MeshaDatabase.Get_database(context);
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_analysis_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        
        holder.tvDescription.setText(transaction.getTransactionDescription());
        holder.tvAmount.setText(CurrencyFormatter.format(Math.abs(transaction.getTransactionAmount())));
        holder.tvDate.setText(dateFormat.format(new Date(transaction.getEntryTime())));

        // Set all amounts to green
        holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.green));

        // Load beta account details
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                BetaAccount betaAccount = database.betaAccountDao()
                    .getBetaAccountById(transaction.getBetaAccountId());
                
                if (betaAccount != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        context.getMainExecutor().execute(() -> {
                            holder.tvBetaAccountName.setText(betaAccount.getBetaAccountName());

                            try {
                                String iconPath = betaAccount.getBetaAccountIcon().replace("Assets/", "");
                                InputStream is = context.getAssets().open(iconPath);
                                holder.transaction_icon.setImageBitmap(BitmapFactory.decodeStream(is));
                                is.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                                holder.transaction_icon.setImageResource(icon_mesha);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    context.getMainExecutor().execute(() ->
                        holder.transaction_icon.setImageResource(icon_mesha)
                    );
                }
            }
        });

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransactionClick(transaction);
            }
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvAmount, tvDate, tvBetaAccountName;
        ImageView transaction_icon;

        ViewHolder(View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvTransactionDescription);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
            tvDate = itemView.findViewById(R.id.tvTransactionDate);
            tvBetaAccountName = itemView.findViewById(R.id.tvBetaAccountName);
            transaction_icon = itemView.findViewById(R.id.transaction_icon);
        }
    }
} 