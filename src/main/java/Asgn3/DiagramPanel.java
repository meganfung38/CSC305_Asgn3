package Asgn3;
import javax.swing.*;
import java.awt.*;

/**
 * displays UML class diagrams generated from GitHub repository analysis
 *
 * @author Megan Fung
 * @version 1.0
 */
public class DiagramPanel extends JPanel {

    private JScrollPane scrollPane;
    private UMLPanel umlPanel;

    /**
     * constructor
     */
    public DiagramPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
    }

    /**
     * generates and displays UML diagram from analysis results
     * @param analysis the analyzed repository data
     */
    public void showDiagram(GHRepoAnalyzed analysis) {
        // clear existing diagram
        clear();

        // generate PlantUML syntax
        String umlSource = PlantUMLGenerator.generateUML(analysis);

        // create panel with rendered diagram
        umlPanel = new UMLPanel(umlSource);

        // add with scrollbars
        scrollPane = new JScrollPane(umlPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);

        // refresh display
        revalidate();
        repaint();
    }

    /**
     * clears the diagram panel
     */
    public void clear() {
        removeAll();
        umlPanel = null;
        scrollPane = null;
        revalidate();
        repaint();
    }
}
