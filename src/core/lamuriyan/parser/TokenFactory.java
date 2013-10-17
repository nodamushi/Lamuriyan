package lamuriyan.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;
import lamuriyan.parser.token.TokenType;



/*
 * charを受け取って文字列に変換する処理を請け負うクラス。
 * 
 * 基本的な処理は
 * charを受け取る→CharCategoryの取得→現在処理中のTokenFactoryのサブクラスがあれば処理を回す
 * →なければ、対応するTokenFactoryを作るか、そのまま一文字のTokenを作成する→
 * →charを使用したかどうかのフラグを立てる。（\escape を判定するには半角スペース等が必要だが、
 * 半角スペースは\escapeには含まれない為、使用されないということ。）
 * 
 * みたいな感じらしい。いろいろ例外もあるけど。
 * 
*/
class TokenFactory{
    protected boolean consumed;
    protected CharCategoryDefine define;
//    protected boolean neadBlock=false;
    protected boolean skipSpace=true;
    private int x = 0;//行頭から何文字目か
    protected boolean mathmode =false;//'の処理用
    
    
    public void setMathMode(boolean b){
        mathmode = b;
    }
    public TokenFactory(CharCategoryDefine def){
        define = def;
    }
    
    public boolean isConsumedLastChar(){ 
        return consumed;
    }
    
//    public void setNeadBlock(boolean b){
//        neadBlock=b;
//    }
    
    
    
    protected TokenFactory current;
    
    protected boolean finished(){
        return false;
    }
    
    public Token eof(){
        if(current!=null){
            Token ret = append((char)0);
            return ret;
        }
        return null;
    }
    
    public void setVerbatimMode(){
        current = new VerbatimeFoactory(define);
    }
    
    public void endVerbatimMode(){
        current = null;
    }
    
    public boolean isVerbatimeMode(){
        return current instanceof VerbatimeFoactory;
    }
    
    public void setVerbatimeMode_findGroupMode(boolean find){
        if(isVerbatimeMode()){
            ((VerbatimeFoactory)current).setFindGroup(find);
        }
    }
    
    public void setVerbMode(){
        current =new VerbCommandFactory(define);
    }
    
    public void setVerbMode(char start){
        current = new VerbCommandFactory(define);
        current.append(start,CharCategory.OTHERCHARACTOR);
    }
    
    public void endVerbMode(){
        if(current instanceof VerbCommandFactory){
            current = null;
        }
    }
    
    public boolean isVerbMode(){
        return current instanceof VerbCommandFactory;
    }
    
    public boolean isDefinedVerbStartChar(){
        if(isVerbMode()){
            return((VerbCommandFactory)current).definedfirst;
        }
        return true;
    }
    final public Token append(char c){
        x++;
        CharCategory cc=define.get(c);
        if(cc==CharCategory.NEWLINE)x=0;
        skipSpace = cc==CharCategory.NEWLINE ||(skipSpace && cc==CharCategory.SPACE);
        return append(c,cc);
    }
    
    
    protected Token append(char c,CharCategory cc){
        if(current!=null){
            Token ret = current.append(c,cc);
            consumed = current.consumed;
            if(!consumed){
                x--;
            }
            if(current.finished())current = null;
            return ret;
        }

        consumed = true;
        switch(cc){
            case UPPER:
            case UNDERBAR:
            case ACTIVE:
            case AND:
                return new Token(TokenType.ESCAPE,Character.toString(c));
            case OTHERCHARACTOR:
                if(mathmode && c=='\''){
                    current = new PrimeFactory(define);
                    break;
                }
            case ALPHABET:
                if(startWithMultipleString(c)){//!'や?'や''や``の為
                    current = new MultiByteFactory(define, c, this);
                    break;
                }
                if(x==1){//行頭フック
//                    CharPair cp = startActiveChar.get(c);
                    String cp = startActiveChar.get(c);
                    if(cp!=null){
//                        return new Token(TokenType.ESCAPE,cp.escape);
                        return new Token(TokenType.ESCAPE,cp);
                    }
                }
//                return new Token(c);
                return Token.getCharToken(c);
            case ENDGROUP:
                return new Token(TokenType.ENDGROUP,c);
            case BEGINGROUP:
                return new Token(TokenType.BEGINGROUP,c);
            case ESCAPE:
                current =new EscapeFactory(c,define);
                break;
            case COMMENT_START:
                current = new CommentFactory(c,define);
                break;
            case NEWLINE:
                current = new NewParaFactory(c,define);
                break;
            case SPACE:
                if(skipSpace){
                    break;
                }
                return new Token(TokenType.SPACE, c);
            case PARAMETER:
                current = new ArgumentFactory(c,define);
                break;
            case DOLLAR:
                current = new DollarFactory(define);
            default:
                return null;
        }
        return null;
    }
    
