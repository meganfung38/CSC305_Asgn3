package Asgn3;
import javax.swing.*;

/**
 * options:
 * file --> open from URL and exit
 * action --> reload and clear
 * help --> about
 *
 * @author Megan Fung
 * @version 1.0
 */

public class MenuBar extends JMenuBar {

    /**
     * constructor
     * @param mainFrame reference to main frame
     */
    public MenuBar(MainFrame mainFrame) {

        // file
        JMenu file = new JMenu("File");
        JMenuItem openURL = new JMenuItem("Open from URL...");
        openURL.addActionListener(e -> mainFrame.onOkClicked(null));
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> System.exit(0));
        file.add(openURL);
        file.add(exit);

        // action
        JMenu action = new JMenu("Action");
        JMenuItem reload = new JMenuItem("Reload");
        reload.addActionListener(e -> mainFrame.onOkClicked(null));
        JMenuItem clear = new JMenuItem("Clear");
        clear.addActionListener(e -> mainFrame.clearGrid());
        action.add(reload);
        action.add(clear);

        // help
        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(
                e ->
                        JOptionPane.showMessageDialog(mainFrame,
                                "Assignment 02\n By Megan Fung \n",
                                "About",
                                JOptionPane.INFORMATION_MESSAGE)
        );
        help.add(about);

        // add components
        add(file);
        add(action);
        add(help);

    }

}
