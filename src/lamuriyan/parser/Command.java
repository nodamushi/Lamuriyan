package lamuriyan.parser;

import lamuriyan.parser.macro.*;
import lamuriyan.parser.token.Token;

/**
 * TeXのコマンドシーケンス（\commandみたいな）と実際の中身をつなぐ為のクラス。<br>
 * 基本的には名前と中身を持つだけだったんだけど、何度も同じことを書くのが嫌だったので中身が文字列かとかマクロかとか本質で無いメソッドが増えた。
 * @author nodamushi
 *
 */
public class Command{
    private Object value;
    private String name;
    
    public Command(String name,Object value){
        this.name = name;
        redefine(value);
    }
    
    /**
     * 中身を取得
     * @return
     */
    public Object get(){
        return value;
    }
    /**
     * コマンド名を取得
     * @return
     */
    public String getName(){
        return name;
    }
    /**
     * 中身は同じだが、コマンド名が違うオブジェクトを作成する。
     * @param newname
     * @return
     */
    public Command copyMacro(String newname){
        Command ct = new Command();
        ct.name = newname;
        ct.isMacro = isMacro;
        ct.isFunc=isFunc;
        ct.value = value;
        return ct;
    }
    private Command(){}
    
    /**
     * 中身が一致するかどうかを判定する。
     * @param c
     * @return
     */
    public boolean isSameCommand(Command c){
        if(c==null)return false;
        return value==c.value;
    }
    
    
    //以下はユーティリティー的なメソッド
    
    private boolean isMacro;
    private boolean isFunc;
    
    public boolean isIfCommand(){
        return get() instanceof IFCommand;
    }
    
    public void redefine(Object value){
        this.value = value;
        if(value instanceof Macro){
            isMacro = true;
            isFunc =false;
        }else if(value instanceof Function){
            isFunc = true;
            isMacro = false;
        }else{
            isFunc = false;
            isMacro = false;
        }
    }
    
    public Macro getAsMacro(){
        if(isMacro())
            return (Macro)value;
        else return null;
    }
    
    public Function getAsFunction(){
        if(isFunction())
            return (Function)value;
        else return null;
    }
    
    public boolean isMacro(){
        return isMacro;
    }
    
    public boolean isFunction(){
        return isFunc;
    }
    
    public Counter getAsCounter(){
        if(isCounter()){
            return (Counter)value;
        }else return null;
    }
    
    public boolean isCounter(){
        return value instanceof Counter;
    }
    
    public boolean isList(){
        return value instanceof TokenChainList;
    }
    
    public String getAsString(){
        if(isString()){
            return (String)value;
        }else return null;
    }
    
    
    public boolean isString(){
        return value instanceof String;
    }
    
    public Token getAsToken(){
        if(isToken()){
            return (Token)value;
        }else return null;
    }
    
    public boolean isToken(){
        return value instanceof Token;
    }
    
    public TokenChainList getAsList(){
        if(isList()){
            return (TokenChainList)value;
        }else return null;
    }
    
    
}
