package org.evosuite.lm;

/**
 * Created by mat on 30/08/2016.
 */
public class LanguageModel {
    private static String repeat(String string,int length){
        StringBuilder ret = new StringBuilder();
        int size = 0;
        while(size < length){
            ret.append(string);
        }
        return ret.toString();
    }
    public static String replace(String value) {
        return repeat("lol ", value.length());
    }
}
