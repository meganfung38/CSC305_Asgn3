package Asgn3;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

/**
 * displays project directory structure
 * uses JTree to visualize folder hierarchy
 */
public class SidePanel extends JPanel {

    // components
    private final DefaultTreeModel treeModel;
    private final JTree tree;

    /**
     * constructor
     */
    public SidePanel() {

        // config
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(220,600));

        // initialize components
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("src");  // default root label is src
        treeModel = new DefaultTreeModel(root); // manages all files in project directory (represented as nodes)
        tree = new JTree(treeModel);  // JTree to manage full project structure

        // add components to panel
        add(new JScrollPane(tree), BorderLayout.CENTER);

    }

    /**
     * updates panel with tree structure from GH repo link
     * @param filePaths all existing files within GH repo
     */
    public void showStructure(java.util.List<String> filePaths) {

        // create new root
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Project");

        // iterate through file paths and add to tree
        for (String path : filePaths) {

            // get subfolders
            String[] folders = path.split("/");

            DefaultMutableTreeNode current = root; // current folder
            for (int i = 0; i < folders.length; i++) {
                String folder = folders[i];
                if (folder.isBlank()) { continue; }  // skip empty

                boolean isFile = (i == folders.length - 1);  // no more subfolders
                if (isFile) {
                    DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(folder);
                    current.add(fileNode);
                } else {
                    current = getCreateChild(current, folder);
                }
            }
        }

        treeModel.setRoot(root);  // update root directory
        tree.setModel(treeModel);  // update tree with file paths
        tree.expandRow(0);  // expand for file visibility

    }

    /**
     * either gets an existing folder node or creates one
     * @param current current subfolder
     * @param folder folder to get/ create
     * @return returns a folder node for target
     */
    private DefaultMutableTreeNode getCreateChild(DefaultMutableTreeNode current, String folder) {

        // iterate over subfolders from current
        for(int i = 0; i < current.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) current.getChildAt(i);
            if (child.getUserObject().equals(folder)) {  // found match
                return child;  // return existing folder node
            }
        }

        // create folder node
        DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(folder);
        current.add(folderNode);
        return folderNode;

    }

}
