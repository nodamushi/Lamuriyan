package lamuriyan.parser.macro;

import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.List;

import lamuriyan.parser.LamuriyanEngine;
import lamuriyan.parser.token.*;
import lamuriyan.parser.token.TokenChain.PaierIterator;
import nodamushi.fa.AutomatonFactory;
import nodamushi.fa.MoveFunction;


/**
 * TeXマクロの定義や、実行の為の引数検索処理などを行います。<br>
 * 実際にマクロの実行はMacroProcesインターフェースを実装したオブジェクトが行います。
 * @author nodamushi
 *
 */
public class Macro{

    static final int defParam = 1000;
    
    private final String name;//マクロの名前
    private AutomatonFactory<TokenPair> automatonfactory;//引数検索のオートマトンを作成するファクトリ
    private int argsize;//引数の長さ
    private ArrayList<Argument> arglist = new ArrayList<>(4);//各引数の種類を保存
    private TokenChain blockValue;//マクロ処理の中身。置き換え文字列
    private MacroProcess func;//マクロ処理で実行する内容
    private boolean useBlock = true;//{～}を一つの引数とみなすかどうか
    private boolean useNumber = false;//連続する数字を一つの引数と見なすかどうか
    
    
    public void setUseBlock(boolean b){
        useBlock = b;
    }
    
    public void setUseNumber(boolean b){
        useNumber=b;
    }
    
    public boolean isUseBlock(){return useBlock;}
    public boolean isUseNumber(){return useNumber;}
    
    public Object run(LamuriyanEngine doc,List<Token> args) throws Exception{
        if(args==null){
            if(argsize != 0){
                System.err.println("引数が足りません");
                return null;
            }
        }else if(args.size() != argsize){
            System.err.println("引数が足りません");
            return null;
        }
        if(func!=null){
            return func.run(doc, args);
        }
        return null;
    }
    
    public boolean hasError(){
        return func.hasErrorMessage();
    }
    
    public String[] getError(){
        return func.getErrorMessage();
    }
    
    public boolean isNoArgumentMacro(){
        return argsize == 0;
    }
    
    public int getArgumentLength(){
        return argsize;
    }
    
    public class MacroDFA{
        private AutomatonFactory<TokenPair>.DFA dfa;
        private TokenChain input;
        private PaierIterator iterator;
        public MacroDFA(TokenChain input){
            dfa = automatonfactory.createDFA();
            this.input = input;
            iterator = input.pairs();
        }
        
        /**
         * inputとして渡したTokenChainに文字列を追加したら呼んでください。<br>
         * なお、このオートマトンは入力として、一つ前の文字と次の文字の二つを使います。<br>
         * この二つ目の値は、半角スペース文字である場合にのみ利用されます。<br>
         * なので、現在の値がスペース文字である場合はStringChainに先読みした値を入れておかなければ、正しく判別することが出来ません。
         * 常に一つ先読みした値を入れておくようにするのが楽だと思います。<br>
         * @return -1:入力を受け付けることが出来ません。失敗です。これ以上はinput()を呼び出すことは出来ません。<br>
         * 0:入力を受け付けることが出来ました。ただし、終了状態には到達していません。<br>
         * 1:入力を受け付けることが出来、かつ、終了状態に到達しました。これ以上は入力を入れるとエラーになります。<br>
         * -2:inputから生成したiteratorのhasNext関数がfalseを返しました。そもそも入力がありません。
         */
        public int input(){
            if(iterator.hasNext()){
                TokenPair pair = iterator.next();
                if(dfa.input(pair)){
                    //入力成功
                    if(isEndState())return 1;//終了状態
                    else{
                        //空の入力をした場合に終了状態になるかどうかチェック。
                        pair = iterator.getPrePair();
                        if(dfa.preinput(pair)){//終了状態に辿り着ける場合は終了状態にまで持って行く
                            dfa.input(iterator.next());
                            return 1;
                        }
                        //終了状態には辿り着かない
                        return 0;
                    }
                }else{//入力失敗
                    return -1;
                }
            }else{//hasNextがfalse
                return -2;
            }
        }
        
        /**
         * 現在終了状態にあるかどうかです
         * @return
         */
        public boolean isEndState(){
            return dfa.isEndState();
        }
        
