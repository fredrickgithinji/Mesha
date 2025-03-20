package com.dzovah.mesha.Methods.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class LocalStorageUtil {
    private static final String TAG = "LocalStorageUtil";

    /**
     * Save a bitmap image to internal storage
     * @param context Application context
     * @param bitmap The bitmap to save
     * @param filename Filename to save as
     * @return true if save was successful, false otherwise
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
     * Load a bitmap image from internal storage
     * @param context Application context
     * @param filename Filename to load
     * @return The loaded bitmap or null if it doesn't exist
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
     * Check if an image exists in internal storage
     * @param context Application context
     * @param filename Filename to check
     * @return true if the image exists, false otherwise
     */
    public static boolean imageExists(Context context, String filename) {
        File file = new File(context.getFilesDir(), filename);
        boolean exists = file.exists();
        Log.d(TAG, "Image " + (exists ? "exists" : "doesn't exist") + ": " + filename);
        return exists;
    }

    /**
     * Delete an image from internal storage
     * @param context Application context
     * @param filename Filename to delete
     * @return true if deletion was successful, false otherwise
     */
    public static boolean deleteImage(Context context, String filename) {
        File file = new File(context.getFilesDir(), filename);
        boolean success = file.delete();
        Log.d(TAG, "Image deletion " + (success ? "successful" : "failed") + ": " + filename);
        return success;
    }
}