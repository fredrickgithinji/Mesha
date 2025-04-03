package com.dzovah.mesha.PActivities.PAdapters;

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

import com.dzovah.mesha.Database.Entities.PBetaAccount;
import com.dzovah.mesha.Database.Entities.PTransaction;
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

/**
 * RecyclerView adapter for displaying transaction items in the analysis view.
 * <p>
 * This adapter populates a RecyclerView with Transaction items specifically for
 * the analysis section of the app, displaying the transaction description, amount,
 * date, and associated beta account information.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see RecyclerView.Adapter
 * @see PTransaction
 */
public class PAnalysisTransactionAdapter extends RecyclerView.Adapter<PAnalysisTransactionAdapter.ViewHolder> {
    /** Application context used for inflating layouts and accessing resources */
    private final Context context;
    /** List of Transaction objects to display */
    private List<PTransaction> transactions;
    /** Date formatter for displaying transaction dates */
    private final SimpleDateFormat dateFormat;
    /** Listener for transaction click events */
    private OnTransactionClickListener listener;
    /** Database instance for accessing beta account data */
    private final MeshaDatabase database;

    /**
     * Interface for handling transaction click events.
     */
    public interface OnTransactionClickListener {
        /**
         * Called when a transaction item is clicked.
         *
         * @param transaction The Transaction that was clicked
         */
        void onTransactionClick(PTransaction transaction);
    }

    /**
     * Constructs a new AnalysisTransactionAdapter.
     *
     * @param context The context used for inflating layouts and accessing resources
     */
    public PAnalysisTransactionAdapter(Context context) {
        this.context = context;
        this.transactions = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        this.database = MeshaDatabase.Get_database(context);
    }

    /**
     * Updates the list of transactions displayed by this adapter.
     *
     * @param transactions The new list of Transaction objects to display
     */
    public void setTransactions(List<PTransaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    /**
     * Sets the listener for transaction click events.
     *
     * @param listener The listener to be notified of transaction clicks
     */
    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    /**
     * Creates a new ViewHolder for analysis transaction items.
     *
     * @param parent The parent ViewGroup into which the new View will be added
     * @param viewType The view type of the new View (not used in this implementation)
     * @return A new ViewHolder that holds the View for each transaction item
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_analysis_transaction, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds transaction data to a ViewHolder.
     * <p>
     * Sets the transaction description, formatted amount, and date. Also loads
     * the associated beta account details and configures a click listener.
     * </p>
     *
     * @param holder The ViewHolder to update with transaction data
     * @param position The position of the transaction in the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PTransaction transaction = transactions.get(position);
        
        holder.tvDescription.setText(transaction.getPTransactionDescription());
        holder.tvAmount.setText(CurrencyFormatter.format(Math.abs(transaction.getPTransactionAmount())));
        holder.tvDate.setText(dateFormat.format(new Date(transaction.getPEntryTime())));

        // Set all amounts to green
        holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.green));

        // Load beta account details
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                PBetaAccount betaAccount = database.PbetaAccountDao()
                    .getPBetaAccountById(transaction.getPBetaAccountId());
                
                if (betaAccount != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        context.getMainExecutor().execute(() -> {
                            holder.tvBetaAccountName.setText(betaAccount.getPBetaAccountName());

                            try {
                                String iconPath = betaAccount.getPBetaAccountIcon().replace("Assets/", "");
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

    /**
     * Returns the total number of transactions in the data set.
     *
     * @return The number of transactions
     */
    @Override
    public int getItemCount() {
        return transactions.size();
    }

    /**
     * ViewHolder class for caching views used in the analysis transaction item layout.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        /** TextView for displaying the transaction description */
        TextView tvDescription;
        /** TextView for displaying the transaction amount */
        TextView tvAmount;
        /** TextView for displaying the transaction date */
        TextView tvDate;
        /** TextView for displaying the beta account name */
        TextView tvBetaAccountName;
        /** ImageView for displaying the associated account icon */
        ImageView transaction_icon;

        /**
         * Constructs a new ViewHolder and finds all required views.
         *
         * @param itemView The transaction item view to hold and find references from
         */
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