    public String debag(){
        String str = "TokenFactory";
        if(current!=null){
            str += "->"+current.debag();
        }
        return str;
    }
    
    private static class StringPair{
        String from,to;
       public StringPair(String a,String b){
           from = a;
           to=b;
    } 
    }
    private ArrayList<StringPair> multibytepatterns=new ArrayList<>();
    {
        multibytepatterns.add(new StringPair("!`","\\iexcl"));
        multibytepatterns.add(new StringPair("?`","\\iquest"));
        multibytepatterns.add(new StringPair("''","\\rdquo"));
        multibytepatterns.add(new StringPair("``","\\ldquo"));
    }
    
    public void addMultibyte(String str,String to){
        if(str==null || str.length()<2)return;//長さが1ならアクティブを使え
        for(StringPair sp:multibytepatterns){
            if(sp.from.equals(str)){
                sp.to=to;
                return;
            }
        }
        multibytepatterns.add(new StringPair(str, to));
    }
    
    public void removeMulthByte(String str){
        for(StringPair sp:multibytepatterns){
            if(sp.from.equals(str)){
                multibytepatterns.remove(sp);
                return;
            }
        }
    }
//    
//     ただのcharとStrignの構造体みたいだけど
//     なんでわざわざこうなってるのかよく分からん
//     たぶん、いろいろ機能を追加テストしてる際に使ったんだろうねー
//     数ヶ月後の私にはもうわからない
//         
//     必要なさそうなのでとりあえずコメントアウトしてみる
//    private static class CharPair{
//        char c;
//        String escape;
//    }
//    //行頭フックよう
//    private HashMap<Character, CharPair> startActiveChar = new HashMap<>();
//    
//    /**
//     * 行頭フック文字の設定をします
//     * @param c
//     * @param replace nullの時、cのマップを削除
//     */
//    public void setLineStartActiveChar(char c,String replace){
//        if(replace==null){
//            startActiveChar.remove(c);
//        }else{
//            CharPair cp = new CharPair();
//            cp.c = c;
//            cp.escape = replace;
//            startActiveChar.put(c, cp);
//        }
//    }
    
    
    //行頭フックよう
    private HashMap<Character, String> startActiveChar = new HashMap<>();
    
    /**
     * 行頭フック文字の設定をします
     * @param c
     * @param replace nullの時、cのマップを削除
     */
    public void setLineStartActiveChar(char c,String replace){
        if(replace==null){
            startActiveChar.remove(c);
        }else{
            startActiveChar.put(c, replace);
        }
    }
    
    //複数文字で一文字扱いになる文字の最初の文字かどうか
    private boolean startWithMultipleString(char c){
        for(StringPair s:multibytepatterns){
            if(s.from.charAt(0)==c)return true;
        }
        return false;
    }
    //cで始まる複数文字で一文字扱いになる文字の集合を得る
    private List<StringPair> getMatchMultipleStrings(char c){
        ArrayList<StringPair> a = new ArrayList<TokenFactory.StringPair>();
        for(StringPair s:multibytepatterns){
            if(s.from.charAt(0)==c)a.add(s);
        }
        return a;
    }
    //ただの構造体
    private static class C{
        List<StringPair> list;
        StringPair fullmatch;
    }
    //strに一致するものだけ抜き出した新たなリストを生成する。
    private static C getSearchMultipleStrings(String str,List<StringPair> multibytepatterns) {
        ArrayList<StringPair> a = new ArrayList<TokenFactory.StringPair>();
        StringPair fullmatch=null;
        for(StringPair s:multibytepatterns){
            if(s.from.startsWith(str)){
                if(s.from.equals(str)){
                    fullmatch=s;
                }else
                    a.add(s);
            }
        }
        C c = new C();
        c.list = a;
        c.fullmatch = fullmatch;
        return c;
    }
    
    
    
