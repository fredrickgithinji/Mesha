package com.dzovah.mesha.Activities.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.dzovah.mesha.Database.Entities.AlphaAccount;
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

public class AlphaAccountAdapter extends RecyclerView.Adapter<AlphaAccountAdapter.AccountViewHolder> {
    private final Context context;
    private List<AlphaAccount> accounts;
    private OnAccountActionListener actionListener;

    public interface OnAccountActionListener {
        void onAccountClicked(AlphaAccount account, int position);
        void onAccountUpdated();
    }

    public AlphaAccountAdapter(Context context) {
        this.context = context;
        this.accounts = new ArrayList<>();
    }

    public void setOnAccountActionListener(OnAccountActionListener listener) {
        this.actionListener = listener;
    }

    public void setAccounts(List<AlphaAccount> accounts) {
        this.accounts = accounts;
        notifyDataSetChanged();
    }

    @Override
    public AccountViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.alpha_account_item, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AccountViewHolder holder, int position) {
        AlphaAccount account = accounts.get(position);
        holder.tvAccountName.setText(account.getAlphaAccountName());
        holder.tvAccountBalance.setText(CurrencyFormatter.format(account.getAlphaAccountBalance()));

        // Load icon from assets
        try {
            String iconPath = account.getAlphaAccountIcon().replace("Assets/", "");
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

    @Override
    public int getItemCount() {
        return accounts != null ? accounts.size() : 0;
    }

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAccountIcon;
        TextView tvAccountName;
        TextView tvAccountBalance;

        AccountViewHolder(View itemView) {
            super(itemView);
            ivAccountIcon = itemView.findViewById(R.id.alpha_account_icon);
            tvAccountName = itemView.findViewById(R.id.alpha_account_name);
            tvAccountBalance = itemView.findViewById(R.id.alpha_account_balance);
        }
    }
} 