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
package net.imagej.plugin.visualFunctionSynthesizer;

import ij.*;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import net.imagej.plugin.visualFunctionSynthesizer.gui.MainAppFrame;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>VisualFunctionSynthesizer")
public class VisualSynthesizer implements Command {

    @Parameter
    private ImageJ ij;

    @Parameter
    private LogService log;

    public static final String PLUGIN_NAME = "VisualFunctionSynthesizer";
    public static final String VERSION = version();

    private static String version() {
        String version = null;
        final Package pack = VisualSynthesizer.class.getPackage();
        if (pack != null) {
            version = pack.getImplementationVersion();
        }
        return version == null ? "DEVELOPMENT" : version;
    }

    @Override
    public void run() {
        // Launch JavaFX interface
        MainAppFrame app = new MainAppFrame(ij);
        app.setTitle(PLUGIN_NAME + " version " + VERSION);
        app.init();
        
    }

    public static void main(final String... args) throws Exception {
        // Launch ImageJ as usual.
        final ImageJ ij = net.imagej.Main.launch(args);

        // Launch the command.
        ij.command().run(VisualSynthesizer.class, true);
    }

    public void functionOne(String title, String type, int width, int height, int slices) {


        // Tyoe: "32-bit White"
        ImagePlus imagePlus = IJ.createImage(title, "32-bit Black", width, height, slices);

        int w = imagePlus.getWidth();
        int h = imagePlus.getHeight();


        ImageProcessor processor = imagePlus.getProcessor();

        for(int y_=0; y_<h; y_++) {

                double y = (double) y_/h;
                double dy = 10*(y - 0.5);

                for(int x_=0;x_<w; x_++) {

                    double x = (double) x_/w;
                    double dx = 10*(x - 0.5);

                    double dist = Math.sin(dx)*Math.sin(dx) + Math.sin(dy)*Math.sin(dy);

                    processor.putPixelValue(x_,y_, dist);
                }

        }

        imagePlus.show();
    }
}
