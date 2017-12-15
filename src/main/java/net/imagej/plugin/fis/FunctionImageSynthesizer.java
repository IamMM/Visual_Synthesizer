package net.imagej.plugin.fis;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class FunctionImageSynthesizer {

    // Constants
    private static final int MAX_PREVIEW_SIZE = 128 ;

    public void functionToImage(String title, String type, int width, int height, int slices, double[] min, double[] max, String function) {

        ImagePlus imagePlus = IJ.createImage(title, type, width, height, slices);

        ImageProcessor processor;

        double[] range = new double[min.length];
        for (int i = 0; i < range.length; i++) {
            range[i] = Math.abs(min[i] - max[i]);
        }

        String currFunc;

        for(int z_=0; z_<slices; z_++) {
            processor = imagePlus.getImageStack().getProcessor(z_+1);
            System.out.println(imagePlus.getCurrentSlice());
            double z = (double) z_ / slices; // 0..1
            double dz = range[2] * z + min[2]; // min..max

            for(int y_=0; y_<height; y_++) {

                double y = (double) y_ / height; // 0..1
                double dy = range[1] * y + min[1]; // min..max

                for (int x_ = 0; x_ < width; x_++) {

                    double x = (double) x_ / width; // 0..1
                    double dx = range[0] * x + min[0]; //min..max

                    currFunc = function.replace("x", "" +   dx).replace("y", "" + dy).replace("z", "" + dz);

                    String val = IJ.runMacro("return '' + " + currFunc + ";");
                    double val_ = Double.parseDouble(val);

                    processor.putPixelValue(x_, y_, val_);
                }
            }
        }

        imagePlus.show();
    }

    public void functionGrayToImage(ImagePlus imagePlus, double[] min, double[] max, String function) {
        // example macro: "code=v=v+50*sin(d/10)"
        String macro = "code=v=" + function;
        FunctionParser.applyMacro(imagePlus, macro, min, max);

        imagePlus.show();
    }

    public Image getPreview(String type, int width, int height, double[] min, double[] max, String function) {

        if(width>MAX_PREVIEW_SIZE) {
            width = MAX_PREVIEW_SIZE;
            height = height*MAX_PREVIEW_SIZE/width;
        }

        if(height>MAX_PREVIEW_SIZE) {
            width = width*MAX_PREVIEW_SIZE/height;
            height = MAX_PREVIEW_SIZE;
        }

        int[] preview = new int[width*height];

        double[] range = new double[min.length];
        for (int i = 0; i < range.length; i++) {
            range[i] = Math.abs(min[i] - max[i]);
        }

        String currFunc;

        for(int y_=0; y_<height; y_++) {

            double y = (double) y_/height; // 0..1
            double dy = range[1] * y + min[1]; // min..max

            for(int x_=0;x_<width; x_++) {
                int pos = y_ * width + x_;

                double x = (double) x_/width; // 0..1
                double dx = range[0] * x + min[0]; //min..max

                currFunc = function.replace("x", "" +   dx).replace("y", "" + dy).replace("z", "" + "0");

                String val = IJ.runMacro("return '' + " + currFunc + ";");
                double val_ = Double.parseDouble(val);

                // normalize
                int value = (int) (val_/3*255);

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
