package lamuriyan.parser;

import static java.util.Objects.*;

import java.util.*;

import lamuriyan.parser.macro.*;
import lamuriyan.parser.macro.Define_Macro.IFMacro;
import lamuriyan.parser.macro.Define_Macro.TDef;
import lamuriyan.parser.macro.Macro.MacroDFA;
import lamuriyan.parser.node.env.Environment;
import lamuriyan.parser.node.env.TextEnvironment;
import lamuriyan.parser.token.Char;
import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;
import lamuriyan.parser.token.TokenType;

//展開に関する内容のほとんどをここに集約した成果やばいぐらい複雑。addメソッドが展開の基本メソッド

/**
 * 文字列の展開を行う為の領域。
 * @author nodamushi
 *
 */
public class ExpandArea implements CommandMap{
    
    public static class ExpandAfter{
        public TokenChain token;
    }
    
    private TokenChain chain=new TokenChain();
    private TokenChain result = new TokenChain();
    private TokenChain _result;//数値解析モードの時、一時的に移す場所
    private Token before;//何の為に用意したのかもう忘れた………
    private boolean isPlain=true;
    private Macro macro;
    private MacroDFA automaton;
    private LamuriyanEngine engine;
    private boolean useNumber;
    private ExpandAreaOperator operator;
    private int ifdepth=0;
    private boolean defglobalflag=false;
    private boolean gotoElse=false;
    private boolean gotoFI;
    private int gotoDepth=-1;//gotoElse,gotoFIで、このgotoDepthと同じifdepthになったら終わる
    private boolean returntrue_if_run_Function = false;
    //ifの返値を保存
    private ArrayDeque<Boolean> ifresultStack=new ArrayDeque<>();
    
    public LamuriyanEngine getEngine(){
        return engine;
    }
    
    
    public ExpandAreaOperator getOperator(){
        return operator;
    }
    
    public TokenChain getTokens(){
        return result;
    }
    
    
    public boolean hasResultTokens(){
        return result.size()!=0;
    }
    
    public void stateClear(){
        result.clear();
    }
    
    
    public boolean neadBlock(){return !isPlain;}
    
    
    public void setUseNumber(){
        useNumber = true;
    }
    
    
    public ExpandArea(LamuriyanEngine en){
        this(null,en,null);
    }
    
    public ExpandArea(CommandMap e,LamuriyanEngine en,ExpandAreaOperator op){
        if(e==null){
            current=global=new GlobalCommandMap();
        }else{
            current = global = e;
        }
        engine = requireNonNull(en);
        operator=op==null?LamuriyanEngine.DefaultExpandAreaOperator:op;
    }
    
