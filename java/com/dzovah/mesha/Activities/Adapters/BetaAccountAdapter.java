package com.dzovah.mesha.Activities.Adapters;

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

public class BetaAccountAdapter extends RecyclerView.Adapter<BetaAccountAdapter.BetaAccountViewHolder> {
    private List<BetaAccount> betaAccounts;
    private final Context context;
    private OnAccountActionListener actionListener;

    public BetaAccountAdapter(Context context) {
        this.context = context;
    }

    public void setBetaAccounts(List<BetaAccount> betaAccounts) {
        this.betaAccounts = betaAccounts;
        notifyDataSetChanged();
    }

    private void handleItemClick(BetaAccount account, int position) {
        Intent intent = new Intent(context, BetaAccountDetailActivity.class);
        intent.putExtra("beta_account_id", account.getBetaAccountId());
        context.startActivity(intent);
    }

    @Override
    public BetaAccountViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_beta_account, parent, false);
        return new BetaAccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BetaAccountViewHolder holder, int position) {
        BetaAccount account = betaAccounts.get(position);
        holder.tvAccountName.setText(account.getBetaAccountName());
        
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
        holder.tvAccountBalance.setText(format.format(account.getBetaAccountBalance()));

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

    @Override
    public int getItemCount() {
        return betaAccounts != null ? betaAccounts.size() : 0;
    }

    static class BetaAccountViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAccountIcon;
        TextView tvAccountName;
        TextView tvAccountBalance;

        BetaAccountViewHolder(View itemView) {
            super(itemView);
            ivAccountIcon = itemView.findViewById(R.id.ivAccountIcon);
            tvAccountName = itemView.findViewById(R.id.tvAccountName);
            tvAccountBalance = itemView.findViewById(R.id.tvAccountBalance);
        }
    }

    public void setOnAccountActionListener(OnAccountActionListener listener) {
        this.actionListener = listener;
    }

    public interface OnAccountActionListener {
        void onAccountUpdated();
    }
} 