/*
 * The MIT License
 *
 * Copyright 2016 Fiji.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author: Maximilian Maske
 */
package net.imagej.plugin.fis;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.macro.Interpreter;
import ij.macro.Program;
import ij.macro.Tokenizer;
import ij.plugin.filter.ImageMath;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.awt.*;

public class FunctionImageSynthesizer extends ImageMath {

    // Constants
    private static final int MAX_PREVIEW_SIZE = 128 ;

    /*--- Macro to Image ---*/

    public void functionToImage(ImagePlus imagePlus, double[] min, double[] max, String function) {
        // example macro: "code=v=v+50*sin(d/10)"
        String macro = "code=v=" + function;
        applyMacro(imagePlus, macro, min, max);
    }

    private void applyMacro(ImagePlus imagePlus, String macro, double[]min, double[]max) {

        ImageProcessor ip = imagePlus.getProcessor();

        double[] range = new double[min.length];
        for (int i = 0; i < range.length; i++) {
            range[i] = Math.abs(min[i] - max[i]);
        }

        int PCStart = 23;
        Program pgm = (new Tokenizer()).tokenize(macro);
        boolean hasX = pgm.hasWord("x");
        boolean hasZ = pgm.hasWord("z");
        boolean hasA = pgm.hasWord("a");
        boolean hasD = pgm.hasWord("d");
        boolean hasGetPixel = pgm.hasWord("getPixel");
        int w = ip.getWidth();
        int h = ip.getHeight();
        int w2 = w/2;
        int h2 = h/2;
        String code =
                "var v,x,y,z,w,h,d,a;\n"+
                        "function dummy() {}\n"+
                        macro+";\n"; // code starts at program counter location 'PCStart'
        Interpreter interpreter = new Interpreter();
        interpreter.run(code, null);
        if (interpreter.wasError()) return;

        Prefs.set(MACRO_KEY, macro);
        interpreter.setVariable("w", w);
        interpreter.setVariable("h", h);
        interpreter.setVariable("z", ip.getSliceNumber()-1);
        int bitDepth = ip.getBitDepth();
        Rectangle r = ip.getRoi();
        int inc = r.height/50;
        if (inc<1) inc = 1;
        int slices = imagePlus.getNSlices();
        double v;
        int pos, v2;
        if (bitDepth==8) { // 8-Bit
            byte[] pixels1 = (byte[])ip.getPixels();
            byte[] pixels2 = pixels1;
            if (hasGetPixel)
                pixels2 = new byte[w*h];
            for (int y=r.y; y<(r.y+r.height); y++) {
                if (y%inc==0) IJ.showProgress(y-r.y, r.height);

                double y_ = (double) y / h; // 0..1
                double dy = range[1] * y_ + min[1]; // min..max
                interpreter.setVariable("y", dy);

                for (int x=r.x; x<(r.x+r.width); x++) {
                    pos = y * w + x;
                    v = pixels1[pos]&255;
                    interpreter.setVariable("v", v);

                    double x_ = (double) x / w; // 0..1
                    double dx = range[1] * x_ + min[1]; // min..max
                    if (hasX) interpreter.setVariable("x", dx);

                    if (hasA) interpreter.setVariable("a", getA((h-y-1)-h2, x-w2));
                    if (hasD) interpreter.setVariable("d", getD(x-w2,y-h2));
                    interpreter.run(PCStart);
                    v2 = (int)interpreter.getVariable("v");
                    if (v2<0) v2 = 0;
                    if (v2>255) v2 = 255;
                    pixels2[pos] = (byte)v2;
                }
            }
            if (hasGetPixel) System.arraycopy(pixels2, 0, pixels1, 0, w*h);
        } else if (bitDepth==24) { // RGB
            int rgb, red, green, blue;
            int[] pixels1 = (int[])ip.getPixels();
            int[] pixels2 = pixels1;
            if (hasGetPixel)
                pixels2 = new int[w*h];
            for (int y=r.y; y<(r.y+r.height); y++) {
                if (y%inc==0)
                    IJ.showProgress(y-r.y, r.height);
                interpreter.setVariable("y", y);
                for (int x=r.x; x<(r.x+r.width); x++) {
                    if (hasX) interpreter.setVariable("x", x);
                    if (hasA) interpreter.setVariable("a", getA((h-y-1)-h2, x-w2));
                    if (hasD) interpreter.setVariable("d", getD(x-w2,y-h2));
                    pos = y*w+x;
                    rgb = pixels1[pos];
                    if (hasGetPixel) {
                        interpreter.setVariable("v", rgb);
                        interpreter.run(PCStart);
                        rgb = (int)interpreter.getVariable("v");
                    } else {
                        red = (rgb&0xff0000)>>16;
                        green = (rgb&0xff00)>>8;
                        blue = rgb&0xff;
                        interpreter.setVariable("v", red);
                        interpreter.run(PCStart);
                        red = (int)interpreter.getVariable("v");
                        if (red<0) red=0; if (red>255) red=255;
                        interpreter.setVariable("v", green);
                        interpreter.run(PCStart);
                        green= (int)interpreter.getVariable("v");
                        if (green<0) green=0; if (green>255) green=255;
                        interpreter.setVariable("v", blue);
                        interpreter.run(PCStart);
                        blue = (int)interpreter.getVariable("v");
                        if (blue<0) blue=0; if (blue>255) blue=255;
                        rgb = 0xff000000 | ((red&0xff)<<16) | ((green&0xff)<<8) | blue&0xff;
                    }
                    pixels2[pos] = rgb;
                }
            }
            if (hasGetPixel) System.arraycopy(pixels2, 0, pixels1, 0, w*h);
        } else if (ip.isSigned16Bit()) { //
            for (int y=r.y; y<(r.y+r.height); y++) {
                if (y%inc==0)
                    IJ.showProgress(y-r.y, r.height);
                interpreter.setVariable("y", y);
                for (int x=r.x; x<(r.x+r.width); x++) {
                    v = ip.getPixelValue(x, y);
                    interpreter.setVariable("v", v);
                    if (hasX) interpreter.setVariable("x", x);
                    if (hasA) interpreter.setVariable("a", getA((h-y-1)-h2, x-w2));
                    if (hasD) interpreter.setVariable("d", getD(x-w2,y-h2));
                    interpreter.run(PCStart);
                    ip.putPixelValue(x, y, interpreter.getVariable("v"));
                }
            }
        } else if (bitDepth==16) {
            short[] pixels1 = (short[])ip.getPixels();
            short[] pixels2 = pixels1;
            if (hasGetPixel)
                pixels2 = new short[w*h];
            for (int y=r.y; y<(r.y+r.height); y++) {
                if (y%inc==0)
                    IJ.showProgress(y-r.y, r.height);
                interpreter.setVariable("y", y);
                for (int x=r.x; x<(r.x+r.width); x++) {
                    pos = y*w+x;
                    v = pixels1[pos]&65535;
                    interpreter.setVariable("v", v);
                    if (hasX) interpreter.setVariable("x", x);
                    if (hasA) interpreter.setVariable("a", getA((h-y-1)-h2, x-w2));
                    if (hasD) interpreter.setVariable("d", getD(x-w2,y-h2));
                    interpreter.run(PCStart);
                    v2 = (int)interpreter.getVariable("v");
                    if (v2<0) v2 = 0;
                    if (v2>65535) v2 = 65535;
                    pixels2[pos] = (short)v2;
                }
            }
            if (hasGetPixel) System.arraycopy(pixels2, 0, pixels1, 0, w*h);
        } else {  //32-bit
            for(int z_=0; z_<slices; z_++) {
                ip = imagePlus.getImageStack().getProcessor(z_ + 1);
                double z = (double) z_ / slices; // 0..1
                double dz = range[2] * z + min[2]; // min..max
                if(hasZ) interpreter.setVariable("z", dz);

                float[] pixels1 = (float[])ip.getPixels();
                float[] pixels2 = pixels1;
                if (hasGetPixel)
                    pixels2 = new float[w*h];
                for (int y = r.y; y < (r.y + r.height); y++) {
                    if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

                    double y_ = (double) y / h; // 0..1
                    double dy = range[1] * y_ + min[1]; // min..max
                    interpreter.setVariable("y", dy);

                    for (int x = r.x; x < (r.x + r.width); x++) {
                        pos = y * w + x;
                        v = pixels1[pos];
                        interpreter.setVariable("v", v);

                        double x_ = (double) x / w; // 0..1
                        double dx = range[1] * x_ + min[1]; // min..max
                        if (hasX) interpreter.setVariable("x", dx);

                        if (hasA) interpreter.setVariable("a", getA((h - y - 1) - h2, x - w2));
                        if (hasD) interpreter.setVariable("d", getD(x - w2, y - h2));
                        interpreter.run(PCStart);
                        pixels2[pos] = (float) interpreter.getVariable("v");
                    }
                }
                if (hasGetPixel) System.arraycopy(pixels2, 0, pixels1, 0, w*h);
            }
        }
        IJ.showProgress(1.0);
    }

