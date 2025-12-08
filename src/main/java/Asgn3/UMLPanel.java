package Asgn3;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * panel that renders PlantUML diagrams as images
 *
 * @author Megan Fung
 * @version 1.0
 */
public class UMLPanel extends JPanel {

    // fields
    private BufferedImage image;

    /**
     * constructor
     * @param umlSource PlantUML syntax string
     */
    public UMLPanel(String umlSource) {
        setBackground(Color.WHITE);

        try {
            // convert PlantUML syntax to png 
            SourceStringReader reader = new SourceStringReader(umlSource);
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            // create png
            reader.outputImage(os, new FileFormatOption(FileFormat.PNG));
            os.close();

            // make it displayable
            image = ImageIO.read(new ByteArrayInputStream(os.toByteArray()));

        } catch (IOException e) {
            e.printStackTrace();
            image = null;
        }
    }

    /**
     * paints the UML diagram
     * @param g graphics context
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (image != null) {
            g.drawImage(image, 0, 0, this);
        } else {
            // show error message if image failed to load
            g.setColor(Color.RED);
            g.drawString("failed to generate diagram", 10, 20);
        }
    }

    /**
     * returns preferred size based on image dimensions
     * @return dimension of the image
     */
    @Override
    public Dimension getPreferredSize() {
        if (image != null) {
            return new Dimension(image.getWidth(), image.getHeight());
        }
        return super.getPreferredSize();
    }
}

