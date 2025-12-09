package Asgn3;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * combines all components
 *
 * @author Megan Fung
 * @version 1.0
 */

public class MainFrame extends JFrame {

    // components
    private final GHOperations ghOperations;
    private final TopBar topBar;
    private final BottomPanel bottomPanel;
    private final SidePanel sidePanel;
    private final GridPanel gridPanel;
    private final MetricsPanel metricsPanel;
    private final DiagramPanel diagramPanel;
    private final JTabbedPane tabbedPane;

    // logger
    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);

    /**
     * constructor
     */
    public MainFrame() {

        logger.info("Initializing MainFrame: configuration, environment setup, components setup...");

        // config
        setTitle("Megan's Final Project");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 1000);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        // initialize environment
        EnvLoader.loadEnv(".env");
        String token = System.getProperty("GH_TOKEN");
        ghOperations = new GHOperations(token);

        // create menu bar
        setJMenuBar(new MenuBar(this));

        // initialize components
        topBar = new TopBar(this::onOkClicked);
        bottomPanel = new BottomPanel();
        sidePanel = new SidePanel();
        gridPanel = new GridPanel(bottomPanel);
        metricsPanel = new MetricsPanel(bottomPanel);
        diagramPanel = new DiagramPanel();

        // create tabs for grid, metrics, and diagram
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Grid", gridPanel);
        tabbedPane.addTab("Metrics", metricsPanel);
        tabbedPane.addTab("Diagram", diagramPanel);

        // add to frame
        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(sidePanel), BorderLayout.WEST);
        add(tabbedPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);  // render

    }

    /**
     * triggers file analysis + renders grid of squares
     * @param actionEvent triggering event
     */
    public void onOkClicked(ActionEvent actionEvent) {

        logger.info("Ok Button Clicked: opening GH URL...");

        String url = topBar.getUrl().trim();  // get url

        logger.info("Validating GH URL: looking for form: https://github.com/<GH USER>/<REPO NAME>/tree/<BRANCH>/<folder(s)...>");

        // validate URL
        if (!url.startsWith("https://github.com/") || !url.contains("/tree/")) {

            // update center panel
            JOptionPane.showMessageDialog(this, "Invalid GH folder URL.\nMust be in form: https://github.com/<GH USER>/<REPO NAME>/tree/<BRANCH>/<folder(s)...>");
            return;
        }

        logger.info("Analyzing GH URL: calculating file level and class level metrics for grid and metrics panel...");

        try {

            // analyze files in GH URL
            GHRepoAnalyzer GHRepoAnalyzer = new GHRepoAnalyzer(ghOperations);
            GHRepoAnalyzed analysis = GHRepoAnalyzer.analyzeFiles(url);

            if (analysis.getFileMetrics().isEmpty()) {
                JOptionPane.showMessageDialog(this, "No .java files found in GH folder");
                gridPanel.clearGrid();
                metricsPanel.showMetrics(List.of());
                diagramPanel.clear();
                return;
            }

            logger.info("Analysis Complete: updating panels...");

            // update panels
            sidePanel.showStructure(analysis.getFilePaths());
            gridPanel.showMetrics(new ArrayList<>(analysis.getFileMetrics().values()));
            metricsPanel.showMetrics(new ArrayList<>(analysis.getClassMetrics().values()));
            diagramPanel.showDiagram(analysis);
            tabbedPane.setSelectedIndex(0);

        } catch (Exception e)  {

            JOptionPane.showMessageDialog(this, e.getMessage());
            gridPanel.clearGrid();
            metricsPanel.showMetrics(List.of());
            diagramPanel.clear();

        }

    }

    /**
     * clears grid and resets top bar
     */
    public void clearGrid() {

        gridPanel.clearGrid(); // refresh center panel for new GH URL
        metricsPanel.showMetrics(List.of());  // refresh center panel for new GH URL
        diagramPanel.clear();  // refresh diagram panel for new GH URL
        sidePanel.clear();  // refresh side panel for new GH URL
        topBar.resetUrl();  // reset top bar for new GH URL

        bottomPanel.setMessage("CLEARED: Ready for new GH URL.");

    }

}
