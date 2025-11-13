package Asgn3;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * displays 2d diagram of analyzed class level metrics for classes (represented as circles) in GH repo
 * Metrics: abstraction, instability, and distance
 * x: instability (0 = stable, 1 = unstable)
 * y: abstraction (0 = concrete, 1 = abstract)
 * painful + useless regions
 * diagonal balance line (I = 1 - A)
 *
 * @author Megan Fung
 * @version 1.0
 */
public class MetricsPanel extends JPanel {

    // components
    private List<ClassLevelMetrics> classes;
    private List<Rectangle> circleClasses;
    private final BottomPanel bottomPanel;

    // logger
    private static final Logger logger = LoggerFactory.getLogger(MetricsPanel.class);

    /**
     * constructor
     */
    public MetricsPanel(BottomPanel bottomPanel) {

        // config
        setBackground(Color.white);
        setToolTipText("");  // enable tooltip

        // initialize components
        this.bottomPanel = bottomPanel;

        // enable mouse clicking events
        MouseAdapter mouseAdapter = new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                logger.info("Class Circle Clicked");
                circleClicked(e.getPoint());
            }

            @Override
            public void mouseMoved(MouseEvent e) { circleHover(e.getPoint()); }

        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);

    }

    /**
     * updates panel with class level metrics
     * @param classMetricsList list of ClassLevelMetrics objects (one for each class in GH folder)
     */
    public void showMetrics(List<ClassLevelMetrics> classMetricsList) {

        // initialize file objects
        this.classes = classMetricsList;

        // update panel
        repaint();

    }

    /**
     * draws 2d diagram
     * @param g the <code>Graphics</code> object to protect
     */
    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);  // clear old drawings

        // check if there are no files to display
        if (classes == null || classes.isEmpty()) {
            return;
        }

        // config
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // get panel dimensions
        int width = getWidth();
        int height = getHeight();

        // padding config
        int pad = 60;

        // plot dimensions
        int plotWidth = width - 2 * pad;
        int plotHeight = height - 2 * pad;

        // apply background color (light green)
        g2d.setColor(new Color(215, 235, 225));
        g2d.fillRect(pad, pad, plotWidth, plotHeight);

        // painful + useless bubbles
        g2d.setColor(Color.white);
        g2d.fillOval(pad - 220 / 2, height - pad - 220 / 2, 220, 220);
        g2d.setColor(Color.black);
        g2d.drawString("Painful", pad + 10, height - pad - 10);
        g2d.setColor(Color.white);
        g2d.fillOval(width - pad - 220 / 2, pad - 220 / 2, 220, 220);
        g2d.setColor(Color.black);
        g2d.drawString("Useless", width - pad - 80, pad + 20);

        // draw x, y, diagonal balance line
        g2d.drawLine(pad, height - pad, width - pad, height - pad);
        g2d.drawLine(pad, height - pad, pad, pad);
        g2d.drawLine(pad, pad, width - pad, height - pad);

        // store circle clickable regions
        circleClasses = new java.util.ArrayList<>();

        int radius = 12; // circle radius

        // iterate over class metrics
        for (ClassLevelMetrics classMetric : classes) {

            // scale instability (x) and abstraction (y)
            int x = pad + (int)(plotWidth * classMetric.getI());
            int y = height - pad - (int)(plotHeight * classMetric.getA());

            // save class's clickable region
            Rectangle circleClass = new Rectangle(x - radius, y - radius, radius * 2, radius * 2);
            circleClasses.add(circleClass);

            // draw circle per class
            g2d.setColor(Color.blue);
            g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            g2d.setColor(Color.darkGray);
            g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2d.drawString(classMetric.getClassName(), x + radius, y + 4);

        }

        g2d.dispose();

    }

    /**
     * updates tooltip with class level metrics
     * @param event the {@code MouseEvent} that initiated the
     *              {@code ToolTip} display
     * @return metrics for specific class (represented as circle) currently being hovered over
     */
    @Override
    public String getToolTipText(MouseEvent event) {

        // check if there is no files to display metrics for
        if (classes == null || classes.isEmpty()) {
            return null;
        }

        // get panel dimensions
        int width = getWidth();
        int height = getHeight();

        // padding config
        int pad = 60;

        // plot dimensions
        int plotWidth = width - 2 * pad;
        int plotHeight = height - 2 * pad;

        // iterate through files and construct tooltip text displaying file metrics
        for (ClassLevelMetrics currClass : classes) {

            // get position
            int x = pad + (int)(plotWidth * currClass.getI());
            int y = height - pad - (int)(plotHeight * currClass.getA());

            // calculate spatial positioning for tooltip detection
            Rectangle r = new Rectangle(x - 9, y - 9, 18, 18);

            // check if current hover is over a file's spatial positioning
            if (r.contains(event.getPoint())) {

                // return metrics for file
                return String.format("<html><b>%s</b><br>A=%.2f<br>I=%.2f<br>D=%.2f<br>Ca=%d Ce=%d</html>",
                        currClass.getClassName(), currClass.getA(), currClass.getI(), currClass.getD(), currClass.getCa(), currClass.getCe());

            }

        }

        return null;  // mouse is not currently over a file

    }

    /**
     * helper function to trigger hover tooltip
     * @param p point clicked
     */
    private void circleHover(Point p) {

        // trigger tooltip
        setToolTipText(getToolTipText(new MouseEvent(this, 0, 0, 0, p.x, p.y, 0, false)));

    }

    /**
     * helper function to trigger bottom panel updates
     * @param p point clicked
     */
    private void circleClicked(Point p) {

        // check if there are no classes
        if (classes == null || circleClasses == null) { return; }

        // store class names
        List<String> clickedClasses = new java.util.ArrayList<>();

        // iterate over all circle regions
        for (int i = 0; i < circleClasses.size(); i++) {

            // point clicked is within region of a circle
            Rectangle curr = circleClasses.get(i);
            if (curr.contains(p)) { clickedClasses.add(classes.get(i).getClassName()); }

        }

        // update bottom panel with class name(s)
        if (!clickedClasses.isEmpty()) {
            bottomPanel.setMessage(String.join(", ", clickedClasses));
            repaint();
        }

    }

}
