package lamuriyan.parser;

import java.util.HashMap;

import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;



/**
 * コマンドを保持する領域の定義
 * @author nodamushi
 *
 */
public interface CommandMap{
    /**
     * そりゃーもう、グローバルですよ。閉じられませんよ。
     */
    public static final int GLOBALBLOCK=1000;
    /**
     * {で始まり}で終わるブロック
     */
    public static  final int CHARBLCOK = 0;
    /**
     * \bgroupのコマンドでつくって、\egroupのコマンドで閉じるタイプのブロック。数式モードで有効
     */
    public static final int COMMANDBLOCK=1;
    /**
     * \begin～\endで構築されるブロック
     */
    public static final int ENVIRONMENTBLOCK=2;
    /**
     * getCommand(name,false)を呼びます
     * @param name
     * @return
     */
    public Command getCommand(String name);
    /**
     * setCommand(com)
     * @param com
     */
    public void setCommand(Command com);
    /**
     * このCommandMapが階層の管理をしているCommandMapであった場合、最も規定となる階層に対してコマンドを定義する。<br>
     * 階層を定義していないCommandMapの場合、このメソッドの呼び出しをしてはならない。
     * @param com
     */
    public void setGlobalCommand(Command com);
    /**
     * コマンドを削除する。
     * @param name
     */
    public void removeCommand(String name);
    /**
     * このCommandMapが階層の管理をしているCommandMapであった場合、新たな階層を作成する。<br>
     * 階層を定義していないCommandMapの場合、このメソッドの呼び出しをしてはならない。
     * @param blocktype
     */
    public void pushBlock(int blocktype);
    /**
     * このCommandMapが階層の管理をしているCommandMapであった場合、一番上の階層を削除する。<br>
     * 階層を定義していないCommandMapの場合、このメソッドの呼び出しをしてはならない。
     * @param blocktype
     * @throws IllegalArgumentException blocktypeと現在のタイプが異なる場合（\beginのブロックを}で閉じようとしたなど）
     */
    public void popBlock(int blocktype)throws IllegalArgumentException;
    /**
     * このCommandMapが階層の管理をしているCommandMapであった場合、現在のブロックのタイプを返す。<br>
     * そうでない場合は自分のブロックタイプを返す。
     * @return
     */
    public int getCurrentBlockType();
    /**
     * ブロックが終了した後に発行するTokenを返します。
     * @return
     */
    public Token getAfterBlock();
    /**
     * ブロックが終了した際に発行するTokenを定義します。
     * @param t
     */
    public void setAfterBlock(Token t);
    
    /**
     * このブロックが終了し、popされた直後に呼び出されます。
     * @param engine 
     */
    public void popped(LamuriyanEngine engine);
    
}



class BlockCommandMap implements CommandMap{
    private HashMap<String, Command> commands = new HashMap<>(10);
    private int type;
    
    public BlockCommandMap(int type){
        this.type = type;
    }
    
    @Override
    public Command getCommand(String name){
        if(commands.containsKey(name))return commands.get(name);
        return null;
    }
    @Override
    public void setCommand(Command com){
        commands.put(com.getName(),com);
    }

    @Override
    public void removeCommand(String name){
        commands.remove(name);
    }

    @Override@Deprecated
    public void pushBlock(int blocktype){}

    @Override@Deprecated
    public void popBlock(int blocktype) throws IllegalArgumentException{}
    private Token afterBlock;
    public void setAfterBlock(Token t){
        afterBlock = t;
    }
    public Token getAfterBlock(){
        return afterBlock;
    }
    
    @Override
    public int getCurrentBlockType(){
        return type;
    }
    
    @Override
    public void popped(LamuriyanEngine engine){}

    @Override @Deprecated
    public void setGlobalCommand(Command com){}
}
class GlobalCommandMap implements CommandMap{
    private HashMap<String, Command> commands = new HashMap<>(10);
//    private HashMap<String, Command> macros = new HashMap<>(10);
    private int type;
    
    public GlobalCommandMap(){
        this.type = CommandMap.GLOBALBLOCK;
    }
    
    @Override
    public Command getCommand(String name){
        if(commands.containsKey(name))return commands.get(name);
        return null;
    }
    @Override
    public void setCommand(Command com){
        commands.put(com.getName(),com);
    }

    @Override
    public void removeCommand(String name){
        commands.remove(name);
    }
    @Override@Deprecated
    public void pushBlock(int blocktype){}

    @Override@Deprecated
    public void popBlock(int blocktype) throws IllegalArgumentException{}
    public void setAfterBlock(Token t){
    }
    public Token getAfterBlock(){
        return null;
    }
    @Override
    public int getCurrentBlockType(){
        return type;
    }

    @Override
    public void setGlobalCommand(Command com){setCommand(com);}
    
    @Override
    public void popped(LamuriyanEngine engine){}
    
}


//\tdef直後の{}専用のブロック
//ブロックが終了した直後のpoppedメソッドにてengineにcommandname名の
//トークン列を保存する要求をする
class NotArgumentBlock extends BlockCommandMap{

    private Object option;
    private String commandname;
    private PopedAction action;
    private TokenChain tc = new TokenChain();
    private boolean appendToThisBlock;
    
    public NotArgumentBlock(int type,boolean appendToThisBlock){
        super(type);
        this.appendToThisBlock=appendToThisBlock;
    }
    
    public boolean isAppendToThisBlock(){
        return appendToThisBlock;
    }
    
//    public void setName(String name){
//        commandname = name;
//    }
    
    public void setAction(PopedAction action){
        this.action=action;
    }
    public void setOption(Object option){
        this.option = option;
    }
    
    @Override
    public void popped(LamuriyanEngine engine){
        engine.endTDefBlockMode(this);
        if(action!=null){
            action.poped(engine, option,tc);
        }
//        Command c = new Command(commandname, tc);
//        if(global)
//            engine.defineGlobalCommand(c);
//        else
//            engine.defineCommand(c);
    }
    
    public void appendToken(Token t){
        tc.add(t);
    }
    
}








