package com.example.expensemanagement;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.expensemanagement.model.BudgetEntity;
import com.example.expensemanagement.model.CategoryEntity;
import com.example.expensemanagement.model.TransactionEntity;
import com.example.expensemanagement.model.UserEntity;
import com.example.expensemanagement.model.UserSettingsEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                UserEntity.class,
                UserSettingsEntity.class,
                CategoryEntity.class,
                TransactionEntity.class,
                BudgetEntity.class,
        },
        version = 16,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "expense_manager.db";
    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService executor = Executors.newFixedThreadPool(4);

    public abstract AppDao appDao();

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME
                            )
                            .fallbackToDestructiveMigration()
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    executor.execute(() -> {
                                        if (INSTANCE != null) {
                                            INSTANCE.appDao().insertAllCategories(buildSeedCategories());
                                        }
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static List<CategoryEntity> buildSeedCategories() {
        String now = "2024-01-01T00:00:00";
        List<CategoryEntity> list = new ArrayList<>();
        list.add(new CategoryEntity("cat_1", null, null, "Ăn uống", "expense", null, "#F44336", 1, 1, now));
        list.add(new CategoryEntity("cat_2", null, null, "Lương", "income", null, "#4CAF50", 1, 2, now));
        return list;
    }
}