    private double getD(int dx, int dy) {
        return Math.hypot(dx,dy);
    }

    private double getA(int y, int x) {
        double angle = Math.atan2(y, x);
        if (angle<0) angle += 2*Math.PI;
        return angle;
    }

    private int[] normalize(double[] src) {
        int[] norm = new int[src.length];

        double min = src[0], max = src[0];
        for (double v : src) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }

        double range = Math.abs(min-max);

        for (int pos = 0; pos < src.length; pos++) {
            int n = (int) ((src[pos]/range - min) * 255);
            norm[pos] = 0xFF000000 | (n&0xFF<<16) | (n&0xFF<<8) | n&0xFF;
        }

        return norm;
    }

    /*--- PREVIEW ---*/

    public Image getPreview(ImagePlus imagePlus, double[] min, double[] max, String function) {
        int width = imagePlus.getWidth();
        int height = imagePlus.getHeight();

        if(width>MAX_PREVIEW_SIZE) {
            height = height*MAX_PREVIEW_SIZE/width;
            width = MAX_PREVIEW_SIZE;
        }

        if(height>MAX_PREVIEW_SIZE) {
            width = width*MAX_PREVIEW_SIZE/height;
            height = MAX_PREVIEW_SIZE;
        }
        ImageProcessor ip = imagePlus.getProcessor();
        ip.setInterpolate(true);
        ImageProcessor resized = ip.resize(width, height);


        ImagePlus preview = new ImagePlus();
        preview.setProcessor(resized);
        String macro = "code=v=" + function;
        applyMacro(preview, macro, min, max);

        return imagePlusToJavaFxImage(preview);

    }

    private Image imagePlusToJavaFxImage (ImagePlus imagePlus) {
        ImageProcessor ip = imagePlus.getProcessor();
        int w = ip.getWidth();
        int h = ip.getHeight();
        int bitDepth = ip.getBitDepth();
        ip.resetMinAndMax();
        int[] values = new int[ip.getPixelCount()];
        if(bitDepth==8)  {
            byte[] pixels = (byte[])ip.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                int value = pixels[i]&0xFF;
                values[i] = 0xFF000000 | (value<<16) | (value<<8) | value;
            }
        } else if(bitDepth==16 || bitDepth==32){
            ByteProcessor byteProcessor = ip.convertToByteProcessor(true);
            byte[] pixels = (byte[])byteProcessor.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                int value = pixels[i]&0xFF;
                values[i] = 0xFF000000 | (value<<16) | (value<<8) | value;
            }
        } else if(bitDepth==24) {
            values = (int[])ip.getPixels();
        }
        
        WritableImage wr = new WritableImage(w, h);
        PixelWriter pw = wr.getPixelWriter();
        pw.setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), values, 0, w);
        return wr;
    }
}
