package com.dzovah.mesha.PActivities.PAdapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.dzovah.mesha.Database.Entities.PAlphaAccount;
import com.dzovah.mesha.R;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;
import com.dzovah.mesha.Methods.Dialogs.EditAccountDialog;
import com.dzovah.mesha.Database.MeshaDatabase;
import java.util.ArrayList;
import com.dzovah.mesha.Database.Utils.CurrencyFormatter;

/**
 * RecyclerView adapter for displaying AlphaAccount items in a list.
 * <p>
 * This adapter populates a RecyclerView with AlphaAccount items, displaying
 * the account name, balance, and icon. It handles click events on account items,
 * which can be used for navigation or showing additional account details, as well
 * as long-press gestures for editing account information.
 * </p>
 * <p>
 * The adapter uses the {@link OnAccountActionListener} interface to communicate
 * user interactions back to the hosting activity or fragment.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see RecyclerView.Adapter
 * @see PAlphaAccount
 */
public class PAlphaAccountAdapter extends RecyclerView.Adapter<PAlphaAccountAdapter.AccountViewHolder> {
    /** Application context used for inflating layouts and accessing resources */
    private final Context context;
    
    /** List of AlphaAccount objects to display */
    private List<PAlphaAccount> accounts;
    
    /** Listener for account interaction events */
    private OnAccountActionListener actionListener;

    /**
     * Interface for handling account-related user interactions.
     * <p>
     * This interface defines callback methods that are triggered when the user
     * interacts with an account item in the RecyclerView.
     * </p>
     */
    public interface OnAccountActionListener {
        /**
         * Called when an account item is clicked.
         *
         * @param account The AlphaAccount that was clicked
         * @param position The position of the clicked account in the adapter
         */
        void onAccountClicked(PAlphaAccount account, int position);
        
        /**
         * Called when an account has been updated or deleted.
         * <p>
         * This notifies the listener that the account list may need refreshing.
         * </p>
         */
        void onAccountUpdated();
    }

    /**
     * Constructs a new AlphaAccountAdapter.
     *
     * @param context The context used for inflating layouts and accessing resources
     */
    public PAlphaAccountAdapter(Context context) {
        this.context = context;
        this.accounts = new ArrayList<>();
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
     * Updates the list of accounts displayed by this adapter.
     * <p>
     * This method will trigger a UI refresh to show the new account list.
     * </p>
     *
     * @param accounts The new list of AlphaAccount objects to display
     */
    public void setAccounts(List<PAlphaAccount> accounts) {
        this.accounts = accounts;
        notifyDataSetChanged();
    }

    /**
     * Creates a new ViewHolder for account items.
     * <p>
     * This method inflates the alpha_account_item layout for each item in the RecyclerView.
     * </p>
     *
     * @param parent The parent ViewGroup into which the new View will be added
     * @param viewType The view type of the new View (not used in this implementation)
     * @return A new AccountViewHolder that holds the View for each account item
     */
    @Override
    public AccountViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.alpha_account_item, parent, false);
        return new AccountViewHolder(view);
    }

    /**
     * Binds account data to a ViewHolder.
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
    public void onBindViewHolder(AccountViewHolder holder, int position) {
        PAlphaAccount account = accounts.get(position);
        holder.tvAccountName.setText(account.getPAlphaAccountName());
        holder.tvAccountBalance.setText(CurrencyFormatter.format(account.getPAlphaAccountBalance()));

        // Load icon from assets
        try {
            String iconPath = account.getPAlphaAccountIcon().replace("Assets/", "");
            InputStream is = context.getAssets().open(iconPath);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            holder.ivAccountIcon.setImageBitmap(bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onAccountClicked(account, position);
            }
        });

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
     * Returns the total number of accounts in the data set.
     *
     * @return The number of accounts, or 0 if the accounts list is null
     */
    @Override
    public int getItemCount() {
        return accounts != null ? accounts.size() : 0;
    }

    /**
     * ViewHolder class for caching views used in the account item layout.
     * <p>
     * This class holds references to the views within the alpha_account_item layout
     * to improve recycling performance by avoiding repeated calls to findViewById().
     * </p>
     */
    static class AccountViewHolder extends RecyclerView.ViewHolder {
        /** ImageView for displaying the account icon */
        ImageView ivAccountIcon;
        
        /** TextView for displaying the account name */
        TextView tvAccountName;
        
        /** TextView for displaying the account balance */
        TextView tvAccountBalance;

        /**
         * Constructs a new AccountViewHolder and finds all required views.
         *
         * @param itemView The account item view to hold and find references from
         */
        AccountViewHolder(View itemView) {
            super(itemView);
            ivAccountIcon = itemView.findViewById(R.id.alpha_account_icon);
            tvAccountName = itemView.findViewById(R.id.alpha_account_name);
            tvAccountBalance = itemView.findViewById(R.id.alpha_account_balance);
        }
    }
}