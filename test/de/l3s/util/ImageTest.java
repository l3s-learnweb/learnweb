package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ImageTest {
    private static final Image baseImg = Image.blank(200, 250);

    @Test
    void getWidth() {
        assertEquals(200, baseImg.getWidth());
    }

    @Test
    void getHeight() {
        assertEquals(250, baseImg.getHeight());
    }

    @Test
    void getAspectRatio() {
        assertEquals(0.8, baseImg.getAspectRatio());
    }

    @Test
    void getResizedToWidth() {
        Image resized = baseImg.getResizedToWidth(100);
        assertEquals(100, resized.getWidth());
        assertEquals(125, resized.getHeight());
    }

    @Test
    void getCroppedAndResized() {
        Image resized = baseImg.getCroppedAndResized(100, 100);
        assertEquals(100, resized.getWidth());
        assertEquals(100, resized.getHeight());
    }

    @Test
    void getResized() {
        Image resized = baseImg.getResized(100, 125);
        assertEquals(100, resized.getWidth());
        assertEquals(125, resized.getHeight());

        Image resized2 = baseImg.getResized(100, 110);
        assertEquals(88, resized2.getWidth());
        assertEquals(110, resized2.getHeight());

        Image resized3 = baseImg.getResized(110, 150);
        assertEquals(110, resized3.getWidth());
        assertEquals(138, resized3.getHeight());
    }

    @Test
    void crop() {
        Image resized = baseImg.crop(100, 100, 200, 200);
        assertEquals(100, resized.getWidth());
        assertEquals(100, resized.getHeight());
    }
}