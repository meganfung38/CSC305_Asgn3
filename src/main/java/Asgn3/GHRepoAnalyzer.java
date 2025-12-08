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

        // prepare objects for class level metric computations
        for (JavaClass javaClass : classes) {
            classMetrics.put(javaClass.name, new ClassLevelMetrics(javaClass.name));
        }

        // analyze class relationships (calculate Ca and Ce)
        inspectSignatures(classes, classMetrics);
        inspectBodies(classes, classMetrics);

        // analyze additional relationships (composition, aggregation, association, singleton)
        analyzeRelationships(classes, classMetrics);

        // add placeholder metrics for external classes 
        addExternalClasses(classMetrics);

        // compute class level metrics (A, I, D)
        double A = calculateClassMetrics(classes, classMetrics);

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

        // remove comments and strings to avoid false matches
        String cleanedContent = removeCommentsAndStrings(fileContent);

        // pattern for class signatures
        Pattern signaturePattern = Pattern.compile("(?s)\\b(class|interface)\\s+(\\w+)\\s*(.*?)\\{");
        Matcher matcher = signaturePattern.matcher(cleanedContent);

        // extract classes
        while (matcher.find()) {
            String classType = matcher.group(1);
            String className = matcher.group(2);
            String endOfSig = matcher.group(3).trim();  // extends or implements
            
            int openBrace = matcher.end() - 1;
            int closeBrace = findCloseBrace(fileContent, openBrace);
            if (closeBrace == -1) { continue; }  // skip malformed class

            // create JavaClass
            JavaClass currClass = new JavaClass(className, endOfSig, openBrace, closeBrace);
            currClass.fullBody = fileContent.substring(openBrace, closeBrace + 1);

            // set class type 
            if (classType.equals("interface")) {
                currClass.classType = "interface";
            } else {
                // check if abstract class by looking backwards in cleaned content
                int matchStart = matcher.start();
                int lookBackStart = Math.max(0, matchStart - 100);
                String precedingText = cleanedContent.substring(lookBackStart, matchStart);
                
                if (Pattern.compile("\\babstract\\s+class\\s*$").matcher(precedingText).find()) {
                    currClass.classType = "abstract";
                } else {
                    currClass.classType = "class";
                }
            }

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

            // generalization
            Matcher extendsMatcher = extendsPattern.matcher(signatureContent);
            if (extendsMatcher.find()) {
                String parentClass = extendsMatcher.group(1);
                classMetrics.get(className).incrementCe();
                classMetrics.get(className).addExtends(parentClass);
                if (classMetrics.containsKey(parentClass)) {
                    classMetrics.get(parentClass).incrementCa();
                }
            }

            // realization
            Matcher implementsMatcher = implementsPattern.matcher(signatureContent);
            if (implementsMatcher.find()) {
                String implementations = implementsMatcher.group(1);
                for (String implementation : implementations.split(",")) {
                    String interfaceName = implementation.trim();
                    classMetrics.get(className).incrementCe();
                    classMetrics.get(className).addImplements(interfaceName);
                    if (classMetrics.containsKey(interfaceName)) {
                        classMetrics.get(interfaceName).incrementCa();
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

                // check if other class is referenced in body
                Matcher matcher = Pattern.compile("\\b" + Pattern.quote(otherClassName) + "\\b").matcher(cleanedBody);
                if (matcher.find()) {  // found reference
                    classMetrics.get(otherClassName).incrementCa();  // increment other class's incoming dependencies
                    classMetrics.get(currClassName).incrementCe();  // increment current class's outgoing dependencies
                }
            }
        }

    }

    /**
     * adds placeholder ClassLevelMetrics for external classes
     * @param classMetrics existing metrics map
     */
    private void addExternalClasses(Map<String, ClassLevelMetrics> classMetrics) {
        Set<String> externalClasses = new HashSet<>();
        Set<String> externalInterfaces = new HashSet<>();
        Set<String> externalExtends = new HashSet<>();

        // collect all referenced classes and track their source (implements vs extends)
        for (ClassLevelMetrics metric : classMetrics.values()) {
            externalExtends.addAll(metric.getExtendsClasses());
            externalInterfaces.addAll(metric.getImplementsInterfaces());
            externalClasses.addAll(metric.getCompositions());
            externalClasses.addAll(metric.getAggregations());
            externalClasses.addAll(metric.getAssociations());
            externalClasses.addAll(metric.getDependencies());
        }

        // combine all external references
        externalClasses.addAll(externalExtends);
        externalClasses.addAll(externalInterfaces);

        // add placeholder metrics for external classes with correct type
        for (String externalClass : externalClasses) {
            if (!classMetrics.containsKey(externalClass)) {
                ClassLevelMetrics external = new ClassLevelMetrics(externalClass);
                
                // determine type based on how it's used
                if (externalInterfaces.contains(externalClass)) {
                    external.setClassType("interface");
                    external.setAbstract(true);  // interfaces are abstract
                } else if (externalExtends.contains(externalClass)) {
                    external.setClassType("class");  // could be abstract, but we assume concrete
                    external.setAbstract(false);
                } else {
                    external.setClassType("class");  // default for field/method references
                    external.setAbstract(false);
                }
                
                classMetrics.put(externalClass, external);
            }
        }
    }

    /**
     * analyzes class relationships
     * @param classes all JavaClass objects
     * @param classMetrics store relationships in this object
     */
    private void analyzeRelationships(List<JavaClass> classes, Map<String, ClassLevelMetrics> classMetrics) {

        Set<String> classNames = classMetrics.keySet();

        for (JavaClass currentClass : classes) {
            String className = currentClass.name;
            ClassLevelMetrics metrics = classMetrics.get(className);

            // check if class is singleton
            if (SingletonDetector.isSingleton(currentClass)) {
                metrics.setSingleton(true);
            }

            // analyze fields 
            analyzeFieldRelationships(currentClass, classMetrics, classNames);

            // analyze methods
            analyzeMethodRelationships(currentClass, classMetrics, classNames);
        }
    }

    /**
     * analyzes field declarations to determine composition/aggregation/association
     * @param javaClass the class to analyze
     * @param classMetrics metrics storage
     * @param classNames available class names
     */
    private void analyzeFieldRelationships(JavaClass javaClass,
                                           Map<String, ClassLevelMetrics> classMetrics,
                                           Set<String> classNames) {

        List<FieldAnalyzer.FieldInfo> fields = FieldAnalyzer.extractFields(javaClass.fullBody, classNames);

        for (FieldAnalyzer.FieldInfo field : fields) {
            // determine if composition, aggregation, or association
            String relationship = FieldAnalyzer.determineFieldRelationship(field, javaClass.fullBody);

            if (relationship.equals("composition")) {
                classMetrics.get(javaClass.name).addComposition(field.fieldType);
            } else if (relationship.equals("aggregation")) {
                classMetrics.get(javaClass.name).addAggregation(field.fieldType);
            } else {  // association
                classMetrics.get(javaClass.name).addAssociation(field.fieldType);
            }
        }
    }

    /**
     * analyzes method usages for association/dependency
     * @param javaClass the class to analyze
     * @param classMetrics metrics storage
     * @param classNames available class names
     */
    private void analyzeMethodRelationships(JavaClass javaClass,
                                            Map<String, ClassLevelMetrics> classMetrics,
                                            Set<String> classNames) {

        String cleanedBody = cleanBody(javaClass.cleanedBody);

        // check for singleton usage (association)
        Set<String> singletonUsages = MethodAnalyzer.findSingletonUsages(cleanedBody, classNames);
        for (String singleton : singletonUsages) {
            classMetrics.get(javaClass.name).addAssociation(singleton);
        }

        // check for temporary usage (dependency)
        Set<String> temporaryUsages = MethodAnalyzer.findTemporaryUsages(cleanedBody, classNames);
        for (String tempClass : temporaryUsages) {
            // only add if not already in extends/implements/fields/singletons
            if (!isAlreadyRelated(javaClass.name, tempClass, classMetrics)) {
                classMetrics.get(javaClass.name).addDependency(tempClass);
            }
        }
    }

    /**
     * checks if a relationship already exists between two classes
     * @param className source class
     * @param otherClass target class
     * @param classMetrics metrics to check
     * @return true if relationship exists
     */
    private boolean isAlreadyRelated(String className, String otherClass,
                                     Map<String, ClassLevelMetrics> classMetrics) {
        ClassLevelMetrics metrics = classMetrics.get(className);
        return metrics.getExtendsClasses().contains(otherClass) ||
               metrics.getImplementsInterfaces().contains(otherClass) ||
               metrics.getCompositions().contains(otherClass) ||
               metrics.getAggregations().contains(otherClass) ||
               metrics.getAssociations().contains(otherClass);
    }

    /**
     * calculates class level metrics (A, I, D)
     * @param classes list of JavaClass objects (to derive abstractness from classType)
     * @param classMetrics object to store metrics in
     * @return A (abstractness of the entire codebase)
     */
    private double calculateClassMetrics(List<JavaClass> classes, Map<String, ClassLevelMetrics> classMetrics) {

        // maps class names to their object representation
        Map<String, JavaClass> classLookup = new HashMap<>();
        for (JavaClass javaClass : classes) {
            classLookup.put(javaClass.name, javaClass);
        }

        // calculate A (abstract classes / total classes)
        int totalClasses = classes.size();
        long abstractClasses = classes.stream().filter(JavaClass::isAbstract).count();
        double A = (totalClasses == 0) ? 0.0 : (double) abstractClasses / totalClasses;

        // calculate instability and distance for each class
        for (ClassLevelMetrics classMetric : classMetrics.values()) {

            // calculate instability (proportion of outgoing dependencies)
            double I = (classMetric.getCa() + classMetric.getCe() == 0) ? 0.0 :
                    (double) classMetric.getCe() / (classMetric.getCa() + classMetric.getCe());
            double D = Math.abs(A + I - 1.0);

            // get the JavaClass and derive isAbstract from classType
            JavaClass javaClass = classLookup.get(classMetric.getClassName());
            if (javaClass != null) {
                // internal class - set type and abstractness from JavaClass
                classMetric.setClassType(javaClass.classType);
                classMetric.setAbstract(javaClass.isAbstract());
            }
            // external classes already have classType and isAbstract set in addExternalClasses()
            
            classMetric.setI(I);
            classMetric.setD(D);

        }

        return A;

    }

    /**
     * removes comments and strings from source code while preserving positions
     * @param content original source code
     * @return cleaned version with comments/strings replaced by spaces
     */
    private String removeCommentsAndStrings(String content) {
        StringBuilder result = new StringBuilder(content);
        
        boolean inString = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        char prevChar = '\0';
        
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            
            // handle string literals
            if (c == '"' && prevChar != '\\' && !inLineComment && !inBlockComment) {
                inString = !inString;
                result.setCharAt(i, ' ');
            } else if (inString) {
                if (c != '\n') result.setCharAt(i, ' ');
            }
            
            // handle line comments
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
            
            // handle block comments
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
