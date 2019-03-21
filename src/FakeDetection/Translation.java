package FakeDetection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;

public class Translation implements Callable<String> {
    private int maxThreads = 20;
    private String[] translated;
    private String[] phrases;
    
    public Translation(String[] p) {
        translated = new String[p.length];
        phrases = p;
    }
    
    @Override
    public String call() throws Exception {
        Thread[] threads = new Thread[maxThreads];
        String translation = "";
        
        for (int t=0;t<maxThreads;t++)
            threads[t] = createThread(t);

        
        boolean activeThreads = true;
        while (activeThreads) {
            activeThreads = false;
            for (Thread t : threads) 
                if (t.isAlive()) activeThreads = true;
            sleep(50); }
        
        for (int p=0; p<translated.length; p++)
            translation += translated[p] + " ";
        return translation;
    }

    private Thread createThread(int t) {
        Thread thread = new Thread() {
            @Override public void run() { try {
               for (int c=0;c<phrases.length;c++) 
                   if (c%maxThreads==t) {
                       translated[c] = translate(phrases[c]);
                       System.out.println(c+" "+phrases[c]);
                       System.out.println(c+" "+translated[c]);
                   }
            
               sleep(50);
               } catch (Exception ex) { Logger.getLogger(
                       Translation.class.getName())
                       .log(Level.SEVERE, null, ex);
               }}};
        
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
        return thread;
    }
    
    private String translate(String phrase) {
        try {
            String url = "https://translate.googleapis.com/"
                    + "translate_a/single?client=gtx"
                    + "&sl=el&tl=en&dt=t&q=" 
                    + URLEncoder.encode(phrase, "UTF-8");

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection)
                    obj.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;

            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine); }
            in.close();

            JSONArray jsonArray = 
                    new JSONArray(response.toString());
            JSONArray jsonArray2 = (JSONArray) jsonArray.get(0);
            JSONArray jsonArray3 = (JSONArray) jsonArray2.get(0);
            return jsonArray3.get(0).toString();
        
        } catch (Exception ex) {
            System.out.println("Translation of '" 
                    + phrase + "' unavailable!");
            return "";
        }
    }
}