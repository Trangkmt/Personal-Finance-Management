package com.example.expensemanagement.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "user_settings",
    indices = { @Index(value = "user_id", unique = true) },
    foreignKeys = {
        @ForeignKey(entity = UserEntity.class,
            parentColumns = "user_id", childColumns = "user_id",
            onDelete = ForeignKey.CASCADE)
    }
)
public class UserSettingsEntity {

    @PrimaryKey @NonNull @ColumnInfo(name = "setting_id")          public String settingId;
    @NonNull @ColumnInfo(name = "user_id")                         public String userId;
    @NonNull @ColumnInfo(name = "theme")                           public String theme = "light";
    @ColumnInfo(name = "daily_reminder_enabled")                   public int dailyReminderEnabled = 1;
    @NonNull @ColumnInfo(name = "daily_reminder_time")             public String dailyReminderTime = "21:00:00";
    @ColumnInfo(name = "budget_alert_enabled")                     public int budgetAlertEnabled = 1;
    @ColumnInfo(name = "sms_reading_enabled")                      public int smsReadingEnabled = 0;
    @ColumnInfo(name = "biometric_enabled")                        public int biometricEnabled = 0;
    @ColumnInfo(name = "app_lock_enabled")                         public int appLockEnabled = 0;
    @ColumnInfo(name = "app_lock_timeout")                         public int appLockTimeout = 5;
    @ColumnInfo(name = "gps_suggestion_enabled")                   public int gpsSuggestionEnabled = 1;
    @ColumnInfo(name = "first_day_of_week")                        public int firstDayOfWeek = 1;
    @NonNull @ColumnInfo(name = "date_format")                     public String dateFormat = "dd/MM/yyyy";
    @NonNull @ColumnInfo(name = "updated_at")                      public String updatedAt;

    public UserSettingsEntity(@NonNull String settingId,
                               @NonNull String userId,
                               @NonNull String updatedAt) {
        this.settingId = settingId;
        this.userId    = userId;
        this.updatedAt = updatedAt;
    }
}