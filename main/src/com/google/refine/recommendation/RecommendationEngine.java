package com.google.refine.recommendation;

import com.google.refine.InterProjectModel;
import com.google.refine.browsing.DecoratedPredicate;
import com.google.refine.browsing.facets.NominalPredicate;
import com.google.refine.model.Column;
import com.google.refine.model.Project;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import com.google.refine.model.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by hejian on 1/26/15.
 */
public class RecommendationEngine {
    private Project _project;
    private List<Correlation> _correlations = null;
    private ArrayList<Integer>[] _columnInvlists = null;
    private static final int MAX_NUM_QUESTION = 5;

    protected static final Logger logger = LoggerFactory.getLogger("RecommendationEngine");

    public RecommendationEngine() {

    }

    public ArrayList<Integer>[] computeColumnInvLists(int rowIndex, int columnIndex, String from) {
        int numColumns = _project.columnModel.columns.size();
        int numRows = _project.rows.size();
        ArrayList<Integer>[] columnInvlists = new ArrayList[numColumns];
        for (int c = 0; c < numColumns; ++c) {
            ArrayList<Integer> columnInvlist = new ArrayList<Integer>();
            Object standardValue = _project.rows.get(rowIndex).getCell(c).value;
            for (int r = 0; r < numRows; ++r) {
                if (_project.rows.get(r).getCell(c).value.equals(standardValue)) {
                    columnInvlist.add(r);
                }
            }
            columnInvlists[c] = columnInvlist;
        }
        return columnInvlists;
    }
    public List<NominalPredicate> recommendChanges(Project project, int rowIndex, int columnIndex, String from, String to) {
        if (_project == null) {
            _project = project;
            _correlations = loadCorrelations();
            _columnInvlists = computeColumnInvLists(rowIndex, columnIndex, from);
        }

        for (Correlation correlation : _correlations) {
            if ((power(2, columnIndex) & correlation.mask) > 0) {
                Set<Integer> invlist = new HashSet<Integer>();
                for (int i = 0; i < correlation.columns.size(); ++i) {
                    if (i == 0)
                        invlist = new HashSet<Integer>(_columnInvlists[correlation.columns.get(0).getCellIndex()]);
                    else
                        invlist.retainAll(_columnInvlists[correlation.columns.get(i).getCellIndex()]);
                }
                correlation.invList = new ArrayList<Integer>(invlist);
                correlation.enrichScore = invlist.size() * correlation.score;
            } else {
                correlation.enrichScore = 0;
            }
        }
        Collections.sort(_correlations, new CorrelationComparator());

        List<NominalPredicate> choices = new LinkedList<NominalPredicate>();
        Row selectedRow = _project.rows.get(rowIndex);
        for (int i = 0; i < Math.min(_correlations.size(), MAX_NUM_QUESTION); ++i) {
            Correlation correlation = _correlations.get(i);
            int numColumn = correlation.columns.size();
            int[] columnIDs = new int[numColumn];
            Object[] values = new Object[numColumn];
            String label = "";
            for (int cid = 0; cid < numColumn; ++cid) {
                int columnID = correlation.columns.get(cid).getCellIndex();
                columnIDs[cid] = columnID;
                values[cid] = selectedRow.getCell(columnID).value;
                label = label + correlation.columns.get(cid).getName() + "=" + values[cid].toString();
                if (cid < numColumn - 1)
                    label = label + ", ";
            }
            DecoratedPredicate predicate = new DecoratedPredicate(columnIDs, values, label);
            NominalPredicate nominalPreidcate = new NominalPredicate(predicate);
            nominalPreidcate.count = correlation.invList.size();
            choices.add(nominalPreidcate);
        }
        return choices;
    }

    public List<Correlation> loadCorrelations() {
        String workDir = System.getProperty("user.dir");

        String fileName = _project.getMetadata().getName().split(" ")[0];
        logger.info("file_name: " + fileName);
        String correlationFileName = workDir + "/static/" + fileName + "/" + fileName + "_cors.csv";

        List<Correlation> correlations = new ArrayList<Correlation>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(correlationFileName));

            // First line is key columns
            String keyColumns = bufferedReader.readLine();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(":");
                String[] columnNames = parts[0].split(",");
                Correlation correlation = new Correlation();

                Boolean invalidCorrelation = false;
                for (String columnName : columnNames) {
                    if (_project.columnModel.getColumnByName(columnName) == null) {
                        invalidCorrelation = true;
                        break;
                    }
                    correlation.columns.add(_project.columnModel.getColumnByName(columnName));
                }
                if (invalidCorrelation)
                    continue;

                if (parts[1].equals("SOFT_FD"))
                    correlation.score = 1.0;
                else
                    correlation.score = Double.parseDouble(parts[1]);
                correlation.mask = computeMaskFromColumns(correlation.columns);
                correlations.add(correlation);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return correlations;
    }

    long computeMaskFromColumns(List<Column> columns) {
        long sum = 0;
        for (Column column : columns) {
            sum += power(2, column.getCellIndex());
        }
        return sum;
    }

    long power(int base, int exp) {
        long power = 1;
        for (int i = 0; i < exp; ++i)
            power *= base;
        return power;
    }
}

class Correlation {
    List<Column> columns = new ArrayList<Column>();
    double score;
    double enrichScore;
    long mask;
    List<Integer> invList;

    @Override
    public String toString() {
        StringBuilder line = new StringBuilder();
        for (Column column : columns) {
            line.append(column.getName() + ",");
        }
        line.append(":" + Double.toString(score));
        return line.toString();
    }


}
class CorrelationComparator implements Comparator<Correlation> {
    @Override
    public int compare(Correlation o1, Correlation o2) {
        // write comparison logic here like below , it's just a sample
        if (o1.enrichScore < o2.enrichScore)
            return 1;
        else
            return -1;
    }
}
