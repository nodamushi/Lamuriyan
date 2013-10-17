package lamuriyan.parser.macro;

import java.util.ArrayList;
import java.util.List;

import lamuriyan.parser.LamuriyanEngine;
import lamuriyan.parser.token.Token;


/**
 * printErrorとか、並列処理をしたときに何が起こるか分からないので、一つのTeXファイルを処理する上で、並列処理は考えない。<br>
 * そのために、Define_Macroはstaticではない。（静的なマクロオブジェクトは作らない）
 * 
 * @author nodamushi
 */
public abstract class MacroProcess{
    
    private List<String> errs = new ArrayList<>(),messages = new ArrayList<>();
    protected Macro macro;
    
    public void setTeXMacro(Macro m){
        macro = m;
    }
    
    public boolean hasErrorMessage(){
        return errs.size()!=0;
    }
    
    public void clearMessages(){
        errs.clear();
        messages.clear();
    }
    
    protected void printError(String err){
        errs.add((macro!=null?macro.getName()+" error:":"errro:")+err);
    }
    
    public String[] getErrorMessage(){
        return errs.toArray(new String[errs.size()]);
    }
    
    
    protected void print(String message){
        messages.add((macro!=null?macro.getName()+" :":"")+message);
    }
    
    public String[] getMessage(){
        return messages.toArray(new String[messages.size()]);
    }
    
    public final Object run(LamuriyanEngine engine,List<Token> args) throws Exception{
        clearMessages();
        return _run(engine, args);
    }
    protected abstract Object _run(LamuriyanEngine engine,List<Token> args) throws Exception;
}


