package FakeDetection;

import java.util.HashMap;
import java.util.Map;

// Bag of Words class.
public class BoWords {
    
    private String[] words;
    private Map<String, Integer> unigramCount 
            = new HashMap<String, Integer>();
    private Map<String, Integer> bigramCount 
            = new HashMap<String, Integer>();
    
    // Constructor.
    public BoWords(TextProcess text) {
        
        // Extracting words from text.
        words = text.getSimple().split(" ");
        
        // Counting bigrams.
        for (int c = 0; c < words.length - 1; c++) {
            String comb = words[c]+" "+words[c+1];
            bigramCount.putIfAbsent(comb, 0);
            bigramCount.put(comb, bigramCount.get(comb)+ 1);
        }
        
        // Counting unigrams.
        for (int c = 0; c < words.length; c++) {
            unigramCount.putIfAbsent(words[c], 0);
            unigramCount.put(words[c], 
                    unigramCount.get(words[c])+ 1);
        }
    }
    
    public Map<String, Integer> getBigramCount() {
        return bigramCount;
    }
    
    public Map<String, Integer> getUnigramCount() {
        return unigramCount;
    }    
}