    private int blockDepthforMacro = 0;
    private TokenChain blockValueforMacro = null;
    private boolean notNextEscapeExpand=false;
    private Token blockforMacro = null;
    private StringBuilder numberchar = new StringBuilder();
    private boolean currentIsNumber = false;
    private ExpandArea numberExpandArea=null;
    private TokenChain numberExpandBuffer = new TokenChain();
    
    
    private boolean runMacro(Token t) throws Exception{
        if(macro.isUseNumber()){//数値をとるマクロだけは展開が特殊
            if(t.getType() == TokenType.__EXPANDMARKER__)return false;
            //コマンドシーケンスか、グループの展開
            if(numberExpandArea!=null){
                if(numberExpandArea.add(t)){
                    TokenChain tc = numberExpandArea.getTokens();
                    if(tc.size()==0){
                        numberExpandArea = null;
                        //何も結果が返ってこなくて、
                        //現在数字だった場合は、ここまでが数字
                        if(currentIsNumber){
                            currentIsNumber=false;
                            numberExpandBuffer.add(new Token(TokenType.__NUMBER__,
                                    numberchar.toString()));
                            return true;
                        }
                    }else{
                        Token first = tc.get(0);
                        numberExpandBuffer.add(new Token(TokenType.__EXPANDMARKER__));
                        numberExpandBuffer.addAll(tc);
                        
                        //__EXPANDMARKER__が返ってくるときは、
                        //numberExpandAreaも数値を扱うマクロを展開してるとき
                        //そうじゃないときは展開が終了しているとき。
                        if(first.getType() != TokenType.__EXPANDMARKER__){
                            numberExpandArea=null;
                        }else{
                            numberExpandArea.stateClear();
                        }
                        return true;
                    }
                }
                return false;
            }
            boolean notexpand = notNextEscapeExpand;
            notNextEscapeExpand=false;
            switch(t.getType()){
                case CHAR://普通の文字列の時だけinputに通す。それ以外の時は__EXPANDMARKER__を先頭に付けて出力
                    char c = t.getChar();
                    if(c>='0' && c<='9'){
                        if(currentIsNumber){
                            numberchar.append(c);
                            return false;
                        }else{
                            currentIsNumber = true;
                            numberchar.setLength(0);
                            numberchar.append(c);
                            return false;
                        }
                    }if(c == '-'){//マイナス
                        if(currentIsNumber){
                            numberExpandBuffer.add(new Token(TokenType.__EXPANDMARKER__));
                            if(numberchar.length()==1 && numberchar.charAt(0)=='-'){
                                numberExpandBuffer.add(new Token('-'));
                            }else{
                                numberExpandBuffer.add(new Token(TokenType.__NUMBER__,
                                        numberchar.toString()));
                            }
                            currentIsNumber = true;
                            numberchar.setLength(0);
                            numberchar.append(c);
                            return true;
                        }else{
                            currentIsNumber = true;
                            numberchar.setLength(0);
                            numberchar.append(c);
                            return false;
                        }
                    }
                    
                    if(c == '`'){
                        notNextEscapeExpand=true;
                        return false;
                    }
                    break;
                case BEGINGROUP://{}を展開する必要があれば展開する
                    if(!macro.isUseBlock()){
                        break;
                    }
                case ESCAPE://展開する
                    if(notexpand){
                        break;
                    }
                    Command com = getCommand(t.toString());
                    if(com!=null && com.getAsMacro() instanceof IFMacro){
                        break;
                    }
                    numberExpandArea = new ExpandArea(this, engine, operator);
                    numberExpandArea.setUseNumber();
                    numberExpandArea.returntrue_if_run_Function = true;
                    numberExpandBuffer.addAll(new Token(TokenType.__EXPANDMARKER__),t);
                    return true;
                case __NUMBER__://パターンマッチで困るので、Charに変換
                    t = new Token(TokenType.CHAR,t.toString());
                    t.setNumberFlag();
                    break;
            }
            
            //数字とは無関係だったが、
            //それまでが数字モードだった場合は、そこで、その数字は完結する
            if(currentIsNumber){
                numberExpandBuffer.add(new Token(TokenType.__EXPANDMARKER__));
                if(numberchar.length()==1 && numberchar.charAt(0)=='-'){
                    numberExpandBuffer.add(new Token('-'));
                }else{
                    numberExpandBuffer.add(new Token(TokenType.__NUMBER__,
                            numberchar.toString()));
                }
                if(t.getType() != TokenType.SPACE)//SPACEは無視する
                    numberExpandBuffer.add(t);
                currentIsNumber=false;
                return true;
            }
            
            //ここまでたどり着いた場合は、後は普通にマクロが実行できるかどうか確認する。
        }//数字を使うマクロの特殊処理おわり。
        
        
        switch(t.getType()){
            case BEGINGROUP:
                if(macro.isUseBlock()){
                    if(blockDepthforMacro == 0){
                        blockValueforMacro = new TokenChain();
                        blockforMacro = new Token(TokenType.__TOKEN__GROUP__,t.getChar());
                        blockforMacro.setTokenChain(blockValueforMacro);
                        blockDepthforMacro++;
                        return false;
                    }else{
                        blockDepthforMacro++;
                        blockValueforMacro.add(t);
                        return false;
                    }
                }
                break;
            case ENDGROUP:
                if(macro.isUseBlock()){
                    if(blockDepthforMacro == 1){
                        blockforMacro.setTokenEndChar(t.getChar());
                        blockDepthforMacro=0;
                        t=blockforMacro;
                        blockforMacro=null;
                        blockValueforMacro=null;
                        break;
                    }else if(blockDepthforMacro==0){
                        //error マクロ破棄
                        isPlain = true;
                        break;
                    }else{
                        blockValueforMacro.add(t);
                        blockDepthforMacro--;
                        return false;
                    }
                }
                break;
            case COMMENT:
            case UNDEFINED:
                return false;
            default:
                if(blockDepthforMacro!=0){
                    blockValueforMacro.add(t);
                    return false;
                }
        }
        chain.add(t);
        int i=automaton.input();
        if(i==1){//入力完了
            List<Token> args = automaton.getArguments();
            Object o =macro.run(engine, args);
            if(macro.hasError()){
                StringBuilder sb = new StringBuilder();
                for(String s:macro.getError()){
                    sb.append(s).append("\n");
                }
                engine.printError(sb.toString());
            }
            isPlain=true;
            Macro macro = this.macro;
            if(macro.isUseNumber()){
                result = _result;//元に戻す。
            }
            this.macro=null;
            automaton = null;
            boolean dglobal = defglobalflag;
            defglobalflag = false;
            return _result(o, dglobal, returntrue_if_run_Function, macro);
        }else if(i<0){//エラー
            engine.printError("マクロの引数エラーが発生しました:["+macro.getName()+"]  引数:"+chain.toString());
            defglobalflag=false;
            isPlain = true;
            macro =null;
            automaton = null;
        }
        return false;
    }
    
