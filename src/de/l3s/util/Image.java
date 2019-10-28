package de.l3s.util;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import org.apache.log4j.Logger;

import com.twelvemonkeys.image.ResampleOp;

//import com.mortennobel.imagescaling.AdvancedResizeOp;
//import com.mortennobel.imagescaling.MultiStepRescaleOp;

/**
 * This is a utility class for performing basic functions on an image,
 * such as retrieving, resizing, cropping, and saving.
 *
 * @version 1.0
 * @author James H.
 */
public class Image
{
    private static final Logger log = Logger.getLogger(Image.class);

    BufferedImage img;

    /**
     * Load image from InputStream
     *
     * @param input
     * @throws IOException
     */
    public Image(InputStream input) throws IOException
    {
        img = ImageIO.read(input);
        input.close();

        if(img == null)
            throw new IllegalArgumentException("Can't create image from this stream");
    }

    /**
     * Constructor for taking a BufferedImage
     *
     * @param img
     */
    public Image(BufferedImage img)
    {
        this.img = img;
    }

    public Image(java.awt.Image image)
    {
        this.img = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(image, null, null);
    }

    /**
     * @return Width of the image in pixels
     */
    public int getWidth()
    {
        return img.getWidth();
    }

    /**
     * @return Height of the image in pixels
     */
    public int getHeight()
    {
        return img.getHeight();
    }

    /**
     * @return Aspect ratio of the image (width / height)
     */
    public double getAspectRatio()
    {
        return (double) getWidth() / (double) getHeight();
    }

    /**
     * Generate a new Image object resized to a specific width, maintaining
     * the same aspect ratio of the original
     *
     * @param width
     * @return Image scaled to new width
     */
    public Image getResizedToWidth(int width)
    {
        if(width > getWidth())
        {
            //throw new IllegalArgumentException("Width " + width + " exceeds width of image, which is " + getWidth());
            log.warn("Width " + width + " exceeds width of image, which is " + getWidth());
            return this;
        }
        int newHeight = width * img.getHeight() / img.getWidth();

        BufferedImageOp resampler = new ResampleOp(width, newHeight, ResampleOp.FILTER_LANCZOS); // A good default filter, see class documentation for more info
        BufferedImage resizedImage = resampler.filter(img, null);

        /*
        MultiStepRescaleOp rescale = new MultiStepRescaleOp(width, nHeight);
        rescale.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Soft);
        BufferedImage resizedImage = rescale.filter(img, null);
        */

        return new Image(resizedImage);
    }

    /**
     * First crops the image to the aspect ratio given by the parameters height and width and then resize it
     *
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    public Image getCroppedAndResized(int maxWidth, int maxHeight)
    {
        if(maxWidth > getWidth() && maxHeight > getHeight())
            return new Image(img);

        double ratio = (double) maxWidth / (double) maxHeight;

        int newWidth = Math.min(getWidth(), 1050);
        int newHeight = (int) Math.round(newWidth / ratio);

        if(newHeight > getHeight()) // the website is not very long
        {
            newHeight = getHeight();
        }

        int cutOff = (getWidth() - newWidth) / 2;

        return crop(cutOff, 0, newWidth + cutOff, newHeight).getResized(maxWidth, maxHeight);
    }

    public Image getResized(int maxWidth, int maxHeight, boolean croppedToAspectRatio)
    {
        if(croppedToAspectRatio)
            return getCroppedAndResized(maxWidth, maxHeight);
        else
            return getResized(maxWidth, maxHeight);
    }

    public Image getResized(int maxWidth, int maxHeight)
    {
        if(maxWidth > getWidth() && maxHeight > getHeight())
            return new Image(img);

        int newHeight = getHeight(), newWidth = getWidth();

        if(newWidth > maxWidth)
        {
            double ratio = (double) maxWidth / (double) newWidth;
            newHeight = (int) Math.round(newHeight * ratio);
            newWidth = maxWidth;
        }
        if(newHeight > maxHeight)
        {
            double ratio = (double) maxHeight / (double) newHeight;
            newWidth = (int) Math.round(newWidth * ratio);
            newHeight = maxHeight;
        }

        /*
        MultiStepRescaleOp rescale = new MultiStepRescaleOp(newWidth, newHeight);
        rescale.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Soft);
        BufferedImage resizedImage = rescale.filter(img, null);
        */
        BufferedImageOp resampler = new ResampleOp(newWidth, newHeight, ResampleOp.FILTER_LANCZOS); // A good default filter, see class documentation for more info
        BufferedImage resizedImage = resampler.filter(img, null);