    private static class MultiByteFactory extends TokenFactory{
        protected boolean finished() {
            return true;
        }
        List<StringPair> list;
        TokenChain tc =  new TokenChain();
        StringBuilder sb=new StringBuilder();
        TokenFactory _this;
        public MultiByteFactory(CharCategoryDefine def,char ch,TokenFactory source){
            super(def);
            tc.add(new Token(ch));
            sb.append(ch);
            _this =source;
            list = _this.getMatchMultipleStrings(ch);
        }
        @Override
        public Token append(char c ,CharCategory cc){
            consumed=false;
            switch(cc){
                case OTHERCHARACTOR:
                case ALPHABET:
                    break;
                default:
                    Token t = new Token(TokenType.__TOKEN__GROUP__);
                    t.setTokenChain(tc);
                    return t;
            }
            sb.append(c);
            C ret = getSearchMultipleStrings(sb.toString(), list);
            list = ret.list;
            if(ret.fullmatch!=null){//TODO 最短一致の検索になってるみたいだけど、最長一致にしなくて良いのかな？
                consumed = true;
                Token t= new Token(TokenType.ESCAPE,ret.fullmatch.to);
                tc.clear();
                tc.add(t);
            }
            if(list.size()==0){
                Token t = new Token(TokenType.__TOKEN__GROUP__);
                t.setTokenChain(tc);
                return t;
            }
            consumed = true;
            Token t= new Token(c);
            tc.add(t);
            return null;
        }
    }
}
/*
 * 数式モードにおける '×n の扱いは、\sp{\prime×n}に置換される。
 * それの処理っぽい。
 */
class PrimeFactory extends TokenFactory{
    TokenChain tc = new TokenChain();
    public PrimeFactory(CharCategoryDefine def){
        super(def);
        tc.addAll(
                new Token(TokenType.ESCAPE,"\\sp") ,new Token(TokenType.BEGINGROUP),
                new Token(TokenType.ESCAPE,"\\@startchargroup"),new Token('o'),//\\@startchargroupのオプションみたい
                new Token(TokenType.ESCAPE,"\\prime"));
        
    }
    boolean f=false;
    protected boolean finished(){
        return f;
    }
    
    @Override
    protected Token append(char c ,CharCategory cc){
        consumed = false;
        if(cc==CharCategory.OTHERCHARACTOR && c=='\''){
            consumed = true;
            tc.add(new Token(TokenType.ESCAPE,"\\prime"));
        }else{
            tc.add(new Token(TokenType.ESCAPE,"\\@endchargroup"));
            tc.add(new Token(TokenType.ENDGROUP));
            Token t =new Token(TokenType.__TOKEN__GROUP__);
            t.setTokenChain(tc);
            f=true;
            return t;
        }
        return null;
    }
    
}
/*
 * $数式$か$$数式$$かがあるから、その処理っぽい
 */
class DollarFactory extends TokenFactory{

    public DollarFactory(CharCategoryDefine def){
        super(def);
    }
    @Override
    protected boolean finished(){
        return true;
    }
    @Override
    protected Token append(char c ,CharCategory cc){
        if(cc == CharCategory.DOLLAR){
            consumed = true;
            return new Token(TokenType.ESCAPE,"$$");
        }else{
            consumed=false;
            return new Token(TokenType.ESCAPE,'$');
        }
    }
    
}

/*
 * \begin{～}で始まり\end{～}で終わる文字をそのまま取得する環境の処理っぽい
 */
class VerbatimeFoactory extends TokenFactory{
    
    public VerbatimeFoactory(CharCategoryDefine def){
        super(def);
    }
    
//    boolean finished=false;//これ書き換わらないみたいだが
    private boolean findgroup=false;
    