    private boolean _add(Token t) throws Exception{
        boolean inputed=false;
        boolean dglobal = defglobalflag;
        defglobalflag = false;//マクロじゃない場合はすぐにfalseに
        //引数のあるマクロを実行する場合はdglobalを使って戻す。
        
        Command command=null;
        switch(t.getType()){
            case BEGINGROUP:
                if(engine.shouldCreateTDefBlock()){
                    pushTDefBlock(CHARBLCOK);
                }else
                    pushBlock(CHARBLCOK);
                result.add(Token.BEGINGROUP);
                inputed=true;
                break;
            case BEGINMATHMODEGROUP:
                pushBlock(COMMANDBLOCK);
                break;
            case ENDGROUP:
                if(CHARBLCOK!=getCurrentBlockType()){
                    System.err.println("ブロックを閉じることが出来ません。現在のブロックの状態"+
                getCurrentBlockType()+" 閉じようとしたブロックの状態"+CHARBLCOK);
                }else{
                    inputed = returntrue_if_run_Function;
                    Token after = getAfterBlock();
                    popBlock(CHARBLCOK);
                    if(after!=null){
                        result.add(after);
                        inputed = true;
                    }
                }
                break;
            case ENDMATHMODEGROUP:
                if(COMMANDBLOCK!=getCurrentBlockType()){
                    System.err.println("ブロックを閉じることが出来ません。現在のブロックの状態"+
                            getCurrentBlockType()+" 閉じようとしたブロックの状態"+COMMANDBLOCK);
                }else{
                    popBlock(COMMANDBLOCK);
                }
                break;
            case NEWLINE:
                if(result.size()!=0){
                    inputed=false;
                    break;
                }
            case ARGUMENT:
            case CHAR:
                before = t;
                result.add(t);
                inputed=true;
                break;
                   
            case SPACE:
                inputed=true;
                result.add(t);
                before = t;
                break;
            case __TOKEN__GROUP__:
                result.addAll(t.getTokenChain());
                inputed = true;
                break;
            case ESCAPE:
                if(command==null)
                    command=getCommand(t.toString());
                if(command==null){
                    engine.printError(t.toString()+"が定義されていません\n");
                    return false;
                }
                Object obf= command.get();
                if(command.isMacro()){
                    Macro macro = command.getAsMacro();
                    
                    if(macro instanceof IFMacro){
                        ifdepth++;
                    }
                    
                    if(macro.isNoArgumentMacro()){
                        Object o=macro.run(engine, null);
                        return _result(o, dglobal, returntrue_if_run_Function, macro);
                    }else{
                        defglobalflag = dglobal;
                        chain.clear();
                        chain.add(Token.Undefined);
                        isPlain=false;
                        this.macro = macro;
                        automaton = macro.createAutomaton(chain);
                        if(macro.isUseNumber()){//resultをnumberExpandBufferに入れ替える。
                            _result = result;
                            result = numberExpandBuffer;
                            result.clear();
                        }
                    }
                }else if(command.isFunction()){
                    Function function = command.getAsFunction();
                    inputed = returntrue_if_run_Function;
                    try {
                        Object o=function.run(engine);
                        return _result(o, dglobal, inputed, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else if(obf == Define_Macro.VERBEMODE){
                    if(operator.canUseVarb() ){
                        if(t.getTokenEndChar()!=0){
                            operator.setVarb(t.getTokenEndChar());
                        }else{
                            operator.setVarb();
                        }
                    }
                }else if(obf==Define_Macro.DEFGLOBAL){
                    defglobalflag=true;
                }else if(obf==Define_Macro.ELSE){
                    if(ifresultStack.isEmpty()){
                        engine.printError(t.toString()+":elseがif構造の外で見つかりました。");
                    }else{
                        boolean b = ifresultStack.peek();
                        if(b){
                            gotoFI=true;
                            gotoElse = false;
                            gotoDepth=ifdepth;
                        }else{
                            engine.printError(t.toString()+":elseの構文エラーが見つかりました。" +
                                    "おそらく\\fiの閉じ忘れと思われます。");
                        }
                    }
                }else if(obf==Define_Macro.FI){
                    if(ifresultStack.isEmpty()){
                        engine.printError(t.toString()+":ifがif構造の外で見つかりました。");
                    }else{
                        gotoFI=false;
                        gotoElse=false;
                        ifresultStack.pop();
                        ifdepth--;
                    }
                }else{
                    inputed = _result(obf, dglobal, true, null);
                }
                
//                if(obf instanceof TokenChain){
//                    result.addAll((TokenChain)obf);
//                    inputed=true;
//                }else if(obf instanceof Token){
//                    result.add((Token)obf);
//                    inputed = true;
//                }else if(obf instanceof MathEscape){
//                    result.add( new Token((MathEscape)obf));
//                    inputed = true;
//                }else if(command.isCounter() && useNumber){
//                    result.addAll(Token.toCharToken(command.getAsCounter().getAsString()));
//                    inputed=true;
//                }else if(command.isString()){
//                    result.addAll(Token.toCharToken(command.getAsString()));
//                    inputed=true;
//                }
                break;
        }
        
        return inputed;
    }
    
    private boolean skipIF(Token t){
        if(gotoElse){//elseまでスキップ。ただし、fiが来たら終わる
            if(t.getType()==TokenType.ESCAPE){
                Command c = engine.getCommand(t.toString());
                if(c!=null){
                    Object o = c.get();
                    if(o instanceof IFMacro){
                        ifdepth++;
                    }else if(o == Define_Macro.FI){
                        if(gotoDepth == ifdepth){
                            gotoElse =false;
                            ifresultStack.pop();
                        }
                        ifdepth--;
                    }else if(o == Define_Macro.ELSE){
                        if(gotoDepth == ifdepth){
                            gotoElse = false;
                        }
                    }
                }
            }
            return false;
        }else{//fiまでスキップ
            if(t.getType()==TokenType.ESCAPE){
                Command c = engine.getCommand(t.toString());
                if(c!=null){
                    Object o = c.get();
                    if(o instanceof IFMacro){
                        ifdepth++;
                    }else if(o == Define_Macro.FI){
                        if(gotoDepth == ifdepth){
                            gotoFI =false;
                            ifresultStack.pop();
                        }
                        ifdepth--;
                    }
                }
            }
            return false;
        }
    }
    /**
     * 
     * @param t
     * @return resultにトークンが出力された場合trueが返ります。
     * @throws Exception
     */
    public boolean add(Token t) throws Exception{
        //if文処理でelseに飛ぶ必要があるか、fiに飛ぶ必要がある場合の処理。
        if(gotoElse||gotoFI){
            return skipIF(t);
        }
        
        if(isPlain){
            return _add(t);
        }else{//マクロ引数検索中の場合
            return runMacro(t);
        }
    }


    private boolean _result(Object o,boolean dglobal,boolean inputed,Macro macro) throws Exception{
        if(o==null)return inputed;
        if(o instanceof Token){
            Token tt = (Token)o;
            if(tt.getType()==TokenType.__TOKEN__GROUP__){
                result.addAll(tt.getTokenChain());
            }else
                result.add(tt);
            inputed = true;
        }else if(o instanceof TokenChain){
            result.addAll((TokenChain)o);
            inputed = true;
        }else if(o instanceof UserMacroProcess.MacroExpandData){
            Object[] arg = ((UserMacroProcess.MacroExpandData) o).getData();
            for(Object obj:arg){
                if(obj instanceof TokenChain){
                    result.addAll((TokenChain)obj);
                }else if(obj instanceof Token){
                    result.add((Token)obj);
                }
            }
            inputed = true;
            
        }else if(o instanceof Boolean){
            if(macro instanceof IFMacro){
                boolean bb = (Boolean)o;
                ifresultStack.push(bb);
                if(!bb){
                    gotoElse=true;
                    gotoDepth = ifdepth;
                }
            }
        }else if(o instanceof String){
            if(macro == engine.getOnVerbatimMacro()){
                operator.setVerbatim((String)o);
            }else{
                result.addAll(Token.toCharToken((String)o));
                inputed = true;
            }
        }else if(o instanceof Command){
            if(dglobal)
                setGlobalCommand((Command)o);
            else
                setCommand((Command)o);
        }else if(o instanceof Command[]){
            if(dglobal)for(Command c:(Command[])o)
                setGlobalCommand(c);
            else for(Command c:(Command[])o)
                setCommand(c);
        }else if(o instanceof Macro){//引数なしのマクロやifは絶対に返さないことがルール
            macro = (Macro)o;
            
            if(macro instanceof IFMacro){
                ifdepth++;
            }
            
            if(macro.isNoArgumentMacro()){
                return _result(macro.run(engine, null), dglobal, returntrue_if_run_Function, macro);
            }else{
                defglobalflag = dglobal;
                chain.clear();
                chain.add(Token.Undefined);
                isPlain=false;
                this.macro = macro;
                automaton = macro.createAutomaton(chain);
                if(macro.isUseNumber()){//resultをnumberExpandBufferに入れ替える。
                    _result = result;
                    result = numberExpandBuffer;
                    result.clear();
                }
            }
        }else if(o instanceof Counter && useNumber){
            result.addAll(Token.toCharToken(((Counter)o).getAsString()));
            inputed = true;
        }else if(o instanceof ExpandAfter){//resultに追加はするがinputedフラグは立たない
            result.addAll(((ExpandAfter)o).token);
        }else if(o instanceof MathEscape){
            result.add( new Token((MathEscape)o));
            inputed = true;
        }else if(o instanceof TDef){
            engine.startTDefBlockMode(((TDef) o).name,dglobal);
        }
        return inputed;
    }
    
    
    
    
    
    /////command map---------------------------------------------------------------------------
    
    private ArrayDeque<CommandMap> cmstack = new ArrayDeque<>();
    private CommandMap current;
    private final CommandMap global;
    
    
    @Override
    public Command getCommand(String name){
        Command c= current.getCommand(name);
        if(c==null){
            for(CommandMap map:cmstack){
                c = map.getCommand(name);
                if(c!=null)return c;
            }
        }
        return c;
    }



    @Override
    public void setCommand(Command com ){
        if(Char.isFontPropName(com.getName())){
            if(!com.isString()){
                return;
            }
        }
        if("\\useparagraph".equals(com.getName())&&!(com.get() instanceof String)){
            engine.printError("useparagraph がマクロ");
        }
        current.setCommand(com);
    }


    @Override
    public void setGlobalCommand(Command com){
        if(Char.isFontPropName(com.getName())){
            if(!com.isString()){
                return;
            }
        }
        global.setGlobalCommand(com);
    }

    @Override
    public void removeCommand(String name){
        if(Char.isFontPropName(name)&& current == global){
            return;
        }
        current.removeCommand(name);
    }

    
    private void pushTDefBlock(int blocktype){
        if(blocktype!=CHARBLCOK && blocktype!=COMMANDBLOCK )return;
        engine.pushNewCharCategoryBlock();
        TDefBlock map = new TDefBlock(blocktype);
        if(engine.setTDefBlock(map)){
            cmstack.push(current);
            current = map;
        }else{
            BlockCommandMap bmap = new BlockCommandMap(CHARBLCOK);
            cmstack.push(current);
            current = bmap;
        }
    }

    @Override
    public void pushBlock(int blocktype){
        if(blocktype!=CHARBLCOK && blocktype!=COMMANDBLOCK && blocktype!=ENVIRONMENTBLOCK)return;
        engine.pushNewCharCategoryBlock();
        BlockCommandMap map = new BlockCommandMap(blocktype);
        cmstack.push(current);
        current = map;
    }


    @Override
    public void popBlock(int blocktype){
        if(getCurrentBlockType()!=blocktype)engine.printError(
                "閉じるブロックのタイプが合っていません。現在の状態"+getCurrentBlockType()+" 引数："+blocktype);
        if(blocktype==GLOBALBLOCK)engine.printError(
                "グローバルブロックは閉じることが出来ません。");
        engine.popCharCategoryBlock();
        CommandMap old = current;
        current = cmstack.pop();
        old.popped(engine);
        Command c =getCommand("\\useparagraph");
        if(c!=null){
            Environment e=engine.getCurrentEnvironment();
            if(e instanceof TextEnvironment){
                TextEnvironment te =((TextEnvironment) e);
                if("f".equals(c.getAsString())){
                    te.setEnableParagraph(false);
                }else if("t".equals(c.getAsString())){
                    te.setEnableParagraph(true);
                }
            }
        }
    }


    @Override
    public int getCurrentBlockType(){
        return current.getCurrentBlockType();
    }

    @Override
    public Token getAfterBlock(){
        return current.getAfterBlock();
    }
    @Override
    public void setAfterBlock(Token t){
        current.setAfterBlock(t);
    }
    
    
    @Override
    public void popped(LamuriyanEngine engine){
    }
    
}
