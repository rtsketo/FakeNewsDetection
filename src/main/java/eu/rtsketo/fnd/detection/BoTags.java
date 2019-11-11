package eu.rtsketo.fnd.detection;

import eu.rtsketo.fnd.tagger.RDRPOSTagger;
import eu.rtsketo.fnd.tagger.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Bag of Part of Speech class.
class BoTags {
    private Map<String, Integer> tagTriCount = new HashMap<>();
    private Map<String, Integer> tagQuaCount = new HashMap<>();
    private String[] tOrder;
    
    // Constructor.
    BoTags(TextProcess tp) {
        String[] phrase1 = tp.getPhrases();
        String[] tagged = new String[phrase1.length];
        tOrder = new String[phrase1.length];
        
        try {   // PoS-tagging.
            RDRPOSTagger tree = new RDRPOSTagger();
            HashMap<String, String> FREQDICT;
            String rdrPath;
            String dicPath;
            
            if (tp.isEnglish()) {
                rdrPath = getClass().getResource("/models/en-pos.RDR").getPath();
                dicPath = getClass().getResource("/models/en-pos.DICT").getPath();
            } else {
                rdrPath = getClass().getResource("/models/el-upos.RDR").getPath();
                dicPath = getClass().getResource("/models/el-upos.DICT").getPath();    
            }
            
            tree.constructTreeFromRulesFile(rdrPath);
            FREQDICT = Utils.getDictionary(dicPath);
            
            // PoS tagging each phrase(sentence) of text.
            for (int p = 0; p < phrase1.length; p++) {
                tagged[p] = tree.tagSentence(FREQDICT, phrase1[p]);
                System.out.println(tagged[p]);
                tOrder[p] = "$start$ ";
                
                // Splitting sentences into words and tags.
                String[] wordWithTags = tagged[p].split(" ");
                for (String wordWithTag : wordWithTags) {
                    String[] split = wordWithTag.split("/");
                    String tag = split[split.length-1];
                    tOrder[p] += tag + " "; 
                }
                
                tOrder[p] += "$$end$$";
            }


            // Counting tags.
            for (String phrase : tOrder) {
                String[] tag = phrase.split(" ");
  
                // Trigram.
                for (int c = 0; c < tag.length - 2; c++) {
                    String comb = tag[c]+" "+tag[c+1]+" "+tag[c+2];
                    tagTriCount.putIfAbsent(comb, 0);
                    tagTriCount.put(comb, tagTriCount.get(comb)+ 1);
                }
                
                // Quadgram.
                for (int c = 0; c < tag.length - 3; c++) {
                    String comb = tag[c]+" "+tag[c+1]+
                            " "+tag[c+2]+" "+tag[c+3];
                    tagQuaCount.putIfAbsent(comb, 0);
                    tagQuaCount.put(comb, tagQuaCount.get(comb)+ 1);
                }
            }
            
        } catch (IOException ex) {
            System.out.println("Greek UPoS are missing!");
        }
    }
    
    Map<String, Integer> getTagTriCount() {
        return tagTriCount;
    }
    Map<String, Integer> getTagQuaCount() {
        return tagQuaCount;
    }
    String[] getTagOrder() {
        return tOrder;
    }
}