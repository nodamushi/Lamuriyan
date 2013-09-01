package lamuriyan.html;

import lamuriyan.parser.node.LmElement;
import lamuriyan.parser.node.LmNode;
import lamuriyan.parser.node.LmTextNode;
import lamuriyan.parser.node.env.RootDocument;
import lamuriyan.parser.node.env.TextEnvironment;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;


public class HTMLParseConverter implements Converter{

    public HTMLParseConverter(String str){
    }
    
    @Override
    public boolean acceptable(LmNode node ,HTMLConverter source ,
            RootDocument root,String property){
        return true;
    }

    @Override
    public Node convert(LmNode convertnode ,HTMLConverter source ,
            RootDocument root,String property) throws Exception{
        String value=findText(convertnode);
        DocumentFragment n = source.parseHTML(value);
        return n;
    }
    
    protected String findText(LmNode node){
        StringBuilder sb = new StringBuilder();
        findText(node,sb);
        return sb.toString();
    }
    
    private void findText(LmNode node,StringBuilder sb){
        if(!node.isElement()){
            if(LmTextNode.TEXT_NODE_NAME.equals(node.getName()))
                sb.append(node.getValue());
            return;
        }
        LmElement e = (LmElement)node;
        for(LmNode n:e.getChildren()){
            if(n instanceof LmTextNode){
                sb.append(n.getValue());
            }
            else if(n.isElement()){
                 findText(n,sb);
            }
        }
    }

}