        /**
         * このオートマトンが終了状態にあるとき、inputのTokenChainから引数のリスト生成します。<br>
         * 終了状態にないときはnullが返ります。
         * @return
         */
        public List<Token> getArguments(){
            if(!dfa.isEndState())return null;
            ArrayList<Token> strs = new ArrayList<>();
            int[] slist = dfa.getStateHistory();
            for(int i=0;i<argsize;i++){
                Argument a = arglist.get(i);
                int st = a.state,stnext=st+1;
                //s 引数の状態番号が一番最初に現れる位置　sn 引数の状態番号+1の状態が最初に現れた位置
                //e 引数の状態番号が最後に現れた位置
                int s=Integer.MAX_VALUE,e=-1,sn=Integer.MAX_VALUE;
                boolean find=false;
                for(int k=0;k<slist.length;k++){
                    int ss = slist[k];
                    if(st==ss){
                        if(!find){
                            s=k;
                            find = true;
                        }
                        e=k+1;
                    }
                    if(stnext == ss){
                        if(sn==Integer.MAX_VALUE)sn=k;
                    }
                }
                if(!find){//マッチしなかった場合は空のグループを追加して次へ
                    TokenChain tc = new TokenChain();
                    Token t = new Token(TokenType.__TOKEN__GROUP__);
                    t.setTokenChain(tc);
                    strs.add(t);
                    continue;
                }
                
                TokenChain subchain = input.subChain(min(s,sn), e);
                subchain.trim();//空白の削除
//                TokenChain deg = input.subChain(s, e);//デバッグ用
//                if(a.type==0){
//                    if(strc.size()!=1)throw new RuntimeException("引数の長さが1じゃねぇええ。バグジャー"
//                            +input+" ("+s+","+e+") a.type="+a.type+"  引数"+a.number+"番目　a.state:"+a.state);
//                    Token ret  = strc.get(0);
//                    strs.add(ret);
//                }else 
                if(a.type==defParam){//for def
                    Token tt = new Token(TokenType.__PATTERN__GROUP__MATCH__);
                    tt.setTokenChain(subchain);
                    strs.add(tt);
                }else{
                    if(subchain.size()==1){
                        Token tt = subchain.get(0);
                        strs.add(tt);
                    }else{
                        TokenChain ret = new TokenChain();
                        for(Token t:subchain){
                            switch(t.getType()){
                                case __TOKEN__GROUP__://まとめてしまったグループを元に戻す。
                                    ret.add(new Token(TokenType.BEGINGROUP,t.getChar()));
                                    ret.addAll(t.getTokenChain());
                                    ret.add(new Token(TokenType.ENDGROUP,t.getTokenEndChar()));
                                    break;
                                default:
                                    ret.add(t);
                            }
                        }
                        Token tt = new Token(TokenType.__TOKEN__GROUP__);
                        tt.setTokenChain(ret);
                        strs.add(tt);
                    }
                }
            }
            return strs;
        }
        
    }
    
    public MacroDFA createAutomaton(TokenChain input){
        return new MacroDFA(input);
    }
    
    public String getName(){
        return name;
    }
    
    private static class Argument{
        int state;
        int type;//0 通常,defParam \defで用いる
    }
    
    //\def定義用
    public Macro(String name,AutomatonFactory<TokenPair> automaton,TokenChain blockvalue,MacroProcess process,int[] argstate,int[] argtype) {
        this.name = name;
        this.automatonfactory = automaton;
        int i=0;
        for(int s:argstate){
            Argument a = new Argument();
            a.type = argtype[i];
            a.state = s;
            arglist.add(a);
            i++;
        }
        argsize = i;
        this.blockValue=blockvalue;
        if(blockValue!=null){
            UserMacroProcess m = new UserMacroProcess(blockValue,argsize,name);
            m.setTeXMacro(this);
            this.func=m;
        }else
            this.func = process;
        
    }
    public Macro(Token name,TokenChain block,TokenChain argspattern,MacroProcess func) {
        this(name.toString(),block,argspattern,func);
    }
    
    
    /**
     * 
     * @param argspattern
     * @throws Exception 
     */
    public Macro(String name,TokenChain block,TokenChain argspattern,MacroProcess func){
        if(argspattern!=null)
            argspattern = argspattern.removeComment();
        this.name = name;
        //状態遷移関数
        int argsize = 0;//引数の数
        
        //マクロの引数処理
        if(argspattern!=null){
            ArrayList<DefineMoveFunctionBase> fs = new ArrayList<>();
            fs.add(new UndefinedMove(0));
            int state=1;//状態番号 undefinedmoveが0を使うので1から
            int backTo=-1;//パターンマッチしなかった場合に戻る状態。
            for(int i=0,e=argspattern.size();i<e;i++){
                Token ts = argspattern.get(i);
                switch(ts.getType()){
                    case ESCAPE:
                        Token t = new Token(ts.getType(),ts.toString());
                        fs.add(new PatternMove(state, backTo, t));
                        state++;
                        break;
                    case SPACE:
                    case NEWLINE:
                    case CHAR:
                        t = new Token(ts.getType(),ts.getChar());
                        fs.add(new PatternMove(state, backTo, t));
                        state++;
                        break;

                    case ARGUMENT:
                        argsize++;
                        if(!ts.equals("#"+argsize))throw new RuntimeException("マクロ定義エラー：\\def"+name+"において引数#の番号があっていません。");
                        Argument ags = new Argument();
                        ags.state =state;
                        ags.type=0;
                        arglist.add(ags);
                        backTo =state;
                        fs.add(new ArgumentMove(state));
                        state++;

                        break;
                    default:
                        throw new RuntimeException("エラー："+name+"の定義中に、引数の中に"+ts.getType()+"が現れました。"+argspattern.toString());
                }

            }
            //パターンマッチの前後のつながりの遷移設定をする。
            DefineMoveFunctionBase bbf = null,b=null;
            for(DefineMoveFunctionBase d:fs){
                d.init(bbf, b);
                bbf = b;
                b = d;
            }
            automatonfactory =new AutomatonFactory<>(state+1, state);
            for(DefineMoveFunctionBase d:fs){
                automatonfactory.setMoveFunction(d.state, d);
            }
        }//引数処理終わり
        
        this.argsize = argsize;
        if(func==null && block==null)throw new RuntimeException("処理が定義されていない");
        if(block!=null){
            blockValue = block;
            UserMacroProcess m = new UserMacroProcess(blockValue,argsize,name);
            m.setTeXMacro(this);
            this.func=m;
        }else{
            this.func = func;
            func.setTeXMacro(this);
        }
    }
    
    
    
