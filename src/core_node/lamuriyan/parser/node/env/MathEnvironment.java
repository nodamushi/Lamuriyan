package lamuriyan.parser.node.env;

import java.util.List;

import lamuriyan.parser.Command;
import lamuriyan.parser.EnvironmentConstructor;
import lamuriyan.parser.EnvironmentFactory;
import lamuriyan.parser.LamuriyanEngine;
import lamuriyan.parser.macro.Function;
import lamuriyan.parser.macro.MathEscape;
import lamuriyan.parser.macro.Macro;
import lamuriyan.parser.macro.MacroProcess;
import lamuriyan.parser.node.LmNode;
import lamuriyan.parser.node.LmTextNode;
import lamuriyan.parser.token.Char;
import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;
import lamuriyan.parser.token.TokenType;
import lamuriyan.parser.token.Char.CharType;



public class MathEnvironment extends Environment{
    
    
    private static final EnvironmentConstructor inlineConstructor = new EnvironmentConstructor(){
        public Environment create(LamuriyanEngine engine ,EnvironmentFactory factory){
            return new MathEnvironment(factory.getName(), engine, true);
        }
    };
    
    private static final EnvironmentConstructor blockConstructor = new EnvironmentConstructor(){
        public Environment create(LamuriyanEngine engine ,EnvironmentFactory factory){
            return new MathEnvironment(factory.getName(),engine,false);
        }
    };
    
    public static final EnvironmentFactory inlineFactory =
            new EnvironmentFactory("\\inlinemath\\", inlineConstructor, 0, null, null, null);
    public static final EnvironmentFactory blockFactory =
            new EnvironmentFactory("\\blockmath\\", blockConstructor, 0, null, null, null);
    
    private static Function begingroup = new Function(){
        TokenChain tc = new TokenChain();
        {
            tc.addAll(new Token(TokenType.ESCAPE,"mgfrag"),new Token(TokenType.BEGINGROUP,'{'));
        }
        public Object run(LamuriyanEngine engine) throws Exception{
            return tc;
        }
    };

    private static Function relax = new Function(){
        public Object run(LamuriyanEngine engine) throws Exception{
            return null;
        }
    };
    
    /**
     * MathCell用のコンストラクタ。　何にもしない。
     * @param name
     * @param staticList
     * @param engine
     */
    protected MathEnvironment(String name,LamuriyanEngine engine){
        super(name,engine);
    }

