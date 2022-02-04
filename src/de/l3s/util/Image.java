package de.l3s.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.twelvemonkeys.image.ResampleOp;

/**
 * This is a utility class for performing basic functions on an image,
 * such as retrieving, resizing, cropping, and saving.
 *
 * @author James H.
 * @version 1.0
 */
public class Image {
    private static final Logger log = LogManager.getLogger(Image.class);

    private final BufferedImage img;

    /**
     * Load image from InputStream.
     */
    public Image(InputStream input) throws IOException {
        img = ImageIO.read(input);
        input.close();

        if (img == null) {
            throw new IllegalArgumentException("Can't create image from this stream");
        }
    }

    /**
     * Constructor for taking a BufferedImage.
     */
    public Image(BufferedImage img) {
        this.img = img;
    }

    public Image(java.awt.Image image) {
        this.img = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(image, null, null);
    }

    /**
     * @return Width of the image in pixels
     */
    public int getWidth() {
        return img.getWidth();
    }

    /**
     * @return Height of the image in pixels
     */
    public int getHeight() {
        return img.getHeight();
    }

    /**
     * @return Aspect ratio of the image (width / height)
     */
    public double getAspectRatio() {
        return (double) getWidth() / (double) getHeight();
    }

    /**
     * Generate a new Image object resized to a specific width, maintaining the same aspect ratio of the original.
     *
     * @return Image scaled to new width
     */
    public Image getResizedToWidth(int width) {
        if (width > getWidth()) {
            //throw new IllegalArgumentException("Width " + width + " exceeds width of image, which is " + getWidth());
            log.warn("Width {} exceeds width of image, which is {}", width, getWidth());
            return this;
        }
        int newHeight = width * img.getHeight() / img.getWidth();

        BufferedImageOp resampler = new ResampleOp(width, newHeight, ResampleOp.FILTER_LANCZOS); // A good default filter, see class documentation for more info
        BufferedImage resizedImage = resampler.filter(img, null);
        return new Image(resizedImage);
    }

    public Image getResized(int maxWidth, int maxHeight, boolean croppedToAspectRatio) {
        if (croppedToAspectRatio) {
            return getCroppedAndResized(maxWidth, maxHeight);
        } else {
            return getResized(maxWidth, maxHeight);
        }
    }

    public Image getResized(int maxWidth, int maxHeight) {
        if (maxWidth > getWidth() && maxHeight > getHeight()) {
            return new Image(img);
        }

        int newHeight = getHeight();
        int newWidth = getWidth();

        if (newWidth > maxWidth) {
            double ratio = (double) maxWidth / (double) newWidth;
            newHeight = (int) Math.round(newHeight * ratio);
            newWidth = maxWidth;
        }
        if (newHeight > maxHeight) {
            double ratio = (double) maxHeight / (double) newHeight;
            newWidth = (int) Math.round(newWidth * ratio);
            newHeight = maxHeight;
        }

        BufferedImageOp resampler = new ResampleOp(newWidth, newHeight, ResampleOp.FILTER_TRIANGLE);
        BufferedImage resizedImage = resampler.filter(img, null);
        return new Image(resizedImage);
    }

    /**
     * First crops the image to the aspect ratio given by the parameters height and width and then resize it.
     */
    public Image getCroppedAndResized(int maxWidth, int maxHeight) {
        if (maxWidth > getWidth() && maxHeight > getHeight()) {
            return new Image(img);
        }

        double ratio = (double) maxWidth / (double) maxHeight;
        int newWidth = getWidth();
        int newHeight = (int) Math.round(newWidth / ratio);

        if (newHeight > getHeight()) { // the website is not very long
            newHeight = getHeight();
        }

        int cutOff = (getWidth() - newWidth) / 2;
        return crop(cutOff, 0, newWidth + cutOff, newHeight).getResized(maxWidth, maxHeight);
    }

    /**
     * Generate a new Image object cropped to a new size.
     *
     * @param x1 Starting x-axis position for crop area
     * @param y1 Starting y-axis position for crop area
     * @param x2 Ending x-axis position for crop area
     * @param y2 Ending y-axis position for crop area
     * @return Image cropped to new dimensions
     */
    public Image crop(int x1, int y1, int x2, int y2) {
        if (x1 < 0 || x2 <= x1 || y1 < 0 || y2 <= y1 || x2 > getWidth() || y2 > getHeight()) {
            throw new IllegalArgumentException("invalid crop coordinates");
        }

        int type = img.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : img.getType();
        int nNewWidth = x2 - x1;
        int nNewHeight = y2 - y1;
        BufferedImage cropped = new BufferedImage(nNewWidth, nNewHeight, type);

        Graphics2D g = cropped.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setComposite(AlphaComposite.Src);
        g.drawImage(img, 0, 0, nNewWidth, nNewHeight, x1, y1, x2, y2, null);
        g.dispose();

        return new Image(cropped);
    }

