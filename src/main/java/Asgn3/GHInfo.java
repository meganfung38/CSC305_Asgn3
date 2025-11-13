package Asgn3;

/**
 * record object that represents a GH user
 * each record stores metadata about GH user for accessing repo contents
 * @param owner fields
 */

public record GHInfo(String owner, String repo, String ref) {
}
