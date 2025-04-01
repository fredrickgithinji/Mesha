package com.dzovah.mesha.Activities.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.dzovah.mesha.Database.Entities.Category;
import com.dzovah.mesha.R;

import java.util.List;

/**
 * Custom ArrayAdapter for displaying Category items in a Spinner widget.
 * <p>
 * This adapter populates a Spinner with Category items, displaying
 * the category name in both the collapsed spinner view and in the dropdown list.
 * It customizes both the main view and dropdown view to use the same layout.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see ArrayAdapter
 * @see Category
 */
public class CategorySpinnerAdapter extends ArrayAdapter<Category> {

    /** Application context used for inflating layouts and accessing resources */
    private final Context context;
    
    /** List of Category objects to display in the spinner */
    private final List<Category> categories;

    /**
     * Constructs a new CategorySpinnerAdapter.
     *
     * @param context The context used for inflating layouts and accessing resources
     * @param categories The list of Category objects to display in the spinner
     */
    public CategorySpinnerAdapter(Context context, List<Category> categories) {
        super(context, R.layout.item_category_spinner, categories);
        this.context = context;
        this.categories = categories;
    }

    /**
     * Gets a View that displays the data at the specified position in the data set.
     * <p>
     * This is the view displayed when the spinner is in its collapsed state.
     * </p>
     *
     * @param position The position of the item within the adapter's data set
     * @param convertView The old view to reuse, if possible
     * @param parent The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    /**
     * Gets a View that displays the dropdown data at the specified position in the data set.
     * <p>
     * This is the view displayed when the spinner is in its expanded/dropdown state.
     * </p>
     *
     * @param position The position of the item within the adapter's data set
     * @param convertView The old view to reuse, if possible
     * @param parent The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position
     */
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    /**
     * Creates a view to display a category item.
     * <p>
     * This method is used by both getView and getDropDownView to avoid code duplication.
     * It inflates the item_category_spinner layout if necessary and sets the category name.
     * </p>
     *
     * @param position The position of the item within the adapter's data set
     * @param convertView The old view to reuse, if possible
     * @param parent The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position
     */
    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                .inflate(R.layout.item_category_spinner, parent, false);
        }

        TextView categoryName = convertView.findViewById(R.id.categoryName);
        Category category = categories.get(position);
        categoryName.setText(category.getCategory());

        return convertView;
    }
}