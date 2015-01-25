package com.google.refine.browsing;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;

/**
 * Store a value and its text label, in case the value is not a string itself.
 * For instance, if a value is a date, then its label can be one particular
 * rendering of that date.
 *
 * Facet choices that are presented to the user as text are stored as decorated values.
 */
public class DecoratedPredicate implements Jsonizable {
    final public String[] predicateColumns;
    final public Object[] predicateValues;
    final public String label;

    public DecoratedPredicate(String[] predicateColumns, Object[] predicateValues, String label) {
        this.predicateColumns = predicateColumns;
        this.predicateValues = predicateValues;
        this.label = label;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("l"); writer.value(label);
        writer.key("predicts"); writer.array();
        for (int i = 0; i < predicateColumns.length; ++i) {
            writer.object();
            writer.key("c"); writer.value(predicateColumns[i]);
            writer.key("v"); writer.value(predicateValues[i]);
            writer.endObject();
        }
        writer.endArray();
        writer.endObject();
    }
}

