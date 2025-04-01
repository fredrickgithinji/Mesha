package com.dzovah.mesha.Database.Utils;

import androidx.room.TypeConverter;

/**
 * Type converter for the Room database to handle TransactionType enum conversion.
 * <p>
 * This class provides methods to convert between the {@link TransactionType} enum
 * and its String representation for storage in the SQLite database. Room uses
 * these converter methods automatically when storing or retrieving enum values.
 * </p>
 * <p>
 * Room cannot store enum values directly in the database, so this converter
 * allows transactions to use the TransactionType enum in business logic while
 * ensuring proper database storage as strings.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see TransactionType
 * @see androidx.room.TypeConverter
 */
public class TransactionTypeConverter {
    /**
     * Converts a String value to the corresponding TransactionType enum value.
     * <p>
     * This method is used by Room when loading data from the database.
     * It takes the string representation of the transaction type stored in the
     * database and converts it back to the appropriate enum value.
     * </p>
     *
     * @param value String representation of the TransactionType
     * @return The corresponding TransactionType enum value, or null if the input is null
     */
    @TypeConverter
    public static TransactionType toTransactionType(String value) {
        return value == null ? null : TransactionType.valueOf(value);
    }

    /**
     * Converts a TransactionType enum value to its String representation.
     * <p>
     * This method is used by Room when saving data to the database.
     * It takes a TransactionType enum value and converts it to its string
     * representation for storage in the database.
     * </p>
     *
     * @param type The TransactionType enum value to convert
     * @return String representation of the TransactionType, or null if the input is null
     */
    @TypeConverter
    public static String fromTransactionType(TransactionType type) {
        return type == null ? null : type.name();
    }
}