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
 */
package net.imagej.plugin.fis;

import ij.*;
import ij.process.ImageProcessor;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import net.imagej.ImageJ;
import net.imagej.plugin.fis.gui.MainAppFrame;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>Function Image Synthesizer")
public class FunctionImageSynthesizer implements Command {

    // Constants
    private static final int MAX_PREVIEW_SIZE = 128 ;

    @Parameter
    private static ImageJ ij;

    @Parameter
    private LogService log;

    private static final String PLUGIN_NAME = "Function Image Synthesizer";
    private static final String VERSION = version();

    private static String version() {
        String version = null;
        final Package pack = FunctionImageSynthesizer.class.getPackage();
        if (pack != null) {
            version = pack.getImplementationVersion();
        }
        return version == null ? "0.1" : version;
    }

    @Override
    public void run() {
        // Launch JavaFX interface
        MainAppFrame app = new MainAppFrame(ij);
        app.setTitle(PLUGIN_NAME + " " + VERSION);
        app.init();
    }

    public static void main(final String... args) throws Exception {
        // Launch ImageJ as usual.
        ij = net.imagej.Main.launch(args);

        // Launch the command.
        ij.command().run(FunctionImageSynthesizer.class, true);
    }

    public void functionOne(String title, String type, int width, int height, int slices, double min, double max, String function) {

        ImagePlus imagePlus = IJ.createImage(title, type, width, height, slices);

        ImageProcessor processor = imagePlus.getProcessor();

        double range = Math.abs(min - max);

        String currFunc;

        for(int z_=0; z_<slices; z_++) {
            processor.setSliceNumber(z_);
            double z = (double) z_ / slices; // 0..1
            double dz = range * z + min; // min..max

            for(int y_=0; y_<height; y_++) {

                double y = (double) y_ / height; // 0..1
                double dy = range * y + min; // min..max

                for (int x_ = 0; x_ < width; x_++) {

                    double x = (double) x_ / width; // 0..1
                    double dx = range * x + min; //min..max

                    currFunc = function.replace("x", "" +   dx).replace("y", "" + dy);

                    String val = IJ.runMacro("return '' + " + currFunc + ";");
                    double val_ = Double.parseDouble(val);

//                    double dist = Math.sin(dx) * Math.sin(dx) + Math.sin(dy) * Math.sin(dy);

                    processor.putPixelValue(x_, y_, val_);
                }
            }
        }

        imagePlus.show();
    }

    public Image getPreview(String type, int width, int height, double min, double max, String function) {

        if(width>MAX_PREVIEW_SIZE) {
            height = height*MAX_PREVIEW_SIZE/width;
            width = MAX_PREVIEW_SIZE;
        }

        if(height>MAX_PREVIEW_SIZE) {
            width = width*MAX_PREVIEW_SIZE/height;
            height = MAX_PREVIEW_SIZE;
        }

        int[] preview = new int[width*height];

        double range = Math.abs(min - max);

        String currFunc;

        for(int y_=0; y_<height; y_++) {

            double y = (double) y_/height; // 0..1
            double dy = range*y+min; // min..max

            for(int x_=0;x_<width; x_++) {
                int pos = y_ * width + x_;

                double x = (double) x_/width; // 0..1
                double dx = range * x+min; //min..max

                currFunc = function.replace("x", "" +   dx).replace("y", "" + dy);

                String val = IJ.runMacro("return '' + " + currFunc + ";");
                double val_ = Double.parseDouble(val);

                //double dist = Math.sin(dx)*Math.sin(dx) + Math.sin(dy)*Math.sin(dy);
                // normalize
                int value = (int) (val_/2*255);

                preview[pos] = 0xFF000000 | (value<<16) | (value<<8) | value ;

                if(dx == 0 || dy == 0) preview[pos] = 0xFFFF0000;
            }

        }

        return pixelToImage(preview, width, height);
    }

    private Image pixelToImage(int[] pixels, int width, int height) {
        WritableImage wr = new WritableImage(width, height);
        PixelWriter pw = wr.getPixelWriter();
        pw.setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        return wr;
    }
}
