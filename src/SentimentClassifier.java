import java.io.File;
import java.io.IOException;

import com.aliasi.classify.ConditionalClassification;
import com.aliasi.classify.LMClassifier;
import com.aliasi.util.AbstractExternalizable;

public class SentimentClassifier {
// class sentimen classifier Lingpipe
    String[] categories;  
    LMClassifier classifier;  
    public SentimentClassifier() {  
    try {  
       classifier= (LMClassifier) AbstractExternalizable.readObject(new File("classifier.txt"));  
       categories = classifier.categories();  
    }  
    catch (ClassNotFoundException e) {  
       e.printStackTrace();  
    }  
    catch (IOException e) {  
       e.printStackTrace();  
    }  
    }  
    public String classify(String text) {  
    ConditionalClassification classification = classifier.classify(text);  
    return classification.bestCategory();  
    }  
 }  