    void setFindGroup(boolean b){
        if(!b){
            current =null;
        }
        findgroup=b;
    }
    //自分で終了判定をすることはないみたいなのでコメントアウト。
//    @Override
//    protected boolean finished(){
//        return finished;
//    }
    
    public Token append(char c,CharCategory cc){
        if(current!=null){
            Token ret = current.append(c,cc);
            consumed = current.consumed;
            if(current.finished())current = null;
            return ret;
        }
        consumed = true;
        switch(cc){
            case ESCAPE:
                current =new EscapeFactory(c,define);
                break;
            case IGNORE:
                break;
            case BEGINGROUP://ESCAPEの上にあったけど、たぶんミスじゃない？
                if(findgroup){
                    current = new VerbBlock(c,define);
                    break;
                }
            default:
                return new Token(c);
        }
        return null;
    }
    
    static class VerbBlock extends TokenFactory{
        boolean finished=false;
        int readed = 0;
        char start;
        StringBuilder sb=new StringBuilder(),alls = new StringBuilder();
        
        public VerbBlock(char c,CharCategoryDefine def){
            super(def);
            start=c;
            alls.append(c);
        }
        @Override
        protected boolean finished(){
            return finished;
        }
        
        public Token append(char c,CharCategory cc){
            readed++;
            if(readed>1000){
                consumed=false;
                return new Token(TokenType.UNDEFINED,alls.toString());
            }
            switch(cc){
                case BEGINGROUP:
                case AND:
                case NEWLINE:
                case DOLLAR:
                case PARAMETER:
                case ESCAPE:
                case COMMENT_START:
                case UNDERBAR:
                case UPPER:
                    consumed=false;
                    return new Token(TokenType.UNDEFINED,alls.toString());
                case ALPHABET:
                case OTHERCHARACTOR:
                    alls.append(c);
                    sb.append(c);
                    consumed=true;
                    return null;
                case ENDGROUP:
                    consumed=true;
                    alls.append(c);
                    Token tt=new Token(TokenType.__VERB__,alls.toString());
                    tt.setTokenChain(Token.toCharTokenChain(sb.toString()));
                    return tt;
                case SPACE:
                    alls.append(c);
                case IGNORE:
                    consumed=true;
                    return null;
                default:
                    consumed=false;
                    return new Token(TokenType.UNDEFINED);
            }
        }
    }
    
}
/*
 * \verb+～+の処理
 */
class VerbCommandFactory extends TokenFactory{
    char first;//\verb+～+では+だと思う
    boolean definedfirst=false;
    boolean end = false;
    VerbCommandFactory(CharCategoryDefine def){
        super(def);
    }
    @Override
    protected boolean finished(){
        return end;
    }
    @Override
    public Token append(char c,CharCategory cc){
        consumed = true;
        if(!definedfirst){
            if(cc != CharCategory.IGNORE){
                first = c;
                definedfirst = true;
            }
        }else{
            if(c==first){
                end = true;
                return null;
            }
            switch(cc){
                case IGNORE:
                    break;
                case NEWLINE:
                    end =true;
                    System.err.println("verbの途中で改行が現れました。verbを強制終了します");
                    return null;
                default:
                    return new Token(TokenType.CHAR,c);
            }
        }
        return null;
    }
}
//#1 ～#9 の処理
class ArgumentFactory extends TokenFactory{
    char ch;
    public ArgumentFactory(char c,CharCategoryDefine def){
        super(def);
        ch = c;
    }
    
    boolean finished = false;
    @Override
    protected boolean finished(){
        return finished;
    }
    
