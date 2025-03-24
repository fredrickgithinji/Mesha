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

public class CategorySpinnerAdapter extends ArrayAdapter<Category> {

    private final Context context;
    private final List<Category> categories;

    public CategorySpinnerAdapter(Context context, List<Category> categories) {
        super(context, R.layout.item_category_spinner, categories);
        this.context = context;
        this.categories = categories;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

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