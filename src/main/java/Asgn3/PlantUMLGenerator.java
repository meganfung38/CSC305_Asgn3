package Asgn3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * generates PlantUML syntax from analyzed class metrics
 *
 * @author Megan Fung
 * @version 1.0
 */
public class PlantUMLGenerator {

    /**
     * creates UML diagram
     * @param analysis the analyzed repository data
     * @return PlantUML syntax string
     */
    public static String generateUML(GHRepoAnalyzed analysis) {
        StringBuilder uml = new StringBuilder();

        // create a copy of classMetrics and add external classes (only for diagram)
        Map<String, ClassLevelMetrics> diagramMetrics = new HashMap<>(analysis.getClassMetrics());
        addExternalClasses(diagramMetrics);

        // header config
        uml.append("@startuml\n");
        uml.append("!pragma layout smetana\n");
        uml.append("hide empty members\n\n");

        // generate class declarations with stereotypes
        for (ClassLevelMetrics classMetric : diagramMetrics.values()) {
            uml.append(generateClassDeclaration(classMetric));
        }

        uml.append("\n");

        // generate all relationships
        for (ClassLevelMetrics classMetric : diagramMetrics.values()) {
            uml.append(generateRelationships(classMetric));
        }

        uml.append("\n@enduml");
        return uml.toString();
    }
    
    /**
     * adds placeholder ClassLevelMetrics for external classes (JPanel, etc.)
     * only used for diagram generation, not for metrics calculations
     * @param classMetrics existing metrics map (will be modified)
     */
    private static void addExternalClasses(Map<String, ClassLevelMetrics> classMetrics) {
        Set<String> externalClasses = new HashSet<>();
        Set<String> externalInterfaces = new HashSet<>();
        Set<String> externalExtends = new HashSet<>();

        // collect all referenced classes and track their source
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
                    external.setAbstract(true);
                } else if (externalExtends.contains(externalClass)) {
                    external.setClassType("class");
                    external.setAbstract(false);
                } else {
                    external.setClassType("class");
                    external.setAbstract(false);
                }
                
                classMetrics.put(externalClass, external);
            }
        }
    }

    /**
     * generates class declaration 
     * @param metric class metrics
     * @return PlantUML class declaration
     */
    private static String generateClassDeclaration(ClassLevelMetrics metric) {
        String className = metric.getClassName();
        String classType = metric.getClassType();

        if (metric.isSingleton()) {
            // singleton 
            return "class " + className + " << (S,#FF7700) singleton >> {\n}\n";
        } else if ("interface".equals(classType)) {
            // interface 
            return "interface " + className + " << (I,#87CEEB) >> {\n}\n";
        } else if ("abstract".equals(classType)) {
            // abstract class 
            return "abstract class " + className + " << (A,#FFD700) >> {\n}\n";
        } else {
            // concrete class 
            return "class " + className + " << (C,#90EE90) >> {\n}\n";
        }
    }

    /**
     * gets all class relationships
     * @param metric class metrics containing relationships
     * @return PlantUML relationship syntax
     */
    private static String generateRelationships(ClassLevelMetrics metric) {
        StringBuilder sb = new StringBuilder();
        String className = metric.getClassName();

        // generalization (extends)
        for (String parent : metric.getExtendsClasses()) {
            sb.append(className).append(" --|> ").append(parent).append("\n");
        }

        // realization (implements)
        for (String iface : metric.getImplementsInterfaces()) {
            sb.append(className).append(" ..|> ").append(iface).append("\n");
        }

        // composition (strong ownership)
        for (String composed : metric.getCompositions()) {
            sb.append(className).append(" *-- ").append(composed).append("\n");
        }

        // aggregation (weak ownership)
        for (String aggregated : metric.getAggregations()) {
            sb.append(className).append(" o-- ").append(aggregated).append("\n");
        }

        // association
        for (String associated : metric.getAssociations()) {
            sb.append(className).append(" -- ").append(associated).append("\n");
        }

        // dependency 
        for (String dependency : metric.getDependencies()) {
            if (!isStrongerRelationship(metric, dependency)) {
                sb.append(className).append(" ..> ").append(dependency).append("\n");
            }
        }

        // singleton self-reference
        if (metric.isSingleton()) {
            sb.append(className).append(" o-- ").append(className)
              .append(" : -instance\n");
        }

        return sb.toString();
    }

    /**
     * checks if a stronger relationship already exists
     * prevents showing dependency when extends/implements/etc already exists
     *
     * @param metric class metrics
     * @param otherClass target class
     * @return true if stronger relationship exists
     */
    private static boolean isStrongerRelationship(ClassLevelMetrics metric, String otherClass) {
        return metric.getExtendsClasses().contains(otherClass) ||
               metric.getImplementsInterfaces().contains(otherClass) ||
               metric.getCompositions().contains(otherClass) ||
               metric.getAggregations().contains(otherClass) ||
               metric.getAssociations().contains(otherClass);
    }
}

