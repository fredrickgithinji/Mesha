package com.dzovah.mesha.Methods.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import com.dzovah.mesha.Activities.Adapters.IconAdapter;
import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.R;
import com.google.android.material.textfield.TextInputEditText;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;

import java.io.IOException;

public class CreateAccountDialog {
    private final Context context;
    private final MeshaDatabase database;
    private OnAccountCreatedListener listener;
    private AlertDialog dialog;
    private final boolean isBetaAccount;
    private final int parentAlphaId;

    public interface OnAccountCreatedListener {
        void onAccountCreated(Object account); // Changed to Object to handle both types
    }

    // Constructor for Alpha Account
    public CreateAccountDialog(Context context, MeshaDatabase database) {
        this.context = context;
        this.database = database;
        this.isBetaAccount = false;
        this.parentAlphaId = -1;
    }

    // Constructor for Beta Account
    public CreateAccountDialog(Context context, MeshaDatabase database, int alphaAccountId) {
        this.context = context;
        this.database = database;
        this.isBetaAccount = true;
        this.parentAlphaId = alphaAccountId;
    }

    public void setOnAccountCreatedListener(OnAccountCreatedListener listener) {
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
        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Setup icon recycler view
        String[] iconPaths = CreateAccountDialog.getIconPaths(context);  // Updated to use static method
        final String[] selectedIcon = {null};

        GridLayoutManager layoutManager = new GridLayoutManager(context, 4);
        rvIcons.setLayoutManager(layoutManager);
        IconAdapter adapter = new IconAdapter(iconPaths, iconPath -> selectedIcon[0] = iconPath);
        rvIcons.setAdapter(adapter);

        btnCreate.setOnClickListener(v -> {
            String accountName = etAccountName.getText().toString();
            if (accountName.isEmpty()) {
                Toast.makeText(context, "Please enter an account name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isBetaAccount) {
                createBetaAccount(accountName, selectedIcon[0]);
            } else {
                createAlphaAccount(accountName, selectedIcon[0]);
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (dialog != null) {
                dialog.dismiss();
            }
        });

        return dialogView;
    }

    public static String[] getIconPaths(Context context) {
        try {
            String[] paths = context.getAssets().list("icons");
            Log.d("IconDebug", "Found " + (paths != null ? paths.length : 0) + " icons");
            if (paths != null) {
                for (String path : paths) {
                    Log.d("IconDebug", "Icon path: " + path);
                }
            }
            return paths != null ? paths : new String[0];
        } catch (IOException e) {
            Log.e("IconDebug", "Error loading icons: " + e.getMessage());
            e.printStackTrace();
            return new String[0];
        }
    }

    private void createAlphaAccount(String accountName, String selectedIcon) {
        try {
            String iconPath = selectedIcon != null ? "Assets/icons/" + selectedIcon : "Assets/icons/default_icon.png";
            AlphaAccount newAccount = new AlphaAccount(accountName, iconPath, 0.0);
            
            MeshaDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    database.alphaAccountDao().insert(newAccount);
                    handleSuccess(newAccount);
                } catch (Exception e) {
                    handleError(e);
                }
            });
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void createBetaAccount(String accountName, String selectedIcon) {
        try {
            String iconPath = selectedIcon != null ? "Assets/icons/" + selectedIcon : "Assets/icons/default_icon.png";
            BetaAccount newAccount = new BetaAccount(parentAlphaId, accountName, iconPath, 0.0);
            
            MeshaDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    database.betaAccountDao().insert(newAccount);
                    database.betaAccountDao().updateAlphaAccountBalance(parentAlphaId);
                    handleSuccess(newAccount);
                } catch (Exception e) {
                    handleError(e);
                }
            });
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleSuccess(Object account) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() -> {
                if (listener != null) {
                    listener.onAccountCreated(account);
                }
                if (dialog != null) {
                    dialog.dismiss();
                }
            });
        }
    }

    private void handleError(Exception e) {
        e.printStackTrace();
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() ->
                Toast.makeText(context, "Failed to create account", Toast.LENGTH_SHORT).show()
            );
        }
    }
}