package Asgn3;

import java.util.regex.Pattern;

/**
 * detects if a class follows the singleton pattern
 *
 * @author Megan Fung
 * @version 1.0
 */
public class SingletonDetector {

    /**
     * checks if a class implements the singleton pattern
     * @param javaClass the class to check
     * @return true if singleton pattern detected
     */
    public static boolean isSingleton(JavaClass javaClass) {
        String body = javaClass.fullBody;

        // check for private static instance field
        boolean hasStaticInstance = (body.contains("private static") && body.contains(javaClass.name)) ||
                                   (body.contains("private static final") && body.contains(javaClass.name));

        // check for public static getInstance-like method
        boolean hasGetInstance = (body.contains("public static") && body.contains(javaClass.name)) &&
                                (body.contains("getInstance(") || body.contains("instance(") || body.contains("get("));

        return hasStaticInstance && hasGetInstance;
    }
}

