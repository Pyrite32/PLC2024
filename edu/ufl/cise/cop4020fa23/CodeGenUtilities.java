package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;
import edu.ufl.cise.cop4020fa23.runtime.ImageOps;
import edu.ufl.cise.cop4020fa23.runtime.PixelOps;

import java.awt.Color;
import java.awt.image.BufferedImage;

public final class CodeGenUtilities {
    
    public static void helloWorld()
    {
        ConsoleIO.write("Hello, World!");
    }

    public static int addPixels(int packedA, int packedB) {
        Color a = new Color(packedA);
        Color b = new Color(packedB);
        return PixelOps.pack(a.getRed() + b.getRed(),
                             a.getGreen() + b.getGreen(),
                             a.getBlue() + b.getBlue());
    }

    public static int asPixel(int pixel) {
        return PixelOps.pack(pixel, pixel, pixel);
    }

    public static BufferedImage setAllPixelsAndGive(BufferedImage i, int color)
    {
        BufferedImage result = ImageOps.cloneImage(i);
        ImageOps.setAllPixels(result, color);
        return result;
    }

    public static int widthOf(BufferedImage i) {
        int width = i.getWidth();
        return width;
    }

    public static int heightOf(BufferedImage i) {
        int height = i.getHeight();
        return height;
    }

}