    @Override
    public Token append(char c,CharCategory cc){
        if(cc==CharCategory.OTHERCHARACTOR && (c >= '1' && c <= '9')){
            consumed = true;
            finished=true;
            return new Token(TokenType.ARGUMENT,new String(new char[]{ch,c}));
        }
        finished=true;
        consumed = false;
        return new Token(TokenType.CHAR,ch);
    }
    public String debag(){
        return "ArgumentFactory";
    }
}
//一回の改行か二回改行か
class NewParaFactory extends TokenFactory{
    char ch;
    public NewParaFactory(char c,CharCategoryDefine def){
        super(def);
        ch = c;
    }
    boolean finished = false;
    @Override
    protected boolean finished(){
        return finished;
    }
    @Override
    public Token append(char c,CharCategory cc){
        switch(cc){
            case NEWLINE:
                consumed = true;
                finished=true;
                Token t = new Token(TokenType.__TOKEN__GROUP__);
                TokenChain tc = new TokenChain();
                tc.addAll(new Token(TokenType.NEWLINE,ch),new Token(TokenType.NEWLINE,c)
                ,new Token(TokenType.ESCAPE, "\n\n"));
                t.setTokenChain(tc);
                return t;
        }
        finished=true;
        consumed = false;
        return new Token(TokenType.NEWLINE,ch);
    }
    public String debag(){
        return "NewParaFactory";
    }
}
//%で始まるコメント
class CommentFactory extends TokenFactory{
    StringBuilder sb,line;
    boolean multicomment=false;
    //%begincomment というコメントにすると複数行に出来るようにしてたらしい
    private static final Pattern begin=Pattern.compile("^%\\s*begincomment\\s*$");
    //%endcommentで複数行のコメントが終わるらしい
    private static final Pattern end = Pattern.compile("^%\\s*endcomment\\s*$");
    public CommentFactory(char ch,CharCategoryDefine def){
        super(def);
        sb = new StringBuilder();
        sb.append(ch);
    }
    boolean finished = false;
    @Override
    protected boolean finished(){
        return finished;
    }
    @Override
    public Token append(char c,CharCategory cc){
        consumed = true;
        if(multicomment)line.append(c);
        else sb.append(c);
        switch(cc){
            case IGNORE:
                finished=true;
                return new Token(TokenType.COMMENT,sb.toString());
            case NEWLINE:
                if(multicomment){
                    sb.append(line);
                    Matcher m = end.matcher(line);
                    if(m.find()){
                        finished=true;
                        return new Token(TokenType.COMMENT,sb.toString());
                    }
                    line.setLength(0);
                }else{
                    Matcher m = begin.matcher(sb);
                    if(m.find()){
                        multicomment=true;
                        line = new StringBuilder();
                        sb.append("\n");
                        break;
                    }
                    finished=true;
                    return new Token(TokenType.COMMENT,sb.toString());
                }
        }
        return null;
    }
    public String debag(){
        return "CommentFactory";
    }
}
//\escapeの処理
class EscapeFactory extends TokenFactory{
    StringBuilder sb;
    public EscapeFactory(char ch,CharCategoryDefine def){
        super(def);
        sb = new StringBuilder();
        sb.append(ch);
    }
    boolean finished = false;
    @Override
    protected boolean finished(){
        return finished;
    }
    
    @Override
    public Token append(char c,CharCategory cc){
        switch(cc){
            case ALPHABET:
                sb.append(c);
                consumed=true;
                return null;
            case SPACE:
                consumed=true;
                boolean endset = true;
                if(sb.length()==1){
                    sb.append(c);
                    endset = false;
                }
                finished = true;
                Token ret= new Token(TokenType.ESCAPE, sb.toString());
                if(endset)ret.setTokenEndChar(c);
                return ret;
            case NEWLINE:
                finished = true;
                consumed=false;
                return new Token(TokenType.ESCAPE,sb.toString());
            case IGNORE:
                finished=true;
                consumed = true;
                return new Token(TokenType.ESCAPE,sb.toString());
            case AND:
            case PARAMETER:
            case BEGINGROUP:
            case COMMENT_START:
            case DOLLAR:
            case ENDGROUP:
            case ESCAPE:
            case UNDERBAR:
            case UPPER:
            case OTHERCHARACTOR:
                if(sb.length()==1){
                    consumed = true;
                    sb.append(c);
                    finished=true;
                    return new Token(TokenType.ESCAPE, sb.toString());
                }
            default:
                finished=true;
                consumed = false;
                return new Token(TokenType.ESCAPE, sb.toString());
        }
    }
    public String debag(){
        return "EscapeFactory";
    }
}

