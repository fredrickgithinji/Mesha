package com.dzovah.mesha.Database.Services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dzovah.mesha.Database.Daos.AlphaAccountDao;
import com.dzovah.mesha.Database.Daos.BetaAccountDao;
import com.dzovah.mesha.Database.Daos.PAlphaAccountDao;
import com.dzovah.mesha.Database.Daos.PBetaAccountDao;
import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.Entities.PAlphaAccount;
import com.dzovah.mesha.Database.Entities.PBetaAccount;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.Database.Utils.TransactionManager;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Service class to manage account operations and ensure data integrity.
 * <p>
 * This class provides an abstraction layer over the database operations
 * and ensures that data integrity is maintained when working with both
 * regular and hidden account systems.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 */
public class AccountService {
    private static final String TAG = "AccountService";
    private final MeshaDatabase database;
    private final TransactionManager transactionManager;
    private final Executor executor;

    /**
     * Constructor for the AccountService.
     *
     * @param context The application context
     */
    public AccountService(@NonNull Context context) {
        this.database = MeshaDatabase.Get_database(context);
        this.transactionManager = new TransactionManager(database);
        this.executor = MeshaDatabase.databaseWriteExecutor;
    }

    /**
     * Creates a new Alpha account.
     *
     * @param alphaAccount The Alpha account to create
     * @param callback Callback to be invoked when the operation completes
     */
    public void createAlphaAccount(AlphaAccount alphaAccount, ServiceCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                if (!alphaAccount.validateData()) {
                    callback.onError("Invalid Alpha account data");
                    return;
                }
                
                database.alphaAccountDao().insert(alphaAccount);
                // Since Room generates the ID, we need to query to get the latest ID
                List<AlphaAccount> accounts = database.alphaAccountDao().getAllAlphaAccounts();
                if (!accounts.isEmpty()) {
                    // Assume the latest inserted account is the last in the list when sorted
                    AlphaAccount createdAccount = accounts.get(accounts.size() - 1);
                    callback.onSuccess(createdAccount.getAlphaAccountId());
                } else {
                    callback.onError("Failed to retrieve created account");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating Alpha account", e);
                callback.onError("Error creating Alpha account: " + e.getMessage());
            }
        });
    }

    /**
     * Creates a new hidden PAlpha account.
     *
     * @param pAlphaAccount The PAlpha account to create
     * @param callback Callback to be invoked when the operation completes
     */
    public void createPAlphaAccount(PAlphaAccount pAlphaAccount, ServiceCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                if (!pAlphaAccount.validateData()) {
                    callback.onError("Invalid PAlpha account data");
                    return;
                }
                
                database.PalphaAccountDao().insert(pAlphaAccount);
                // Since Room generates the ID, we need to query to get the latest ID
                List<PAlphaAccount> accounts = database.PalphaAccountDao().getAllPAlphaAccounts();
                if (!accounts.isEmpty()) {
                    // Assume the latest inserted account is the last in the list when sorted
                    PAlphaAccount createdAccount = accounts.get(accounts.size() - 1);
                    callback.onSuccess(createdAccount.getPAlphaAccountId());
                } else {
                    callback.onError("Failed to retrieve created account");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating PAlpha account", e);
                callback.onError("Error creating PAlpha account: " + e.getMessage());
            }
        });
    }

    /**
     * Creates a new Beta account.
     *
     * @param betaAccount The Beta account to create
     * @param callback Callback to be invoked when the operation completes
     */
    public void createBetaAccount(BetaAccount betaAccount, ServiceCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                database.betaAccountDao().insert(betaAccount);
                
                // Get the ID of the newly created Beta account
                // This is a simplification - you may need to query for it by other attributes
                List<BetaAccount> betaAccounts = database.betaAccountDao().getBetaAccountsByAlphaAccountId(betaAccount.getAlphaAccountId());
                int betaAccountId = -1;
                for (BetaAccount beta : betaAccounts) {
                    if (beta.getBetaAccountName().equals(betaAccount.getBetaAccountName())) {
                        betaAccountId = beta.getBetaAccountId();
                        break;
                    }
                }
                
                if (betaAccountId != -1) {
                    // Update parent Alpha account balance
                    final int finalBetaAccountId = betaAccountId;
                    boolean updated = transactionManager.updateBetaAndAlphaBalance(finalBetaAccountId);
                    if (!updated) {
                        Log.w(TAG, "Failed to update Alpha account balance after Beta account creation");
                    }
                    
                    callback.onSuccess(finalBetaAccountId);
                } else {
                    callback.onError("Failed to retrieve created Beta account ID");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating Beta account", e);
                callback.onError("Error creating Beta account: " + e.getMessage());
            }
        });
    }

    /**
     * Creates a new hidden PBeta account.
     *
     * @param pBetaAccount The PBeta account to create
     * @param callback Callback to be invoked when the operation completes
     */
    public void createPBetaAccount(PBetaAccount pBetaAccount, ServiceCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                database.PbetaAccountDao().insert(pBetaAccount);
                
                // Get the ID of the newly created PBeta account
                // This is a simplification - you may need to query for it by other attributes
                List<PBetaAccount> pBetaAccounts = database.PbetaAccountDao().getPBetaAccountsByPAlphaAccountId(pBetaAccount.getPAlphaAccountId());
                int pBetaAccountId = -1;
                for (PBetaAccount pBeta : pBetaAccounts) {
                    if (pBeta.getPBetaAccountName().equals(pBetaAccount.getPBetaAccountName())) {
                        pBetaAccountId = pBeta.getPBetaAccountId();
                        break;
                    }
                }
                
                if (pBetaAccountId != -1) {
                    // Update parent PAlpha account balance
                    final int finalPBetaAccountId = pBetaAccountId;
                    boolean updated = transactionManager.updatePBetaAndPAlphaBalance(finalPBetaAccountId);
                    if (!updated) {
                        Log.w(TAG, "Failed to update PAlpha account balance after PBeta account creation");
                    }
                    
                    callback.onSuccess(finalPBetaAccountId);
                } else {
                    callback.onError("Failed to retrieve created PBeta account ID");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating PBeta account", e);
                callback.onError("Error creating PBeta account: " + e.getMessage());
            }
        });
    }

    /**
     * Updates an Alpha account.
     *
     * @param alphaAccount The updated Alpha account
     * @param callback Callback to be invoked when the operation completes
     */
    public void updateAlphaAccount(AlphaAccount alphaAccount, ServiceCallback<Void> callback) {
        executor.execute(() -> {
            try {
                if (!alphaAccount.validateData()) {
                    callback.onError("Invalid Alpha account data");
                    return;
                }
                
                database.alphaAccountDao().update(alphaAccount);
                callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Error updating Alpha account", e);
                callback.onError("Error updating Alpha account: " + e.getMessage());
            }
        });
    }

    /**
     * Updates a PAlpha account.
     *
     * @param pAlphaAccount The updated PAlpha account
     * @param callback Callback to be invoked when the operation completes
     */
    public void updatePAlphaAccount(PAlphaAccount pAlphaAccount, ServiceCallback<Void> callback) {
        executor.execute(() -> {
            try {
                if (!pAlphaAccount.validateData()) {
                    callback.onError("Invalid PAlpha account data");
                    return;
                }
                
                database.PalphaAccountDao().update(pAlphaAccount);
                callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Error updating PAlpha account", e);
                callback.onError("Error updating PAlpha account: " + e.getMessage());
            }
        });
    }

    /**
     * Ensures balances are accurate for all account types.
     * <p>
     * This method performs a full recalculation of all account balances
     * to ensure they are consistent with their transactions.
     * </p>
     *
     * @param callback Callback to be invoked when the operation completes
     */
    public void recalculateAllBalances(ServiceCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean success = transactionManager.updateAllAlphaAccountBalances();
                callback.onSuccess(success);
            } catch (Exception e) {
                Log.e(TAG, "Error recalculating balances", e);
                callback.onError("Error recalculating balances: " + e.getMessage());
            }
        });
    }

    /**
     * Deletes an Alpha account and all its associated Beta accounts.
     *
     * @param alphaAccountId The ID of the Alpha account to delete
     * @param callback Callback to be invoked when the operation completes
     */
    public void deleteAlphaAccount(int alphaAccountId, ServiceCallback<Void> callback) {
        executor.execute(() -> {
            try {
                AlphaAccountDao alphaDao = database.alphaAccountDao();
                BetaAccountDao betaDao = database.betaAccountDao();
                
                // Delete all associated Beta accounts first
                List<BetaAccount> betaAccounts = betaDao.getBetaAccountsByAlphaAccountId(alphaAccountId);
                for (BetaAccount beta : betaAccounts) {
                    betaDao.delete(beta);
                }
                
                // Delete the Alpha account
                AlphaAccount alphaAccount = alphaDao.getAlphaAccountById(alphaAccountId);
                if (alphaAccount != null) {
                    alphaDao.delete(alphaAccount);
                }
                
                callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting Alpha account", e);
                callback.onError("Error deleting Alpha account: " + e.getMessage());
            }
        });
    }

    /**
     * Deletes a PAlpha account and all its associated PBeta accounts.
     *
     * @param pAlphaAccountId The ID of the PAlpha account to delete
     * @param callback Callback to be invoked when the operation completes
     */
    public void deletePAlphaAccount(int pAlphaAccountId, ServiceCallback<Void> callback) {
        executor.execute(() -> {
            try {
                PAlphaAccountDao pAlphaDao = database.PalphaAccountDao();
                PBetaAccountDao pBetaDao = database.PbetaAccountDao();
                
                // Delete all associated PBeta accounts first
                List<PBetaAccount> pBetaAccounts = pBetaDao.getPBetaAccountsByPAlphaAccountId(pAlphaAccountId);
                for (PBetaAccount pBeta : pBetaAccounts) {
                    pBetaDao.delete(pBeta);
                }
                
                // Delete the PAlpha account
                PAlphaAccount pAlphaAccount = pAlphaDao.getPAlphaAccountById(pAlphaAccountId);
                if (pAlphaAccount != null) {
                    pAlphaDao.delete(pAlphaAccount);
                }
                
                callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting PAlpha account", e);
                callback.onError("Error deleting PAlpha account: " + e.getMessage());
            }
        });
    }

    /**
     * Callback interface for service operations.
     *
     * @param <T> The type of result
     */
    public interface ServiceCallback<T> {
        /**
         * Called when the operation succeeds.
         *
         * @param result The operation result
         */
        void onSuccess(T result);
        
        /**
         * Called when the operation fails.
         *
         * @param error The error message
         */
        void onError(String error);
    }
}
