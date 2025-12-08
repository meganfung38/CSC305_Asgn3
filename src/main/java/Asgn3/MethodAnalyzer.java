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
            // method parameters
            Pattern paramPattern = Pattern.compile(
                    "\\((?:[^()]*,\\s*)*" + Pattern.quote(className) + "\\s+\\w+[^)]*\\)"
            );

            // return type
            Pattern returnPattern = Pattern.compile(
                    "\\b" + Pattern.quote(className) + "\\s+\\w+\\s*\\("
            );

            // local variable
            Pattern localVarPattern = Pattern.compile(
                    "\\b" + Pattern.quote(className) + "\\s+\\w+\\s*="
            );

            if (paramPattern.matcher(cleaned).find() ||
                returnPattern.matcher(cleaned).find() ||
                localVarPattern.matcher(cleaned).find()) {
                usages.add(className);
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
            // find getInstance function 
            Pattern singletonPattern = Pattern.compile(
                    "\\b" + Pattern.quote(className) + 
                    "\\.(?:getInstance|instance|get)\\s*\\("
            );

            if (singletonPattern.matcher(cleaned).find()) {
                singletons.add(className);
            }
        }

        return singletons;
    }

    /**
     * cleans body by removing strings and comments
     * @param body source code
     * @return cleaned version
     */
    private static String cleanBody(String body) {
        return body
                .replaceAll("\"(\\\\.|[^\"\\\\])*\"", "")
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("//.*", "");
    }
}

