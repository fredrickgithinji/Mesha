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

public class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconViewHolder> {
    private final String[] iconPaths;
    private int selectedPosition = -1;
    private final OnIconSelectedListener listener;

    public interface OnIconSelectedListener {
        void onIconSelected(String iconPath);
    }

    public IconAdapter(String[] iconPaths, OnIconSelectedListener listener) {
        this.iconPaths = iconPaths;
        this.listener = listener;
        Log.d("IconDebug", "IconAdapter created with " + iconPaths.length + " icons");
    }

    @Override
    public IconViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_icon, parent, false);
        return new IconViewHolder(view);
    }

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

    @Override
    public int getItemCount() {
        return iconPaths != null ? iconPaths.length : 0;
    }

    static class IconViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;

        IconViewHolder(View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.ivIcon);
        }
    }
}