package eu.rtsketo.fnd.detection;

import java.util.HashMap;
import java.util.Map;

// Bag of Words class.
public class BoWords {

    private Map<String, Integer> unigramCount = new HashMap<>();
    private Map<String, Integer> bigramCount = new HashMap<>();
    
    // Constructor.
    public BoWords(TextProcess text) {
        
        // Extracting words from text.
        String[] words = text.getSimple().split(" ");
        
        // Counting bigrams.
        for (int c = 0; c < words.length - 1; c++) {
            String comb = words[c]+" "+ words[c+1];
            bigramCount.putIfAbsent(comb, 0);
            bigramCount.put(comb, bigramCount.get(comb)+ 1);
        }
        
        // Counting unigrams.
        for (String word : words) {
            unigramCount.putIfAbsent(word, 0);
            unigramCount.put(word,
                    unigramCount.get(word) + 1);
        }
    }
    
    Map<String, Integer> getBigramCount() {
        return bigramCount;
    }
    Map<String, Integer> getUnigramCount() {
        return unigramCount;
    }    
}