    public MathEnvironment(String name, LamuriyanEngine engine,boolean inline){
        super(name, engine);
        setInline(inline);
        setProperty(TAGNAMECOMMAND, "math");
        setAttr( "xmlns","http://www.w3.org/1998/Math/MathML");
        if(!inline){
            setAttr("display", "block");
        }
        
        //{でグループが開始された後に\mfragを追加し、{の中身をmfragで囲う。
        //最後に}を追加する為に、いったんmgfragというコマンドを通す。
        engine.defineCommand(new Command(Token.BEGINGROUP.toString(),begingroup));
        
        TokenChain arg = new TokenChain();
        arg.add(Token.Args1);
        final TokenChain program=new TokenChain();
        program.addAll(new Token(TokenType.ESCAPE,"\\mfrag"),new Token(TokenType.BEGINGROUP,'{'),
                Token.MATHBEGINGROUP,//ユーザー用のフック
                Token.Args1,new Token(TokenType.ENDGROUP,'}'),
                new Token(TokenType.ENDGROUP,'}')//グループを閉じる為に}をもう一つ追加。
        );
        Macro mgfrag = new Macro("mgfrag", program, arg,null);
        engine.defineCommand(new Command("mgfrag",mgfrag));
        
        //{の後の処理を変更させることが出来ないように、everygegingroupを別関数に置き換える。
        //このためだけに作ったbegingroupフックだが、一応ユーザー定義できるように、Token.MATHBEGINGROUPを定義するようにする。
        arg.clear();
        arg.addAll(new Token(TokenType.CHAR,'='),Token.Args1);
        Macro everybegingroup = new Macro("\\everybegingroup",null,arg,new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t=args.get(0);
                switch (t.getType()) {
                    case __TOKEN__GROUP__:
                        return new Command(Token.MATHBEGINGROUP.toString(),t.getTokenChain());
                    default:
                        return new Command(Token.MATHBEGINGROUP.toString(),t);
                }
            }
        });
        engine.defineCommand(new Command(everybegingroup.toString(),everybegingroup));
        //デフォルトの{の後の処理。engineにdefineRelaxとか作るべきかなぁ
        engine.defineCommand(new Command(Token.MATHBEGINGROUP.toString(),relax));
        //デフォルトでの定義は&amp;であり、インプット時に全部ばらっバラにされてしまうので、再定義。
        engine.defineCommand(new Command("\\&",new MathEscape("&", "mo", false,null)));
        engine.defineCommand(new Command("\\backslash",new MathEscape("\\", "mo", false,null)));
        
        Command sp = engine.getCommand("\\sp");
        Command sb = engine.getCommand("\\sb");
        Command mathbgroup = engine.getCommand("\\@mathbgroup");
        Command mathegroup = engine.getCommand("\\@mathegroup");
        engine.defineCommand(sp.copyMacro("^"));
        engine.defineCommand(sb.copyMacro("_"));
        engine.defineCommand(mathbgroup.copyMacro("\\bgroup"));
        engine.defineCommand(mathegroup.copyMacro("\\egroup"));
        
    }
    
    private LmTextNode currentappendnode;
    private boolean chargroup=false;
    private boolean isnumber = false;
    private MathTokenType grouptype=null;
    private String underoverflag=null;
    private char beforenumber;
    
    public static enum MathTokenType{
        OPERATER,IDENTIFIER,NUMBER,TEXT
    }
    
    
    private void inputNumber(char c){
        beforenumber = c;
        if(isnumber){
            currentappendnode.append(c);
        }else{
            currentappendnode = new LmTextNode("mn");
            currentappendnode.append(c);
            super.add(currentappendnode,false);
        }
    }
    
    
    public void startGroupType(MathTokenType type){
        if(type==null)return;
        if(grouptype!=null){
            if(grouptype==type)return;//まぁ、同じだったらいいでしょ

            //重複して定義することは現在許可していないけど、スタック構造とかにした方がいいのかな？
            engine.printError("グループタイプを二重に定義することは出来ません。");
            return;
        }
        
        grouptype= type;
        chargroup =true;
        if(type!=MathTokenType.NUMBER || !isnumber){
            currentappendnode=null;
        }
    }
    
    public void endGroupType(){
        chargroup = false;
        grouptype = null;
    }
    
    private void createCurrentAppendNode(){
        switch(grouptype){
            case OPERATER:
                currentappendnode=new LmTextNode("mo");
                break;
            case IDENTIFIER:
                currentappendnode=new LmTextNode("mi");
                break;
            case NUMBER:
                currentappendnode = new LmTextNode("mn");
                break;
            case TEXT:
                currentappendnode = new LmTextNode("mtext");
                break;
        }
    }
    private boolean inputBeforeSpace=false;
    @Override
    public Token input(Char t){
        boolean isBeforeSpace=inputBeforeSpace;
        inputBeforeSpace=t.type==CharType.SPACE;
        switch(t.type){//charとmathescapeを処理する
            case CHAR:
                if(chargroup){//chargroupが定義されているときは、ひとまとまりに扱う。
                    if(currentappendnode==null){
                        createCurrentAppendNode();
                        super.add(currentappendnode,false);
                        if(underoverflag!=null){
                            currentappendnode.setAttr("muoflag","on");
                            underoverflag=null;
                        }
                    }
                    currentappendnode.append(t.ch);
                }else{//定義されていないときは一文字ずつばらす。数字は例外。
                    char c = t.ch;
                    if(isnumber){
                        if((c>='0'&&c<='9')||(c=='.' && c!=beforenumber)){//"."が二回連続で来た場合は数字と見なさない。
                            inputNumber(c);
                            break;
                        }
                    }
                    
                    if(c>='0'&&c<='9'){
                        inputNumber(c);
                        isnumber = true;//isnumberはメソッド呼び出しの後に変更する
                    }else{
                        if(c == '∞'){//無限は数値？記号？
                            inputNumber(c);
                            return null;
                        }
                        
                        isnumber = false;
                        switch(c){
                            //デフォルトでmoにする文字列達。
                            case '+':case '=':case '-':case '/':
                            case '<':case '>':
                            case '(':case ')':case '{':case '}':
                            case '[':case ']':case ',':case '\'':
                            case '!': case'|': case'*':
                                //二バイト文字列 「きごう」で変換して出てきたのを片っ端から登録しただけ
                                //環境依存文字でnodamushiに見えない物も登録していない。
                                //ちなみに、私は「ゆたぽん（コーディング）」フォントを愛用
                            case '∫':case '∑':case '±':case '≦':case '≧':case '≪':case '≫':
                            case '×':case '…':case '≠':case '≒':case '≡':case '∞':case '∃':
                            case '∇':case '∴':case '∮':case '⊥':case '∠':case '∟':case '⊿':
                            case '∵':case '∩':case '∪':case '∈':case '∋':case '⊆':case '⊇':
                            case '⊂':case '⊃':case '∧':case '∨':case '￢':case '⇒':case '∀':
                            case '⇔':case '⌒':case '∂':case '∽':case '∝':case '∬':case '☆':
                            case '→':case '←':case '↑':case '↓':case '∥':case '△':case '▽':
                            case '～':
                                LmTextNode n=new LmTextNode("mo",1);
                                n.append(c);
                                super.add(n,false);
                                break;
                            default:
                                n=new LmTextNode("mi",1);
                                n.append(c);
                                super.add(n,false);
                                break;
                        }
                    }
                    
                }
                break;
                
                
            case MATHESCAPE://tex.decoder.parser.macro.MathEscape参照
                if(chargroup){
                    if(currentappendnode==null){
                        createCurrentAppendNode();
                        super.add(currentappendnode,false);
                    }
                    currentappendnode.append(t.mathescape.value);
                    if(underoverflag!=null){
                        currentappendnode.setAttr("muoflag","on");
                        underoverflag=null;
                    }
                }else{
                    isnumber = false;
                    currentappendnode=null;
                    LmTextNode node = new LmTextNode(t.mathescape.type,t.mathescape.value.length());
                    node.setValue(t.mathescape.value);
                    if(t.mathescape.isOverUnder){
                        node.setAttr("muoflag","on");
                    }
                    if(t.mathescape.mathvariantAttr!=null){
                        node.setAttr("mathvariant",t.mathescape.mathvariantAttr);
                    }
                    super.add(node,false);
                }
                break;
            case SPACE:
                if(chargroup){
                    if(grouptype==MathTokenType.TEXT && !isBeforeSpace){
//                        currentappendnode.append(t.ch);
                        currentappendnode=null;
                        LmNode n;
                        super.add(n=new LmNode("mspace"), false);
                        n.setAttr("width", "1ex");
                    }
                }else{
                    isnumber=false;
                    currentappendnode=null;
                }
        }
        return null;
    }

    @Override
    public boolean add(LmNode n ,boolean moveCurrent){
        isnumber=false;
        currentappendnode=null;
        underoverflag=null;
        return super.add(n, moveCurrent);
    }
    
    public void  underoverFlag(){
        if(chargroup&&currentappendnode!=null){
            currentappendnode.setAttr("muoflag","on");
            underoverflag=null;
        }else
            underoverflag="on";
    }
    
    
    @Override
    protected void currentChanged(){
        
    }
    
    @Override
    public boolean isAcceptDisplayNode(){
        return true;
    }
}
