

package com.google.refine.operations.cell;

import java.util.*;

import com.google.refine.browsing.DecoratedPredicate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.operations.EngineDependentMassCellOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RecommendedEditOperation extends EngineDependentMassCellOperation {
    final protected String         _expression;
          protected String         _from;
          protected String         _to;
          protected String         _choiceString;
          protected List<DecoratedPredicate>      _decoratedPredicates;

    protected static final Logger logger = LoggerFactory.getLogger("recommendedEditOperation");

    public RecommendedEditOperation(JSONObject engineConfig,
                                    String columnName,
                                    String expression,
                                    String from,
                                    String to,
                                    String choicesString) {
        super(engineConfig, columnName, true);
        _from = from;
        _to = to;
        _expression = expression;
        _choiceString = choicesString;
        _decoratedPredicates = new LinkedList<DecoratedPredicate>();
        try {
            JSONArray choices = ParsingUtilities.evaluateJsonStringToArray(choicesString);
            int length = choices.length();
            for (int i = 0; i < length; i++) {
                JSONObject nominalJson = choices.getJSONObject(i);
                JSONObject predicateJson = nominalJson.getJSONObject("v");
                DecoratedPredicate decoratedPredict = DecoratedPredicate.initializeFromJSON(predicateJson);
                _decoratedPredicates.add(decoratedPredict);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {

        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("columnName"); writer.value(_columnName);
        writer.key("expression"); writer.value(_expression);
        writer.key("from"); writer.value(_from);
        writer.key("to"); writer.value(_to);
        writer.key("correctChoices"); writer.value(_choiceString);
        writer.endObject();
    }

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.has("engineConfig") && !obj.isNull("engineConfig") ?
                obj.getJSONObject("engineConfig") : null;

        return new RecommendedEditOperation(
                engineConfig,
                obj.getString("columnName"),
                obj.getString("expression"),
                obj.getString("from"),
                obj.getString("to"),
                obj.getString("correctChoices"));
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Recommended edit cells in column " + _columnName;
    }

    @Override
    protected String createDescription(Column column,
                                       List<CellChange> cellChanges) {

        return "Change " + cellChanges.size() + " cells from " + _from + " to " + _to 
            + " in column " + column.getName();
    }

    @Override
    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);

        Evaluable eval = MetaParser.parse(_expression);
        Properties bindings = ExpressionUtils.createBindings(project);

        return new RowVisitor() {
            int                         columnIndex;
            Properties                  bindings;
            List<CellChange>            cellChanges;
            Evaluable                   eval;

            List<DecoratedPredicate>    decoratedPredicates;
            String                      from;
            String                      to;

            public RowVisitor init(
                    int columnIndex,
                    Properties bindings,
                    List<CellChange> cellChanges,
                    Evaluable eval,
                    List<DecoratedPredicate>    decoratedPredicates,
                    String from,
                    String to
            ) {
                this.columnIndex = columnIndex;
                this.bindings = bindings;
                this.cellChanges = cellChanges;
                this.eval = eval;
                this.decoratedPredicates = decoratedPredicates;
                this.from = from;
                this.to = to;
                logger.info("from = " + from + " to = " + to);
                return this;
            }

            @Override
            public void start(Project project) {
                // nothing to do
            }

            @Override
            public void end(Project project) {
                // nothing to do
            }

            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                Cell cell = row.getCell(columnIndex);
                for (DecoratedPredicate decoratedPredicate : decoratedPredicates) {
                    int[] columnIDs = decoratedPredicate.predicateColumnIDs;
                    Object[] values = decoratedPredicate.predicateValues;
                    Boolean needChange = true;
                    for (int i = 0; i < columnIDs.length; ++i) {
                        if (!row.getCell(columnIDs[i]).toString().equals(values[i].toString())) {
                            needChange = false;
                            break;
                        }
                    }
                    if (needChange) {
//                        logger.info("to: " + this.to + ", from: " + this.from);
                        if (to == null) {
                            logger.info("stupid happened again!!!!!!!!!!");
                            return false;
                        }
                        Cell newCell = new Cell(to, (cell != null) ? cell.recon : null);
                        CellChange cellChange = new CellChange(rowIndex, columnIndex, cell, newCell);
                        cellChanges.add(cellChange);
                        break;
                    }
                }
                return false;
            }
        }.init(column.getCellIndex(), bindings, cellChanges, eval, _decoratedPredicates, _from, _to);
    }
}
