package lamuriyan.parser.token;

import java.util.ArrayList;
import java.util.List;

import lamuriyan.parser.macro.MathEscape;


public class Token{
    
    public static final Token
    Args1 = new Token(TokenType.ARGUMENT, "#1"),
    Args2 = new Token(TokenType.ARGUMENT, "#2"),
    Args3 = new Token(TokenType.ARGUMENT, "#3"),
    Args4 = new Token(TokenType.ARGUMENT, "#4"),
    Args5 = new Token(TokenType.ARGUMENT, "#5"),
    Args6 = new Token(TokenType.ARGUMENT, "#6"),
    Args7 = new Token(TokenType.ARGUMENT, "#7"),
    Args8 = new Token(TokenType.ARGUMENT, "#8"),
    Args9 = new Token(TokenType.ARGUMENT, "#9"),
    Undefined = new Token(TokenType.UNDEFINED),
    ONVERBATIM_TOKEN = new Token(TokenType.ESCAPE,"ONVERBATIMEMODE"),
    NEWPARAGRAPH=new Token(TokenType.ESCAPE,"newparagraph"),
    NEWLINECOMMAND=new Token(TokenType.ESCAPE,"\n"),
    BEGINGROUP = new Token(TokenType.ESCAPE,"begingroup"),
    MATHBEGINGROUP = new Token(TokenType.ESCAPE,"mathbegingroup"),
    
    relax = new Token(TokenType.ESCAPE,"\\relax");
    
    
    private static List<Token> CHARTOKENS=new ArrayList<>();
    static{
        for(int i=33;i<123;i++){
            Token t = new Token((char)i);
            CHARTOKENS.add(t);
        }
        for(int i=0x3000;i<=0x30fe;i++){
            Token t = new Token((char)i);
            CHARTOKENS.add(t);
        }
    }
    
    public static Token getCharToken(char ch){
        if(33<=ch && ch<123){
            return CHARTOKENS.get(ch-33);
        }
        
        if(0x3000<=ch && ch<=-0x30fe){
            return CHARTOKENS.get(ch-0x3000+123-33);
        }
        return new Token(ch);
    }
    
    public static final Token[] relax9(){
        return new Token[]{
                relax,relax,relax,relax,relax,relax,relax,relax,relax
        };
    }
//    public static Token toCharBlockToken(String str){
//        TokenChain tc = toCharTokenChain(str);
//        Token t = new Token(TokenType.__TOKEN__LIST__);
//        t.setTokenChain(tc);
//        return t;
//    }
    
    public static Token getArgumentToken(int i){
        switch(i){
            case 1:
                return Args1;
            case 2:
                return Args2;
            case 3:
                return Args3;
            case 4:
                return Args4;
            case 5:
                return Args5;
            case 6:
                return Args6;
            case 7:
                return Args7;
            case 8:
                return Args8;
            case 9:
                return Args9;
            default:
                return null;
        }
    }
    
    public static Token escape(String str){
        if(str.startsWith("\\"))return new Token(TokenType.ESCAPE,str);
        else return new Token(TokenType.ESCAPE,"\\"+str);
    }
    
    public static Token[] toCharToken(String str){
        char[] cs = str.toCharArray();
        Token[] ret = new Token[cs.length];
        for(int i=0;i<cs.length;i++){
            ret[i] = new Token();
            ret[i].setChar(cs[i]);
        }
        return ret;
    }
    
    public static TokenChain toCharTokenChain(String str){
        TokenChain tc = new TokenChain();
        char[] cs = str.toCharArray();
        for(int i=0;i<cs.length;i++){
            Token t = new Token();
            t.setChar(cs[i]);
            tc.add(t);
        }
        return tc;
    }
    
    final private TokenType type;
    private char ch;//Charの時用
    private String string;
    private TokenChain sc=null;
    private char blockend;
    private boolean protect = false;
    
    

    
    public Token(){
        type = TokenType.CHAR;
    }
    public Token(char c){
        type = TokenType.CHAR;
        ch = c;
    }
    public Token(TokenType type){
        this.type = type;
    }
    
    public Token(TokenType type,char c){
        this.type = type;
        ch = c;
    }
    
    public Token(TokenType type,String str){
        this.type = type;
        string = str;
    }
    
    public void setTokenChain(TokenChain sc){
        this.sc = sc;
    }
    
    /**
     * グループの時などのように、階層構造を持つ場合は、これを持ちます。<br>
     * 親子関係を定義する物であり、兄弟を定義するものではない
     * @return
     */
    public boolean hasStringChain(){return sc!=null;}
    public TokenChain getTokenChain(){return sc;}
    public void setTokenEndChar(char c){blockend=c;}
    public char getTokenEndChar(){return blockend;}
    
    public TokenType getType(){return type;}
    
    public void setChar(char c){
        ch = c;
    }
    
    public char getChar(){
        return ch;
    }
    
    
    @Override
    public boolean equals(Object a){
        if(a==null)return false;
        if(a == this)return true;
        if(a instanceof Token){
            Token t = (Token)a;
            if(t.type!=type)return false;
            if(string==null)return t.ch == ch;
            return string.equals(t.string);
        }
        if(a instanceof String){
            String s=(String)a;
            if(string == null){
                if(s.length()==1)return s.charAt(0) == ch;
                return false;
            }
            return s.equals(string);
        }
        return false;
    }
    
    
    @Override
    public String toString(){
        if(hasStringChain()){
            return getTokenChain().toString();
        }
        if(string == null)return ch!=0? Character.toString(ch):"";
        return string;
    }
    
    
    public String getStringProperty(){
        return string;
    }

    public Token get(int i){
        if(sc!=null){
            return sc.get(i);
        }else return null;
    }
    
    public int size(){
        if(sc==null)return 0;
        else return sc.size();
    }
    private boolean isnumber = false;
    public boolean isNumber(){
        return isnumber;
    }
    /**
     * ExpandArea以外からは呼ばないこと
     */
    public void setNumberFlag(){
        isnumber = true;
    }
    
    public boolean isProtected(){
        return protect;
    }
    
    public void setProtect(boolean b){
        protect = b;
    }
    
    
    private MathEscape mathescape;
    public Token(MathEscape mathescape){
        this.mathescape = mathescape;
        type = TokenType.MATHESCAPE;
    }
    
    public MathEscape getMathEscape(){return mathescape;}
    
}
