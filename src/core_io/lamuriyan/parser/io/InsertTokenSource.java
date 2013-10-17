package lamuriyan.parser.io;

import java.util.Collection;

import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;



public class InsertTokenSource implements TokenSource{
    
    private Token[] tokens;
    private int bpos=0;
    private int length;
    public InsertTokenSource(Token... tokens){
        this.tokens = tokens.clone();
        length = tokens.length;
    }
    
    public InsertTokenSource(TokenChain chain){
        tokens = chain.getTokens().toArray(new Token[chain.getTokens().size()]);
        length = tokens.length;
    }
    
    public InsertTokenSource(Collection<Token> tokens){
        this.tokens = tokens.toArray(new Token[tokens.size()]);
        length = this.tokens.length;
    }
    
    public Token read(){
        return tokens[bpos++];
    }
    
    public Token preread(){
        return tokens[bpos];
    }
    
    public boolean isEnd(){
        return bpos == length;
    }
    
    @Override
    public void close(){
    }

}
