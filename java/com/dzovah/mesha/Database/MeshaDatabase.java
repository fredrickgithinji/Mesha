package com.dzovah.mesha.Database;

import android.content.Context;

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

    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            
            databaseWriteExecutor.execute(() -> {
                CategoryDao dao = INSTANCE.categoryDao();
                
                // Insert default categories
                Category general = new Category("General", "general_icon.png");
                Category food = new Category("Food", "food_icon.png");
                Category transport = new Category("Transport", "transport_icon.png");
                Category utilities = new Category("Utilities", "utilities_icon.png");
                
                dao.insert(general);  // This will be ID 1
                dao.insert(food);     // This will be ID 2
                dao.insert(transport);// This will be ID 3
                dao.insert(utilities);// This will be ID 4
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
                    .addCallback(sRoomDatabaseCallback)
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