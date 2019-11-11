package eu.rtsketo.fnd.detection;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.languagetool.JLanguageTool;
import org.languagetool.language.Greek;
import org.languagetool.rules.RuleMatch;

public class TextProcess {
    private boolean eng;
    private BoTags bot;
    private BoWords bow;
    private String text;
    private String[] phrase;
    private List<String> phrases;
    private String translation = "";
    
    TextProcess(String t) throws IOException {
        this(t, false);
    }
    
    private TextProcess(String t, boolean eng) throws IOException {
        if (t.startsWith("http")) {
            InputStream is = new URL(t).openStream();
            text = Jsoup.parse(is,"UTF-8",t).body().text();
        } else text = t;
        
        phrases = new ArrayList();
        text = text.replace("<br>", " ")
                .replaceAll("\\s{2,}", " ");
        this.eng = eng;
        
        Locale loc = new Locale("el","GR");
        if (eng) loc = new Locale("en","US");
        
        BreakIterator sentenceIterator =
            BreakIterator.getSentenceInstance(loc);
        
        int prev = 0;
        sentenceIterator.setText(text);
        int next = Math.max(sentenceIterator.next(),0);
        do {
            phrases.add(text.substring(prev, next));
            prev = next;
            next = sentenceIterator.next();
        } while (next > BreakIterator.DONE);
    }
    
    public String getText() {
        return text;
    }
    
    public String[] getPhrases() {
        if (phrase == null)
            phrase = phrases.toArray(new String[0]);
        return phrase;
    }
    
    public String getSimple() {
        if (!this.isEnglish())
            return text     // Remove any non-Greek character.
                    .replaceAll("[^\\p{InGreek}]+", " ")
                    .replaceAll("\\s{2,}", " ")
                    .trim().toLowerCase();
        
        return text     // Remove punctuation.
                .replaceAll("\\p{Punct}", " ")
                .replace("\n"," ").replace("\r", " ")
                .replace("\t", " ").replace("·"," ")
                .replace("«"," ").replace("»"," ")
                .replace("–", " ").replace("•", " ")
                .replace("("," ").replace(")"," ")
                .replace("|", " ")
                .replaceAll("\\d+"," ")
                .replaceAll("\\s{2,}", " ")
                .trim().toLowerCase();
    }   
    
    public TextProcess getEnglish() throws IOException {       
        if (getSimple().equals("") 
                || isEnglish())
            return this;
        
        if (translation.equals("")) {
            try {
                ExecutorService service
                        = Executors.newFixedThreadPool(1);
                Future<String> translate = service.submit(
                        new Translation(getPhrases()));
                translation = translate.get();
                service.shutdownNow();
            } catch (Exception ex) {
                Logger.getLogger(TextProcess.class.getName())
                        .log(Level.SEVERE, null, ex); }}
        return new TextProcess(translation, true);
    }
    
    Map<String, Integer> getCount(String method) {
        switch (method) {
            case "tqgr":
            case "tqen":
                if (bot == null) bot = new BoTags(this);
                return bot.getTagQuaCount();
            case "ttgr":
            case "tten":
                if (bot == null) bot = new BoTags(this);
                return bot.getTagTriCount();
            case "bigr":
            case "bien":
                if (bow == null) bow = new BoWords(this);
                return bow.getBigramCount();
            case "unigr":
            case "unien":
                if (bow == null) bow = new BoWords(this);
                return bow.getUnigramCount();
            default: return null;
        }
    }
    
    int spellcheck() throws IOException {
        String text = this.text
                .replaceAll("[^\\p{InGreek}]+", " ")
                .replaceAll("\\s{2,}", " ").trim();
        
        JLanguageTool tool = new JLanguageTool(new Greek());
        List<RuleMatch> matches = tool.check(text);
        
        if (matches.size()>0)
            System.out.println("Erroneous Words: ");
        
        for (RuleMatch match : matches) {
            String error = text.substring(match.getFromPos(),
                    match.getToPos());
            System.out.print(error + " ");
        }
        System.out.println("\nNumber of Errors: "+matches.size());
        return matches.size();
    }
    
    String[] getTagOrder() {
        if (bot == null) bot = new BoTags(this);
        return bot.getTagOrder();
    }
    
    public boolean isEnglish() {
        return eng;
    }
}