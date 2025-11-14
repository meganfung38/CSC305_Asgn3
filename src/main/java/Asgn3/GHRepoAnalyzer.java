package Asgn3;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * computes GH Repo metrics
 * size --> number of lines (non-empty)
 * complexity --> number of control statements
 * abstraction --> proportion of classes that are abstract
 * instability --> proportion of outgoing dependencies
 * distance --> |A + I - 1|
 *
 * @author Megan Fung
 * @version 1.0
 */

public class GHRepoAnalyzer {

    // components
    private final GHOperations ghOperations;

    /**
     * constructor
     * @param ghOperations GH operations helper
     */
    public GHRepoAnalyzer(GHOperations ghOperations) {

        // initialize components
        this.ghOperations = ghOperations;

    }

    /**
     * analyzes all files for a GH folder URL
     * @param GHUrl GH folder URL
     * @return list of objects containing metrics for each file in GH folder
     * @throws IOException if GH API access fails
     */
    public GHRepoAnalyzed analyzeFiles(String GHUrl) throws IOException {

        // GH setup
        GHInfo ghInfo = GHOperations.parseGHURL(GHUrl);
        List<String> filePaths = ghOperations.listFilesRecursive(GHUrl);

        // map filenames to its contents
        Map<String, String> fileContents = loadFiles(ghInfo, filePaths);

        // map filenames to file level metrics
        Map<String, FileLevelMetrics> fileMetrics = calculateFileMetrics(fileContents);

        // create container for classes
        List<JavaClass> classes = new ArrayList<>();

        // iterate over file contents in GH repo to extract all classes
        for (String fileContent : fileContents.values()) {
            List<JavaClass> fileClasses = extractClasses(fileContent);
            cleanClassBodies(fileClasses, fileContent);
            classes.addAll(fileClasses);
        }

        // create container for class level metrics
        Map<String, ClassLevelMetrics> classMetrics = new HashMap<>();

        // create container for class types (true for abstract, false otherwise)
        Map<String, Boolean> isAbstract = new HashMap<>();

        // prepare objects for class level metric computations
        for (JavaClass javaClass : classes) {
            classMetrics.put(javaClass.name, new ClassLevelMetrics(javaClass.name));
            isAbstract.put(javaClass.name, javaClass.isAbstract);
        }

        // analyze class relationships (calculate Ca and Ce)
        inspectSignatures(classes, classMetrics);
        inspectBodies(classes, classMetrics);

        // compute class level metrics
        double A = calculateClassMetrics(isAbstract, classMetrics);

        return new GHRepoAnalyzed(fileMetrics, classMetrics, A, filePaths);

    }

    /**
     * loads all files from GH URL
     * @param info  GHInfo objects containing GH metadata
     * @param filePaths  files in GH folder
     * @return  maps file names to its contents
     * @throws IOException throw error if no java files
     */
    private Map<String, String> loadFiles(GHInfo info, List<String> filePaths) throws IOException {

        Map<String, String> fileContents = new HashMap<>();
        for (String filePath : filePaths) {

            // confirm java file
            if (!filePath.toLowerCase().endsWith(".java")) {
                continue;
            }
            String fileContent = ghOperations.getFileContent(info.owner(), info.repo(), filePath, info.ref());
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            fileContents.put(fileName, fileContent);
        }
        return fileContents;
    }

    /**
     * calculates file level metrics (size + complexity)
     * @param fileContents filenames mapped to their contents
     * @return  filenames mapped to their file metrics
     */
    private Map<String, FileLevelMetrics> calculateFileMetrics(Map<String, String> fileContents) {

        Map<String, FileLevelMetrics> fileMetrics = new HashMap<>();  // result

        // no files to analyze
        if (fileContents.isEmpty()) {
            return fileMetrics;
        }

        // iterate over file content map to calculate file level metrics
        for (var fileContent : fileContents.entrySet()) {

            String name = fileContent.getKey();
            String content = fileContent.getValue();

            // create object for current file
            FileLevelMetrics file = new FileLevelMetrics(name);

            // calculate size (non-empty lines)
            int size = (int) content.lines().filter(line -> !line.trim().isEmpty()).count();
            file.setSize(size);

            // calculate complexity (# of control statements)
            file.setComplexity(countComplexity(content));

            // add file to file metrics list
            fileMetrics.put(name, file);

        }

        return fileMetrics;

    }