    /**
     * Useful function to crop and resize an image to a square.
     * This is handy for thumbnail generation.
     *
     * @param width Width of the resulting square
     * @param cropEdgesPct Specifies how much of an edge all around the square to crop,
     * which creates a zoom-in effect in the center of the resulting square. This may
     * be useful, given that when images are reduced to thumbnails, the detail of the
     * focus of the image is reduced. Specifying a value such as 0.1 may help preserve
     * this detail. You should experiment with it. The value must be between 0 and 0.5
     * (representing 0% to 50%)
     * @return Image cropped and resized to a square; returns the same image if image is smaller than width parameter
     */
    public Image getResizedToSquare2(int width, double cropEdgesPct) {
        if (cropEdgesPct < 0 || cropEdgesPct > 0.5) {
            throw new IllegalArgumentException("Crop edges pct must be between 0 and 0.5. " + cropEdgesPct + " was supplied.");
        }
        if (width > getWidth()) {
            return new Image(img);
        }

        //crop to square first. determine the coordinates.
        int cropMargin = (int) Math.abs(Math.round(((img.getWidth() - img.getHeight()) / 2.0)));
        int x1 = 0;
        int y1 = 0;
        int x2 = getWidth();
        int y2 = getHeight();
        if (getWidth() > getHeight()) {
            x1 = cropMargin;
            x2 = x1 + y2;
        } else {
            y1 = cropMargin;
            y2 = y1 + x2;
        }

        //should there be any edge cropping?
        if (cropEdgesPct != 0) {
            int cropEdgeAmt = (int) ((x2 - x1) * cropEdgesPct);
            x1 += cropEdgeAmt;
            x2 -= cropEdgeAmt;
            y1 += cropEdgeAmt;
            y2 -= cropEdgeAmt;
        }

        // generate the image cropped to a square
        Image cropped = crop(x1, y1, x2, y2);

        // now resize. we do crop first then resize to preserve detail
        Image resized = cropped.getResizedToWidth(width);
        cropped.dispose();

        return resized;
    }

    /**
     * Write image to a file, specify image type.
     * This method will overwrite a file that exists with the same name.
     *
     * @param file File to write image to
     * @param type jpg, gif, etc.
     */
    public void writeToFile(File file, String type) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File argument was null");
        }
        ImageIO.write(img, type, file);
    }

    /**
     * Write image to a stream, specify image type.
     *
     * @param type jpg, gif, etc.
     */
    public void writeToFile(OutputStream os, String type) throws IOException {
        ImageIO.write(img, type, os);
        os.close();
    }

    /**
     * Streams the image to the InputStream.
     */
    public InputStream getInputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(img, "png", os);
        os.close();
        return new ByteArrayInputStream(os.toByteArray());
    }

    /**
     * Free up resources associated with this image.
     */
    public void dispose() {
        img.flush();
    }

    /**
     * Used mostly for testing purposes.
     */
    public static Image blank(int width, int height) {
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphic = bufferedImage.createGraphics();
        graphic.setPaint(Color.lightGray);
        graphic.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
        graphic.dispose();

        return new Image(bufferedImage);
    }

    /**
     * Used for creating Watermarks in Glossary.
     */
    public static Image fromText(final String text) {
        // create the font you wish to use
        Font font = new Font("Tahoma", Font.PLAIN, 18);

        // create the FontRenderContext object which helps us to measure the text
        FontRenderContext frc = new FontRenderContext(null, true, true);

        // get the height and width of the text
        Rectangle2D bounds = font.getStringBounds(text, frc);
        int width = (int) bounds.getWidth();
        int height = (int) bounds.getHeight();

        // create a BufferedImage object
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // calling createGraphics() to get the Graphics2D
        Graphics2D graphic = bufferedImage.createGraphics();
        graphic.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        graphic.fillRect(0, 0, width, height);
        graphic.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        Color textColor = new Color(0, 0, 0, 0.5f);
        graphic.setColor(textColor);
        graphic.setFont(font);
        graphic.drawString(text, (float) bounds.getX(), (float) -bounds.getY());

        // releasing resources
        graphic.dispose();

        return new Image(bufferedImage);
    }
}
