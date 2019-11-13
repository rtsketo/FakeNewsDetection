package eu.rtsketo.fnd.detection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Double.min;
import static java.lang.Integer.min;
import static java.lang.Math.abs;
import static java.lang.Math.round;
import static java.lang.Thread.sleep;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.util.Pair;

public class FakeDetection {
    private final Charset utf8 = StandardCharsets.UTF_8;
    private List<String> deadlinks;
    private final int numMeth = 11;
    private int maxResults = 20;
    private DetectionGUI gui;
    private String[] types;
    private DBControl db;
    private boolean rare;
    private boolean spell;
    private boolean links;
    
    
    public FakeDetection(String dbase)
            throws SQLException {
        System.setProperty("http.agent", "Chrome");
        deadlinks = new ArrayList<>();
        db = new DBControl(dbase);
        types = db.getTypes();
        db.setMinValue(1);
        links = false;
        spell = false;
        rare = true;
    }
    
    FakeDetection(DetectionGUI gui) throws SQLException {
        this("edu.db");
        this.gui = gui;
    }
    
    void learnFromFiles() {
        int learnSum = 0;
        
        if (gui != null) {
            try (Stream<String> lines = Files.lines(
                    Paths.get("News/fake.txt"))) {
                learnSum += lines.count(); } catch (IOException ex) {
                output("The file 'fake.txt' in "
                        + "News folder was not found."); }
        
            try (Stream<String> lines = Files.lines(
                    Paths.get("News/good.txt"))) {
                learnSum += lines.count(); } catch (IOException ex) {
                output("The file 'good.txt' in "
                        + "News folder was not found."); }
            
            gui.setLearnSum(learnSum);
        }
        
        if (learnFromFile("good") &&
                learnFromFile("fake"))
                output("\n\n\n\n\n\n\n\n\n\n"
                        + "Learning Completed!");
        
        if (links) {
            clearLinks("good");
            clearLinks("fake");
        }
    }
    
