package org.glygen.array.util;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class SparqlUtils {
    
    private static final Map<String, String> SPARQL_ESCAPE_SEARCH_REPLACEMENTS = ImmutableMap.<String, String>builder()
            .put("\t", "\\t")
            .put("\n", "\\n")
            .put("\r", "\\r")
            .put("\b", "\\b")
            .put("\f", "\\f")
            .put("\"", "\\\"")
            .put("'", "\\'")
            .put("\\", "\\\\")
            .put("(", "\\(")
            .put(")", "\\)")
            .put("[", "\\[")
            .put("]", "\\]")
            .put("{", "\\{")
            .put("}", "\\}")
            .put("+", "\\+")
            .build();

    
    public static String escapeSpecialCharacters (String value) {
        StringBuffer bufOutput = new StringBuffer(value);
        for (int i = 0; i < bufOutput.length(); i++) {
            String replacement = SPARQL_ESCAPE_SEARCH_REPLACEMENTS.get("" + bufOutput.charAt(i));
            if(replacement!=null) {
                bufOutput.deleteCharAt(i);
                bufOutput.insert(i, replacement);
                // advance past the replacement
                i += (replacement.length() - 1);
            }
        }
        String doubled = bufOutput.toString().replace("\\", "\\\\");
        return doubled;
    }

}
