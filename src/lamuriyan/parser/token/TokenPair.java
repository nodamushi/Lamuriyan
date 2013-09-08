package lamuriyan.parser.token;

/**
 * MacroのDFAの入力で使う
 * @author nodamushi
 *
 */
public class TokenPair{
    private Token a,b;
    
    void init(Token before,Token after){
        a = before;b = after;
    }
    
    public Token before(){
        return a;
    }
    
    public Token after(){
        return b;
    }
}
