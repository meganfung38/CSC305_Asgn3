package Asgn3;
import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

/**
 * loads environment variables (GH token)
 *
 * @author Megan Fung
 * @version 1.0
 */

public class EnvLoader {

    /**
     * loads all environment variables
     * @param path path to .env file
     */
    public static void loadEnv(String path) {

        try (Stream<String> lines = Files.lines(Paths.get(path))) {
            lines.forEach(line -> {
                if (!line.startsWith("#") && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    System.setProperty(parts[0].trim(), parts[1].trim());
                }
            });
        } catch (IOException e) {
            System.out.println("No .env file found or could not load it.");
        }

    }

}
