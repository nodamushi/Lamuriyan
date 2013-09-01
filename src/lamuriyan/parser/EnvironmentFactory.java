package lamuriyan.parser;

import static java.util.Objects.*;

import java.util.List;

import lamuriyan.parser.macro.Macro;
import lamuriyan.parser.macro.MacroProcess;
import lamuriyan.parser.node.env.Environment;
import lamuriyan.parser.node.env.TextEnvironment;
import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;
import lamuriyan.parser.token.TokenType;


/**
 * Environmentを生成するクラス。<br>
 * 作り始めの特に何もお考えなしだった時に作ってしまって、しかも、
 * 割と根幹だったせいもあって直せず、初期の手探りでかなりぐっちゃぐちゃな様子が残りまくっています。<br>
 * 動くので手直しはしたくない………。
 * @author nodamushi
 *
 */
public class EnvironmentFactory{
    private String name;
    private Macro beginMacro,endMacro,beginSecondMacro,beginlastMacro;
    private Token beginToken,endToken;
    private EnvironmentConstructor env;
//    private TokenChain option;
    /**
     * TextEnvironment.createrを用いてテキスト環境を作ります。
     * @param envname
     * @param beginMacroArgumentSize
     * @param beginProgram
     * @param endProgram
     * @throws Exception 
     */
    public EnvironmentFactory(String envname,
            int beginMacroArgumentSize,TokenChain beginProgram,TokenChain endProgram,TokenChain option) {
        this(envname, TextEnvironment.constructor, beginMacroArgumentSize, beginProgram, endProgram,option);
    }
    //作り始めた当初あんまりマクロ理解してなかったので、も～ぐっちゃぐちゃ。
    //でも動くから手を付けたくない………
    public EnvironmentFactory(String envname,EnvironmentConstructor envcreater,
            int beginMacroArgumentSize,TokenChain beginProgram,TokenChain endProgram,TokenChain option){
        this.name = requireNonNull(envname,"environment name is null").trim();
        if(name.isEmpty())throw new RuntimeException("environment name is empty!");
        env = requireNonNull(envcreater,"CreateEnvironment is null!");
//        this.option =option;
        final String begintoken = "\\begin"+name;
        final String endtoken = "\\end"+name;
        final String seccondtoken = "\\@begin"+name;
        final String lasttoken = "\\@@begin"+name;
        beginToken = Token.escape(begintoken);
        endToken = Token.escape(endtoken);
        if(beginMacroArgumentSize<0||beginMacroArgumentSize>9)throw new IllegalArgumentException("引数は9つまでです。");
        TokenChain tc = new TokenChain();
        for(int i=0;i<beginMacroArgumentSize;i++){
            tc.add(Token.getArgumentToken(i+1));
        }
        if(beginProgram==null)beginProgram=new TokenChain();
        if(endProgram==null)endProgram =new TokenChain();
        
        if(option!=null){
            beginlastMacro =new Macro(lasttoken, beginProgram, tc, null);
            
            //次の文字が[だったら\\@begin～という名前のコマンドを発行するだけ。
            //そうで無かったら、\\@begin～[デフォルト]を発行する
            //という内容のマクロを定義しているらしい
            TokenChain program = new TokenChain();
            program.addAll(Token.escape("\\@ifnextchar"),new Token('['),
                    new Token(TokenType.BEGINGROUP,'{'),Token.escape(seccondtoken),new Token(TokenType.ENDGROUP,'}') ,
                    new Token(TokenType.BEGINGROUP,'{'),Token.escape(seccondtoken),new Token('['));
            program.addAll(option);
            program.addAll(new Token(']'),new Token(TokenType.ENDGROUP,'}') );
            beginMacro = new Macro(beginToken,program, null, null);

            //また新しいマクロの定義開始。\\@begin～という名前のマクロらしい
            //引数は[#1]らしい。
            tc = new TokenChain();
            tc.addAll(new Token('['),Token.Args1,new Token(']'));
            //どうやら、オプションとして受け付けた引数をオプションでは無く、引数に変換するために、\\@@begin～{#1}を生成するらしい。
            MacroProcess process = new MacroProcess(){
                protected Object _run(LamuriyanEngine engine ,List<Token> args)
                        throws Exception{
                    Token a1 = args.get(0);
                    TokenChain tc = new TokenChain();
                    tc.addAll(Token.escape(lasttoken),new Token(TokenType.BEGINGROUP,'{'));
                    if(a1.getType() == TokenType.__TOKEN__GROUP__){
                        tc.addAll(a1.getTokenChain());
                    }else{
                        tc.add(a1);
                    }
                    tc.add(new Token(TokenType.ENDGROUP, '}'));
                    return tc;
                }
            };
            beginSecondMacro = new Macro(seccondtoken, null, tc, process);
        }else
            beginMacro = new Macro(beginToken, beginProgram, tc, null);//オプションをとらないマクロ
        tc = new TokenChain();
        endMacro = new Macro(endToken,endProgram,tc,null);
        
    }
    public String getName(){
        return name;
    }
    public Environment create(LamuriyanEngine engine){
        Environment ret= env.create(engine,this);
        ret.setEnvironmentFactory(this);
        return ret;
    }
    
    //以下はもう、当人もなんとなくしか意味が分かってない。
    //でも動くのでもーいーやってかんじです。はい。
    
    //\beginで環境が作られた直後に生成するコマンドシーケンスと、そのマクロの内容だったはず。
    public Command getBeginCommand(){
        return new Command(beginMacro.getName(), beginMacro);
    }
    
    //え～っとね、たしか………、\begin直後に実行するマクロがオプションをとるときの………
    //まぁ、そんな感じ。
    public Command getBeginSecondCommand(){
        if(beginSecondMacro==null)return null;
        return new Command(beginSecondMacro.getName(),beginSecondMacro);
    }
    //\beginの後に実行するマクロがオプションが必要だったときに、実際のマクロの実行するコマンドの取得だったはず。
    public Command getBeginLastCommand(){
        if(beginlastMacro==null)return null;
        return new Command(beginlastMacro.getName(),beginlastMacro);
    }
    
    //\endが出た後に発行されるコマンドとマクロの内容。
    public Command getEndCommand(){
        return new Command(endMacro.getName(),endMacro);
    }
    
    
    public Token getBeginToken(){
        return beginToken;
    }
    
    public Token getEndToken(){
        return endToken;
    }
}
