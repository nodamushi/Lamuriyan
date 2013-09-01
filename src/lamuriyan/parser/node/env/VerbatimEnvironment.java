package lamuriyan.parser.node.env;

import static java.util.Objects.*;
import lamuriyan.parser.Command;
import lamuriyan.parser.EnvironmentConstructor;
import lamuriyan.parser.EnvironmentFactory;
import lamuriyan.parser.LamuriyanEngine;
import lamuriyan.parser.macro.Function;
import lamuriyan.parser.token.Char;
import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;
import lamuriyan.parser.token.TokenType;

/**
 * Verbatime環境下で文字列を読み取り、その文字列に対して処理や他プログラムに渡して結果を生成する為の環境のベースとなる環境<br>
 * この環境は文字列を受け取ることのみを目的としており、ノードの操作に関しては責任を放棄しています。
 * @author nodamushi
 *
 */
public abstract class VerbatimEnvironment extends Environment{

    private static final Token initToken = new Token(TokenType.ESCAPE,"%%verbatiminit%%");
    private static final Function initfunc =new Function(){
        @Override
        public Object run(LamuriyanEngine engine) throws Exception{
            Environment e=engine.getCurrentEnvironment();
            if(e instanceof VerbatimEnvironment){
                VerbatimEnvironment ve =(VerbatimEnvironment)e;
                ve._init();
            }
            return null;
        }
    };
    
    private static final Token finishToken= new Token(TokenType.ESCAPE, "%%verbatimfinish%%");
    private static final Function finishfunc=new Function(){
        @Override
        public Object run(LamuriyanEngine engine) throws Exception{
            Environment e=engine.getCurrentEnvironment();
            if(e instanceof VerbatimEnvironment){
                VerbatimEnvironment ve =(VerbatimEnvironment)e;
                ve.finish();
            }
            return null;
        }
    };
    
    
    /**
     * initメソッド、finishメソッドを呼び出すファクトリを作成します
     * @param envName 環境名です。constructorで生成する環境の名前と同じにしてください
     * @param constructor 環境を作成するコンストラクターです。生成する環境の名前はenvNameと同じにしてください。
     * @param beginProgram 初期化マクロ。不要な場合はnull
     * @param option beginProgramのマクロのオプション。不要な場合はnull
     * @param beginArgSize beginProgramに必要な引数の数。beginProgramがnullの時は値は何でも0として扱われます。
     * @return ファクトリ
     * @throws NullPointerException envName,constructorがnullの場合
     * @throws IllegalArgumentException envNameが空文字の場合
     */
    protected static EnvironmentFactory createFactory(
            String envName,
            EnvironmentConstructor constructor,
            TokenChain beginProgram,TokenChain option,int beginArgSize)
                    throws NullPointerException,IllegalArgumentException
    {
        requireNonNull(envName,"envName is null.");
        requireNonNull(constructor, "constructor is null.");
        if(envName.isEmpty()){
            throw new IllegalArgumentException("envName is empty.");
        }
        TokenChain begin=new TokenChain(),end = new TokenChain();
//        begin.addAll(Token.relax);
//        begin.addAll(Token.ONVERBATIM_TOKEN,new Token(TokenType.BEGINGROUP,'{'));
//        begin.addAll(Token.toCharToken(envName));
//        begin.add(new Token(TokenType.ENDGROUP,'}'));
        if(beginProgram!=null)
            begin.addAll(beginProgram);
        else {
            option = null;
            beginArgSize=0;
        }
        
        begin.add(initToken);
        end.add(finishToken);
        
        EnvironmentFactory fact = new EnvironmentFactory(envName,constructor, beginArgSize, begin, end, option);
        
        return fact;
    }
    /**
     * initメソッド、finishメソッドを呼び出すファクトリを作成します
     * @param envName 環境名です。constructorで生成する環境の名前と同じにしてください
     * @param constructor 環境を作成するコンストラクターです。生成する環境の名前はenvNameと同じにしてください。
     * @return ファクトリ
     * @throws NullPointerException envName,constructorがnullの場合
     * @throws IllegalArgumentException envNameが空文字の場合
     */
    protected static EnvironmentFactory createFactory(String envName,EnvironmentConstructor constructor)
            throws NullPointerException,IllegalArgumentException
    {
        return createFactory(envName, constructor,null,null,0);
    }
    
    
    public VerbatimEnvironment(String name, LamuriyanEngine engine){
        super(name, engine);
        engine.defineCommand(new Command(finishToken.toString(), finishfunc));
        engine.defineCommand(new Command(initToken.toString(), initfunc));
    }

    private StringBuilder sb=new StringBuilder();
    
    @Override
    public Token input(Char t){
        switch(t.type){
            case CHAR:
            case SPACE:
                sb.append(t.ch);
                break;
            case MATHESCAPE:
                sb.append(t.mathescape.value);
        }
        return null;
    }

    protected String getText(){
        return sb.toString();
    }
    
    protected StringBuilder getTextBuffer(){
        return sb;
    }

    /**
     * 入力が終わり、環境が閉じられる前に呼び出されます。
     */
    abstract protected void finish();
    
    private void _init(){
        engine.setVerbatim(getName());
        init();
    }
    
    /**
     * 初期化マクロが実行された後、入力が始まる前に呼び出されます。
     */
    abstract protected void init();
    
    

}
