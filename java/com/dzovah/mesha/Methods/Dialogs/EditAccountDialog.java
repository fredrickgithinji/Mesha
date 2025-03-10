package com.dzovah.mesha.Methods.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dzovah.mesha.Activities.Adapters.IconAdapter;
import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.R;
import com.google.android.material.textfield.TextInputEditText;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;

public class EditAccountDialog {
    private final Context context;
    private final MeshaDatabase database;
    private final Object account;
    private OnAccountEditedListener listener;
    private AlertDialog dialog;
    private final boolean isBetaAccount;

    public interface OnAccountEditedListener {
        void onAccountEdited();
        void onAccountDeleted();
    }

    public EditAccountDialog(Context context, MeshaDatabase database, Object account) {
        this.context = context;
        this.database = database;
        this.account = account;
        this.isBetaAccount = account instanceof BetaAccount;
    }

    public void setOnAccountEditedListener(OnAccountEditedListener listener) {
        this.listener = listener;
    }

    public void show() {
        View dialogView = getView();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        dialog = builder.create();
        dialog.show();
    }

    private View getView() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_account, null);
        TextInputEditText etAccountName = dialogView.findViewById(R.id.etAccountName);
        RecyclerView rvIcons = dialogView.findViewById(R.id.rvIcons);
        Button btnSave = dialogView.findViewById(R.id.btnCreate);
        Button btnDelete = dialogView.findViewById(R.id.btnCancel);

        // Set current values based on account type
        String currentName = isBetaAccount ? 
            ((BetaAccount)account).getBetaAccountName() : 
            ((AlphaAccount)account).getAlphaAccountName();
        String currentIcon = isBetaAccount ? 
            ((BetaAccount)account).getBetaAccountIcon() : 
            ((AlphaAccount)account).getAlphaAccountIcon();

        etAccountName.setText(currentName);
        btnSave.setText("Save Changes");
        btnDelete.setText("Delete Account");

        String[] iconPaths = CreateAccountDialog.getIconPaths(context);
        final String[] selectedIcon = {currentIcon};
        
        GridLayoutManager layoutManager = new GridLayoutManager(context, 4);
        rvIcons.setLayoutManager(layoutManager);
        IconAdapter adapter = new IconAdapter(iconPaths, iconPath -> selectedIcon[0] = iconPath);
        rvIcons.setAdapter(adapter);

        btnSave.setOnClickListener(v -> {
            String newName = etAccountName.getText().toString();
            if (newName.isEmpty()) {
                Toast.makeText(context, "Please enter an account name", Toast.LENGTH_SHORT).show();
                return;
            }
            updateAccount(newName, selectedIcon[0]);
        });

        btnDelete.setOnClickListener(v -> {
            String message = isBetaAccount ? 
                "Are you sure you want to delete this account? This will delete all associated transactions." :
                "Are you sure you want to delete this account? This will delete all associated beta accounts and transactions.";

            new AlertDialog.Builder(context)
                .setTitle("Delete Account")
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
        });

        return dialogView;
    }

    private void updateAccount(String newName, String newIcon) {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                String iconPath = newIcon != null ? "Assets/icons/" + newIcon : 
                    (isBetaAccount ? ((BetaAccount)account).getBetaAccountIcon() : 
                    ((AlphaAccount)account).getAlphaAccountIcon());

                if (isBetaAccount) {
                    BetaAccount betaAccount = (BetaAccount)account;
                    betaAccount.setBetaAccountName(newName);
                    betaAccount.setBetaAccountIcon(iconPath);
                    database.betaAccountDao().update(betaAccount);
                } else {
                    AlphaAccount alphaAccount = (AlphaAccount)account;
                    alphaAccount.setAlphaAccountName(newName);
                    alphaAccount.setAlphaAccountIcon(iconPath);
                    database.alphaAccountDao().update(alphaAccount);
                }
                
                handleSuccess();
            } catch (Exception e) {
                handleError(e);
            }
        });
    }

    private void deleteAccount() {
        MeshaDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (isBetaAccount) {
                    BetaAccount betaAccount = (BetaAccount)account;
                    database.betaAccountDao().delete(betaAccount);
                    database.betaAccountDao().updateAlphaAccountBalance(betaAccount.getAlphaAccountId());
                } else {
                    database.alphaAccountDao().delete((AlphaAccount)account);
                }
                handleSuccess();
            } catch (Exception e) {
                handleError(e);
            }
        });
    }

    private void handleSuccess() {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() -> {
                if (listener != null) {
                    if (dialog.getCurrentFocus() == dialog.findViewById(R.id.btnCancel)) {
                        listener.onAccountDeleted();
                    } else {
                        listener.onAccountEdited();
                    }
                }
                dialog.dismiss();
            });
        }
    }

    private void handleError(Exception e) {
        e.printStackTrace();
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() ->
                Toast.makeText(context, "Operation failed", Toast.LENGTH_SHORT).show()
            );
        }
    }
} 