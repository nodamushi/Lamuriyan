package nodamushi.fa;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class DFAStateException extends RuntimeException{
    
    private static String createMessage(int c,Object o,Collection<Integer> go){
        String str="";
        for(int i:go){
            str+=i+",";
        }
        return "複数の遷移先が見つかりました。\n現在の状態"+c+"　遷移先:"+str+"\n入力"+o.toString();
    }
    
    
    DFAStateException(int current,Object input,Collection<Integer> go){
        super(createMessage(current, input, go));
    }
}
