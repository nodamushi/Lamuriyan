package lamuriyan.parser.node.env;

import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import javax.script.ScriptException;

import lamuriyan.parser.EnvironmentConstructor;
import lamuriyan.parser.EnvironmentFactory;
import lamuriyan.parser.LamuriyanEngine;
import lamuriyan.parser.io.LamuriyanFileUtilities;
import lamuriyan.parser.label.Label;
import lamuriyan.parser.label.RefTarget;
import lamuriyan.parser.node.LmElement;
import lamuriyan.parser.node.LmNode;
import lamuriyan.parser.node.LmTextNode;
import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;
import lamuriyan.parser.token.TokenType;
import nodamushi.hl.*;
import nodamushi.hl.NHLight.Result;
import nodamushi.hl.html.HTMLTemplateEngine;

public class NHLightEnvironment extends VerbatimEnvironment{
    
    private static final String envName = "code";//TODO 一時的にnhlightにしてるけど、codeに変更する
    private static final String LANGUAGE="language";
    private static final String FILEPROP="file";
    private static final String IDPROP="id";
    private static final String ENCODEPROP="encoding";
    private static final String TEMPLATEPROP="template";
    private static final String TEMPLATEENCODEPROP="template-encoding";
    private static final String TOKENDEFINEPROP="token-defin"; 
    private static final String CLASSNAMEPROP="class";
    private static final String EVENLINEPROP="evenclass";
    private static final String ODDLINEPROP="oddclass";
    private static HTMLTemplateEngine defaultTemplate = NHLight.getDefaultTemplate();
    
    public static final EnvironmentFactory factory;
    static{
        TokenChain beginProgram = new TokenChain();
        TokenChain option = new TokenChain();
        //beginProgram引数
        //[propname=value,propname=value...]{language}
        //
        //beginProgramの内容
        //\setprops#1
        //\setprop{language}#2
        beginProgram.add(Token.escape("\\debug"));
        beginProgram.addAll(
                Token.escape("\\setprops"),
                new Token(TokenType.BEGINGROUP, '{'),
                Token.Args1,
                new Token(TokenType.ENDGROUP,'}'),
                Token.escape("\\setprop"),
                new Token(TokenType.BEGINGROUP, '{')
                );
        beginProgram.addAll(Token.toCharToken(LANGUAGE));
        beginProgram.addAll(
                new Token(TokenType.ENDGROUP,'}'),
                new Token(TokenType.BEGINGROUP, '{'),
                Token.Args2,
                new Token(TokenType.ENDGROUP,'}')
                );
        beginProgram.add(Token.escape("\\debug"));
        EnvironmentConstructor constructor = new EnvironmentConstructor(){
            @Override
            public Environment create(LamuriyanEngine engine ,EnvironmentFactory factory){
                return new NHLightEnvironment(engine);
            }
        };
        factory = createFactory(envName, constructor, beginProgram, option, 2);
    }

    private String language;
    private String id,classname,evenlinename,oddlinename;
    private String tokenTypeClassNameDefine;
    private HTMLTemplateEngine template;
    
    
    
    public NHLightEnvironment(LamuriyanEngine engine){
        super(envName, engine);
    }

    @Override
    protected void init(){
        String s = getProperty(LANGUAGE);
        if(s!=null){
            language=s;
        }else language = "text";
        
        id = getProperty(IDPROP);
        tokenTypeClassNameDefine = getProperty(TOKENDEFINEPROP);
        classname = getProperty(CLASSNAMEPROP);
        evenlinename=getProperty(EVENLINEPROP);
        oddlinename=getProperty(ODDLINEPROP);
        
        
        s = getProperty(FILEPROP);
        
        if(s!=null){
            Path path=engine.searchFile(s);
            if(path!=null){
                String enc = getProperty(ENCODEPROP);
                try {
                    String str = LamuriyanFileUtilities.read(path.toFile(), enc);
                    getTextBuffer().append(str);
                } catch (UnsupportedCharsetException | IOException e) {
                    engine.printError(s+"ファイルの読み込みに失敗しました。error message;"+e.getMessage());
                }
            }
        }
        
        
        s=getProperty(TEMPLATEPROP);
        if(s!=null){
            Path path = engine.searchFile(s);
            if(path!=null){
                String enc = getProperty(TEMPLATEENCODEPROP);
                template = HTMLTemplateEngine.createEngine(path, enc);
            }
        }
        if(template==null){
            template = defaultTemplate;
        }
    }

    @Override
    protected void finish(){
        String text = getText();
        if(text.isEmpty())return;
        NHLight nhlight = new NHLight(language, text);
        nhlight.parse();
        try {
            Result ret=nhlight.convertToElement(template, tokenTypeClassNameDefine, null, id, classname, evenlinename, oddlinename);
            Element e = ret.element;
            LmNode n = toNode(e, ret.linenumbers);
            add(n);
        } catch (ScriptException e) {
            engine.printError("NHLightのテンプレート実行時にJavaScriptの例外が発生しました。"+e.getMessage());
        }
    }
    
    
    private LmNode toNode(Element n,Collection<Pair<String, Integer>> labeldata){
        
        
        Element e=(Element)n;
        
        String name = e.getNodeName();
        LmElement el = new LmElement(name);
        
        Map<String, Attr> attrs = e.getAttributes();
        for(String key:attrs.keySet()){
            String value = attrs.get(key).getValue();
            if(HTMLTemplateEngine.LabelAttrName.equals(key)){
                //ラベル処理
                String show=null;
                for(Pair<String, Integer> p:labeldata){
                    if(p.getA().equals(value)){
                        show = p.getB().toString();
                    }
                }
                if(show!=null){
                    Label label = new Label(el, show);
                    RefTarget reftarget = engine.createRefTarget(value, label);
                    engine.getDocument().addRefTarget(reftarget);
                }
                
            }else{
                el.setAttr(key, value);
            }
        }
        
        
        if(e.hasChildNodes()){
            for(Node nn:e.getChildNodes()){
                
                if(nn.getNodeType()==Node.TEXT_NODE){
                    LmTextNode node = new LmTextNode();
                    node.setValue(nn.getNodeValue());
                    el.add(node);
                }else
                    el.add(toNode((Element)nn, labeldata));
            }
        }
        
        return el;
    }


    @Override
    protected void currentChanged(){
        
    }



}
