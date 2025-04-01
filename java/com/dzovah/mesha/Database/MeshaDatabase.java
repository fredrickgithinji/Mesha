package com.dzovah.mesha.Database;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.dzovah.mesha.Database.Daos.AlphaAccountDao;
import com.dzovah.mesha.Database.Daos.BetaAccountDao;
import com.dzovah.mesha.Database.Daos.CategoryDao;
import com.dzovah.mesha.Database.Daos.MeshansDao;
import com.dzovah.mesha.Database.Daos.TransactionDao;
import com.dzovah.mesha.Database.Entities.AlphaAccount;
import com.dzovah.mesha.Database.Entities.BetaAccount;
import com.dzovah.mesha.Database.Entities.Category;
import com.dzovah.mesha.Database.Entities.Transaction;
import com.dzovah.mesha.Database.Entities.Meshans;
import com.dzovah.mesha.Database.Utils.TransactionTypeConverter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main database configuration class for the Mesha financial management application.
 * <p>
 * This abstract class extends {@link RoomDatabase} and provides the central database
 * configuration using Room persistence library. It defines all the entities managed
 * by the database, sets up database version control, and provides access to the
 * Data Access Objects (DAOs) for performing database operations.
 * </p>
 * <p>
 * The database is implemented as a singleton to prevent multiple instances of the
 * database being opened at the same time, which could lead to data inconsistencies.
 * </p>
 * <p>
 * The class also manages database initialization, including the creation of default
 * transaction categories when the database is first created.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 */
@Database(
    entities = {
        AlphaAccount.class,
        BetaAccount.class,
        Transaction.class,
        Category.class,
        Meshans.class
    },
    version = 1,
    exportSchema = true
)
@TypeConverters({TransactionTypeConverter.class})
public abstract class MeshaDatabase extends RoomDatabase {
    
    /**
     * Abstract method to access the AlphaAccount Data Access Object.
     * Room automatically generates the implementation at compile time.
     *
     * @return The AlphaAccountDao instance for AlphaAccount database operations
     */
    public abstract AlphaAccountDao alphaAccountDao();
    
    /**
     * Abstract method to access the BetaAccount Data Access Object.
     * Room automatically generates the implementation at compile time.
     *
     * @return The BetaAccountDao instance for BetaAccount database operations
     */
    public abstract BetaAccountDao betaAccountDao();
    
    /**
     * Abstract method to access the Transaction Data Access Object.
     * Room automatically generates the implementation at compile time.
     *
     * @return The TransactionDao instance for Transaction database operations
     */
    public abstract TransactionDao transactionDao();
    
    /**
     * Abstract method to access the Category Data Access Object.
     * Room automatically generates the implementation at compile time.
     *
     * @return The CategoryDao instance for Category database operations
     */
    public abstract CategoryDao categoryDao();
    
    /**
     * Abstract method to access the Meshans (user) Data Access Object.
     * Room automatically generates the implementation at compile time.
     *
     * @return The MeshansDao instance for Meshans (user) database operations
     */
    public abstract MeshansDao meshansDao();

    /**
     * Singleton instance of the database.
     * Volatile ensures visibility of changes across threads.
     */
    private static volatile MeshaDatabase INSTANCE;
    
    /**
     * ExecutorService for performing database operations asynchronously.
     * Using a thread pool helps manage resources efficiently and avoids blocking the UI thread.
     */
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
        Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    /**
     * Callback for database creation events.
     * This callback is triggered when the database is created for the first time,
     * and is used to populate it with initial data like default categories.
     */
    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        /**
         * Called when the database is created for the first time.
         * This method populates the database with default transaction categories.
         *
         * @param db The newly created SupportSQLiteDatabase instance
         */
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            
            // Create default categories when the database is created
            databaseWriteExecutor.execute(() -> {
                try {
                    CategoryDao categoryDao = INSTANCE.categoryDao();
                    
                    // Create and insert default categories
                    Category generalCategory = new Category("General", "General expenses");
                    Category foodCategory = new Category("Food", "Food and dining");
                    Category transportCategory = new Category("Transportation", "Transport and travel");
                    Category utilitiesCategory = new Category("Utilities", "Bills and utilities");
                    Category entertainmentCategory = new Category("Entertainment", "Leisure activities");
                    
                    categoryDao.insert(generalCategory);
                    categoryDao.insert(foodCategory);
                    categoryDao.insert(transportCategory);
                    categoryDao.insert(utilitiesCategory);
                    categoryDao.insert(entertainmentCategory);
                    
                    // Log success
                    Log.d("MeshaDatabase", "Default categories created successfully");
                } catch (Exception e) {
                    // Log the error
                    Log.e("MeshaDatabase", "Error creating default categories", e);
                }
            });
        }
    };

    /**
     * Gets the singleton instance of the database, creating it if necessary.
     * <p>
     * This method follows the singleton pattern with double-checked locking
     * for thread safety. The first call to this method will create the database;
     * subsequent calls will return the existing instance.
     * </p>
     *
     * @param context The application context
     * @return The singleton MeshaDatabase instance
     */
    public static MeshaDatabase Get_database(final Context context) {
        if (INSTANCE == null) {
            synchronized (MeshaDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        MeshaDatabase.class,
                        "Mesha_database"
                    )
                    .addCallback(roomCallback)
                    .fallbackToDestructiveMigration() // Handles schema changes by recreating tables
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Closes the database connection and releases resources.
     * <p>
     * This method should be called when the database is no longer needed,
     * typically in an application's onDestroy() method or when switching users.
     * </p>
     */
    public static void closeDatabase() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}