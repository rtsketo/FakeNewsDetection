package eu.rtsketo.fnd.detection;

import java.io.IOException;
import static java.lang.Integer.min;
import static java.lang.Math.round;
import static java.lang.Thread.sleep;
import java.net.URL;
import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Pair;

public class DetectionGUI extends Application implements Initializable {
    private final int numMeth = 11;
    
    private CompletableFuture<Pair<String,Double>[]> results;
    private Pair<String,Double>[] reason = new Pair[numMeth];
    private boolean resultChanged = true;
    private ProgressBar[] bars;
    private FakeDetection fd;
    private Preferences pref;
    private int learningSum;
    private int learningCur;
    private int strlen = 0;
    private int line = 1;
    private final int fixed = 597;
    @FXML private TextFlow console;
    @FXML private ScrollPane scroll;
    @FXML private CheckBox chkVera;
    @FXML private CheckBox chkRare;
    @FXML private CheckBox chkSmoo;
    @FXML private CheckBox chkLink;
    @FXML private Button btnCheck;
    @FXML private Button btnFiles;
    @FXML private Button btnInput;
    @FXML private TextField input;
    @FXML private ProgressBar progUniGr;
    @FXML private ProgressBar progBiGr;
    @FXML private ProgressBar progTagTriGr;
    @FXML private ProgressBar progTagQuaGr;
    @FXML private ProgressBar progSynGr;
    @FXML private ProgressBar progUniEn;
    @FXML private ProgressBar progBiEn;
    @FXML private ProgressBar progTagTriEn;
    @FXML private ProgressBar progTagQuaEn;
    @FXML private ProgressBar progSynEn;
    @FXML private ProgressBar progOverall;
    @FXML private ProgressBar progLearn; 
    @FXML private Label txtUniGr;
    @FXML private Label txtBiGr;
    @FXML private Label txtTagTriGr;
    @FXML private Label txtTagQuaGr;
    @FXML private Label txtSynGr;
    @FXML private Label txtUniEn;
    @FXML private Label txtBiEn;
    @FXML private Label txtTagTriEn;
    @FXML private Label txtTagQuaEn;
    @FXML private Label txtSynEn;
    @FXML private Label txtOverall;
        
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass()
                .getResource("/fxml/DetectionGUI.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.setTitle("Fake Detection");
        stage.getIcons().add(new Image(DetectionGUI.class
                .getResourceAsStream("/assets/icon.png")));
        
        stage.initStyle(StageStyle.UNIFIED);
        stage.setMinHeight(fixed+130);
        stage.setMaxHeight(fixed+140);
        stage.setMaxWidth(fixed+100);
        stage.setMinWidth(fixed);
        stage.setHeight(fixed+130);
        stage.setWidth(fixed);
        stage.setScene(scene);
        stage.show();
        
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent t) {
                if(fd != null) fd.close();
                Platform.exit();
                System.exit(0);
            }
        });
    }

    public void print(String text) {
        Platform.runLater(()->{
                Text line = new Text(text);
                line.setFont(Font.font("Monospaced",
                        FontWeight.BOLD, 12));
                line.setFill(Color.web("#a9b7c6"));
                if (console.getChildren().size()>100)
                    console.getChildren().remove(0);
                console.getChildren().add(line);
                scroll.layout();
                scroll.setVvalue(scroll.getVmax());
        });
    }
    
    void println(String text) {
        if (strlen < text.length())
            strlen = text.length();
        
        text = String.format("%-" + 
                min(32,strlen) + "s", text);
        
        if (line > 256) line = 1;
        if (line++%2==0) text += "\n";
        else text += "\t";
        
        print(text);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            pref = Preferences
                    .userNodeForPackage(DetectionGUI.class);
            fd = new FakeDetection(this);
            fd.setLinks(pref.getBoolean("link", false));
            fd.setRare(pref.getBoolean("rare", true));
            fd.setMin(pref.getInt("min", 1));
            fd.setSpell(false);
            
            bars = new ProgressBar[] { progUniGr,
                progUniEn, progBiGr, progBiEn, progTagTriGr,
                progTagTriEn, progTagQuaGr, progTagQuaEn,
                progSynGr, progSynEn, progOverall };
            
            chkRare.setSelected(!pref.getBoolean("rare", true));
            chkRare.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                pref.putBoolean("rare", !chkRare.isSelected());
                fd.setRare(!chkRare.isSelected()); }});
            
            chkSmoo.setSelected(pref.getInt("min", 1)==0? false:true);
            chkSmoo.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                pref.putInt("min", chkSmoo.isSelected()? 1:0);
                fd.setMin((chkSmoo.isSelected())? 1:0); }});
            
            chkLink.setSelected(pref.getBoolean("link", false));
            chkLink.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                pref.putBoolean("link", chkLink.isSelected());
                fd.setLinks(chkLink.isSelected()); }});
                        
            for (int b = 0; b < bars.length; b++) {
                int bar = b;
                bars[b].setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    if (!btnCheck.isDisabled())
                        try {
                            if (results != null && resultChanged)
                                reason = results.get();
                            resultChanged = false;
                        } catch (Exception ex) {
                            Logger.getLogger(DetectionGUI.class.getName())
                                .log(Level.SEVERE, null, ex);
                        }
                    
                    cls();
                    if (reason[bar] != null) {
                        print(reason[bar].getKey());
                           System.out.println(reason[bar].getKey());
                    }
                }});}
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            System.out.println("Database is in use! "
                    + "Retrying in few seconds...");
            try { sleep(5000);
                initialize(location, resources);
            } catch (InterruptedException ex1) { }
        }
        
    }
    
    @FXML
    public void learnFromFiles() {
        btnDisabled(true);
        Thread th = new Thread(()->{
            fd.learnFromFiles();
            btnDisabled(false);});
        th.setPriority(Thread.MAX_PRIORITY);
        th.start();        
    }
    
    @FXML
    public void checkVeracity() {
        btnDisabled(true);
        results = CompletableFuture.supplyAsync(()-> {
                Pair<String,Double>[] result = new Pair[numMeth];
                if (checkInput())
                    result = fd.checkVeracity(input.getText());
                btnDisabled(false);
                return result;
        });
        resultChanged = true;
    }
    
    @FXML
    public void learnFromInput() {
        btnDisabled(true);
        if (checkInput()) {
            Thread th = new Thread(()->{
                    fd.learnFromURL(input.getText(),
                            !chkVera.isSelected());
                    btnDisabled(false); });
            th.setPriority(Thread.MIN_PRIORITY);
            th.start();        
        }
    }
    
    private boolean checkInput() {
        if (input.getText().equals("")) {
            println("Input is empty!");
            return false;
        } return true;
            
    }
    
    public void updateProg(String select, double progress) {
        ProgressBar prog;
        Label label;
        
        switch (select) {
            case "unigr": prog = progUniGr; label = txtUniGr; break;
            case "bigr": prog = progBiGr; label = txtBiGr; break;
            case "ttgr": prog = progTagTriGr; label = txtTagTriGr; break;
            case "tqgr": prog = progTagQuaGr; label = txtTagQuaGr; break;
            case "syngr": prog = progSynGr; label = txtSynGr; break;
            case "unien": prog = progUniEn; label = txtUniEn; break;
            case "bien": prog = progBiEn; label = txtBiEn; break;
            case "tten": prog = progTagTriEn; label = txtTagTriEn; break;
            case "tqen": prog = progTagQuaEn; label = txtTagQuaEn; break;
            case "synen": prog = progSynEn; label = txtSynEn; break;
            default: prog = null; label = null;
        }
        
        Platform.runLater(()->{
            if (prog != null) {
                prog.setProgress(progress);
                updateLabel(label, progress);
                updateOverall();
            }
        });
    }
    
    private void updateLabel(Label lab, double prog) {
                String text = (double)round(prog*10000)/100+"";
                if (text.length()<5)
                    text += "0%";
                else text += "%";
                lab.setText(text);
                
                if (prog > 0.5) 
                    lab.setTextFill(Color.rgb(106, 135, 89));
                else lab.setTextFill(Color.rgb(204, 120, 50));        
    }
    
    private void updateOverall() {
        double prog = 
              ( .9920 * progOf(progUniGr)
              + .9908 * progOf(progUniEn)
              + .9938 * progOf(progBiGr)
              + .9936 * progOf(progBiEn)
              + .8554 * progOf(progTagTriGr)
              + .9824 * progOf(progTagTriEn)
              + .9810 * progOf(progTagQuaGr)
              + .9903 * progOf(progTagQuaEn)
              + .9495 * progOf(progSynGr)
              + .9318 * progOf(progSynEn)) 
              /(.9920 + .9908 + .9938 + .9936 + 
                .8554 + .9824 + .9810 + .9903 +
                .9495 + .9318);
        
        progOverall.setProgress(prog);
        updateLabel(txtOverall, prog);
    }
    
    private double progOf(ProgressBar bar) {
        if (bar.getProgress()==.0) return .5;
        return bar.getProgress();
    }

    public void setLearnSum(int learnSum) {
        learningSum = learnSum;
        learningCur = 0;
        btnDisabled(true);
    }

    public void updateLearn() {
        Platform.runLater(()->{
            progLearn.setProgress((double)learningCur++/learningSum);
            if (learningCur == learningSum) {
                progLearn.setProgress(0);
                btnDisabled(false);
            }
        });
    }
        
    public void btnDisabled(boolean wuh) {
        Platform.runLater(()->{
            if (wuh) progLearn.setProgress(-1);
            else progLearn.setProgress(0);
            btnCheck.setDisable(wuh);
            btnFiles.setDisable(wuh);
            btnInput.setDisable(wuh);
            chkVera.setDisable(wuh);
            chkRare.setDisable(wuh);
            chkSmoo.setDisable(wuh);
            chkLink.setDisable(wuh);
            input.setEditable(!wuh);
            strlen = 0;
        });
    }

    public void cls() {
        Platform.runLater(()->{
            console.getChildren().clear();
        });
    }
}
