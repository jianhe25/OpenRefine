/**
 * Created by hejian on 1/22/15.
 */
package com.google.refine.browsing.facets;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.DecoratedValue;
import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordFilter;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.filters.AllRowsRecordFilter;
import com.google.refine.browsing.filters.AnyRowRecordFilter;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;


public class RecommendChangeFacet implements Facet {
    /*
     * Configuration
     */
    protected String     _name;
    protected String     _expression;
    protected String     _columnName;
    protected boolean    _invert;

    // If true, then facet won't show the blank and error choices
    protected boolean _omitBlank;
    protected boolean _omitError;

    protected List<NominalFacetChoice> _selection = new LinkedList<NominalFacetChoice>();
    protected boolean _selectBlank;
    protected boolean _selectError;

    /*
     * Derived configuration
     */
    protected int        _cellIndex;
    protected Evaluable _eval;
    protected String     _errorMessage;

    /*
     * Computed results
     */
    protected List<NominalFacetChoice> _choices = new LinkedList<NominalFacetChoice>();
    protected int _blankCount;
    protected int _errorCount;

    public RecommendChangeFacet() {
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {

        writer.object();
        writer.key("name"); writer.value(_name);
        writer.key("expression"); writer.value(_expression);
        writer.key("columnName"); writer.value(_columnName);
        writer.key("invert"); writer.value(_invert);

        if (_errorMessage != null) {
            writer.key("error"); writer.value(_errorMessage);
        } else if (_choices.size() > getLimit()) {
            writer.key("error"); writer.value("Too many choices");
            writer.key("choiceCount"); writer.value(_choices.size());
        } else {
            writer.key("choices"); writer.array();
            for (NominalFacetChoice choice : _choices) {
                choice.write(writer, options);
            }
            writer.endArray();

            if (!_omitBlank && (_selectBlank || _blankCount > 0)) {
                writer.key("blankChoice");
                writer.object();
                writer.key("s"); writer.value(_selectBlank);
                writer.key("c"); writer.value(_blankCount);
                writer.endObject();
            }
            if (!_omitError && (_selectError || _errorCount > 0)) {
                writer.key("errorChoice");
                writer.object();
                writer.key("s"); writer.value(_selectError);
                writer.key("c"); writer.value(_errorCount);
                writer.endObject();
            }
        }

        writer.endObject();
    }

    protected int getLimit() {
        Object v = ProjectManager.singleton.getPreferenceStore().get("ui.browsing.listFacet.limit");
        if (v != null) {
            if (v instanceof Number) {
                return ((Number) v).intValue();
            } else {
                try {
                    int n = Integer.parseInt(v.toString());
                    return n;
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return 2000;
    }

    @Override
    public void initializeFromJSON(Project project, JSONObject o) throws JSONException {
        _name = o.getString("name");
        _expression = o.getString("expression");
        _columnName = o.getString("columnName");
        _invert = o.has("invert") && o.getBoolean("invert");

        if (_columnName.length() > 0) {
            Column column = project.columnModel.getColumnByName(_columnName);
            if (column != null) {
                _cellIndex = column.getCellIndex();
            } else {
                _errorMessage = "No column named " + _columnName;
            }
        } else {
            _cellIndex = -1;
        }

        try {
            _eval = MetaParser.parse(_expression);
        } catch (ParsingException e) {
            _errorMessage = e.getMessage();
        }

        _selection.clear();

        JSONArray a = o.getJSONArray("selection");
        int length = a.length();

        for (int i = 0; i < length; i++) {
            JSONObject oc = a.getJSONObject(i);
            JSONObject ocv = oc.getJSONObject("v");

            DecoratedValue decoratedValue = new DecoratedValue(
                    ocv.get("v"), ocv.getString("l"));

            NominalFacetChoice nominalFacetChoice = new NominalFacetChoice(decoratedValue);
            nominalFacetChoice.selected = true;

            _selection.add(nominalFacetChoice);
        }

        _omitBlank = JSONUtilities.getBoolean(o, "omitBlank", false);
        _omitError = JSONUtilities.getBoolean(o, "omitError", false);

        _selectBlank = JSONUtilities.getBoolean(o, "selectBlank", false);
        _selectError = JSONUtilities.getBoolean(o, "selectError", false);
    }

    @Override
    public RowFilter getRowFilter(Project project) {
        return null;
    }

    @Override
    public RecordFilter getRecordFilter(Project project) {
        RowFilter rowFilter = getRowFilter(project);
        return rowFilter == null ? null :
                (_invert ?
                        new AllRowsRecordFilter(rowFilter) :
                        new AnyRowRecordFilter(rowFilter));
    }

    @Override
    public void computeChoices(Project project, FilteredRows filteredRows) {
        _choices.add(new NominalFacetChoice(new DecoratedValue("MartialStatus, City", "Change 'M' to 'TEST_VALUE' because State='AK'")));
        _choices.add(new NominalFacetChoice(new DecoratedValue("State, Zip", "Change 'AK' to 'TEST_VALUE' because Zip=1128'")));
        _choices.add(new NominalFacetChoice(new DecoratedValue("City, State", "Change 'ALEXANDRIA' to 'TEST_VALUE' because State=VA")));
    }

    @Override
    public void computeChoices(Project project, FilteredRecords filteredRecords) {
        _choices.add(new NominalFacetChoice(new DecoratedValue("recommendChange", "Change 'Kazi' to 'Jaewoo' because Phone='120-1788'")));
    }
}
