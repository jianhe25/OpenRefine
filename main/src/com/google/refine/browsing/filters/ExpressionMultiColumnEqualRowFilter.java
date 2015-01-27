/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.browsing.filters;

import com.google.refine.browsing.DecoratedPredicate;
import com.google.refine.browsing.RowFilter;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.Column;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Judge if a row matches by evaluating a given expression on the row, based on a particular
 * column, and checking the result. It's a match if the result is any one of a given list of 
 * values, or if the result is blank or error and we want blank or error values. 
 */
public class ExpressionMultiColumnEqualRowFilter implements RowFilter {
    final protected Evaluable               _evaluable; // the expression to evaluate

    final protected String                  _columnName;
    final protected Project                 _project;
    final protected DecoratedPredicate      _predicate;
    final protected boolean                 _selectBlank;
    final protected boolean                 _selectError;
    final protected boolean                 _invert;

    static final protected Logger logger = LoggerFactory.getLogger("refine.broker");


    public ExpressionMultiColumnEqualRowFilter(
            Evaluable evaluable,
            String columnName,
            Project project,
            DecoratedPredicate predicate,
            boolean selectBlank,
            boolean selectError,
            boolean invert
    ) {
        _evaluable = evaluable;
        _columnName = columnName;
        _project = project;
        _predicate = predicate;
        _selectBlank = selectBlank;
        _selectError = selectError;
        _invert = invert;
    }



    @Override
    public boolean filterRow(Project project, int rowIndex, Row row) {
        return _invert ?
                !internalFilterRow(project, rowIndex, row) :
                internalFilterRow(project, rowIndex, row);
    }
    
    public boolean internalFilterRow(Project project, int rowIndex, Row row) {
        int len = this._predicate.predicateColumnIDs.length;
        for (int selectorIndex = 0; selectorIndex < len; ++selectorIndex) {
            int cellIndex = this._predicate.predicateColumnIDs[selectorIndex];
            String columnName = this._project.columnModel.columns.get(cellIndex).getName();
            Cell cell = cellIndex < 0 ? null : row.getCell(cellIndex);
            Properties bindings = ExpressionUtils.createBindings(project);
            ExpressionUtils.bind(bindings, row, rowIndex, columnName, cell);
            Object value = _evaluable.evaluate(bindings);
            if (value != null) {
                Boolean valid = false;
                if (value.getClass().isArray()) {
                    Object[] a = (Object[]) value;
                    for (Object v : a) {
                        if (testValue(v, selectorIndex)) {
                            valid = true;
                            break;
                        }
                    }
                    if (!valid) return false;
                } else if (value instanceof Collection<?>) {
                    for (Object v : ExpressionUtils.toObjectCollection(value)) {
                        if (testValue(v, selectorIndex)) {
                            valid = true;
                            break;
                        }
                    }
                    if (!valid) return false;
                } else if (value instanceof JSONArray) {
                    JSONArray a = (JSONArray) value;
                    int l = a.length();

                    for (int i = 0; i < l; i++) {
                        try {
                            if (testValue(a.get(i), selectorIndex)) {
                                valid = true;
                                break;
                            }
                        } catch (JSONException e) {
                            // ignore
                        }
                    }
                    if (!valid) return false;
                }  else {
                    valid = testValue(value, selectorIndex);
                    if (!valid) return false;
                }
            }
        }
        return true;
    }
    
//    public boolean internalInvertedFilterRow(Project project, int rowIndex, Row row) {
//        Cell cell = _cellIndex < 0 ? null : row.getCell(_cellIndex);
//
//        Properties bindings = ExpressionUtils.createBindings(project);
//        ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);
//
//        Object value = _evaluable.evaluate(bindings);
//        if (value != null) {
//            if (value.getClass().isArray()) {
//                Object[] a = (Object[]) value;
//                for (Object v : a) {
//                    if (testValue(v)) {
//                        return false;
//                    }
//                }
//                return true;
//            } else if (value instanceof Collection<?>) {
//                for (Object v : ExpressionUtils.toObjectCollection(value)) {
//                    if (testValue(v)) {
//                        return false;
//                    }
//                }
//                return true;
//            } else if (value instanceof JSONArray) {
//                JSONArray a = (JSONArray) value;
//                int l = a.length();
//
//                for (int i = 0; i < l; i++) {
//                    try {
//                        if (testValue(a.get(i))) {
//                            return false;
//                        }
//                    } catch (JSONException e) {
//                        // ignore
//                    }
//                }
//                return true;
//            } // else, fall through
//        }
//
//        return !testValue(value);
//    }
    
    protected boolean testValue(Object v, int predicateIndex) {
        if (ExpressionUtils.isError(v)) {
            return _selectError;
        } else if (ExpressionUtils.isNonBlankData(v)) {
            Object value = this._predicate.predicateValues[predicateIndex];
            Boolean isEqual = (v instanceof Number && value instanceof Number) ?
                    ((Number) value).doubleValue() == ((Number) v).doubleValue() :
                    value.equals(v);
            return isEqual;
        } else {
            return _selectBlank;
        }
    }
    
}