    /**
     * helper function to calculate number of control statements in a file
     * @param content file contents as a string
     * @return number of control statements present
     */
    private int countComplexity(String content) {

        int count = 0;  // establish a counter
        String[] controlStatements = {"if", "switch", "for", "while"};  // control statements

        // clean content: remove string literals and comments
        String cleaned = content.replaceAll("\"(\\\\.|[^\"\\\\])*\"", "");
        cleaned = cleaned.replaceAll("(?s)/\\*.*?\\*/", "");
        cleaned = cleaned.replaceAll("//.*", "");

        // iterate through control statements
        for (String controlStatement : controlStatements) {

            // find instances of control statements in content
            var find_instances = Pattern.compile("\\b" + controlStatement + "\\b").matcher(cleaned);
            while (find_instances.find()) {
                count++; // increment counter
            }

        }

        return count;

    }

    /**
     * get all classes that exist in a file
     * @param fileContent file's source code
     * @return list of JavaClass objects containing class name, signature, and class body as a substring of fileContent
     */
    private List<JavaClass> extractClasses(String fileContent) {

        List<JavaClass> classes = new ArrayList<>();  // create container for objects

        // pattern for class signatures
        // capture: everything before class/interface keyword, class type, class name, rest of signature
        Pattern signaturePattern = Pattern.compile(
                "(?s)" +
                        "\\b([\\w\\s-]*?)\\s*" +  // capture all modifiers/keywords before class/interface
                        "(class|interface)\\s+" +
                        "(\\w+)" +
                        "([^{]*)\\{"
        );
        Matcher matcher = signaturePattern.matcher(fileContent);

        // extract classes
        while (matcher.find()) {

            // save class information 
            String modifiersRaw = matcher.group(1) != null ? matcher.group(1).trim() : "";
            String classType = matcher.group(2);
            String className = matcher.group(3);
            String endOfSig = matcher.group(4).trim();  // extends or implements
            int openBrace = matcher.end() - 1;
            int closeBrace = findCloseBrace(fileContent, openBrace);
            if (closeBrace == -1) { continue; }  // skip malformed class

            // create JavaClass
            JavaClass currClass = new JavaClass(className, endOfSig, openBrace, closeBrace);
            currClass.fullBody = fileContent.substring(openBrace, closeBrace + 1);

            // classify current class
            // abstract if: 1) interface, or 2) has abstract modifier
            currClass.isAbstract = classType.equals("interface") || modifiersRaw.contains("abstract");

            // add to list
            classes.add(currClass);
        }

        return classes;

    }

    /**
     * removes nested class definitions from parent class bodies
     * @param classes list of all classes (including nested)
     * @param fileContent original file content (needed for substring operations)
     */
    private void cleanClassBodies(List<JavaClass> classes, String fileContent) {

        // iterate over extracted classes to remove nestsed classes
        for (JavaClass parentClass : classes) {

            StringBuilder cleaned = new StringBuilder(parentClass.fullBody);
            int parentStart = parentClass.openBrace;

            // iterate over other classes to remove nested classes
            for (JavaClass potentialNested : classes) {

                // skip self
                if (parentClass == potentialNested) { continue; }

                // check if nested (potentialNested's braces are within parent's range)
                boolean isNested = potentialNested.openBrace > parentClass.openBrace &&
                        potentialNested.closeBrace < parentClass.closeBrace;

                // found nested class
                if (isNested) {
                    
                    int nestedStart = potentialNested.openBrace;
                    int nestedEnd = potentialNested.closeBrace;

                    // go back to find start of class signature
                    int declarationStart = nestedStart;
                    while (declarationStart > parentStart) {
                        char c = fileContent.charAt(declarationStart - 1);
                        if (c == ';' || c == '}') {
                            break;  // found the end of previous statement
                        }
                        declarationStart--;
                    }

                    // calculate positions relative to parent's body
                    int relativeStart = declarationStart - parentStart;
                    int relativeEnd = nestedEnd + 1 - parentStart;  // +1 to include closing brace

                    // remove nested class (replace with whitespace)
                    for (int i = relativeStart; i < relativeEnd && i < cleaned.length(); i++) {
                        cleaned.setCharAt(i, ' ');
                    }
                }
            }

            // store cleaned body
            parentClass.cleanedBody = cleaned.toString();
        }

    }

