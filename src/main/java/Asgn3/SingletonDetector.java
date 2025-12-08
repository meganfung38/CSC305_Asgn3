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

        boolean hasStaticInstance = Pattern.compile(
                "(?s)\\bprivate\\s+static\\s+(?:final\\s+)?" +
                Pattern.quote(javaClass.name) + "\\s+\\w+"
        ).matcher(body).find();

        boolean hasGetInstance = Pattern.compile(
                "(?s)\\bpublic\\s+static\\s+(?:final\\s+)?" +
                Pattern.quote(javaClass.name) + "\\s+(?:getInstance|instance|get)\\s*\\("
        ).matcher(body).find();

        return hasStaticInstance && hasGetInstance;
    }
}

