package Test;

import FakeDetection.DBControl;
import FakeDetection.FakeDetection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Math.round;
import static java.lang.System.out;
import static java.lang.Thread.sleep;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

public class Tester {
    static Pair<String, Double>[] result;
    final static int numMeth = 11;
    static List<String> good;
    static List<String> fake;
    static FakeDetection fd;
    static int falseGood[];
    static int falseFake[];
    static PrintWriter pw;
    static Random rand;

    public static void main (String args[]) {
        int reps = 1;
        if (args.length > 0) 
            reps = Integer.parseInt(args[0]);
        
        for (int r=0; r<reps; r++) 
            testAccuracy();
    }
    
    private static void testAccuracy() {
        try {        
            Files.deleteIfExists(Paths.get("test.db"));
            fd = new FakeDetection("test.db");
            fd.setLinks(false);
            fd.setSpell(false);
            fd.setRare(true);
            fd.setMin(1);
            
            falseGood = new int[numMeth];
            falseFake = new int[numMeth];
            good = new ArrayList();
            fake = new ArrayList();
            rand = new Random();

            learnFrom(true);
            learnFrom(false);

            checkAccuracy(true);
            checkAccuracy(false);

            printResults();
            fd.close();

        } catch (Exception ex) {
            Logger.getLogger(DBControl.class.getName())
                    .log(Level.SEVERE, null, ex);
            System.out.println("Database is in use! "
                    + "Retrying in few seconds...");
            try { sleep(50000); }
            catch (InterruptedException eex) { }
            finally { testAccuracy(); }
        }
    }
    
    private static void learnFrom(boolean vera) {
        List<String> pool = new ArrayList();
        String file;
        
        if (vera) file = "good";
        else file = "fake";
        
        try(BufferedReader br = new BufferedReader(
                new FileReader("News/" + file + ".txt"))) {
            
            String url = br.readLine();
            while (url != null) {
                if(rand.nextFloat()>.2)
                    fd.learnFromURL(url, vera);
                else pool.add(url);
                url = br.readLine();
            }
            
        } catch (IOException ex) {
            Logger.getLogger(Tester.class.getName())
                    .log(Level.SEVERE, null, ex);
            System.out.println("The file '" + file +
                    ".txt' in News folder was not found.");
        }
        
        if (vera) good.addAll(pool);
        else fake.addAll(pool);
    }
    
    private static void checkAccuracy(boolean vera) {
        List<String> pool = new ArrayList();
        int falsePool[] = new int[numMeth];
        int ineq = 1;
        
        if (vera) pool.addAll(good);
        else {
            pool.addAll(fake);
            ineq *= -1;
        }

        for (String url : pool) {
            result = fd.checkVeracity(url);
            if (result != null)
                for (int c=0; c<result.length; c++) 
                    if (result[c] != null)
                        if (ineq*result[c].getValue()<.5*ineq)
                            falsePool[c]++;
        }
        
        for (int c=0; c<falsePool.length; c++)
            if (vera) falseGood[c] += falsePool[c];
            else falseFake[c] += falsePool[c];
            
    }
    
    private static void printResults() {
        try(FileWriter fw = new FileWriter("TestResults.txt", true);
                BufferedWriter bw = new BufferedWriter(fw)) {
            
            pw = new PrintWriter(bw);
            String label[] = { "UniGr",  "UniEn", "BiGr",
                "BiEn", "TTGr", "TTEn", "TQGr", "TQEn",
                "SynGr", "SynEn" };
            
            println("\n");
            for(int c=0; c<label.length; c++) 
                println(label[c] + ": \t" + falseGood[c]
                        + ", \t" + falseFake[c] + ", \t" 
                        + (falseGood[c] + falseFake[c])+ ", \t"
                        + roundPercent(1 - ((double)(falseGood[c]
                        + falseFake[c])) / (good.size() + fake.size())));
            println("Total: \t" + good.size() + ", \t" 
                    + fake.size() + ", \t" + (good.size()
                    + fake.size()) + "\n");
            
        } catch (IOException ex) {
            Logger.getLogger(Tester.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }
    
    private static void println(String s) {
        out.println(s);
        pw.println(s);
    }
    
    private static String roundPercent(double num) {
        return (double)round(num*10000)/100+"%";
    }
}