        return new Image(resizedImage);
    }

    public Image getResized(int maxWidth, int maxHeight, int cropPixels)
    {
        int x1 = 0;
        int y1 = 0;
        int x2 = getWidth();
        int y2 = getHeight();

        int cropPixelsX = cropPixels;
        int cropPixelsY = cropPixels;

        if(getWidth() > getHeight())
            cropPixelsX = cropPixels * 2;
        else
            cropPixelsY = cropPixels * 2;

        //should there be any edge cropping?
        if(cropPixels != 0)
        {
            x1 += cropPixelsX;
            x2 -= cropPixelsX;
            y1 += cropPixelsY;
            y2 -= cropPixelsY;
        }

        // generate the image cropped to a square
        Image cropped = crop(x1, y1, x2, y2);

        // now resize. we do crop first then resize to preserve detail
        Image resized = cropped.getResized(maxWidth, maxHeight);
        cropped.dispose();

        return resized;
    }

    /**
     * Generate a new Image object cropped to a new size
     *
     * @param x1 Starting x-axis position for crop area
     * @param y1 Starting y-axis position for crop area
     * @param x2 Ending x-axis position for crop area
     * @param y2 Ending y-axis position for crop area
     * @return Image cropped to new dimensions
     */
    public Image crop(int x1, int y1, int x2, int y2)
    {
        if(x1 < 0 || x2 <= x1 || y1 < 0 || y2 <= y1 || x2 > getWidth() || y2 > getHeight())
            throw new IllegalArgumentException("invalid crop coordinates");

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
     *            which creates a zoom-in effect on the center of the resulting square. This may
     *            be useful, given that when images are reduced to thumbnails, the detail of the
     *            focus of the image is reduced. Specifying a value such as 0.1 may help preserve
     *            this detail. You should experiment with it. The value must be between 0 and 0.5
     *            (representing 0% to 50%)
     * @return Image cropped and resized to a square; returns the same image if image is smaller than width parameter
     */
    public Image getResizedToSquare2(int width, double cropEdgesPct)
    {
        if(cropEdgesPct < 0 || cropEdgesPct > 0.5)
            throw new IllegalArgumentException("Crop edges pct must be between 0 and 0.5. " + cropEdgesPct + " was supplied.");
        if(width > getWidth())
            return new Image(img);

        //crop to square first. determine the coordinates.
        int cropMargin = (int) Math.abs(Math.round(((img.getWidth() - img.getHeight()) / 2.0)));
        int x1 = 0;
        int y1 = 0;
        int x2 = getWidth();
        int y2 = getHeight();
        if(getWidth() > getHeight())
        {
            x1 = cropMargin;
            x2 = x1 + y2;
        }
        else
        {
            y1 = cropMargin;
            y2 = y1 + x2;
        }

        //should there be any edge cropping?
        if(cropEdgesPct != 0)
        {
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
     * Write image to a file, specify image type
     * This method will overwrite a file that exists with the same name
     *
     * @param file File to write image to
     * @param type jpg, gif, etc.
     * @throws IOException
     */
    public void writeToFile(File file, String type) throws IOException
    {
        if(file == null)
            throw new IllegalArgumentException("File argument was null");
        ImageIO.write(img, type, file);
    }

    /**
     * Write image to a stream, specify image type
     *
     * @param os
     * @param type jpg, gif, etc.
     * @throws IOException
     */
    public void writeToFile(OutputStream os, String type) throws IOException
    {
        ImageIO.write(img, type, os);
        os.close();
    }

    public InputStream getInputStream() throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(img, "png", os);
        os.close();
        return new ByteArrayInputStream(os.toByteArray());
    }

    /**
     * Free up resources associated with this image
     */
    public void dispose()
    {
        img.flush();
    }

    public static void main(String[] args) throws FileNotFoundException, IOException
    {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("SVG");
        while(readers.hasNext())
        {
            log.debug("reader: " + readers.next());
        }

        BufferedImage image = ImageIO.read(new FileInputStream("D:\\Learnweb\\test_pattern.webp"));
        if(null == image)
            System.err.println("mist");
    }
}
