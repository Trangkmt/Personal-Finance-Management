package com.example.expensemanagement.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "users",
    indices = {
        @Index(value = "email", unique = true),
        @Index(value = "firebase_uid", unique = true)
    }
)
public class UserEntity {

    @PrimaryKey @NonNull @ColumnInfo(name = "user_id")   public String userId;
    @NonNull @ColumnInfo(name = "email")                 public String email;
    @NonNull @ColumnInfo(name = "password_hash")         public String passwordHash;
    @NonNull @ColumnInfo(name = "full_name")             public String fullName;
    @Nullable @ColumnInfo(name = "avatar_url")           public String avatarUrl;
    @NonNull @ColumnInfo(name = "currency_code")         public String currencyCode = "VND";
    @NonNull @ColumnInfo(name = "language")              public String language = "vi";
    @NonNull @ColumnInfo(name = "auth_provider")         public String authProvider = "local";
    @Nullable @ColumnInfo(name = "firebase_uid")         public String firebaseUid;
    @ColumnInfo(name = "is_active")                      public int isActive = 1;
    @NonNull @ColumnInfo(name = "created_at")            public String createdAt;
    @NonNull @ColumnInfo(name = "updated_at")            public String updatedAt;
    @Nullable @ColumnInfo(name = "last_login_at")        public String lastLoginAt;

    public UserEntity(@NonNull String userId, @NonNull String email,
                      @NonNull String passwordHash, @NonNull String fullName,
                      @Nullable String firebaseUid,
                      @NonNull String createdAt, @NonNull String updatedAt) {
        this.userId       = userId;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.fullName     = fullName;
        this.firebaseUid  = firebaseUid;
        this.createdAt    = createdAt;
        this.updatedAt    = updatedAt;
        this.lastLoginAt  = createdAt;
    }
}