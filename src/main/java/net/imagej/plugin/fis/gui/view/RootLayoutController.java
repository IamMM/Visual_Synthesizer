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
package net.imagej.plugin.fis.gui.view;

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.imagej.plugin.fis.FunctionImageSynthesizer;
import org.scijava.Context;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author Hadrien Mary
 */
public class RootLayoutController implements Initializable, ImageListener{

    private final FunctionImageSynthesizer FIS = new FunctionImageSynthesizer();
    private Image default_preview;
    private boolean doNewImage = true;

    @FXML
    private TextField widthTextField, heightTextField, slicesTextField;

    @FXML
    private TextField minX, minY, minZ;

    @FXML
    private TextField maxX, maxY, maxZ;

    @FXML
    private ChoiceBox<String> imageChoiceBox, typeChoiceBox, fillChoiceBox;

    @FXML
    private ToggleButton xEqualY, yEqualZ;

    @FXML
    private ImageView preview;

    @FXML
    private CheckBox previewCheckBox, drawAxesCheckBox;

    @FXML
    private TextField functionTextArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        ImagePlus.addImageListener(this);

        initDefaultPreview();

        initImageList();

        // fill choice boxes
        String[] typeChoices = new String[]{"8-Bit", "16-Bit", "32-Bit", "RGB"};
        typeChoiceBox.getItems().addAll(typeChoices);
        typeChoiceBox.setValue("32-Bit");

        String[] fillChoices = new String[]{"Black", "White", "Random", "Ramp"};
        fillChoiceBox.getItems().addAll(fillChoices);
        fillChoiceBox.setValue("Black");