    //状態遷移関数の基本クラス
    private static abstract class DefineMoveFunctionBase implements MoveFunction<TokenPair>{
        protected static final int[] error = {-1,1}; 
        public DefineMoveFunctionBase(int state){
            this.state=state;
        }
        //next,secnextはパターンマッチ用　次とその次がパターンマッチでなければnullである。
        //useSecondNextがfalseの時もsecnextはnullである。
        protected Token next,secnext;
        protected boolean useSecondNext=true;
        protected int state;
        
        @Override
        public int move(TokenPair input ,int currentstate)
                throws NullPointerException{
            Token t = input.before(),next = input.after();
            int[] ret = move(t,currentstate);
            if(ret[1]==1)return ret[0];//強制どうの場合は移動先を返す。
            if(this.next==null){
                if(secnext!=null){
                    if(this.secnext.equals(next))return currentstate+2;
                }
                return ret[0];
            }
            if(this.next.equals(next))return currentstate+1;
            else return currentstate;
        }
        
        //0番目:移動先　1番目:強制移動1か、それとも強制0でないか
        //強制でない場合というのは、移動先がcurrentstateかcurrentstate+1かのどちらかである。
        //なお、-1に移動するときは強制移動にすること
        //int[]なのは単にクラスを作るのが面倒くさかっただけのこと。
        abstract int[] move(Token t,int currentstate);
        
        //パターンマッチ専用のメソッド。
        //bbeforeのsecnextと、beforeのnextを設定し、before.useSecondNextをfalseにする。
        //これは、パターンマッチしなければならないトークンがあるのに、次の次のトークンを気にする必要は無いからである。
        abstract void init(DefineMoveFunctionBase bbefore,DefineMoveFunctionBase before);
        
        void setNextToken(Token t){
            next = t;
        }
        
        void setZiNext(Token t){
            if(useSecondNext)secnext = t;
        }
    }
    
    
    //パターンマッチ用の遷移
    private static class PatternMove extends DefineMoveFunctionBase{

        private int backto;
        private Token pattern;
        public PatternMove(int state,int backto,Token t){
            super(state);
            this.backto = backto;
            pattern = t;
            
        }

        @Override
        int[] move(Token t ,int currentstate){
            if(t==null)return error;
            if(pattern.equals(t))return new int[]{currentstate+1,0};
            else return new int[]{backto,1};
        }

        @Override
        void init(DefineMoveFunctionBase bbefore ,DefineMoveFunctionBase before){
            if(before==null)return;
            before.useSecondNext=false;
            before.setNextToken(pattern);
            if(bbefore==null)return;
            bbefore.setZiNext(pattern);
        }
        
    }
    
    //一番最初にExpandAreaはUndefinedのトークンを入れる仕様なので、それに対応する為の遷移
    private static class UndefinedMove extends DefineMoveFunctionBase{

        public UndefinedMove(int state){
            super(state);
        }

        @Override
        int[] move(Token t ,int currentstate){
            if(t.getType()==TokenType.UNDEFINED)return new int[]{currentstate+1,0};
            return error;
        }

        @Override
        void init(DefineMoveFunctionBase bbefore ,DefineMoveFunctionBase before){}
        
    }

    
    //#nの遷移
    private static class ArgumentMove extends DefineMoveFunctionBase{
        public ArgumentMove(int state){
            super(state);
        }

        @Override
        int[] move(Token t,int currentstate){
            if(t == null)return error;
          switch(t.getType()){
              //基本的にSPACEとNEWLINEは無視である。
              case SPACE:
              case NEWLINE:
              case COMMENT://コメントは来ないはず
                  return new int[]{currentstate,0};
              case __TOKEN__GROUP__:
              case ESCAPE:
              case ENDGROUP:
              case BEGINGROUP:
              case CHAR:
              case ARGUMENT:
                  return new int[]{currentstate+1,0};
          }
          return error;
        }

        @Override
        void init(DefineMoveFunctionBase bbefore ,DefineMoveFunctionBase before){
        }
        
    }


}
