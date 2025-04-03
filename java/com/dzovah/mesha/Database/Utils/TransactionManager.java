package com.dzovah.mesha.Database.Utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Transaction;

import com.dzovah.mesha.Database.Daos.AlphaAccountDao;
import com.dzovah.mesha.Database.Daos.BetaAccountDao;
import com.dzovah.mesha.Database.Daos.PAlphaAccountDao;
import com.dzovah.mesha.Database.Daos.PBetaAccountDao;
import com.dzovah.mesha.Database.Daos.TransactionDao;
import com.dzovah.mesha.Database.Daos.PTransactionDao;
import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.Entities.PAlphaAccount;
import com.dzovah.mesha.Database.Entities.PBetaAccount;
import com.dzovah.mesha.Database.MeshaDatabase;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Transaction Manager for handling complex operations requiring data integrity.
 * <p>
 * This class provides methods for executing operations that need to maintain
 * data integrity across both normal and hidden account systems.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 */
public class TransactionManager {
    private static final String TAG = "TransactionManager";
    private final MeshaDatabase database;

    /**
     * Constructor for the TransactionManager.
     *
     * @param database The MeshaDatabase instance
     */
    public TransactionManager(MeshaDatabase database) {
        this.database = database;
    }

    /**
     * Updates all Alpha account balances based on their Beta accounts.
     * <p>
     * This method ensures that every Alpha account's balance correctly reflects
     * the sum of its Beta account balances.
     * </p>
     *
     * @return true if all updates were successful, false otherwise
     */
    @Transaction
    public boolean updateAllAlphaAccountBalances() {
        try {
            // Update regular Alpha accounts
            AlphaAccountDao alphaDao = database.alphaAccountDao();
            BetaAccountDao betaDao = database.betaAccountDao();
            List<AlphaAccount> alphaAccounts = alphaDao.getAllAlphaAccounts();
            
            for (AlphaAccount alpha : alphaAccounts) {
                List<BetaAccount> betaAccounts = betaDao.getBetaAccountsByAlphaAccountId(alpha.getAlphaAccountId());
                alpha.updateBalanceFromBetaAccounts(betaAccounts);
                alphaDao.update(alpha);
            }
            
            // Update hidden P-Alpha accounts
            PAlphaAccountDao pAlphaDao = database.PalphaAccountDao();
            PBetaAccountDao pBetaDao = database.PbetaAccountDao();
            List<PAlphaAccount> pAlphaAccounts = pAlphaDao.getAllPAlphaAccounts();
            
            for (PAlphaAccount pAlpha : pAlphaAccounts) {
                List<PBetaAccount> pBetaAccounts = pBetaDao.getPBetaAccountsByPAlphaAccountId(pAlpha.getPAlphaAccountId());
                pAlpha.updateBalanceFromBetaAccounts(pBetaAccounts);
                pAlphaDao.update(pAlpha);
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error updating Alpha account balances", e);
            return false;
        }
    }

    /**
     * Updates a Beta account balance and its parent Alpha account balance.
     * <p>
     * This method ensures that when a Beta account's balance changes, its parent
     * Alpha account's balance is also updated to maintain data integrity.
     * </p>
     *
     * @param betaAccountId The ID of the Beta account to update
     * @return true if the update was successful, false otherwise
     */
    @Transaction
    public boolean updateBetaAndAlphaBalance(int betaAccountId) {
        try {
            BetaAccountDao betaDao = database.betaAccountDao();
            AlphaAccountDao alphaDao = database.alphaAccountDao();
            TransactionDao transactionDao = database.transactionDao();
            
            // Get the Beta account
            BetaAccount beta = betaDao.getBetaAccountById(betaAccountId);
            if (beta == null) {
                return false;
            }
            
            // Update Beta account balance from transactions
            double calculatedBalance = transactionDao.getBetaAccountBalance(betaAccountId);
            beta.setBetaAccountBalance(calculatedBalance);
            betaDao.update(beta);
            
            // Get and update the parent Alpha account
            int alphaId = beta.getAlphaAccountId();
            AlphaAccount alpha = alphaDao.getAlphaAccountById(alphaId);
            if (alpha == null) {
                return false;
            }
            
            List<BetaAccount> betaAccounts = betaDao.getBetaAccountsByAlphaAccountId(alphaId);
            alpha.updateBalanceFromBetaAccounts(betaAccounts);
            alphaDao.update(alpha);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error updating Beta and Alpha balances", e);
            return false;
        }
    }

    /**
     * Updates a hidden PBeta account balance and its parent PAlpha account balance.
     * <p>
     * This method ensures that when a PBeta account's balance changes, its parent
     * PAlpha account's balance is also updated to maintain data integrity.
     * </p>
     *
     * @param pBetaAccountId The ID of the PBeta account to update
     * @return true if the update was successful, false otherwise
     */
    @Transaction
    public boolean updatePBetaAndPAlphaBalance(int pBetaAccountId) {
        try {
            PBetaAccountDao pBetaDao = database.PbetaAccountDao();
            PAlphaAccountDao pAlphaDao = database.PalphaAccountDao();
            PTransactionDao pTransactionDao = database.PtransactionDao();
            
            // Get the PBeta account
            PBetaAccount pBeta = pBetaDao.getPBetaAccountById(pBetaAccountId);
            if (pBeta == null) {
                return false;
            }
            
            // Update PBeta account balance from transactions
            double calculatedBalance = pTransactionDao.getPBetaAccountBalance(pBetaAccountId);
            pBeta.setPBetaAccountBalance(calculatedBalance);
            pBetaDao.update(pBeta);
            
            // Get and update the parent PAlpha account
            int pAlphaId = pBeta.getPAlphaAccountId();
            PAlphaAccount pAlpha = pAlphaDao.getPAlphaAccountById(pAlphaId);
            if (pAlpha == null) {
                return false;
            }
            
            List<PBetaAccount> pBetaAccounts = pBetaDao.getPBetaAccountsByPAlphaAccountId(pAlphaId);
            pAlpha.updateBalanceFromBetaAccounts(pBetaAccounts);
            pAlphaDao.update(pAlpha);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error updating PBeta and PAlpha balances", e);
            return false;
        }
    }

    /**
     * Executes a database operation asynchronously and returns the result.
     * <p>
     * This method provides a way to execute database operations on a background thread
     * and wait for the result.
     * </p>
     *
     * @param callable The operation to execute
     * @param <T> The return type of the operation
     * @return The result of the operation or null if an error occurred
     */
    public <T> T executeWithResult(@NonNull Callable<T> callable) {
        Future<T> future = MeshaDatabase.databaseWriteExecutor.submit(callable);
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error executing database operation", e);
            return null;
        }
    }
}
