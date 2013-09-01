package lamuriyan.parser.token;


public class TokenPair{
    private Token a,b;
    public TokenPair(Token before,Token after){
        a = before;b = after;
    }
    
    public Token before(){
        return a;
    }
    
    public Token after(){
        return b;
    }
}
