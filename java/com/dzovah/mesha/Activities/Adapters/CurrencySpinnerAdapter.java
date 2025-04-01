package com.dzovah.mesha.Activities.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.dzovah.mesha.Database.Utils.CurrencyType;
import com.dzovah.mesha.R;

/**
 * Custom ArrayAdapter for displaying CurrencyType items in a Spinner widget.
 * <p>
 * This adapter populates a Spinner with CurrencyType enum values, displaying
 * both the currency symbol and name in the collapsed spinner view and in the 
 * dropdown list. It customizes both the main view and dropdown view to use 
 * the same layout.
 * </p>
 *
 * @author Electra Magus
 * @version 1.0
 * @see ArrayAdapter
 * @see CurrencyType
 */
public class CurrencySpinnerAdapter extends ArrayAdapter<CurrencyType> {

    /**
     * Constructs a new CurrencySpinnerAdapter.
     *
     * @param context The context used for inflating layouts and accessing resources
     * @param currencyTypes The array of CurrencyType enum values to display in the spinner
     */
    public CurrencySpinnerAdapter(Context context, CurrencyType[] currencyTypes) {
        super(context, R.layout.item_currency_spinner, currencyTypes);
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
     * Creates a view to display a currency item.
     * <p>
     * This method is used by both getView and getDropDownView to avoid code duplication.
     * It inflates the item_currency_spinner layout if necessary and sets the currency
     * information as a combination of the currency symbol and name.
     * </p>
     *
     * @param position The position of the item within the adapter's data set
     * @param convertView The old view to reuse, if possible
     * @param parent The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position
     */
    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_currency_spinner, parent, false);
        }

        TextView currencyName = convertView.findViewById(R.id.currencyName);
        CurrencyType currency = getItem(position);
        if (currency != null) {
            currencyName.setText(currency.getSymbol() + " - " + currency.name());
        }

        return convertView;
    }
}