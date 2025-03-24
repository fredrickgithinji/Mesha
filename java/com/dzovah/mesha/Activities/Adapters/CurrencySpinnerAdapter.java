package com.dzovah.mesha.Activities.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.dzovah.mesha.Database.Utils.CurrencyType;
import com.dzovah.mesha.R;

public class CurrencySpinnerAdapter extends ArrayAdapter<CurrencyType> {

    public CurrencySpinnerAdapter(Context context, CurrencyType[] currencyTypes) {
        super(context, R.layout.item_currency_spinner, currencyTypes);
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