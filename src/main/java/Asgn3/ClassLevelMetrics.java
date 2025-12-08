package Asgn3;
import java.util.ArrayList;
import java.util.List;

/**
 *  object that represents a single java class and its class level metrics:
 * className -->  file name
 * A --> 1 if class is abstract or an interface, 0 otherwise
 * I --> instability (Ce / (Ca + Ce))
 * D -->distance (|A + I - 1|)
 * Ca --> afferent coupling (incoming dependencies)
 * Ce --> efferent coupling (outgoing dependencies)
 *
 * @author Megan Fung
 *  @version 1.0
 */
public class ClassLevelMetrics {

    // fields
    private final String className;
    private String classType;  
    private boolean isAbstract;
    private double I;
    private double D;
    private int Ca;
    private int Ce;
    private final List<String> extendsClasses = new ArrayList<>();
    private final List<String> implementsInterfaces = new ArrayList<>();
    private final List<String> associations = new ArrayList<>();
    private final List<String> dependencies = new ArrayList<>();
    private final List<String> compositions = new ArrayList<>();
    private final List<String> aggregations = new ArrayList<>();
    private boolean isSingleton = false;

    /**
     * constructor
     * name filename
     */
    public ClassLevelMetrics(String className) {

        // initialize fields
        this.className = className;
        this.Ca = 0;
        this.Ce = 0;

    }

    // setters
    public void setClassType(String classType) { this.classType = classType; }
    public void setAbstract(boolean isAbstract) { this.isAbstract = isAbstract; }
    public void setI(double i) { this.I = i; }
    public void setD(double d) { this.D = d; }
    public void incrementCa() { this.Ca++; }
    public void incrementCe() { this.Ce++; }

    // getters
    public String getClassName() { return className; }
    public String getClassType() { return classType; }
    public double getA() { return isAbstract ? 1.0 : 0.0; }
    public double getI() { return I; }
    public double getD() { return D; }
    public int getCa() { return Ca; }
    public int getCe() { return Ce; }


    /**
     * adds a parent class that this class extends
     * @param parentClass name of parent class
     */
    public void addExtends(String parentClass) {
        if (!extendsClasses.contains(parentClass)) {
            extendsClasses.add(parentClass);
        }
    }

    /**
     * adds an interface that this class implements
     * @param interfaceName name of interface
     */
    public void addImplements(String interfaceName) {
        if (!implementsInterfaces.contains(interfaceName)) {
            implementsInterfaces.add(interfaceName);
        }
    }

    /**
     * adds an association relationship (uses singleton or field)
     * @param className name of associated class
     */
    public void addAssociation(String className) {
        if (!associations.contains(className)) {
            associations.add(className);
        }
    }

    /**
     * adds a dependency relationship (temporary usage)
     * @param className name of dependent class
     */
    public void addDependency(String className) {
        if (!dependencies.contains(className)) {
            dependencies.add(className);
        }
    }

    /**
     * adds a composition relationship (strong ownership)
     * @param className name of composed class
     */
    public void addComposition(String className) {
        if (!compositions.contains(className)) {
            compositions.add(className);
        }
    }

    /**
     * adds an aggregation relationship (weak ownership)
     * @param className name of aggregated class
     */
    public void addAggregation(String className) {
        if (!aggregations.contains(className)) {
            aggregations.add(className);
        }
    }

    /**
     * marks this class as a singleton
     * @param singleton true if singleton pattern detected
     */
    public void setSingleton(boolean singleton) {
        this.isSingleton = singleton;
    }

    /**
     * checks if this class is a singleton
     * @return true if singleton
     */
    public boolean isSingleton()
    {
        return isSingleton;
    }

    /**
     * gets list of parent classes
     * @return list of parent class names
     */
    public List<String> getExtendsClasses() {
        return new ArrayList<>(extendsClasses);
    }

    /**
     * gets list of implemented interfaces
     * @return list of interface names
     */
    public List<String> getImplementsInterfaces() {
        return new ArrayList<>(implementsInterfaces);
    }

    /**
     * gets list of associated classes
     * @return list of associated class names
     */
    public List<String> getAssociations() {
        return new ArrayList<>(associations);
    }

    /**
     * gets list of dependent classes
     * @return list of dependency names
     */
    public List<String> getDependencies() {
        return new ArrayList<>(dependencies);
    }

    /**
     * gets list of composed classes
     * @return list of composition names
     */
    public List<String> getCompositions() {
        return new ArrayList<>(compositions);
    }

    /**
     * gets list of aggregated classes
     * @return list of aggregation names
     */
    public List<String> getAggregations() {
        return new ArrayList<>(aggregations);
    }

}
