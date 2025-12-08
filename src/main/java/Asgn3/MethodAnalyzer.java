package Asgn3;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * analyzes method signatures and bodies for temporary usages and singleton patterns
 *
 * @author Megan Fung
 * @version 1.0
 */
public class MethodAnalyzer {

    /**
     * finds all classes used temporarily in methods
     * includes: method parameters, return types, local variables
     *
     * @param classBody source code of class body
     * @param availableClasses set of class names in the project
     * @return set of class names used temporarily
     */
    public static Set<String> findTemporaryUsages(String classBody, Set<String> availableClasses) {
        Set<String> usages = new HashSet<>();

        // clean the body
        String cleaned = cleanBody(classBody);

        for (String className : availableClasses) {
            // string checks for class usage in signatures and static calls
            String[] patterns = {
                className + " ",
                "(" + className + " ",
                ", " + className + " ",
                " " + className + " ",
                className + ".",
                "(" + className + ".",
                " " + className + "."
            };
            
            for (String pattern : patterns) {
                if (cleaned.contains(pattern)) {
                    usages.add(className);
                    break;
                }
            }
        }

        return usages;
    }

    /**
     * detects singleton usage patterns
     * looks for: ClassName.getInstance() or similar static accessor patterns
     *
     * @param classBody source code of class body
     * @param availableClasses set of class names in the project
     * @return set of singleton classes being used
     */
    public static Set<String> findSingletonUsages(String classBody, Set<String> availableClasses) {
        Set<String> singletons = new HashSet<>();

        String cleaned = cleanBody(classBody);

        for (String className : availableClasses) {
            // string checks for singleton access patterns
            if (cleaned.contains(className + ".getInstance") ||
                cleaned.contains(className + ".instance") ||
                cleaned.contains(className + ".get(")) {
                singletons.add(className);
            }
        }

        return singletons;
    }

    /**
     * cleans body by removing strings and comments (no regex backtracking)
     * @param body source code
     * @return cleaned version
     */
    private static String cleanBody(String body) {
        StringBuilder result = new StringBuilder(body);
        
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

