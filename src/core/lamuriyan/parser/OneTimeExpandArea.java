package lamuriyan.parser;

import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;
import lamuriyan.parser.token.TokenType;

public class OneTimeExpandArea{

    private ExpandArea area;
    private TokenChain buffer = new TokenChain();
    private TokenChain result = new TokenChain();
    
    public OneTimeExpandArea(ExpandArea parentarea){
        area = new ExpandArea(parentarea, parentarea.getEngine(), parentarea.getOperator());
    }
    
    public void setUseNumber(){
        area.setUseNumber();
    }
    
    public void addAll(TokenChain tc){
        buffer.addAll(tc);
    }
    
    public void add(Token t){
        if(t.getType()==TokenType.__TOKEN__GROUP__){
            buffer.addAll(t.getTokenChain());
        }else
            buffer.add(t);
    }
    
    public boolean run() throws Exception{
        for(int i=0;i<buffer.size();i++){
            Token t = buffer.get(i);
            if(area.add(t)){
                result.addAll(area.getTokens());
                area.stateClear();
            }
        }
        return result.size()!=0;
    }
    
    
    public TokenChain getTokens(){
        return result;
    }
    
    
}
