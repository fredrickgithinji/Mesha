package com.dzovah.mesha.Methods.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility class for managing local image storage operations in the Mesha application.
 * <p>
 * This class provides methods for:
 * <ul>
 *   <li>Saving bitmap images to the application's internal storage</li>
 *   <li>Loading images from internal storage</li>
 *   <li>Checking if images exist in storage</li>
 *   <li>Deleting images from storage</li>
 * </ul>
 * </p>
 * <p>
 * The utility uses Android's internal storage mechanism, which ensures that
 * the saved files are private to the application and not accessible by other apps.
 * All operations provide appropriate logging for debugging and error tracking.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see Bitmap
 * @see Context#getFilesDir()
 */
public class LocalStorageUtil {
    /** Tag for logging purposes */
    private static final String TAG = "LocalStorageUtil";

    /**
     * Saves a bitmap image to the application's internal storage.
     * <p>
     * This method compresses the bitmap to PNG format with maximum quality
     * and stores it in the application's private files directory.
     * </p>
     *
     * @param context Application context used to access internal storage
     * @param bitmap The bitmap image to save
     * @param filename The name to save the file as
     * @return true if the save operation was successful, false otherwise
     */
    public static boolean saveImage(Context context, Bitmap bitmap, String filename) {
        try {
            // Use internal storage for app-specific files
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Log.d(TAG, "Image saved successfully: " + filename);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error saving image: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads a bitmap image from the application's internal storage.
     * <p>
     * This method attempts to load and decode a bitmap from the specified
     * file in the application's private files directory.
     * </p>
     *
     * @param context Application context used to access internal storage
     * @param filename The name of the file to load
     * @return The loaded bitmap if successful, or null if the file doesn't exist or couldn't be decoded
     */
    public static Bitmap loadImage(Context context, String filename) {
        try {
            File file = new File(context.getFilesDir(), filename);
            if (!file.exists()) {
                Log.d(TAG, "Image file doesn't exist: " + filename);
                return null;
            }

            FileInputStream fis = new FileInputStream(file);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap: " + filename);
                return null;
            }

            Log.d(TAG, "Image loaded successfully: " + filename);
            return bitmap;
        } catch (IOException e) {
            Log.e(TAG, "Error loading image: " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if an image exists in the application's internal storage.
     * <p>
     * This method verifies whether a file with the specified name exists
     * in the application's private files directory.
     * </p>
     *
     * @param context Application context used to access internal storage
     * @param filename The name of the file to check
     * @return true if the file exists, false otherwise
     */
    public static boolean imageExists(Context context, String filename) {
        File file = new File(context.getFilesDir(), filename);
        boolean exists = file.exists();
        Log.d(TAG, "Image " + (exists ? "exists" : "doesn't exist") + ": " + filename);
        return exists;
    }

    /**
     * Deletes an image from the application's internal storage.
     * <p>
     * This method attempts to delete the specified file from the
     * application's private files directory.
     * </p>
     *
     * @param context Application context used to access internal storage
     * @param filename The name of the file to delete
     * @return true if the deletion was successful, false otherwise
     */
    public static boolean deleteImage(Context context, String filename) {
        File file = new File(context.getFilesDir(), filename);
        boolean success = file.delete();
        Log.d(TAG, "Image deletion " + (success ? "successful" : "failed") + ": " + filename);
        return success;
    }
}