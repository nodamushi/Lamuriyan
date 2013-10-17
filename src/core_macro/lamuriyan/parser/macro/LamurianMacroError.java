package lamuriyan.parser.macro;

public class LamurianMacroError extends RuntimeException{

    public static void throwError(String str) throws LamurianMacroError{
        throw new LamurianMacroError(str);
    }
    
    public LamurianMacroError(String str){
        super(str);
    }
    
    
    
}
