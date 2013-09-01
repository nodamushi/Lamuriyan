
import java.util.ArrayList;


public class CommandLineOption{

    public static class Option{
        public String name,value;
        public Option(String n,String v){
            name = n;
            value = v;
        }
    }
    
    private ArrayList<Option> ops = new ArrayList<>(); 
    
    public CommandLineOption(String[] args){
        for (int i = 0; i < args.length; i++) {
            String string = args[i];
            if(string.startsWith("-")){
                if(args.length == i+1 || args[i+1].startsWith("-")){
                    ops.add(new Option(string, null));
                }else{
                    ops.add(new Option(string, args[i+1]));
                    i++;
                }
            }else{
                ops.add(new Option(null, string));
            }
        }
    }
    
    
    public Option[] getOptions(){
        return ops.toArray(new Option[ops.size()]);
    }
    
    
}
