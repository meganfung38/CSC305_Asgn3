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

        // get class level information
        Map<String, String> classSignatures = new HashMap<>();  // maps class names to its class signature
        Map<String, String> classBodies = new HashMap<>();  // maps class name to its body (source code)
        Map<String, Boolean> isAbstract = new HashMap<>();  // maps class name to a boolean value representing class type
        extractClasses(fileContents, classSignatures, classBodies, isAbstract);  // store maps using file contents

        Map<String, ClassLevelMetrics> classMetrics = new HashMap<>();  // initialize class metrics list
        for (String className : classBodies.keySet()) {
            classMetrics.put(className, new ClassLevelMetrics(className));
        }

        // analyze class relationships
        inspectSignatures(classSignatures, classMetrics);
        inspectBodies(classBodies, classMetrics);

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
     * get all classes that exist in GH folder and stores classSignatures, classBodies, and isAbstract
     * @param fileContents  content within each file of GH folder
     * @param classSignature  maps class name to class signatures
     * @param classBodies maps class name to class bodies
     * @param isAbstract maps class name to its class type
     */
    private void extractClasses(Map<String, String> fileContents,
                                Map<String, String> classSignature,
                                Map<String, String> classBodies,
                                Map<String, Boolean> isAbstract) {

        // iterate over file content map
        for (String fileContent : fileContents.values()) {

            // class signature pattern
            // first detect class type
            // second detect class name
            Matcher classPattern = Pattern
                    .compile("(?s)\\b(class|interface)\\s+(\\w+)\\s*(.*?)\\{")
                    .matcher(fileContent);

            while (classPattern.find()) {

                String classType = classPattern.group(1);
                String className = classPattern.group(2);
                String endOfSignature = classPattern.group(3);

                // classify class type
                // true --> abstract and false otherwise
                boolean abstractClass = classType.equals("interface") ||
                        Pattern.compile("\\babstract\\s+class\\s+" + className + "\\b").matcher(fileContent).find();
                isAbstract.put(className, abstractClass);

                // add class to class metrics list
                classSignature.put(className, endOfSignature);

                // find start and end of class body
                int openBrace = classPattern.end() - 1;
                int closeBrace = findCloseBrace(fileContent, openBrace);
                if (closeBrace > openBrace) {  // found valid start and end
                    String body = fileContent.substring(openBrace, closeBrace + 1);
                    classBodies.put(className, body);
                }

            }

        }

    }

    /**
     * inspect class signatures for dependencies
     * @param signatures all class signatures
     * @param classMetrics store metrics in this object
     */
    private void inspectSignatures(Map<String, String> signatures, Map<String, ClassLevelMetrics> classMetrics) {

        // patterns for dependencies in signatures
        Pattern extendsPattern = Pattern.compile("\\bextends\\s+(\\w+)");
        Pattern implementsPattern = Pattern.compile("\\bimplements\\s+([\\w,\\s]+)");

        for (var signature : signatures.entrySet()) {

            String className = signature.getKey();
            String signatureContent = signature.getValue();

            Matcher extendsMatcher = extendsPattern.matcher(signatureContent);
            if (extendsMatcher.find()) {  // found reference
                if (classMetrics.containsKey(extendsMatcher.group(1))) {  // check if class exists
                    classMetrics.get(className).incrementCe();  // increment current class's outgoing dependencies
                    classMetrics.get(extendsMatcher.group(1)).incrementCa();  // increment referenced class's incoming dependencies
                }
            }

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
     * @param bodies all class bodies
     * @param classMetrics store metrics in this object
     */
    private void inspectBodies(Map<String, String> bodies, Map<String, ClassLevelMetrics> classMetrics) {

        // examine class relationships
        for (var currClass : bodies.entrySet()) {

            String currClassName =  currClass.getKey();
            String cleanedBody = cleanBody(currClass.getValue());  // don't inspect strings and comments in body

            // iterate over all other classes
            for (String otherClassName : bodies.keySet()) {

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
