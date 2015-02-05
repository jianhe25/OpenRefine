package com.google.refine.browsing;

import java.util.Properties;

import com.google.refine.model.Project;
import com.google.refine.util.MathUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;

/**
 * Store a predicate and its text label, in case the value is not a string itself.
 * For instance, if a value is a date, then its label can be one particular
 * rendering of that date.
 *
 * Facet choices that are presented to the user as text are stored as decorated values.
 */
public class DecoratedPredicate implements Jsonizable {
    final public int[] predicateColumnIDs;
    final public Object[] predicateValues;
    final public String label;

    public DecoratedPredicate(int[] predicateColumnIDs, Object[] predicateValues, String label) {
        this.predicateColumnIDs = predicateColumnIDs;
        this.predicateValues = predicateValues;
        this.label = label;
    }

    public static DecoratedPredicate initializeFromJSON(JSONObject predicateJson) throws JSONException {
        JSONArray predicates = predicateJson.getJSONArray("predicates");
        int len = predicates.length();
        int[] predicateColumnIDs = new int[len];
        Object[] predicateValues = new Object[len];
        for (int j = 0; j < len; ++j) {
            JSONObject predicate = predicates.getJSONObject(j);
            predicateColumnIDs[j] = predicate.getInt("c");
            predicateValues[j] = predicate.get("v");
        }
        String label = predicateJson.getString("l");
        return new DecoratedPredicate(predicateColumnIDs, predicateValues, label);
    }
    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("l"); writer.value(label);
        writer.key("predicates"); writer.array();
        for (int i = 0; i < predicateColumnIDs.length; ++i) {
            writer.object();
            writer.key("c"); writer.value(predicateColumnIDs[i]);
            writer.key("v"); writer.value(predicateValues[i]);
            writer.endObject();
        }
        writer.endArray();
        writer.endObject();
    }

    public long mask() {
        long totalMask = 0;
        for (int columnID : predicateColumnIDs) {
            totalMask += MathUtils.power(2, columnID);
        }
        return totalMask;
    }
}

