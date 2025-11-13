package Asgn3;

import java.util.ArrayList;
import java.util.List;

/**
 * object that represents a single .java file and its file level metrics:
 * size --> number of non-empty lines
 * complexity --> number of control statements
 */
public class FileLevelMetrics {

    private final String name;
    private int size;
    private int complexity;

    /**
     * constructor
     * @param name .java file name
     */
    public FileLevelMetrics(String name) {

        // initialize fields
        this.name = name;

    }

    // getters
    public String getName() { return name; }
    public int getSize() { return size; }
    public int getComplexity() { return complexity; }

    // setters
    public void setSize(int size) { this.size = size; }
    public void setComplexity(int complexity) { this.complexity = complexity; }

}
