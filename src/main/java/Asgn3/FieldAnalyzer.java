package Asgn3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * analyzes field declarations to determine composition/aggregation relationships
 *
 * @author Megan Fung
 * @version 1.0
 */
public class FieldAnalyzer {

    /**
     * represents a field in a class
     */
    static class FieldInfo {

        // fields
        String fieldType;
        String fieldName;
        boolean isFinal;
        boolean isStatic;

        /**
         * constructor
         * @param fieldType field's type
         * @param fieldName field's name
         * @param isFinal final? --> immutable 
         * @param isStatic static? 
         */
        FieldInfo(String fieldType, String fieldName, boolean isFinal, boolean isStatic) {
            this.fieldType = fieldType;
            this.fieldName = fieldName;
            this.isFinal = isFinal;
            this.isStatic = isStatic;
        }
    }

    /**
     * extracts all field declarations from a class body
     * only scans the top of the class (before first method/constructor)
     * to avoid false positives from local variables
     * 
     * @param classBody source code of class body
     * @param availableClasses set of class names in the project
     * @return list of fields that reference other classes
     */
    public static List<FieldInfo> extractFields(String classBody, Set<String> availableClasses) {
        List<FieldInfo> fields = new ArrayList<>();

        // extract only the field declaration section (before first method/constructor)
        String fieldSection = extractFieldSection(classBody);
        String cleaned = cleanForFieldExtraction(fieldSection);

        // simple string-based field extraction (no regex backtracking)
        for (String className : availableClasses) {
            int index = 0;
            while ((index = cleaned.indexOf(className, index)) != -1) {
                // check word boundaries
                boolean startOk = (index == 0) || !Character.isJavaIdentifierPart(cleaned.charAt(index - 1));
                boolean endOk = (index + className.length() >= cleaned.length()) || 
                               !Character.isJavaIdentifierPart(cleaned.charAt(index + className.length()));
                
                if (startOk && endOk) {
                    // extract field name after the type
                    int afterType = index + className.length();
                    while (afterType < cleaned.length() && Character.isWhitespace(cleaned.charAt(afterType))) {
                        afterType++;
                    }
                    
                    StringBuilder fieldName = new StringBuilder();
                    while (afterType < cleaned.length() && Character.isJavaIdentifierPart(cleaned.charAt(afterType))) {
                        fieldName.append(cleaned.charAt(afterType));
                        afterType++;
                    }
                    
                    if (fieldName.length() > 0) {
                        // check for field end marker
                        while (afterType < cleaned.length() && Character.isWhitespace(cleaned.charAt(afterType))) {
                            afterType++;
                        }
                        if (afterType < cleaned.length() && (cleaned.charAt(afterType) == ';' || cleaned.charAt(afterType) == '=')) {
                            // look back for modifiers
                            int lookBack = Math.max(0, index - 200);
                            String precedingText = cleaned.substring(lookBack, index);
                            boolean isFinal = precedingText.contains("final");
                            boolean isStatic = precedingText.contains("static");
                            
                            fields.add(new FieldInfo(className, fieldName.toString(), isFinal, isStatic));
                        }
                    }
                }
                index++;
            }
        }

        return fields;
    }

    /**
     * determines whether a field represents composition, aggregation, or association
     * 
     * composition: Strong ownership, lifecycle dependency
     *   - private field (strong encapsulation)
     *   - instantiated internally with 'new' (part cannot exist without whole)
     * 
     * aggregation: Weaker "whole-part" relationship
     *   - field is passed from outside (constructor parameter, setter)
     *   - part can exist independently
     * 
     * association: Simple reference without strong ownership
     *   - doesn't fit composition or aggregation criteria
     *
     * @param field the field to analyze
     * @param classBody source code to check for instantiation
     * @return "composition", "aggregation", or "association"
     */
    public static String determineFieldRelationship(FieldInfo field, String classBody) {
        
        // composition 
        if (isPrivateField(field, classBody)) {
            // check for "fieldName = new FieldType" pattern
            String[] patterns = {
                field.fieldName + "=new" + field.fieldType,
                field.fieldName + " =new" + field.fieldType,
                field.fieldName + "= new" + field.fieldType,
                field.fieldName + " = new" + field.fieldType,
                field.fieldName + " = new " + field.fieldType
            };
            
            for (String pattern : patterns) {
                if (classBody.contains(pattern)) {
                    return "composition";
                }
            }
        }
        
        // aggregation
        if (isPassedFromOutside(field, classBody)) {
            return "aggregation";
        }
        
        // association 
        return "association";
    }
    
