package com.dzovah.mesha.Activities.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;
import com.dzovah.mesha.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for selecting a single BetaAccount from a list.
 * <p>
 * This adapter displays BetaAccount items with radio buttons to allow
 * the user to select a single account. It maintains the currently selected
 * account position and notifies a listener when a selection is made.
 * Each item displays the account name and current balance.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see RecyclerView.Adapter
 * @see BetaAccount
 */
public class SelectBetaAccountAdapter extends RecyclerView.Adapter<SelectBetaAccountAdapter.AccountViewHolder> {
    /** Application context used for inflating layouts and accessing resources */
    private final Context context;
    
    /** List of BetaAccount objects to display in the RecyclerView */
    private List<BetaAccount> accounts;
    
    /** Index of the currently selected account, -1 if no selection */
    private int selectedPosition = -1;
    
    /** Listener to notify when an account is selected */
    private OnAccountSelectedListener listener;

    /**
     * Interface for notifying when a BetaAccount has been selected.
     */
    public interface OnAccountSelectedListener {
        /**
         * Called when a user selects a BetaAccount.
         *
         * @param account The BetaAccount that was selected
         */
        void onAccountSelected(BetaAccount account);
    }

    /**
     * Constructs a new SelectBetaAccountAdapter with an empty account list.
     *
     * @param context The context used for inflating layouts and accessing resources
     */
    public SelectBetaAccountAdapter(Context context) {
        this.context = context;
        this.accounts = new ArrayList<>();
    }

    /**
     * Sets the listener to notify when an account is selected.
     *
     * @param listener The listener to notify on account selection
     */
    public void setOnAccountSelectedListener(OnAccountSelectedListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the list of BetaAccount objects displayed by this adapter.
     * <p>
     * This method will trigger a UI refresh to show the new account list.
     * </p>
     *
     * @param accounts The new list of BetaAccount objects to display
     */
    public void setAccounts(List<BetaAccount> accounts) {
        this.accounts = accounts;
        notifyDataSetChanged();
    }

    /**
     * Creates a new ViewHolder for account selection items.
     * <p>
     * This method inflates the item_select_beta_account layout for each
     * item in the RecyclerView.
     * </p>
     *
     * @param parent The parent ViewGroup into which the new View will be added
     * @param viewType The view type of the new View (not used in this implementation)
     * @return A new AccountViewHolder that holds the View for each account selection item
     */
    @Override
    public AccountViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_select_beta_account, parent, false);
        return new AccountViewHolder(view);
    }

    /**
     * Binds account data to a ViewHolder.
     * <p>
     * This method populates the ViewHolder's views with account data, including
     * the account name and formatted balance. It also sets up click listeners for
     * both the list item and the radio button to handle account selection.
     * </p>
     *
     * @param holder The ViewHolder to update with account data
     * @param position The position of the account in the adapter's data set
     */
    @Override
    public void onBindViewHolder(AccountViewHolder holder, int position) {
        BetaAccount account = accounts.get(position);
        
        holder.tvAccountName.setText(account.getBetaAccountName());
        holder.tvAccountBalance.setText(CurrencyFormatter.format(account.getBetaAccountBalance()));
        
        // Handle selection
        holder.rbSelect.setChecked(position == selectedPosition);
        
        holder.itemView.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onAccountSelected(account);
            }
        });
        
        holder.rbSelect.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onAccountSelected(account);
            }
        });
    }

    /**
     * Returns the total number of accounts in the data set.
     *
     * @return The number of accounts in the accounts list
     */
    @Override
    public int getItemCount() {
        return accounts.size();
    }

    /**
     * ViewHolder class for caching views used in the account selection item layout.
     * <p>
     * This class holds references to the views within the item_select_beta_account layout
     * to improve recycling performance by avoiding repeated calls to findViewById().
     * </p>
     */
    static class AccountViewHolder extends RecyclerView.ViewHolder {
        /** RadioButton for selecting the account */
        RadioButton rbSelect;
        
        /** TextView for displaying the account name */
        TextView tvAccountName;
        
        /** TextView for displaying the account balance */
        TextView tvAccountBalance;

        /**
         * Constructs a new AccountViewHolder and finds the required views.
         *
         * @param itemView The account selection item view to hold and find references from
         */
        AccountViewHolder(View itemView) {
            super(itemView);
            rbSelect = itemView.findViewById(R.id.rbSelectAccount);
            tvAccountName = itemView.findViewById(R.id.tvSelectAccountName);
            tvAccountBalance = itemView.findViewById(R.id.tvSelectAccountBalance);
        }
    }
}
