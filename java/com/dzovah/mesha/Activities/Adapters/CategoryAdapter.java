package com.dzovah.mesha.Activities.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dzovah.mesha.Database.Entities.Category;
import com.dzovah.mesha.R;

import java.util.List;

/**
 * RecyclerView adapter for displaying transaction categories in a list.
 * <p>
 * This adapter populates a RecyclerView with Category items, displaying
 * the category name in a simple item layout. It provides methods for
 * updating the category list and managing the RecyclerView item views.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see RecyclerView.Adapter
 * @see Category
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    /** List of Category objects to display */
    private List<Category> categories;

    /**
     * Updates the list of categories displayed by this adapter.
     * <p>
     * This method will trigger a UI refresh to show the new category list.
     * </p>
     *
     * @param categories The new list of Category objects to display
     */
    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    /**
     * Creates a new ViewHolder for category items.
     * <p>
     * This method inflates the item_category layout for each item in the RecyclerView.
     * </p>
     *
     * @param parent The parent ViewGroup into which the new View will be added
     * @param viewType The view type of the new View (not used in this implementation)
     * @return A new CategoryViewHolder that holds the View for each category item
     */
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    /**
     * Binds category data to a ViewHolder.
     * <p>
     * This method populates the ViewHolder's TextView with the category name.
     * </p>
     *
     * @param holder The ViewHolder to update with category data
     * @param position The position of the category in the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.categoryName.setText(category.getCategory());
    }

    /**
     * Returns the total number of categories in the data set.
     *
     * @return The number of categories, or 0 if the categories list is null
     */
    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    /**
     * ViewHolder class for caching views used in the category item layout.
     * <p>
     * This class holds a reference to the TextView within the item_category layout
     * to improve recycling performance by avoiding repeated calls to findViewById().
     * </p>
     */
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        /** TextView for displaying the category name */
        TextView categoryName;

        /**
         * Constructs a new CategoryViewHolder and finds the required view.
         *
         * @param itemView The category item view to hold and find references from
         */
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.categoryName);
        }
    }
}