    private boolean learnFromFile(String file) {
        boolean veracity = true;
        if (file.equals("fake"))
            veracity = false;
        
        try(BufferedReader br = Files.newBufferedReader(
                Paths.get("News/" + file + ".txt"), utf8)) {
            
            String url = br.readLine();
            while (url != null) {
            
                learnFromURL(url, veracity);
                
                url = br.readLine();    
                if (gui != null) gui.updateLearn();
            }
            
            return true;
        } catch (Exception ex) {
            output("The file '" + file + ".txt' in "
                    + "News folder was not found.");
            Logger.getLogger(FakeDetection.class.getName())
                        .log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public void learnFromURL(String url, boolean veracity) {
        try {
            TextProcess tpGr = new TextProcess(url);
            TextProcess tpEn = tpGr.getEnglish();
                        
            if (!tpGr.getSimple().equals("")
                    && !tpEn.getSimple().equals("")) {
                
                if (spell) learnSpell(tpGr, veracity);
                for (int t=types.length-4; t>-1; t--) {
                    learnWords(tpEn, veracity, types[t--]);
                    learnWords(tpGr, veracity, types[t]);
                }
            }

        } catch (IOException ex) {
            if (testInternet()) {
                if (links)
                    deadlinks.add(url);
                errorLog(url);
                Logger.getLogger(FakeDetection.class.getName())
                    .log(Level.SEVERE, null, ex);
            }
            else {
                try { sleep(10000); }
                catch (Exception ignored) {}
                finally { learnFromURL(url, veracity); }
            }
        }
    }
    
    private void learnWords(TextProcess tp, 
            boolean veracity, String method) {
        tp.getCount(method)
                .forEach((word,value)->{
            if (word != null)
                db.addWord(word, value, veracity, method);
            outputln(word+": "+value);
        });
    }
    
    private void learnSpell(TextProcess tp, boolean veracity)
            throws IOException {
        db.addSpell(tp.spellcheck(), 
                tp.getSimple().split(" ").length, veracity);
    }
    
    public Pair<String, Double>[] checkVeracity(String url) {
        Pair<String, Double>[] reasoning = null;
        try {
            TextProcess tpGr = new TextProcess(url);
            TextProcess tpEn = tpGr.getEnglish();
            
            if (!tpGr.getSimple().equals("")
                    && !tpEn.getSimple().equals("")) {

                reasoning = new Pair[numMeth];
                if (spell) 
                    reasoning[numMeth-1] = checkSpell(tpGr);

                for (int t=0; t<types.length-3; t++) {
                    reasoning[t] = checkFor(types[t++], tpGr);
                    reasoning[t] = checkFor(types[t], tpEn);
                }

                reasoning[numMeth-3] = checkSyntax(tpGr);
                reasoning[numMeth-2] = checkSyntax(tpEn);
            }
            
        } catch (IOException ex) {
            if (testInternet())
                errorLog(url);
            else {
                try { sleep(10000); }
                catch (Exception ignored) { }
                finally { checkVeracity(url); }
            }
        }
        
        if (gui != null) {
            gui.btnDisabled(false);
            gui.cls();
        }
        
        return reasoning;
    }
    
    private String roundPercent(double num) {
        return (double)round(num*10000)/100+"%";
    }
    
    private void output(String text) {
        if (gui != null) gui.print(text);
        else System.out.print(text);
    }
    
    private void outputln(String text) {
        if (gui != null) gui.println(text);
        else System.out.print(text + ", ");
    }
    
    private Pair<String, Double> checkSpell(TextProcess tp)
            throws IOException {
        double[] ratio = db.getSpell();
        int errorCount = tp.spellcheck();
        int wordCount = tp.getSimple().split(" ").length;
        double percent = (double)errorCount/wordCount;
        double progress = 0;
        
        String reason = "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"
                + "Error ratio: "+roundPercent(percent) +
                "\nThere were found " + errorCount + 
                " unknown words in " + wordCount +
                " words.";
        progress = min(percent,(ratio[0]+ratio[1]))/(ratio[0]+ratio[1]);
        
        if (ratio[1]>ratio[0])
            progress = 1-progress;
        return new Pair(reason, progress);
    }
    
    private Pair<String, Double> checkFor(String method, TextProcess tp) {
            
        String[] reasonWord = new String[maxResults+1];
        double[] reasonInfl = new double[maxResults+1];
        
        int sum = 0;
        double veracity = 0;   
        
        // Getting the learnt ratio of every word, depending
        // on the method, of the text in TextProcess.
        for (Entry<String,Integer> entry : tp.getCount(method).entrySet()) {
            double[] ratio = db.getWord(entry.getKey(), method);
            
            double percent = 0;
            if (ratio[0] != 0 || ratio[1] != 0)
                percent = (ratio[0]-ratio[1]) / (ratio[0]+ratio[1]);
            
            if (abs(percent) == 1 && !rare) continue;
            outputln(entry.getKey()+": "+roundPercent(percent));
            
            veracity += (1+percent)/2 * entry.getValue();
            
            reasonInfl[maxResults] = percent * entry.getValue();
            reasonWord[maxResults] = entry.getKey();
            
            for (int c = maxResults-1; c >= 0; c--)
                iterateReasons(reasonWord, reasonInfl, c);

            sum += entry.getValue();
            
            double progress = veracity/sum;
            
            if (gui!=null) {
                gui.updateProg(method, progress);
                try {
                    if (method.startsWith("t")) sleep(20);
                sleep(10); } catch (InterruptedException ignored) {}
            }
        }
        
        int strlen = 0;
        StringBuilder reason = new StringBuilder("\n\nInfluencive Words:\n");
        for (int c = 0; c < maxResults; c++) 
            if (reasonWord[c] != null && c%2==0) {
                String len = c+1+") "+reasonWord[c]+": "
                        +roundPercent(reasonInfl[c]/sum);
                if (strlen < len.length())
                    strlen = len.length();
            } 
        
        for (int c = 0; c < maxResults; c++) 
            if (reasonWord[c] != null) {
                reason.append(String.format("%-" +
                        min(32, strlen) + "s", c + 1 + ") " +
                        reasonWord[c] + ": " +
                        roundPercent(reasonInfl[c] / sum)));
                
                if (c%2==1) reason.append("\n");
                else reason.append("\t");
            }
        reason.append("\nVeracity: ").append(roundPercent(veracity / sum));
        return new Pair(reason.toString(),veracity/sum);
    }

    private Pair<String, Double> checkSyntax(TextProcess tp) {
            
        int length = maxResults/2;
        String[] reasonWord = new String[length];
        double[] reasonInfl = new double[length];
        
        int sum = 0;
        double veracity = 0;
                
        String[] tagged = tp.getTagOrder();
        String[] phrase = tp.getPhrases();
        String method = tp.isEnglish()? "tqen" : "tqgr";

        for (int p = 0; p < tagged.length; p++) {
            String[] tag = tagged[p].split(" ");
        
            double[] phRatio = {1, 1};
            for (int c = 0; c < tag.length - 3; c++) {
                String qTag = tag[c]+" "+tag[c+1]+
                            " "+tag[c+2]+" "+tag[c+3];
                
                double[] ratio = db.getWord(qTag, method);
                phRatio[0] *= ratio[0]==0? Double.MIN_VALUE:ratio[0];
                phRatio[1] *= ratio[1]==0? Double.MIN_VALUE:ratio[1];
            }
            
            double percent = 0;
            if (phRatio[0] != 0 || phRatio[1] != 0)
                percent = (phRatio[0]-phRatio[1])
                        / (phRatio[0]+phRatio[1]);
            
            if (abs(percent) == 1 && !rare) continue;
            
            veracity += (1+percent)/2;
            
            String reasonPh = phrase[p];
            reasonInfl[length-1] = percent;
            reasonWord[length-1] = reasonPh
                    .substring(0, min(64,
                            reasonPh.length())) + "...";
            
            for (int c = length-2; c >= 0; c--)
                iterateReasons(reasonWord, reasonInfl, c);

            sum++;
            double progress = veracity/sum;
            outputln(sum + ") Veracity: " + roundPercent(percent)+
                    " | " + phrase[p]);
            System.out.println(sum + ") Veracity: " + roundPercent(percent)+
                    " | " + phrase[p]);
            
            if (gui!=null) {
                gui.updateProg(tp.isEnglish()?
                        "synen" : "syngr", progress);
                try { sleep(40); } 
                catch (InterruptedException ignored) {}
            }
        }
        
        StringBuilder reason = new StringBuilder("\n\nInfluencive Phrases:\n");
        for (int c = 0; c < length; c++) 
            if (reasonWord[c] != null) {
                reason.append(c + 1).append(") ")
                        .append(roundPercent(reasonInfl[c] / sum))
                        .append(": ").append(reasonWord[c]).append("\n");
            }
        reason.append("\nVeracity: ").append(roundPercent(veracity / sum));
        return new Pair(reason.toString(),veracity/sum);
    }

    private void iterateReasons(String[] reasonWord, double[] reasonInfl, int c) {
        if (abs(reasonInfl[c]) < abs(reasonInfl[c+1])) {
            double tempPercent = reasonInfl[c+1];
            reasonInfl[c+1] = reasonInfl[c];
            reasonInfl[c] = tempPercent;
            String tempWord = reasonWord[c+1];
            reasonWord[c+1] = reasonWord[c];
            reasonWord[c] = tempWord;
        }
    }

    public void close() {
        try {
            db.closeConnection();
        } catch (SQLException ex) {
            Logger.getLogger(FakeDetection.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }
    
    public void setRare(boolean rare) {
       this.rare = rare;
    }
    public void setMin(int val) {
        db.setMinValue(val);
    }
    public void setSpell(boolean spell) {
        this.spell = spell;
    }
    public void setLinks(boolean links) {
        this.links = links;
    }

    private void errorLog(String url) {
        try(FileWriter fw = new FileWriter("SiteError.log", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter pw = new PrintWriter(bw)) {
            output("\nSite Unavailable: " + url);
            pw.append("\nSite Unavailable: ").append(url);
        } catch (IOException ex) {
            Logger.getLogger(FakeDetection.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }
    
    private boolean testSite(String site) {
        try (Socket sock = new Socket()) {
            InetSocketAddress addr =
                    new InetSocketAddress(site, 80);
            sock.connect(addr, 3000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    private boolean testInternet() {
        return testSite("google.com")
                && testSite("amazon.com")
                && testSite("yahoo.com");
    }
    
    private void clearLinks(String file) {
        try {
            File input = new File("News/" + file + ".txt");
            File temp = new File("News/tmp" + file + ".txt");    
        
            BufferedReader reader = Files.newBufferedReader(input.toPath(), utf8);
            BufferedWriter writer = Files.newBufferedWriter(temp.toPath(), utf8);
        
            String line;
            while ((line = reader.readLine()) != null) 
                if (!deadlinks.contains(line))
                    writer.write(line + System.lineSeparator());
            
            writer.close();
            reader.close();
            while(!temp.renameTo(input))
                sleep(5000);
            
        }   catch (Exception ex) {
            Logger.getLogger(FakeDetection.class.getName())
                    .log(Level.SEVERE, null, ex);
        }   finally { deadlinks.clear(); }
    }    
}