        // init change listener
        imageChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            doNewImage = newValue.equals("new image...");
            if(!doNewImage) {
                ImagePlus tmp = WindowManager.getImage(imageChoiceBox.getValue());
                typeChoiceBox.setValue(getTypeString(tmp.getType()));
                widthTextField.textProperty().setValue(""+tmp.getWidth());
                heightTextField.textProperty().setValue(""+tmp.getHeight());
                slicesTextField.textProperty().setValue(""+tmp.getNSlices());
            }
            typeChoiceBox.disableProperty().setValue(!doNewImage);
            fillChoiceBox.disableProperty().setValue(!doNewImage);
            widthTextField.disableProperty().setValue(!doNewImage);
            heightTextField.disableProperty().setValue(!doNewImage);
            slicesTextField.disableProperty().setValue(!doNewImage);
            updatePreview();
        });

        typeChoiceBox.valueProperty().addListener(observable -> updatePreview());
        fillChoiceBox.valueProperty().addListener(observable -> updatePreview());
        widthTextField.focusedProperty().addListener((observable -> updatePreview()));
        heightTextField.focusedProperty().addListener((observable -> updatePreview()));
        slicesTextField.focusedProperty().addListener(observable -> updatePreview());
        minX.focusedProperty().addListener(observable -> updatePreview());
        maxX.focusedProperty().addListener(observable -> updatePreview());
        maxY.focusedProperty().addListener(observable -> updatePreview());
        minZ.focusedProperty().addListener(observable -> updatePreview());
        maxZ.focusedProperty().addListener(observable -> updatePreview());

        xEqualY.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                xEqualY.textProperty().setValue("=");
                minY.textProperty().setValue(minX.textProperty().getValue());
                maxY.textProperty().setValue(maxX.textProperty().getValue());
                updatePreview();
            } else {
                xEqualY.textProperty().setValue("≠");
            }
        });

        yEqualZ.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                yEqualZ.textProperty().setValue("=");
                minZ.textProperty().setValue(minY.textProperty().getValue());
                maxZ.textProperty().setValue(maxY.textProperty().getValue());
                updatePreview();
            } else {
                yEqualZ.textProperty().setValue("≠");
            }
        });

        minX.textProperty().addListener((observable, oldValue, newValue) -> {
            if(xEqualY.isSelected()) {
                minY.textProperty().setValue(newValue);
            }
        });

        maxX.textProperty().addListener((observable, oldValue, newValue) -> {
            if(xEqualY.isSelected()) {
                maxY.textProperty().setValue(newValue);
            }
        });

        minY.textProperty().addListener((observable, oldValue, newValue) -> {
            if(xEqualY.isSelected()) {
                minX.textProperty().setValue(newValue);
            }

            if(yEqualZ.isSelected()) {
                minZ.textProperty().setValue(newValue);
            }
        });

        maxY.textProperty().addListener((observable, oldValue, newValue) -> {
            if(xEqualY.isSelected()) {
                maxY.textProperty().setValue(newValue);
            }

            if(yEqualZ.isSelected()) {
                maxZ.textProperty().setValue(newValue);
            }
        });

        minZ.textProperty().addListener((observable, oldValue, newValue) -> {
            if(yEqualZ.isSelected()) {
                minY.textProperty().setValue(newValue);
            }
        });

        maxZ.textProperty().addListener((observable, oldValue, newValue) -> {
            if(yEqualZ.isSelected()) {
                maxY.textProperty().setValue(newValue);
            }
        });

        previewCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            drawAxesCheckBox.setDisable(!newValue);
            if(newValue) {
                updatePreview();
            } else {
                showDefaultPreview();
            }
        });

        drawAxesCheckBox.selectedProperty().addListener(observable -> updatePreview());

        showDefaultPreview();
    }

    private String getTypeString(int type) {
        switch (type){
            case ImagePlus.GRAY8: return "8-Bit";
            case ImagePlus.GRAY16: return "16-Bit";
            case ImagePlus.GRAY32: return "32-Bit";
            default: return "RGB";

        }
    }

    private void initDefaultPreview() {
//        URL url = null;
//        try {
//            url = getClass().getResource("/preview.png");
//            BufferedImage img = ImageIO.read(url);
//            default_preview = SwingFXUtils.toFXImage(img, null);
//            preview.setImage(default_preview);
//        }catch (Exception e) {
//            e.printStackTrace();
//        }

        try {
            default_preview = new Image(new FileInputStream("preview.png"));
            preview.setImage(default_preview);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void showDefaultPreview() {
        preview.setImage(default_preview);
    }

    private void updatePreview() {

        if(!previewCheckBox.isSelected()){
            return;
        }

        // GUI
        // meta
        String type = typeChoiceBox.getValue() + " " + fillChoiceBox.getValue();

        // size
        int width = Integer.parseInt(widthTextField.getCharacters().toString().replaceAll("[^\\d.]", ""));
        int height = Integer.parseInt(heightTextField.getCharacters().toString().replaceAll("[^\\d.]", ""));
        int slices = Integer.parseInt(slicesTextField.getCharacters().toString().replaceAll("[^\\d.]", ""));

        // coordinate range
        double[] min = new double[3];
        double[] max = new double[3];


        String x_minFromGUI = minX.getText().replaceAll("[^-\\d.]", "");
        double x_min = x_minFromGUI.equals("")?0:Double.parseDouble(x_minFromGUI);
        String x_maxFromGUI = maxX.getText().replaceAll("[^-\\d.]", "");
        double x_max = x_maxFromGUI.equals("")?width>height?width-1:height-1:Double.parseDouble(x_maxFromGUI);
        min[0] = x_min;
        max[0] = x_max;

        String y_minFromGUI = minY.getText().replaceAll("[^-\\d.]", "");
        double y_min = y_minFromGUI.equals("")?0:Double.parseDouble(y_minFromGUI);
        String y_maxFromGUI = maxY.getText().replaceAll("[^-\\d.]", "");
        double y_max = y_maxFromGUI.equals("")?width>height?width-1:height-1:Double.parseDouble(y_maxFromGUI);
        min[1] = y_min;
        max[1] = y_max;

        String z_minFromGUI = minZ.getText().replaceAll("[^-\\d.]", "");
        double z_min = z_minFromGUI.equals("")?0:Double.parseDouble(z_minFromGUI);
        String z_maxFromGUI = maxZ.getText().replaceAll("[^-\\d.]", "");
        double z_max = z_maxFromGUI.equals("")?slices-1:Double.parseDouble(z_maxFromGUI);
        min[2] = z_min;
        max[2] = z_max;

        // function
        String function = functionTextArea.getText();

        ImagePlus imagePlus;
        if(doNewImage) {
            imagePlus = IJ.createImage(function, type, width, height, slices);
        } else {
            imagePlus = WindowManager.getImage(imageChoiceBox.getValue()).duplicate();
        }

        Image previewImage = FIS.getPreview(imagePlus, min, max, function, drawAxesCheckBox.isSelected());

        preview.setImage(previewImage);
    }

    @FXML
    private void handleButtonAction() {

        // GUI
        // meta
        String type = typeChoiceBox.getValue() + " " + fillChoiceBox.getValue();

        // size
        int width = Integer.parseInt(widthTextField.getCharacters().toString().replaceAll("[^\\d.]", ""));
        int height = Integer.parseInt(heightTextField.getCharacters().toString().replaceAll("[^\\d.]", ""));
        int slices = Integer.parseInt(slicesTextField.getCharacters().toString().replaceAll("[^\\d.]", ""));

        // coordinate range
        double[] min = new double[3];
        double[] max = new double[3];

        String x_minFromGUI = minX.getText().replaceAll("[^-\\d.]", "");
        double x_min = x_minFromGUI.equals("")?0:Double.parseDouble(x_minFromGUI);
        String x_maxFromGUI = maxX.getText().replaceAll("[^-\\d.]", "");
        double x_max = x_maxFromGUI.equals("")?width-1:Double.parseDouble(x_maxFromGUI);
        min[0] = x_min;
        max[0] = x_max;

        String y_minFromGUI = minY.getText().replaceAll("[^-\\d.]", "");
        double y_min = y_minFromGUI.equals("")?0:Double.parseDouble(y_minFromGUI);
        String y_maxFromGUI = maxY.getText().replaceAll("[^-\\d.]", "");
        double y_max = y_maxFromGUI.equals("")?height-1:Double.parseDouble(y_maxFromGUI);
        min[1] = y_min;
        max[1] = y_max;

        String z_minFromGUI = minZ.getText().replaceAll("[^-\\d.]", "");
        double z_min = z_minFromGUI.equals("")?0:Double.parseDouble(z_minFromGUI);
        String z_maxFromGUI = maxZ.getText().replaceAll("[^-\\d.]", "");
        double z_max = z_maxFromGUI.equals("")?slices-1:Double.parseDouble(z_maxFromGUI);
        min[2] = z_min;
        max[2] = z_max;

        // function
        String function = functionTextArea.getText();

        // apply
        ImagePlus imagePlus;
        if(doNewImage) {
            imagePlus = IJ.createImage(function, type, width, height, slices);
        } else {
            imagePlus = WindowManager.getImage(imageChoiceBox.getValue()).duplicate();
            imagePlus.setTitle(function);
        }
        FIS.functionToImage(imagePlus, min, max, function);
        IJ.resetMinAndMax(imagePlus);
        imagePlus.show();
    }

    public void setContext(Context context) {
        context.inject(this);
    }

    @FXML
    private void openMacroHelp() {
        URI uri = null;
        try {
            uri = new URI("https://imagej.nih.gov/ij/developer/macro/functions.html");
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private void initImageList() {
        String[] titles = WindowManager.getImageTitles();
        imageChoiceBox.getItems().add("new image...");
        imageChoiceBox.getItems().addAll(titles);
        imageChoiceBox.setValue("new image...");
    }

    @Override
    public void imageOpened(ImagePlus imp) {
        imageChoiceBox.getItems().add(imp.getTitle());
    }

    @Override
    public void imageClosed(ImagePlus imp) {
        // Avoid throwing IllegalStateException by running from a non-JavaFX thread.
        Platform.runLater(() -> {
            if (imp.getTitle().equals(imageChoiceBox.getValue())) {
                imageChoiceBox.valueProperty().setValue("new image...");
            }
            imageChoiceBox.getItems().remove(imp.getTitle());
        });
    }

    @Override
    public void imageUpdated(ImagePlus imp) {
        // TODO update imageChoiceBox when imp was renamed
        // Avoid throwing IllegalStateException by running from a non-JavaFX thread.
        Platform.runLater(() -> {
            if(imageChoiceBox.getValue().equals(imp.getTitle())) {
                System.out.println(imp.getTitle());
                typeChoiceBox.setValue(getTypeString(imp.getType()));
                widthTextField.textProperty().setValue(""+imp.getWidth());
                heightTextField.textProperty().setValue(""+imp.getHeight());
                slicesTextField.textProperty().setValue(""+imp.getNSlices());
                updatePreview();
            }
        });
    }
}
