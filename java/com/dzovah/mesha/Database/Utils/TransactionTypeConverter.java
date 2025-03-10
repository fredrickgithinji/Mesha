package com.dzovah.mesha.Database.Utils;

import androidx.room.TypeConverter;

public class TransactionTypeConverter {
    @TypeConverter
    public static TransactionType toTransactionType(String value) {
        return value == null ? null : TransactionType.valueOf(value);
    }

    @TypeConverter
    public static String fromTransactionType(TransactionType type) {
        return type == null ? null : type.name();
    }
}