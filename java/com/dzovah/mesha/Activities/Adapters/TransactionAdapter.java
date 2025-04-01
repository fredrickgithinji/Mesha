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

/**
 * RecyclerView adapter for displaying financial transaction items in a list.
 * <p>
 * This adapter populates a RecyclerView with Transaction items, displaying
 * the transaction description, amount, date, time, and associated account icon.
 * It handles formatting of currency values and timestamps, as well as long-press
 * interactions for editing or deleting transactions.
 * </p>
 * <p>
 * The adapter maintains a reference to the BetaAccount associated with these
 * transactions to facilitate editing operations and to display the correct icon.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see RecyclerView.Adapter
 * @see Transaction
 * @see EditTransactionDialog
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    /** Application context used for inflating layouts and accessing resources */
    private final Context context;
    
    /** List of Transaction objects to display */
    private List<Transaction> transactions;
    
    /** Path to the icon image for the beta account */
    private String betaAccountIcon;
    
    /** Date formatter for displaying transaction dates */
    private final SimpleDateFormat dateFormat;
    
    /** Time formatter for displaying transaction times */
    private final SimpleDateFormat timeFormat;
    
    /** The BetaAccount associated with these transactions */
    private BetaAccount betaAccount;

    /**
     * Constructs a new TransactionAdapter.
     * <p>
     * Initializes date and time formatters with locale-appropriate patterns.
     * </p>
     *
     * @param context The context used for inflating layouts and accessing resources
     */
    public TransactionAdapter(Context context) {
        this.context = context;
        this.transactions = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
         this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    /**
     * Updates the list of transactions displayed by this adapter.
     * <p>
     * This method will trigger a UI refresh to show the new transaction list.
     * </p>
     *
     * @param transactions The new list of Transaction objects to display
     */
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    /**
     * Sets the icon for the beta account associated with these transactions.
     * <p>
     * The icon will be displayed alongside each transaction in the list.
     * </p>
     *
     * @param icon The asset path to the beta account icon
     */
    public void setBetaAccountIcon(String icon) {
        this.betaAccountIcon = icon;
        notifyDataSetChanged();
    }

    /**
     * Sets the BetaAccount associated with these transactions.
     * <p>
     * This account reference is used when editing transactions through the
     * EditTransactionDialog.
     * </p>
     *
     * @param account The BetaAccount associated with these transactions
     */
    public void setBetaAccount(BetaAccount account) {
        this.betaAccount = account;
    }

    /**
     * Creates a new ViewHolder for transaction items.
     * <p>
     * This method inflates the item_transaction layout for each item in the RecyclerView.
     * </p>
     *
     * @param parent The parent ViewGroup into which the new View will be added
     * @param viewType The view type of the new View (not used in this implementation)
     * @return A new TransactionViewHolder that holds the View for each transaction item
     */
    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    /**
     * Binds transaction data to a ViewHolder.
     * <p>
     * This method populates the ViewHolder's views with data from the specified transaction.
     * It sets the transaction description, formatted amount, date, time, and loads the 
     * associated account icon. It also configures a long-press listener for editing or
     * deleting the transaction.
     * </p>
     *
     * @param holder The ViewHolder to update with transaction data
     * @param position The position of the transaction in the adapter's data set
     */
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
     * ViewHolder class for caching views used in the transaction item layout.
     * <p>
     * This class holds references to the views within the item_transaction layout
     * to improve recycling performance by avoiding repeated calls to findViewById().
     * </p>
     */
    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        /** TextView for displaying the transaction description */
        TextView tvDescription;
        
        /** TextView for displaying the transaction amount */
        TextView tvAmount;
        
        /** TextView for displaying the transaction date */
        TextView tvDate;
        
        /** TextView for displaying the transaction time */
        TextView tvTime;
        
        /** ImageView for displaying the associated account icon */
        ImageView transaction_icon;

        /**
         * Constructs a new TransactionViewHolder and finds all required views.
         *
         * @param itemView The transaction item view to hold and find references from
         */
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