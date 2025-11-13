package Asgn3;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * text field input for GH folder URLs
 *
 * @author Megan Fung
 * @version 1.0
 */

public class TopBar extends JPanel {

    // components
    private final JTextField urlInput;

    /**
     * constructor
     * @param actionListener action triggered
     */
    public TopBar(ActionListener actionListener) {

        // config
        setLayout(new BorderLayout(5, 5));

        // initialize components
        urlInput = new JTextField("Insert GitHub Folder URL");
        JButton okButton = new JButton("Ok");
        okButton.addActionListener(actionListener);

        // add to panel
        add(urlInput, BorderLayout.CENTER);
        add(okButton, BorderLayout.EAST);

    }

    /**
     * getter
     * @return url input
     */
    public String getUrl() {
        return urlInput.getText().trim();
    }

    /**
     * resets text input
     */
    public  void resetUrl() {
        urlInput.setText("Insert GitHub Folder URL");
    }

}
