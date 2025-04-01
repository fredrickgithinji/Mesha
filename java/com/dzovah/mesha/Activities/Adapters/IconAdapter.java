package com.dzovah.mesha.Activities.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import com.dzovah.mesha.R;
import java.io.IOException;
import java.io.InputStream;

/**
 * RecyclerView adapter for displaying selectable icons in a grid layout.
 * <p>
 * This adapter loads icon images from the app's assets directory and displays them
 * in a RecyclerView, allowing users to select one icon at a time. When an icon is
 * selected, its background changes to indicate the selection, and the selection is
 * communicated to the listener.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see RecyclerView.Adapter
 */
public class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconViewHolder> {
    /** Array of icon file paths in the assets directory */
    private final String[] iconPaths;
    
    /** Index of the currently selected icon, -1 if no selection */
    private int selectedPosition = -1;
    
    /** Listener to notify when an icon is selected */
    private final OnIconSelectedListener listener;

    /**
     * Interface for notifying when an icon has been selected.
     */
    public interface OnIconSelectedListener {
        /**
         * Called when a user selects an icon.
         *
         * @param iconPath The file path of the selected icon within the assets directory
         */
        void onIconSelected(String iconPath);
    }

    /**
     * Constructs a new IconAdapter with the given icon paths and selection listener.
     *
     * @param iconPaths Array of icon file paths within the assets/icons directory
     * @param listener Listener to notify when an icon is selected
     */
    public IconAdapter(String[] iconPaths, OnIconSelectedListener listener) {
        this.iconPaths = iconPaths;
        this.listener = listener;
        Log.d("IconDebug", "IconAdapter created with " + iconPaths.length + " icons");
    }

    /**
     * Creates a new ViewHolder for icon items.
     * <p>
     * This method inflates the item_icon layout for each item in the RecyclerView.
     * </p>
     *
     * @param parent The parent ViewGroup into which the new View will be added
     * @param viewType The view type of the new View (not used in this implementation)
     * @return A new IconViewHolder that holds the View for each icon item
     */
    @Override
    public IconViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_icon, parent, false);
        return new IconViewHolder(view);
    }

    /**
     * Binds icon data to a ViewHolder.
     * <p>
     * This method loads the icon image from the assets directory and sets it to the
     * ImageView in the ViewHolder. It also updates the background to indicate if the
     * icon is currently selected and sets a click listener to handle icon selection.
     * </p>
     *
     * @param holder The ViewHolder to update with icon data
     * @param position The position of the icon in the adapter's data set
     */
    @Override
    public void onBindViewHolder(IconViewHolder holder, int position) {
        String iconPath = iconPaths[position];
        Context context = holder.itemView.getContext();
        
        Log.d("IconDebug", "Trying to load icon: icons/" + iconPath);
        try {
            InputStream is = context.getAssets().open("icons/" + iconPath);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            holder.iconView.setImageBitmap(bitmap);
            is.close();
            Log.d("IconDebug", "Successfully loaded icon: " + iconPath);
        } catch (IOException e) {
            Log.e("IconDebug", "Failed to load icon: " + iconPath + ", Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        holder.itemView.setBackgroundResource(selectedPosition == position ? 
            R.drawable.icon_selected_background : R.drawable.icon_background);
        
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            listener.onIconSelected(iconPath);
        });
    }

    /**
     * Returns the total number of icons in the data set.
     *
     * @return The number of icons, or 0 if the iconPaths array is null
     */
    @Override
    public int getItemCount() {
        return iconPaths != null ? iconPaths.length : 0;
    }

    /**
     * ViewHolder class for caching the ImageView used to display icons.
     * <p>
     * This class holds a reference to the ImageView within the item_icon layout
     * to improve recycling performance by avoiding repeated calls to findViewById().
     * </p>
     */
    static class IconViewHolder extends RecyclerView.ViewHolder {
        /** ImageView for displaying the icon */
        ImageView iconView;

        /**
         * Constructs a new IconViewHolder and finds the required view.
         *
         * @param itemView The icon item view to hold and find references from
         */
        IconViewHolder(View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.ivIcon);
        }
    }
}