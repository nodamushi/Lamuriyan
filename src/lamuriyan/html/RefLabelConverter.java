package lamuriyan.html;

import java.util.ArrayList;
import java.util.Collections;

import lamuriyan.parser.label.RefTarget;
import lamuriyan.parser.node.LmAttr;
import lamuriyan.parser.node.LmID;
import lamuriyan.parser.node.LmNode;
import lamuriyan.parser.node.env.RootDocument;


import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class RefLabelConverter implements Converter{

    private ArrayList<String> nolinks=new ArrayList<>();
    
    public RefLabelConverter(String str){
        if(!str.isEmpty()){
            String[] sp = str.split(",");
            Collections.addAll(nolinks, sp);
        }
    }
    
    @Override
    public boolean acceptable(LmNode node ,HTMLConverter source ,
            RootDocument root ,String property){
        LmAttr attr = node.getAttr("name");
        return attr!=null;
    }

    @Override
    public Node convert(LmNode convertnode ,HTMLConverter source ,
            RootDocument root ,String property) throws Exception{
        LmAttr attr = convertnode.getAttr("name");
        String name = attr.getValue();
        RefTarget target = root.getRefTarget(name);
        if(target==null){
            System.err.println("error:[RefLabelConverter]  \""+name+"\"という名前のラベルは見つかりませんでした。");
            return null;
        }
        
        LmID id =target.id;
        LmNode parent = id.parent();
        String parentname = source.getHTMLTagName(parent);
        LmAttr nolink = convertnode.getAttr("nolink");
        if(nolinks.contains(parentname) || (nolink!=null && "true".equals(nolink.getValue()))){
            String ba = convertnode.getValue();
            String value=target.refvalue;
            if(!ba.isEmpty()){
                String[] sp = ba.split(",",2);
                if(sp.length==1){
                    value = sp[0]+value;
                }else{
                    value = sp[0]+value+sp[1];
                }
            }
            return source.createText(value);
        }
        
        Attr ida = source.createAttr(id);
        Element a = source.createElement("a");
        Attr href = source.createAttr("href", "#"+ida.getValue());
        a.setAttributeNode(href);
        
        String ba = convertnode.getValue();
        String value=target.refvalue;
        if(!ba.isEmpty()){
            String[] sp = ba.split(",",2);
            if(sp.length==1){
                value = sp[0]+value;
            }else{
                value = sp[0]+value+sp[1];
            }
        }
        a.appendChild(source.createText(value));
        PageLinkObject ob = new PageLinkObject(ida, href, id);
        source.addRefLink(ob);
        return a;
    }

}