    /**
     * inspect class signatures for dependencies
     * @param classes all JavaClass objects
     * @param classMetrics store metrics in this object
     */
    private void inspectSignatures(List<JavaClass> classes, Map<String, ClassLevelMetrics> classMetrics) {

        // patterns for dependencies in signatures
        Pattern extendsPattern = Pattern.compile("\\bextends\\s+(\\w+)");
        Pattern implementsPattern = Pattern.compile("\\bimplements\\s+([\\w,\\s]+)");

        // iterate over extracted classes 
        for (JavaClass javaClass : classes) {

            String className = javaClass.name;
            String signatureContent = javaClass.signature;

            // extract dependencies in signature (for extends)
            Matcher extendsMatcher = extendsPattern.matcher(signatureContent);
            if (extendsMatcher.find()) {  // found reference
                if (classMetrics.containsKey(extendsMatcher.group(1))) {  // check if class exists
                    classMetrics.get(className).incrementCe();  // increment current class's outgoing dependencies
                    classMetrics.get(extendsMatcher.group(1)).incrementCa();  // increment referenced class's incoming dependencies
                }
            }

            // extract dependencies in signature (for implements)
            Matcher implementsMatcher = implementsPattern.matcher(signatureContent);
            if (implementsMatcher.find()) {  // found reference
                String implementations = implementsMatcher.group(1);  // all implemented classes
                for (String implementation : implementations.split(",")) {
                    String otherClassName = implementation.trim();
                    if (classMetrics.containsKey(otherClassName)) {  // check if class exists
                        classMetrics.get(className).incrementCe();  // increment current class's outgoing dependencies
                        classMetrics.get(otherClassName).incrementCa();  // increment referenced class's incoming dependencies
                    }
                }
            }

        }

    }

    /**
     * inspect class bodies for dependencies
     * @param classes all JavaClass objects
     * @param classMetrics store metrics in this object
     */
    private void inspectBodies(List<JavaClass> classes, Map<String, ClassLevelMetrics> classMetrics) {

        // examine class relationships
        for (JavaClass currClass : classes) {

            String currClassName = currClass.name;
            String cleanedBody = cleanBody(currClass.cleanedBody);  // use body without nested classes

            // iterate over all other classes
            for (JavaClass otherClass : classes) {

                String otherClassName = otherClass.name;

                // skip current class
                if (currClassName.equals(otherClassName)) { continue; }

                // find references to otherClass
                Matcher matcher = Pattern.compile("\\b" + Pattern.quote(otherClassName) + "\\b").matcher(cleanedBody);
                if (matcher.find()) {  // found reference
                    classMetrics.get(otherClassName).incrementCa();  // increment other class's incoming dependencies
                    classMetrics.get(currClassName).incrementCe();  // increment current class's outgoing dependencies
                }
            }
        }

    }

    /**
     * calculates class level metrics (A, I, D)
     * @param isAbstract boolean list where values represent class type (true for abstract, false otherwise)
     * @param classMetrics object to store metrics in
     * @return A
     */
    private double calculateClassMetrics(Map<String, Boolean> isAbstract, Map<String, ClassLevelMetrics> classMetrics) {

        // calculate A (abstract classes / total classes)
        int totalClasses = isAbstract.size();
        long abstractClasses = isAbstract.values().stream().filter(Boolean::booleanValue).count();
        double A = (totalClasses == 0) ? 0.0 : (double) abstractClasses / totalClasses;

        // calculate instability and distance for each class
        for (ClassLevelMetrics classMetric :  classMetrics.values()) {

            // calculate instability (proportion of outgoing dependencies)
            double I = (classMetric.getCa() + classMetric.getCe() == 0) ? 0.0 :
                    (double) classMetric.getCe() / (classMetric.getCa() + classMetric.getCe());
            double D = Math.abs(A + I - 1.0);

            classMetric.setAbstract(isAbstract.get(classMetric.getClassName()));
            classMetric.setI(I);
            classMetric.setD(D);

        }

        return A;

    }

    /**
     * iterate through rest of file content until we find closing brace that ends class body
     * @param content file content
     * @param openBraceIdx where class body starts
     * @return where class body ends
     */
    private int findCloseBrace(String content, int openBraceIdx) {

        int nestedBraces = 0;
        for (int i = openBraceIdx; i < content.length(); i++) {
            if (content.charAt(i) == '{') { nestedBraces++; }
            else if (content.charAt(i) == '}') {
                nestedBraces--;
                if (nestedBraces == 0) { return i; }
            }
        }

        return -1;

    }

    /**
     * cleans body of a class (removing strings and comments for dependency search)
     * @param body body of a class
     * @return body of class without strings and comments
     */
    private String cleanBody(String body) {

        return body.
                replaceAll("\"(\\\\.|[^\"\\\\])*\"", "").
                replaceAll("(?s)/\\*.*?\\*/", "").
                replaceAll("//.*", "");

    }

}
