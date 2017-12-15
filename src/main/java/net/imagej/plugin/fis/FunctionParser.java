package net.imagej.plugin.fis;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.macro.Program;
import ij.macro.Tokenizer;
import ij.plugin.filter.ImageMath;
import ij.process.ImageProcessor;

import java.awt.*;

class FunctionParser extends ImageMath{
    public static void applyMacro(ImagePlus imagePlus, String macro, double[]min, double[]max) {

        ImageProcessor ip = imagePlus.getProcessor();

        double[] range = new double[min.length];
        for (int i = 0; i < range.length; i++) {
            range[i] = Math.abs(min[i] - max[i]);
        }

        int PCStart = 23;
        Program pgm = (new Tokenizer()).tokenize(macro);
        boolean hasX = pgm.hasWord("x");
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
        if (bitDepth==8) {
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
        } else if (bitDepth==24) {
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
        } else if (ip.isSigned16Bit()) {
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

                float[] pixels1 = (float[])ip.getPixels();
                float[] pixels2 = pixels1;
                if (hasGetPixel)
                    pixels2 = new float[w*h];

                double z = (double) z_ / slices; // 0..1
                double dz = range[2] * z + min[2]; // min..max
                interpreter.setVariable("z", dz);
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

    private static double getD(int dx, int dy) {
        return Math.hypot(dx,dy);
    }

    private static double getA(int y, int x) {
        double angle = Math.atan2(y, x);
        if (angle<0) angle += 2*Math.PI;
        return angle;
    }

    private static int[] normalize(double[] src) {
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
}