    /**
     * checks if field is private
     * @param field field to check
     * @param classBody source code
     * @return true if field is declared as private
     */
    private static boolean isPrivateField(FieldInfo field, String classBody) {
        // look for "private" before field name within reasonable distance
        int privateIdx = classBody.indexOf("private");
        while (privateIdx != -1) {
            int fieldIdx = classBody.indexOf(field.fieldName, privateIdx);
            if (fieldIdx != -1 && fieldIdx - privateIdx < 200) {
                return true;
            }
            privateIdx = classBody.indexOf("private", privateIdx + 1);
        }
        return false;
    }
    
    /**
     * checks if field is passed from outside (constructor parameter or setter)
     * @param field field to check
     * @param classBody source code
     * @return true if field is set via constructor parameter or setter method
     */
    private static boolean isPassedFromOutside(FieldInfo field, String classBody) {
        String fieldType = field.fieldType;
        String fieldName = field.fieldName;
        
        // check for field type in method/constructor parameters
        if (classBody.contains("(" + fieldType) || classBody.contains(", " + fieldType)) {
            return true;
        }
        
        // check for setter methods
        if ((classBody.contains("set") || classBody.contains("Set")) && classBody.contains(fieldType)) {
            return true;
        }
        
        // check for assignment from parameter (field = param, not field = new)
        if (classBody.contains(fieldName + " =") || classBody.contains(fieldName + "=")) {
            int assignIdx = classBody.indexOf(fieldName + " =");
            if (assignIdx == -1) assignIdx = classBody.indexOf(fieldName + "=");
            if (assignIdx != -1) {
                int afterEquals = classBody.indexOf("=", assignIdx) + 1;
                if (afterEquals < classBody.length()) {
                    String afterAssign = classBody.substring(afterEquals, Math.min(afterEquals + 10, classBody.length())).trim();
                    if (!afterAssign.startsWith("new")) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * extracts the field declaration section of a class
     * stops at first method or constructor to avoid local variables
     * 
     * @param classBody original class body
     * @return field section only
     */
    private static String extractFieldSection(String classBody) {
        // find first opening brace after closing parenthesis (method/constructor signature)
        // pattern: ) ... { indicates method/constructor start
        int firstMethodStart = -1;
        
        for (int i = 0; i < classBody.length(); i++) {
            if (classBody.charAt(i) == ')') {
                // found closing paren, now look for opening brace (skip whitespace)
                int j = i + 1;
                while (j < classBody.length() && Character.isWhitespace(classBody.charAt(j))) {
                    j++;
                }
                // check if next non-whitespace char is opening brace
                if (j < classBody.length() && classBody.charAt(j) == '{') {
                    firstMethodStart = j;
                    break;
                }
            }
        }
        
        // if no method found, use entire body (rare case: class with only fields)
        if (firstMethodStart == -1) {
            return classBody;
        }
        
        // return everything before first method
        return classBody.substring(0, firstMethodStart);
    }
    
    /**
     * cleans class body for field extraction
     * removes strings and comments (no regex backtracking)
     *
     * @param classBody original class body
     * @return cleaned version
     */
    private static String cleanForFieldExtraction(String classBody) {
        StringBuilder result = new StringBuilder(classBody);
        
        boolean inString = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        char prevChar = '\0';
        
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            
            if (c == '"' && prevChar != '\\' && !inLineComment && !inBlockComment) {
                inString = !inString;
                result.setCharAt(i, ' ');
            } else if (inString) {
                if (c != '\n') result.setCharAt(i, ' ');
            }
            else if (!inString && !inBlockComment && c == '/' && i + 1 < result.length() && result.charAt(i + 1) == '/') {
                inLineComment = true;
                result.setCharAt(i, ' ');
            } else if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                } else {
                    result.setCharAt(i, ' ');
                }
            }
            else if (!inString && c == '/' && i + 1 < result.length() && result.charAt(i + 1) == '*') {
                inBlockComment = true;
                result.setCharAt(i, ' ');
            } else if (inBlockComment) {
                result.setCharAt(i, ' ');
                if (prevChar == '*' && c == '/') {
                    inBlockComment = false;
                }
            }
            
            prevChar = c;
        }
        
        return result.toString();
    }
}

