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

        try {
            // generate PlantUML syntax
            String umlSource = PlantUMLGenerator.generateUML(analysis);

            // create panel with rendered diagram
            umlPanel = new UMLPanel(umlSource);
            
            // check if rendering succeeded
            if (!umlPanel.isImageLoaded()) {
                showErrorMessage("Diagram rendering failed.");
                return;
            }

            // add with scrollbars
            scrollPane = new JScrollPane(umlPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            add(scrollPane, BorderLayout.CENTER);

            // refresh display
            revalidate();
            repaint();
            
        } catch (StackOverflowError e) {
            showErrorMessage("Stack overflow during diagram generation.\nThe codebase may be too complex.");
        } catch (Exception e) {
            showErrorMessage("Error: " + e.getMessage());
        }
    }
    
    /**
     * displays an error message
     * @param message error message
     */
    private void showErrorMessage(String message) {
        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.setBackground(Color.WHITE);
        
        JLabel errorLabel = new JLabel("<html><div style='padding:20px'>" + message.replace("\n", "<br>") + "</div></html>");
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        errorPanel.add(errorLabel, BorderLayout.CENTER);
        add(errorPanel, BorderLayout.CENTER);
        
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
