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
package net.imagej.plugin.visualFunctionSynthesizer.gui.view;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import net.imagej.plugin.visualFunctionSynthesizer.VisualSynthesizer;
import org.scijava.Context;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author Hadrien Mary
 */
public class RootLayoutController implements Initializable {

    VisualSynthesizer functionGeneratorMain = new VisualSynthesizer();

    @FXML
    private TextField titleTextField, widthTextField, heightTextField, slicesTextField;

    @FXML
    private ChoiceBox<String> typeChoiceBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String[] typeChoices = new String[]{"8-Bit", "16-Bit", "32-Bit", "RGB"};
        typeChoiceBox.getItems().addAll(typeChoices);
        typeChoiceBox.setValue("8-Bit");
    }

    @FXML
    private void handleButtonAction() {
        // parse values from gui
        String title = titleTextField.getCharacters().toString();
        String type = typeChoiceBox.getValue();
        int width = Integer.parseInt(widthTextField.getCharacters().toString());
        int height = Integer.parseInt(heightTextField.getCharacters().toString());
        int slices = Integer.parseInt(slicesTextField.getCharacters().toString());

        // apply
        functionGeneratorMain.functionOne(title, type, width, height, slices);
    }




    public void setContext(Context context) {
        context.inject(this);
    }

}
