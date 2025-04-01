package com.dzovah.mesha.Activities.Adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.dzovah.mesha.Activities.BetaAccountDetailActivity;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Methods.Dialogs.EditAccountDialog;
import com.dzovah.mesha.R;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

import com.dzovah.mesha.Database.Utils.CurrencyFormatter;

/**
 * RecyclerView adapter for displaying BetaAccount items in a list.
 * <p>
 * This adapter populates a RecyclerView with BetaAccount items, displaying
 * the account name, balance, and icon. It handles click events for viewing
 * account details and long-press gestures for editing account information.
 * </p>
 * <p>
 * When a user clicks on a BetaAccount item, they are taken to the
 * BetaAccountDetailActivity to view detailed information and transactions
 * associated with that account.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see RecyclerView.Adapter
 * @see BetaAccount
 * @see BetaAccountDetailActivity
 */
public class BetaAccountAdapter extends RecyclerView.Adapter<BetaAccountAdapter.BetaAccountViewHolder> {
    /** List of BetaAccount objects to display */
    private List<BetaAccount> betaAccounts;
    
    /** Application context used for inflating layouts and accessing resources */
    private final Context context;
    
    /** Listener for account interaction events */
    private OnAccountActionListener actionListener;

    /**
     * Constructs a new BetaAccountAdapter.
     *
     * @param context The context used for inflating layouts and accessing resources
     */
    public BetaAccountAdapter(Context context) {
        this.context = context;
    }

    /**
     * Updates the list of beta accounts displayed by this adapter.
     * <p>
     * This method will trigger a UI refresh to show the new account list.
     * </p>
     *
     * @param betaAccounts The new list of BetaAccount objects to display
     */
    public void setBetaAccounts(List<BetaAccount> betaAccounts) {
        this.betaAccounts = betaAccounts;
        notifyDataSetChanged();
    }

    /**
     * Handles the click event on a BetaAccount item.
     * <p>
     * This method creates an Intent to launch the BetaAccountDetailActivity,
     * passing the selected account's ID as an extra.
     * </p>
     *
     * @param account The BetaAccount that was clicked
     * @param position The position of the clicked account in the adapter
     */
    private void handleItemClick(BetaAccount account, int position) {
        Intent intent = new Intent(context, BetaAccountDetailActivity.class);
        intent.putExtra("beta_account_id", account.getBetaAccountId());
        ((Activity) context).startActivityForResult(intent, 1);
    }

    /**
     * Creates a new ViewHolder for beta account items.
     * <p>
     * This method inflates the beta_account_item layout for each item in the RecyclerView.
     * </p>
     *
     * @param parent The parent ViewGroup into which the new View will be added
     * @param viewType The view type of the new View (not used in this implementation)
     * @return A new BetaAccountViewHolder that holds the View for each account item
     */
    @Override
    public BetaAccountViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.beta_account_item, parent, false);
        return new BetaAccountViewHolder(view);
    }

    /**
     * Binds beta account data to a ViewHolder.
     * <p>
     * This method populates the ViewHolder's views with data from the specified account.
     * It sets the account name, formatted balance, and loads the account icon from assets.
     * It also configures click listeners for item selection and long-press for editing.
     * </p>
     *
     * @param holder The ViewHolder to update with account data
     * @param position The position of the account in the adapter's data set
     */
    @Override
    public void onBindViewHolder(BetaAccountViewHolder holder, int position) {
        BetaAccount account = betaAccounts.get(position);
        holder.tvAccountName.setText(account.getBetaAccountName());
        
        holder.tvAccountBalance.setText(CurrencyFormatter.format(account.getBetaAccountBalance()));

        try {
            String iconPath = account.getBetaAccountIcon().replace("Assets/", "");
            InputStream is = context.getAssets().open(iconPath);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            holder.ivAccountIcon.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        holder.itemView.setOnClickListener(v -> handleItemClick(account, position));

        // Add long-press listener for editing/deleting beta accounts
        holder.itemView.setOnLongClickListener(v -> {
            EditAccountDialog dialog = new EditAccountDialog(context, MeshaDatabase.Get_database(context), account);
            dialog.setOnAccountEditedListener(new EditAccountDialog.OnAccountEditedListener() {
                @Override
                public void onAccountEdited() {
                    if (actionListener != null) {
                        actionListener.onAccountUpdated();
                    }
                }
                
                @Override
                public void onAccountDeleted() {
                    if (actionListener != null) {
                        actionListener.onAccountUpdated();
                    }
                }
            });
            dialog.show();
            return true;
        });
    }

    /**
     * Returns the total number of beta accounts in the data set.
     *
     * @return The number of beta accounts, or 0 if the betaAccounts list is null
     */
    @Override
    public int getItemCount() {
        return betaAccounts != null ? betaAccounts.size() : 0;
    }

    /**
     * ViewHolder class for caching views used in the beta account item layout.
     * <p>
     * This class holds references to the views within the beta_account_item layout
     * to improve recycling performance by avoiding repeated calls to findViewById().
     * </p>
     */
    static class BetaAccountViewHolder extends RecyclerView.ViewHolder {
        /** ImageView for displaying the account icon */
        ImageView ivAccountIcon;
        
        /** TextView for displaying the account name */
        TextView tvAccountName;
        
        /** TextView for displaying the account balance */
        TextView tvAccountBalance;

        /**
         * Constructs a new BetaAccountViewHolder and finds all required views.
         *
         * @param itemView The account item view to hold and find references from
         */
        BetaAccountViewHolder(View itemView) {
            super(itemView);
            ivAccountIcon = itemView.findViewById(R.id.alpha_account_icon);
            tvAccountName = itemView.findViewById(R.id.alpha_account_name);
            tvAccountBalance = itemView.findViewById(R.id.alpha_account_balance);
        }
    }

    /**
     * Sets the listener for account interaction events.
     *
     * @param listener The listener to be notified of account interactions
     */
    public void setOnAccountActionListener(OnAccountActionListener listener) {
        this.actionListener = listener;
    }

    /**
     * Interface for handling account-related user interactions.
     * <p>
     * This interface defines a callback method that is triggered when
     * a beta account has been updated or deleted.
     * </p>
     */
    public interface OnAccountActionListener {
        /**
         * Called when an account has been updated or deleted.
         * <p>
         * This notifies the listener that the account list may need refreshing.
         * </p>
         */
        void onAccountUpdated();
    }
}