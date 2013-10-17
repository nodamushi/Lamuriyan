package lamuriyan.parser.macro;

import static lamuriyan.parser.token.TokenType.*;

import java.util.Arrays;
import java.util.List;

import lamuriyan.parser.LamuriyanEngine;
import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;


/**
 * \defにより定義されるマクロの展開処理を担う
 * @author nodamushi
 *
 */
public class UserMacroProcess extends MacroProcess{
    
    /**
     * UserMacroProcessがマクロを展開した結果返す結果。<br>
     * ExpandArea以外で利用はしない。
     */
    public static class MacroExpandData{
        Object[] data;
        
        MacroExpandData(int size){
            data = new Object[size];
        }
        /**
         * 配列は使い回すので、変更しないこと。<br>
         * 配列には、TokenまたはTokenChainまたはnullが入っている。
         */
        public Object[] getData(){
            return data;
        }
    }
    
    
    private int[] data;//偶数 MacroExpandDataのdataのインデックス。　奇数 引数番号-1
    private MacroExpandData returnValue;//メモリ節約の為、使い回し
    
    public UserMacroProcess(TokenChain program,int argsize,String name)throws RuntimeException{
        
        //マクロの長さと、#nが出てくる回数を数え上げる
        int asize = 0;//#nが出てくる回数
        int size=0;//マクロ長
        int s = 0;
        int e = 0;

        for(Token token:program){
            switch(token.getType()){
                case __PATTERN__GROUP__MATCH__:
                case __TOKEN__GROUP__:
                case UNDEFINED:
                case COMMENT:
                    //通常は来ない………はず
                    throw new RuntimeException("プログラムの中に入ってはならないトークンが入っています。"+token);
                case ARGUMENT:
                    size += s!=e?2:1;
                    s=e;
                    asize++;
            }
            e++;
        }
        if(s!=e)size++;
        
        //データの作成
        s=0;
        e=0;
        int i=0;
        int p=0;
        data = new int[asize*2];
        Arrays.fill(data, -1);
        returnValue = new MacroExpandData(size);
        Object[] retObj = returnValue.data;
        
        for(Token token:program){
            
            if(token.getType() == ARGUMENT)IF:{
                String n = token.toString().substring(1);
                try{
                    int m = Integer.parseInt(n);
                    if(argsize<m){
                        printError("マクロ定義エラー："+name+"　引数の数を超えた番号が指定されています。 "
                                + token.toString());
                        break IF;
                    }
                    
                    if(m<=0){//ここ来るはず無いけど一応。
                        printError("マクロ定義エラー："+name+"　引数の番号が間違っています。 "
                                +token.toString());
                        break IF;
                    }
                    
                    if(s!=e){
                        if(s+1==e)
                            retObj[i]=program.get(s);
                        else
                            retObj[i]=program.subChain(s, e);
                        i++;
                    }
                    s=e+1;
                    data[p++]= i;
                    data[p++]= m-1;
                    i++;//引数を格納する為に一つ開ける。
                }catch(NumberFormatException ex){
                    ex.printStackTrace();//通常来ないはずなんだが
                }    
            }//end if
            
            e++;
        }//end for
        
        if(s!=e){
            if(s+1==e)
                retObj[i] = program.get(s);
            else
                retObj[i] = program.subChain(s, e);
        }
        
    }
    
    //TeXでは引数にしてはならないトークンとかあるらしいど………
//    private void argumentCheck(List<Token> args)throws Exception{
//        
//    }
    @Override
    public Object _run(LamuriyanEngine doc ,List<Token> args) throws Exception{
        for(int i=0,e=data.length;i<e;i++){
            int index = data[i++];
            int arg = data[i];
            if(index==-1)break;
            Token token = args.get(arg);
            if(token.getType()==__TOKEN__GROUP__){
                returnValue.data[index] = token.getTokenChain();
            }else{
                returnValue.data[index] = token;
            }

        }
        return returnValue;
    }
}
