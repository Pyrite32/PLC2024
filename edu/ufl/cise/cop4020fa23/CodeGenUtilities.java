package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;
import edu.ufl.cise.cop4020fa23.runtime.PixelOps;

import java.awt.Color;

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

}
