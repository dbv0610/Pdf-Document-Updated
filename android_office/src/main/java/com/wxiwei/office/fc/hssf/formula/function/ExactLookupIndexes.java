package com.wxiwei.office.fc.hssf.formula.function;

import java.util.Map;

/**
 * Exact-match lookups (VLOOKUP/HLOOKUP/MATCH with 0/FALSE) scan the whole range for every
 * formula; a sheet with thousands of them over the same column is quadratic. While a caller
 * sets {@link #ACTIVE} (the XLSX formula engine does), LookupUtils scans each column/row of a
 * sheet range once into a value -> first index map kept here. The owner must clear the map
 * whenever a cell value changes.
 */
public final class ExactLookupIndexes {
    public static final ThreadLocal<Map<String, Map<Object, Integer>>> ACTIVE = new ThreadLocal<Map<String, Map<Object, Integer>>>();

    private ExactLookupIndexes() {
    }
}
