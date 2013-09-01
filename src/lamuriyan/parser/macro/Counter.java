package lamuriyan.parser.macro;

import java.util.ArrayList;

public class Counter{
    private int value;
    
    private ArrayList<Counter> subcounters = new ArrayList<>();
    
    public Counter(int i){
        value = i;
    }
    
    public Counter(){
    }
    
    
    public int get(){return value;}
    
    public String getAsString(){
        return Integer.toString(value);
    }
    
    public void set(int i){value = i;}
    
    public int increase(){
        value++;
        zeroSubCounters();
        return value;
    }
    
    public void addSubCounter(Counter c){
        if(c!=null && !subcounters.contains(c))
            subcounters.add(c);
    }
    
    public void zeroSubCounters(){
        for(Counter c:subcounters){
            c.set(0);
            c.zeroSubCounters();
        }
    }
    
}
