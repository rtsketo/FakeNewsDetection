/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.rtsketo.fnd.detection;

import java.util.Locale;
import static javafx.application.Application.launch;

/**
 * @author rtsketo
 */
public class Align { 
    public static void main(String[] args) {
        Locale.setDefault(new Locale("el", "GR"));
        System.setProperty("file.encoding", "UTF-8");
        launch(DetectionGUI.class);
    } 
}
