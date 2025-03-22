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

public class SelectBetaAccountAdapter extends RecyclerView.Adapter<SelectBetaAccountAdapter.AccountViewHolder> {
    private final Context context;
    private List<BetaAccount> accounts;
    private int selectedPosition = -1;
    private OnAccountSelectedListener listener;

    public interface OnAccountSelectedListener {
        void onAccountSelected(BetaAccount account);
    }

    public SelectBetaAccountAdapter(Context context) {
        this.context = context;
        this.accounts = new ArrayList<>();
    }

    public void setOnAccountSelectedListener(OnAccountSelectedListener listener) {
        this.listener = listener;
    }

    public void setAccounts(List<BetaAccount> accounts) {
        this.accounts = accounts;
        notifyDataSetChanged();
    }

    @Override
    public AccountViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_select_beta_account, parent, false);
        return new AccountViewHolder(view);
    }

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

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        RadioButton rbSelect;
        TextView tvAccountName;
        TextView tvAccountBalance;

        AccountViewHolder(View itemView) {
            super(itemView);
            rbSelect = itemView.findViewById(R.id.rbSelectAccount);
            tvAccountName = itemView.findViewById(R.id.tvSelectAccountName);
            tvAccountBalance = itemView.findViewById(R.id.tvSelectAccountBalance);
        }
    }
}
