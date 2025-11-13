package Asgn3;

/**
 * record object that represents a GH user
 * each record stores metadata about GH user for accessing repo contents
 * @param owner fields
 *
 * @author Megan Fung
 * @version 1.0
 */

public record GHInfo(String owner, String repo, String ref) {
}
