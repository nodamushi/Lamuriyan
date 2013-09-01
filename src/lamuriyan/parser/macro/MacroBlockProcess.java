package lamuriyan.parser.macro;

import static lamuriyan.parser.token.TokenType.*;

import java.util.List;

import lamuriyan.parser.LamuriyanEngine;
import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;



public class MacroBlockProcess extends MacroProcess{
    private TokenChain program;
    
    public MacroBlockProcess(TokenChain program)throws RuntimeException{
        programCheck(program);
    }
    
    private void programCheck(TokenChain tc)throws RuntimeException{
        program = new TokenChain(tc.size());
        for(Token token:tc){
            switch(token.getType()){
                case __PATTERN__GROUP__MATCH__:
                case __TOKEN__GROUP__:
                case UNDEFINED:
                    //通常は来ない………はず
                    throw new RuntimeException("プログラムの中に入ってはならないトークンが入っています。"+token);
                case COMMENT:
                    break;
                default:
                    program.add(token);
            }
        }
    }
    
    //TeXでは引数にしてはならないトークンとかあるらしいど………
//    private void argumentCheck(List<Token> args)throws Exception{
//        
//    }
    @Override
    public Object _run(LamuriyanEngine doc ,List<Token> args) throws Exception{
//        argumentCheck(args);
        TokenChain newprogram = new TokenChain();
        for(Token t:program){
            switch(t.getType()){
                case ARGUMENT:
                    String n = t.toString().substring(1);
                    try{
                        int m = Integer.parseInt(n);
                        if(args==null || args.size()<m){
                            printError("引数の数を超えています");
                            return null;
                        }
                        Token token = args.get(m-1);
                        if(token.getType()==__TOKEN__GROUP__){
                            newprogram.addAll(token.getTokenChain());
                        }else{
                            newprogram.add(token);
                        }
                    }catch(NumberFormatException e){
                        printError("引数の構文に間違いがあります。");
                        return null;
                    }
                    break;
                default:
                    newprogram.add(t);
                    break;
            }
        }
        return newprogram;
    }
}
