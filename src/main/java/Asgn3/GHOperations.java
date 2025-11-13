package Asgn3;
import java.io.IOException;
import java.util.List;
import javiergs.tulip.GitHubHandler;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * helper for accessing GH folders
 *
 * @author Megan Fung
 * @version 1.0
 */

public class GHOperations {

    // components
    private final GitHubHandler handler;

    /**
     * constructor
     * @param token personal GH token
     */
    public GHOperations(String token) {

        // initialize components
        this.handler = new GitHubHandler(token);

    }

    /**
     * recursively list all files present from a given GH folder URL
     * @param url GH folder URL
     * @return all files as relative paths
     * @throws IOException if GH API call fails
     */
    public List<String> listFilesRecursive(String url) throws IOException {

        // list all files in subfolders
        return handler.listFilesRecursive(url);

    }

    /**
     * parses GH folder URL to extract owner, repo, and ref
     * @param url GH folder URL
     * @return an object containing owner, repo, and ref
     */
    public static GHInfo parseGHURL(String url) {

        try {

            URI uri = new URI(url); // get uri
            String[] repo_routes = uri.getPath().split("/");

            // validate GH URL
            if (repo_routes.length < 5 || !"tree".equals(repo_routes[3])) {
                throw new IllegalArgumentException("Bad GH URL: " + url);
            }
            // example:
            // /<owner>/<repo>/tree/<ref>/....
            return new GHInfo(repo_routes[1], repo_routes[2], repo_routes[4]);


        } catch (URISyntaxException e) {

            throw new IllegalArgumentException("Bad GH URL: " + url, e);

        }

    }

    /**
     * retrieves all file contents
     * @param owner GH owner that owns file
     * @param repo GH repo the file belongs to
     * @param path GH file path
     * @param ref GH branch file is on
     * @return String representation of file contents
     * @throws IOException if GH API call fails
     */
    public String getFileContent(String owner, String repo, String path, String ref) throws IOException {

        // return file content
        return handler.getFileContent(owner, repo, path, ref);

    }

}
