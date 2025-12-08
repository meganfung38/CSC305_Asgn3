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
     * @param classBody source code of class body
     * @param availableClasses set of class names in the project
     * @return list of fields that reference other classes
     */
    public static List<FieldInfo> extractFields(String classBody, Set<String> availableClasses) {
        List<FieldInfo> fields = new ArrayList<>();

        String cleaned = cleanForFieldExtraction(classBody);

        Pattern fieldPattern = Pattern.compile(
                "\\b((?:public|protected|private|final|static|transient|volatile)\\s+)*" +
                "(\\w+)\\s+" +
                "(\\w+)\\s*[;=]"
        );

        Matcher matcher = fieldPattern.matcher(cleaned);

        while (matcher.find()) {
            String modifiers = matcher.group(1) != null ? matcher.group(1) : "";
            String fieldType = matcher.group(2);
            String fieldName = matcher.group(3);

            // only track fields that reference other classes in our analysis
            if (availableClasses.contains(fieldType)) {
                boolean isFinal = modifiers.contains("final");
                boolean isStatic = modifiers.contains("static");

                fields.add(new FieldInfo(fieldType, fieldName, isFinal, isStatic));
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
            Pattern newPattern = Pattern.compile(
                    "\\b" + Pattern.quote(field.fieldName) + "\\s*=\\s*new\\s+" + 
                    Pattern.quote(field.fieldType)
            );
            if (newPattern.matcher(classBody).find()) {
                return "composition";
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
        // look for private modifier in field declaration
        Pattern privatePattern = Pattern.compile(
                "\\bprivate\\s+(?:[\\w<>\\[\\],\\s]+\\s+)*" + 
                Pattern.quote(field.fieldType) + "\\s+" + 
                Pattern.quote(field.fieldName)
        );
        return privatePattern.matcher(classBody).find();
    }
    
    /**
     * checks if field is passed from outside (constructor parameter or setter)
     * @param field field to check
     * @param classBody source code
     * @return true if field is set via constructor parameter or setter method
     */
    private static boolean isPassedFromOutside(FieldInfo field, String classBody) {
        Pattern constructorParamPattern = Pattern.compile(
                "\\b\\w+\\s*\\([^)]*\\b" + 
                Pattern.quote(field.fieldType) + "\\s+" + 
                "\\w+[^)]*\\)"
        );
        
        Pattern setterPattern = Pattern.compile(
                "\\b(?:void|public|protected)\\s+\\w*set\\w*\\s*\\([^)]*" +
                Pattern.quote(field.fieldType) + "\\s+\\w+[^)]*\\)"
        );
        
        // method parameter
        Pattern assignmentFromParam = Pattern.compile(
                "(?:this\\.)?" + Pattern.quote(field.fieldName) + "\\s*=\\s*\\w+(?!\\s*new)"
        );
        
        return constructorParamPattern.matcher(classBody).find() ||
               setterPattern.matcher(classBody).find() ||
               assignmentFromParam.matcher(classBody).find();
    }

    /**
     * cleans class body for field extraction
     * removes strings, comments, and method bodies
     *
     * @param classBody original class body
     * @return cleaned version
     */
    private static String cleanForFieldExtraction(String classBody) {
        String cleaned = classBody;

        // remove string literals
        cleaned = cleaned.replaceAll("\"(\\\\.|[^\"\\\\])*\"", "");

        // remove comments
        cleaned = cleaned.replaceAll("(?s)/\\*.*?\\*/", "");
        cleaned = cleaned.replaceAll("//.*", "");

        // remove method bodies (content between { } after method declarations)
        cleaned = cleaned.replaceAll("\\{[^{}]*\\}", "");

        return cleaned;
    }
}

