package Asgn3;
import javax.swing.*;
import java.awt.*;

/**
 * immutable text area that displays name of currently selected file
 */

public class BottomPanel extends JPanel {

    // components
    private final JTextField selectedFileGrid;

    /**
     * constructor
     */
    public  BottomPanel() {

        // config
        setLayout(new BorderLayout());
        JLabel label = new JLabel("Selected File Name: ");

        // initialize components
        selectedFileGrid = new JTextField();
        selectedFileGrid.setEditable(false); // static (non editable)

        // add to panel
        add(label, BorderLayout.WEST);
        add(selectedFileGrid, BorderLayout.CENTER);

    }

    /**
     * updates displayed file name
     * @param file name of selected file
     */
    public void setSelectedFileGrid(String file) {

        // write to text field
        selectedFileGrid.setText(file);

    }

}
