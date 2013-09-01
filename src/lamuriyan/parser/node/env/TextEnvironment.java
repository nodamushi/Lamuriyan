package lamuriyan.parser.node.env;

import lamuriyan.parser.Command;
import lamuriyan.parser.EnvironmentConstructor;
import lamuriyan.parser.EnvironmentFactory;
import lamuriyan.parser.LamuriyanEngine;
import lamuriyan.parser.node.SpanNode;
import lamuriyan.parser.node.LmElement;
import lamuriyan.parser.node.LmNode;
import lamuriyan.parser.node.LmTextNode;
import lamuriyan.parser.token.*;
import lamuriyan.parser.token.Char.CharType;

/**
 * テキストを追加できる環境。<br>
 * &lt;p>タグの生成を自動で行う
 * @author nodamushi
 *
 */
public class TextEnvironment extends Environment{
    
    
    public static final EnvironmentConstructor constructor = new EnvironmentConstructor(){
        @Override
        public Environment create(LamuriyanEngine engine ,EnvironmentFactory factory){
            TextEnvironment e = new TextEnvironment(factory.getName(), engine);
            return e;
        }
    };
    
    
    public static final String
    PRAGRAPH_ELEMENT_NAME ="p";
    
    protected SpanNode spannode;//textnodeを囲うspan
    protected LmTextNode currentTextNode;
    protected boolean useParagraph=true;
    public TextEnvironment(String name,LamuriyanEngine engine){
        super(name,  engine);
        Command c = engine.getCommand("\\useparagraph");
        if(c!=null && c.isString()){
            useParagraph = c.getAsString().endsWith("t");
        }
    }
    
    public void setEnableParagraph(boolean enable){
        if(useParagraph==enable)return;
        useParagraph = enable;
        if(!enable){
            if(PRAGRAPH_ELEMENT_NAME.equals(current.getName())){
                super.setCurrent_Parent();
            }
        }
    }
    
    public boolean isEnableParagraph(){
        return useParagraph;
    }
    
    /**渡されるtは必ずCharかSpace
     * @param t CharかSpaceのToken
     */
    private Token setText(Char t){
        if(useParagraph&&!inParagraph()){
            //<p>の生成
            makeNewParagraph();
            TokenChain tc = new TokenChain();
            tc.add(Token.NEWPARAGRAPH);
            tc.add(new Token(t.ch));
            Token ret = new Token(TokenType.__TOKEN__GROUP__);
            ret.setTokenChain(tc);
            return ret;
        }
        if(spannode!=null){
            //フォントの設定が直前までと同じかどうかチェック
            if(!spannode.isSameClassName(t)){
                currentTextNode=null;
                current=spannode.getParent();
                spannode=null;
            }
        }
        
        if(currentTextNode==null){
            LmTextNode ttn = new LmTextNode();
            if(t.hasFont()){
                if(spannode == null){
                    spannode = new SpanNode("span");
                    spannode.setFontProperty(t);
                    add(spannode,true);
                    spannode.add(ttn);
                }else{
                    current.add(ttn);
                }
            }else
                current.add(ttn);
            currentTextNode=ttn;
        }else if(t.hasFont() &&spannode == null){
            spannode = new SpanNode("span");
            spannode.setFontProperty(t);
            add(spannode,true);
            LmTextNode ttn = new LmTextNode();
            spannode.add(ttn);
            currentTextNode=ttn;
        }
        
        switch(t.ch){
            case '<':
                Command c = engine.getCommand("\\<");
                if(c!=null&&c.isString()){
                    currentTextNode.append(c.getAsString());
                }else
                    currentTextNode.append("&lt;");
                break;
            case '>':
                c = engine.getCommand("\\>");
                if(c!=null&&c.isString()){
                    currentTextNode.append(c.getAsString());
                }else
                    currentTextNode.append("&gt;");
                break;
            case '"':
                c=engine.getCommand("\\escapedquo");
                if(c!=null&&c.isString()){
                    currentTextNode.append(c.getAsString());
                }else 
                    currentTextNode.append("&quot;");
                break;
            case '\'':
                c = engine.getCommand("\\escapesequot");
                if(c!=null&&c.isString()){
                    currentTextNode.append(c.getAsString());
                }else
                    currentTextNode.append("&#039;");
                break;
            case '“':
                c=engine.getCommand("\\escapelquo");
                if(c!=null&&c.isString()){
                    currentTextNode.append(c.getAsString());
                }else 
                    currentTextNode.append("&ldquo;");
                break;
            case '”':
                c = engine.getCommand("\\escaperdquo");
                if(c!=null && c.isString()){
                    currentTextNode.append(c.getAsString());
                }else 
                    currentTextNode.append("&rdquo;");
                break;
            default:
                currentTextNode.append(t.ch);
        }
        return null;
        
    }
    
    
    public void newParagraphFrag(){
        if(useParagraph){
            LmElement e = getParagraph();
            if(e!=null){
                current = e.getParent();
            }
            currentTextNode=null;
            spannode=null;
        }
    }
    
