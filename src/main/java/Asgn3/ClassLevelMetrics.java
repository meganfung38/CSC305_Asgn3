package Asgn3;


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
    private boolean isAbstract;
    private double I;
    private double D;
    private int Ca;
    private int Ce;

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

    // make fields mutable (setter methods)
    public void setAbstract(boolean isAbstract) { this.isAbstract = isAbstract; }
    public void setI(double i) { this.I = i; }
    public void setD(double d) { this.D = d; }
    public void incrementCa() { this.Ca++; }
    public void incrementCe() { this.Ce++; }

    // access fields (getter methods)
    public String getClassName() { return className; }
    public double getA() { return isAbstract ? 1.0 : 0.0; }
    public double getI() { return I; }
    public double getD() { return D; }
    public int getCa() { return Ca; }
    public int getCe() { return Ce; }

}
