package com.example.expensemanagement.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "categories",
    indices = {
        @Index(value = {"user_id", "type"})
    }
)
public class CategoryEntity {

    @PrimaryKey @NonNull @ColumnInfo(name = "category_id")  public String categoryId;
    @Nullable @ColumnInfo(name = "user_id")                 public String userId;
    @Nullable @ColumnInfo(name = "parent_id")               public String parentId;
    @NonNull @ColumnInfo(name = "name")                     public String name;
    @NonNull @ColumnInfo(name = "type")                     public String type;
    @Nullable @ColumnInfo(name = "icon")                    public String icon;
    @Nullable @ColumnInfo(name = "color")                   public String color;
    @ColumnInfo(name = "is_system")                         public int isSystem = 0;
    @ColumnInfo(name = "sort_order")                        public int sortOrder = 0;
    @NonNull @ColumnInfo(name = "created_at")               public String createdAt;

    public CategoryEntity(@NonNull String categoryId, @Nullable String userId,
                          @Nullable String parentId, @NonNull String name,
                          @NonNull String type, @Nullable String icon,
                          @Nullable String color, int isSystem, int sortOrder,
                          @NonNull String createdAt) {
        this.categoryId = categoryId;
        this.userId     = userId;
        this.parentId   = parentId;
        this.name       = name;
        this.type       = type;
        this.icon       = icon;
        this.color      = color;
        this.isSystem   = isSystem;
        this.sortOrder  = sortOrder;
        this.createdAt  = createdAt;
    }
}