    private void makeNewParagraph(){
        if(!useParagraph)return;
        LmElement paragraph = new LmElement(PRAGRAPH_ELEMENT_NAME);
        paragraph.setInline(false);
        paragraph.setAcceptDisplayNode(false);
        if(current==this)    
            superadd(paragraph);
        else
            current.add(paragraph);
        current = paragraph;
        currentTextNode = null;
        spannode = null;
    }
    
    private boolean inParagraph(){
        LmElement e = current;
        while(e!=this){
            if(PRAGRAPH_ELEMENT_NAME.equals(e.getName()))return true;
            e = e.getParent();
        }
        return false;
    }
    
    private LmElement getParagraph(){
        LmElement e = current;
        while(e!=this){
            if(PRAGRAPH_ELEMENT_NAME.equals(e.getName()))return e;
            e = e.getParent();
        }
        return null;
    }

    private boolean ignoreConsecutiveSpace=true;
    private boolean inputBeforeSpace=false;

    @Override
    public Token input(Char t){
        switch(t.type){
            case SPACE:
                //行頭のSPACEは無視
                if(PRAGRAPH_ELEMENT_NAME.equals(current.getName())){
                    if(current.getChildLength()==0)break;
                }else if(current == this && useParagraph)break;
                //直前に入力されたCharがSpaceで、無視する設定の場合は何もしない。
                if(inputBeforeSpace==true && ignoreConsecutiveSpace)break;
                //そうでなければcase CHARへ。
            case CHAR:
                inputBeforeSpace=t.type==CharType.SPACE;
                return setText(t);
        }
        return null;
    }
    
    @Override
    public void setCurrent_Parent(){
        if(PRAGRAPH_ELEMENT_NAME.equals(current.getName())){
            super.setCurrent_Parent();
            setCurrent_Parent();
            return;
        }else{
            super.setCurrent_Parent();
        }
    }
    
    /**
     * &lt;br>を挿入します。<br>
     * 次の行への高さをvspaceで設定することが出来ます。&lt;span>タグを利用して空白を作ります。<br>
     * nullもしくは空文字の場合は&lt;br>だけを生成します。
     * @param vspace
     */
    public void addBR(String vspace){
        LmNode br = new LmNode("br");
        add(br,false);
        if(vspace!=null && !vspace.isEmpty()){
            LmElement span = new LmElement("span");
            LmTextNode n = new LmTextNode();
            n.append("&nbsp;");
            span.setInline(true);
            span.setAttr("class","vspace");
            span.setAttr("style", "line-height:"+vspace);
            span.add(n);
            add(span,false);
            br = new LmNode("br");
            add(br,false);
        }
    }
    
    @Override
    public boolean add(LmNode n ,boolean moveCurrent){
        if(n==null)return false;
        if(!useParagraph){
            return super.add(n,moveCurrent);
        }
        //pはinline要素しか入れられない。あと、currentTextNodeもリセット
        if(inParagraph()){
            if(n.isInline()){
                current.add(n);
                currentTextNode = null;
                settingNode = n;
                if(moveCurrent &&n instanceof LmElement && !(n instanceof Environment)){
                    setCurrentElement((LmElement)n);
                }
                return true;
            }else{
                LmElement e = getParagraph();
                currentTextNode=null;
                spannode=null;
                if(e != this)
                    current = e.getParent();
                return add(n,moveCurrent);
            }
        }else{
            currentTextNode=null;
            if(n.isInline()){
                makeNewParagraph();
            }
            
            
            return super.add(n,moveCurrent);
        }
        
    }

    @Override
    protected void currentChanged(){
        currentTextNode = null;
        if(!inParagraph())
            newParagraphFrag();
    }
}
