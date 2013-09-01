package lamuriyan.parser.node.env;

import java.util.List;

import lamuriyan.parser.Command;
import lamuriyan.parser.EnvironmentConstructor;
import lamuriyan.parser.EnvironmentFactory;
import lamuriyan.parser.LamuriyanEngine;
import lamuriyan.parser.macro.Macro;
import lamuriyan.parser.macro.MacroProcess;
import lamuriyan.parser.node.LmElement;
import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;




//最初の頃、よく分かってないうちに作ってしまった過去の遺物だけど、まぁ、動くからよし、修正面倒くさいということで残ってる。
public class ListEnvironment extends TextEnvironment{

    private static MacroProcess process = new MacroProcess(){
        
        @Override
        protected Object _run(LamuriyanEngine engine ,List<Token> args)
                throws Exception{
            Environment e=engine.getCurrentEnvironment();
            if (e instanceof ListEnvironment) {
                ListEnvironment l = (ListEnvironment) e;
                l.newListItem();
            }
            return null;
        }
    };
    
    private static Macro macro = new Macro("\\item", null, null, process);
    private static Command com = new Command(macro.getName(), macro);
    
    public static final EnvironmentConstructor creater=new EnvironmentConstructor(){
        @Override
        public Environment create(LamuriyanEngine engine ,EnvironmentFactory factory){
            engine.defineCommand(com);
            return new ListEnvironment(factory.getName(), engine);
        }
    };
    
    
    public static final EnvironmentFactory factory;
    static{
        TokenChain t = new TokenChain();
        factory = new EnvironmentFactory("list", creater, 0, t, t, null);
    }
    
    
//    private TeXElement currentLi = null;

    public ListEnvironment(String name,LamuriyanEngine engine){
        super(name,  engine);
        setTagName(name);
    }
    
    
    public ListEnvironment(LamuriyanEngine engine){
        super("list",  engine);
        setTagName("ol");
    }
    
    
    
    
    public void newListItem(){
        LmElement li = new LmElement("li");
        li.setInline(false);
        superadd(li);
        current = li;
//        currentLi = li;
        currentTextNode=null;
        settingNode=current;
    }
    
    
    
}
