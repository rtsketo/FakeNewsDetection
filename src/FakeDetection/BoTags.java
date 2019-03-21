package FakeDetection;

import POStagger.RDRPOSTagger;
import POStagger.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Bag of Part of Speech class.
public class BoTags {
    private Map<String, Integer> tagTriCount = new HashMap<>();
    private Map<String, Integer> tagQuaCount = new HashMap<>();
    private List<String> tags;
    private String[] phrase;
    private String[] tagged;
    private String[] tOrder;
    
    // Constructor.
    public BoTags(TextProcess tp) {
        phrase = tp.getPhrases();
        tagged = new String[phrase.length];
        tOrder = new String[phrase.length];
        tags = new ArrayList<>();
        
        try {   // PoS-tagging.
            RDRPOSTagger tree = new RDRPOSTagger();
            HashMap<String, String> FREQDICT;
            
            if (tp.isEnglish()) {
                tree.constructTreeFromRulesFile(
                        "Models/POS/English.RDR");
                FREQDICT = Utils.getDictionary(
                        "Models/POS/English.DICT");
            } else {
                tree.constructTreeFromRulesFile(
                        "Models/UniPOS/UD_Greek/el-upos.RDR");
                FREQDICT = Utils.getDictionary(
                        "Models/UniPOS/UD_Greek/el-upos.DICT");    
            }
            
            // PoS tagging each phrase(sentence) of text.
            for (int p = 0; p < phrase.length; p++) {
                tagged[p] = tree.tagSentence(FREQDICT,phrase[p]);
                System.out.println(tagged[p]);
                tOrder[p] = "$start$ ";
                
                // Splitting senteces in to words and tags.
                String[] wordWithTags = tagged[p].split(" ");
                for (String wordWithTag : wordWithTags) {
                    String[] split = wordWithTag.split("/");
                    String tag = split[split.length-1];
                    tOrder[p] += tag + " "; 
                    tags.add(tag);
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
    
    public Map<String, Integer> getTagTriCount() {
        return tagTriCount;
    }
    
    public Map<String, Integer> getTagQuaCount() {
        return tagQuaCount;
    }
    
    public String[] getTagOrder() {
        return tOrder;
    }
}