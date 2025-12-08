package Asgn3;

/**
 * represents a class found in a .java file
 *
 * @author Megan
 * @version 1.0
 */
public class JavaClass {

    // fields
    public final String name;
    public final String signature;
    public String fullBody;  // raw body with nested classes (if any)
    public String cleanedBody;  // body without nested classes (for dependency calculations)
    public final int openBrace;
    public final int closeBrace;
    public String classType;  

    /**
     * constructor
     * @param name  class name
     * @param signature  class signature (identifies class type)
     * @param openBrace  idx of brace that starts body
     * @param closeBrace  idx of brace that closes body
     */
    public JavaClass(String name, String signature, int openBrace, int closeBrace) {

        // initialize fields
        this.name = name;
        this.signature = signature;
        this.openBrace = openBrace;
        this.closeBrace = closeBrace;

    }

    /**
     * determines if this class is abstract (interface or abstract class)
     * @return true if interface or abstract class, false if concrete class
     */
    public boolean isAbstract() {
        return "interface".equals(classType) || "abstract".equals(classType);
    }

}
