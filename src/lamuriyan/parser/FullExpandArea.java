package lamuriyan.parser;

import java.util.*;

import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;
import lamuriyan.parser.token.TokenType;



public class FullExpandArea{
    private ExpandArea area;
    private ArrayDeque<Token> outputBuffer = new ArrayDeque<>();
    private ArrayDeque<Token> buffer = new ArrayDeque<>();
    private TokenChain result = new TokenChain();
    private boolean ignoreNoExpand=true;
    private Token outputBefore;
    
    public FullExpandArea(ExpandArea parentarea){
        area = new ExpandArea(parentarea, parentarea.getEngine(), parentarea.getOperator());
    }
    
    public void setUseNumber(){
        area.setUseNumber();
    }
    
    public void addAll(TokenChain tc){
        tc.pushTo(buffer);
    }
    
    public void add(Token t){
        if(t.getType()==TokenType.__TOKEN__GROUP__){
            t.getTokenChain().pushTo(buffer);
        }else
            buffer.add(t);
    }
    
    public void setUseNoExpand(){
        ignoreNoExpand = false;
    }
    
    
    
    
    public boolean run() throws Exception{
        while(!buffer.isEmpty()){
            Token t = buffer.pop();
            if(area.add(t)){
                area.getTokens().pushTo(outputBuffer);
                output();
                area.stateClear();
            }
        }
        return result.size()!=0;
    }
    
    private void output(){
        while(!outputBuffer.isEmpty()){
            Token t = outputBuffer.pop();
            Token result=null;
            if(!ignoreNoExpand && t.isProtected()){
                t.setProtect(false);
                if(t.getType()==TokenType.__TOKEN__GROUP__)
                    this.result.addAll(t.getTokenChain());
                else
                    this.result.add(t);
                continue;
            }
            t.setProtect(false);
            switch(t.getType()){
                case SPACE:
                    if(outputBefore!=null&&outputBefore.getType()==TokenType.SPACE){
                        break;
                    }
                case CHAR:
                case NEWLINE:
                    outputBefore=t;
                    this.result.add(t);
                    break;
//                case __NOEXPAND_TOKEN__:
//                    t=new Token(TokenType.ESCAPE,t.toString());
//                    if(!ignoreNoExpand){
//                        this.result.add(outputBefore=t);
//                    }else result = t;
//                    break;
//                case __UNEXPAND_TOKENS__:
//                    if(!ignoreNoExpand){
//                        TokenChain tc;
//                        this.result.addAll(tc=t.getTokenChain());
//                        if(tc.size()!=0){
//                            outputBefore=tc.get(tc.size()-1);
//                        }
//                    }else{
//                        result = new Token(TokenType.__TOKEN__GROUP__);
//                        result.setTokenChain(t.getTokenChain());
//                    }
//                    break;
                default:
                    result = t;
                    t=null;
            }//end switch
            
            if(result!=null){
                while(!outputBuffer.isEmpty()){
                    buffer.push(outputBuffer.pollLast());
                }
                if(result.getType() == TokenType.__TOKEN__GROUP__){
                    List<Token> ts = result.getTokenChain().getTokens();
                    for(int i=ts.size()-1;i>=0;i--){
                        Token tt = ts.get(i);
                        buffer.push(tt);
                    }
                }else
                    buffer.push(result);
            }//end if result
        }//end while
    }
    
    
    
    public TokenChain getTokens(){
        return result;
    }
    
    
}
