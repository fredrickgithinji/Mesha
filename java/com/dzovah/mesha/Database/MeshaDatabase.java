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
    
    // DAOs
    public abstract AlphaAccountDao alphaAccountDao();
    public abstract BetaAccountDao betaAccountDao();
    public abstract TransactionDao transactionDao();
    public abstract CategoryDao categoryDao();
    public abstract MeshansDao meshansDao();

    // Singleton instance
    private static volatile MeshaDatabase INSTANCE;
    
    // ExecutorService for database operations
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
        Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
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

    // Get database instance
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

    // Method to close the database
    public static void closeDatabase() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}