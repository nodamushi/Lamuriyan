package lamuriyan.html;

import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lamuriyan.parser.node.LmNode;
import lamuriyan.parser.node.env.RootDocument;


import org.w3c.dom.Node;



public class GroovyConverter implements Converter{
    
    private GroovyScriptEngine engine;
    private Script script;
    private Path path;
    //pathが通っていない場合はエラーになってGroovyConverterは実行されません。
    public GroovyConverter(String scriptPath) 
            throws ClassNotFoundException, IOException, 
            ResourceException, ScriptException{
        //インスタンスを作る前にエラーチェック
        Class.forName("groovy.util.GroovyScriptEngine");
        path = Paths.get(scriptPath).toAbsolutePath();
        Path dir = path.subpath(0, path.getNameCount()-1);
        Path fname = path.getFileName();
        if(!Files.isReadable(path))throw new IOException();
        engine = new GroovyScriptEngine(dir.toString());
        script = engine.createScript(fname.toString(), null);
    }
    
    
    @Override
    public boolean acceptable(LmNode node ,HTMLConverter source ,
            RootDocument root ,String property){
        return true;
    }

    @Override
    public Node convert(LmNode convertnode ,HTMLConverter source ,
            RootDocument root ,String property) throws Exception{
        Binding bind = new Binding();
        bind.setVariable("convertnode", convertnode);
        bind.setVariable("source", source);
        bind.setVariable("root", root);
        bind.setVariable("property", property);
        script.setBinding(bind);
        Object o = script.run();
        if(o == null || o instanceof Node ){
            return (Node)o;
        }else{
            System.err.println("GroovyConverter:スクリプトの戻り値がNodeではありません。"+path);
            return null;
        }
    }

}
