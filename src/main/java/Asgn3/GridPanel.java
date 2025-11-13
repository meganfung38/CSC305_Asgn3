package Asgn3;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * grid of squares visualizing file level metrics for .java files in GH folder URL
 * each square in grid represents a single .java file
 * color --> code complexity
 * transparency --> file size relative to the largest file
 *
 * @author Megan Fung
 * @version 1.0
 */

public class GridPanel extends JPanel {

    // components
    private List<FileLevelMetrics> files;
    private Rectangle[] squares;
    private int clickedSquare = -1;
    private final BottomPanel bottomPanel;

    // logger
    private static final Logger logger = LoggerFactory.getLogger(GridPanel.class);

    /**
     * constructor
     * @param bottomPanel reference so we can update with selected file
     */
    public GridPanel(BottomPanel bottomPanel) {

        // config
        setBackground(Color.white);
        setToolTipText("");

        // initialize components
        this.bottomPanel = bottomPanel;

        // enable mouse clicking events
        MouseAdapter mouseAdapter = new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {  // trigger for square selection

                logger.info("Grid Panel Clicked");
                squareClicked(e.getPoint());

            }

            @Override
            public void mouseMoved (MouseEvent e) {  // trigger for square hover
                squareHover(e.getPoint());
            }

        };

        // add to panel
        addMouseMotionListener(mouseAdapter);
        addMouseListener(mouseAdapter);
    }

    /**
     * clears grid of squares
     */
    public void clearGrid() {

        // clear all visualized data on grid and stored metrics
        files = null;
        squares = null;
        clickedSquare = -1;
        repaint();
        bottomPanel.setMessage("");

    }

    /**
     * updates files stored for grid of squares
     * @param fileMetricsList list of FileLevelMetrics objects (one for each file in GH folder)
     */
    public void showMetrics(List<FileLevelMetrics> fileMetricsList) {

        clearGrid();  // clear all visualized data on grid and stored metrics
        this.files = fileMetricsList;  // store analyzed files
        this.squares = (files == null) ? null : new Rectangle[files.size()];  // initialize squares

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);  // clear old drawings

        // no .java files
        if (files == null || files.isEmpty()) { return; }

        // config
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int totalFiles = files.size();  // number of files in GH URL

        // get square dimensions
        int cols = (int) Math.ceil(Math.sqrt(totalFiles));
        int rows = (int) Math.ceil(totalFiles / (double) cols);

        // config for grid of squares
        int padding = 12;
        int w = getWidth() - padding * 2;
        int h = getHeight() - padding * 2;
        int gridWidth = Math.max(24, w / cols);
        int gridHeight = Math.max(24, h / rows);
        int size = Math.min(gridWidth, gridHeight);

        // create squares for grid
        if (squares == null || squares.length != totalFiles) {
            squares = new Rectangle[totalFiles];  // square per file
        }

        // determine alpha value (max number of lines across all files)
        int alpha = files.stream().mapToInt(FileLevelMetrics::getSize).max().orElse(1);

        // drawing config
        int x = padding;  // current position in grid
        int y = padding;  // current position in grid

        for (int i = 0; i < totalFiles; i++) {

            // get current file
            FileLevelMetrics file = files.get(i);

            // calculate square transparency and color
            float ratio = (float) file.getSize() / alpha;  // relative size compared to the largest file
            float transparency = 0.25f + 0.75f * (float) Math.pow(ratio, 0.5);  // transparency control
            Color color = colorForComplexity(file.getComplexity());  // color in respect to file complexity
            Color fill = getFill(color, transparency);  // final square fill config

            // apply square fill
            g2d.setColor(fill);
            g2d.fillRect(x, y, size, size);

            // border config for clicked square
            g2d.setColor(i == clickedSquare ? Color.black : Color.gray);  // black border if current square is selected (clicked)
            g2d.setStroke(new BasicStroke(i == clickedSquare ? 3f : 1f));  // boldness of border based off selection
            g2d.drawRect(x, y, size, size);

            // save square to stored squares
            squares[i] = new Rectangle(x, y, size, size);

            // go to next square (horizontal then wrap to next row)
            x += size;
            if ((i + 1) % cols == 0) {
                x = padding;
                y += size;
            }

        }

        g2d.dispose();

    }

    /**
     * helper function to determine color of square using complexity
     * @param complexity number of control statements
     * @return color to use for square
     */
    private Color colorForComplexity(int complexity) {

        if (complexity > 10) {
            return Color.red;
        } else if (complexity > 5) {
            return Color.yellow;
        } else {
            return Color.green;
        }

    }

    /**
     * helper function to create final fill for squares
     * @param color color based off complexity
     * @param transparency transparency based off size
     * @return final fill adjusted by color and transparency
     */
    private Color getFill(Color color, float transparency) {

        // calculate transparency value
        int transparencyVal = Math.max(0, Math.min(255, (int) (transparency * 255)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), transparencyVal);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText(MouseEvent e) {

        // no .java files
        if (files == null || files.isEmpty() || squares == null) { return null; }

        // iterate through squares in grid
        for (int i = 0; i < squares.length; i++) {

            // current square
            Rectangle square = squares[i];

            if (square != null && square.contains(e.getPoint())) {

                FileLevelMetrics file = files.get(i);  // current file

                // render file name, size, and complexity in tooltip
                return "<html><b>" + file.getName() + "</b><br/>lines: " + file.getSize() +
                        "<br/>complexity: " + file.getComplexity() + "</html>";

            }

        }

        return null;

    }

    /**
     * helper function to trigger tooltip when user hovers over a square
     * @param point coordinates of mouse
     */
    private void squareHover(Point point) {

        // trigger tooltip
        setToolTipText(getToolTipText(new MouseEvent(this, 0, 0, 0, point.x, point.y, 0, false)));

    }

    /**
     * helper function to trigger bottom panel update when user clicks on a square
     * @param point coordinates of mouse
     */
    private void squareClicked(Point point) {

        // no .java files
        if (files == null || squares == null) { return; }

        // iterate through squares in grid
        for (int i = 0; i < squares.length; i++) {

            if (squares[i] != null && squares[i].contains(point)) {

                clickedSquare = i;  // update square selection
                bottomPanel.setMessage(files.get(i).getName());  // update bottom panel
                repaint();
                break;

            }
        }

    }

}
