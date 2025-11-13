package Asgn3;
import java.util.List;
import java.util.Map;

/**
 * stores GH Repo's full analysis metrics:
 * file level-- size and complexity
 * class level-- instability, distance
 * full repo level-- abstraction and filePaths
 *
 * @author Megan Fung
 * @version 1.0
 */
public class GHRepoAnalyzed {

    // fields
    public final Map<String, FileLevelMetrics> fileMetrics;
    public final Map<String, ClassLevelMetrics> classMetrics;
    public final double A;
    public final List<String> filePaths;

    /**
     * constructor
     * @param fileMetrics strings that map to FileLevelMetrics objects
     * @param classMetrics strings that map to ClassLevelMetrics objects
     * @param A abstraction
     */
    public GHRepoAnalyzed(Map<String, FileLevelMetrics> fileMetrics,
                          Map<String, ClassLevelMetrics> classMetrics,
                          double A,
                          List<String> filePaths) {

        // initialize fields
        this.fileMetrics = fileMetrics;
        this.classMetrics = classMetrics;
        this.A = A;
        this.filePaths = filePaths;

    }

    // getters
    public Map<String, FileLevelMetrics> getFileMetrics() { return fileMetrics; }
    public Map<String, ClassLevelMetrics> getClassMetrics() { return classMetrics; }
    public double getA() { return A; }
    public List<String> getFilePaths() { return filePaths; }

}
