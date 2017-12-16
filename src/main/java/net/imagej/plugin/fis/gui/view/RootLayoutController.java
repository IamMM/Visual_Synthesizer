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
import ij.ImagePlus;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
public class RootLayoutController implements Initializable{

    private FunctionImageSynthesizer FIS = new FunctionImageSynthesizer();
    private Image default_preview;

    @FXML
    private TextField widthTextField, heightTextField, slicesTextField;

    @FXML
    private TextField minX, minY, minZ;

    @FXML
    private TextField maxX, maxY, maxZ;

    @FXML
    private ChoiceBox<String> typeChoiceBox, fillChoiceBox;

    @FXML
    private ToggleButton xEqualY, yEqualZ;

    @FXML
    private ImageView preview;

    @FXML
    private CheckBox previewCheckBox;

    @FXML
    private TextArea functionTextArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        initDefaultPreview();

        // fill choice boxes
        String[] typeChoices = new String[]{"8-Bit", "16-Bit", "32-Bit", "RGB"};
        typeChoiceBox.getItems().addAll(typeChoices);
        typeChoiceBox.setValue("32-Bit");

        String[] fillChoices = new String[]{"Black", "White", "Random", "Ramp"};
        fillChoiceBox.getItems().addAll(fillChoices);
        fillChoiceBox.setValue("Black");

        // init change listener
        widthTextField.textProperty().addListener((observable -> previewCheckBox.setSelected(false)));
        heightTextField.textProperty().addListener((observable -> previewCheckBox.setSelected(false)));
        slicesTextField.textProperty().addListener(observable -> previewCheckBox.setSelected(false));

        xEqualY.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                xEqualY.textProperty().setValue("=");
                minY.textProperty().setValue(minX.textProperty().getValue());
                maxY.textProperty().setValue(maxX.textProperty().getValue());
                previewCheckBox.setSelected(false);
            } else {
                xEqualY.textProperty().setValue("≠");
            }
        });

        yEqualZ.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                yEqualZ.textProperty().setValue("=");
                minZ.textProperty().setValue(minY.textProperty().getValue());
                maxZ.textProperty().setValue(maxY.textProperty().getValue());
                previewCheckBox.setSelected(false);
            } else {
                yEqualZ.textProperty().setValue("≠");
            }
        });

        minX.textProperty().addListener((observable, oldValue, newValue) -> {
            previewCheckBox.setSelected(false);
            if(xEqualY.isSelected()) {
                minY.textProperty().setValue(newValue);
            }
        });

        maxX.textProperty().addListener((observable, oldValue, newValue) -> {
            previewCheckBox.setSelected(false);
            if(xEqualY.isSelected()) {
                maxY.textProperty().setValue(newValue);
            }
        });

        minY.textProperty().addListener((observable, oldValue, newValue) -> {
            previewCheckBox.setSelected(false);
            if(xEqualY.isSelected()) {
                minX.textProperty().setValue(newValue);
            }

            if(yEqualZ.isSelected()) {
                minZ.textProperty().setValue(newValue);
            }
        });

        maxY.textProperty().addListener((observable, oldValue, newValue) -> {
            previewCheckBox.setSelected(false);
            if(xEqualY.isSelected()) {
                maxY.textProperty().setValue(newValue);
            }

            if(yEqualZ.isSelected()) {
                maxZ.textProperty().setValue(newValue);
            }
        });

        minZ.textProperty().addListener((observable, oldValue, newValue) -> {
            previewCheckBox.setSelected(false);
            if(yEqualZ.isSelected()) {
                minY.textProperty().setValue(newValue);
            }
        });

        maxZ.textProperty().addListener((observable, oldValue, newValue) -> {
            previewCheckBox.setSelected(false);
            if(yEqualZ.isSelected()) {
                maxY.textProperty().setValue(newValue);
            }
        });

//        previewCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
//            if(newValue) {
//                updatePreview();
//            } else {
//                showDefaultPreview();
//            }
//        });

        previewCheckBox.setOnMouseClicked(event -> {
            if(previewCheckBox.isSelected()) {
                updatePreview();
            } else {
                showDefaultPreview();
            }
        });

        showDefaultPreview();
    }

    private void initDefaultPreview() {
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
        String type = typeChoiceBox.getValue();

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

        ImagePlus imagePlus = IJ.createImage(function, type, width, height, slices);
        preview.setImage(FIS.getPreview(imagePlus, min, max, function));
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
        ImagePlus imagePlus = IJ.createImage(function, type, width, height, slices);
        FIS.functionToImage(imagePlus, min, max, function);
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

}
