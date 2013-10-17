package lamuriyan.html;

import java.util.List;

import lamuriyan.parser.node.LmAttr;
import lamuriyan.parser.node.LmElement;
import lamuriyan.parser.node.LmNode;
import lamuriyan.parser.node.env.MathEnvironment;
import lamuriyan.parser.node.env.RootDocument;


import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class MathModeConverter implements Converter{

    public MathModeConverter(String param){
    }
    
    
    @Override
    public boolean acceptable(LmNode node ,HTMLConverter source ,
            RootDocument root ,String property){
        return node instanceof LmElement;
    }

    @Override
    public Node convert(LmNode convertnode ,HTMLConverter source ,
            RootDocument root ,String property) throws Exception{
        LmElement e = (LmElement)convertnode;
        Element math = source.createElement("math");
        source.setAttrs(math, e);
        
        _convert(e, math, source, root);
        
        return math;
    }
    private Node _convert_node(LmNode node ,HTMLConverter source ,RootDocument root){
        
        String name = node.getName();
        if("mfrag".equals(name) && node.isElement()){
            LmElement e = (LmElement)node;
            int l = e.getChildLength();
            if(l==0){
                return null;
            }else{
                DocumentFragment df = source.createDocumentFragment();
                _convert((LmElement)node, df, source, root);
                if(df.getChildNodes().getLength()==1){
                    Node n = df.getFirstChild();
                    if(n instanceof Element)
                        source.setAttrs((Element)n, node);
                    return n;
                }else{
                    Element el = source.createElement("mrow");
                    source.setAttrs(el, node);
                    el.appendChild(df);
                    return el;
                }
            }
        }else{
            Node n = source.createNode(node);
            if(node.isElement()){
                _convert((LmElement)node, (Element)n, source, root);
            }
            return n;
        }
    }
    private boolean b(LmAttr a){
        return a!= null && "on".equals(a.getValue());
    }
    
    private void _convert(LmElement parent,Node parente ,HTMLConverter source ,RootDocument root){
        List<LmNode> list = parent.getChildren();
        for(int i=0,len = list.size();i<len;i++){
            LmNode node = list.get(i);
            if(source.isIgnore(node))continue;
            if(source.isSkipNode(node)){
                if(node.isElement()){
                    _convert((LmElement)node, parente, source, root);
                }
                continue;
            }
            if(source.getHTMLTagName(node).equals("meqnum")){
                if(!node.isElement()){
                    continue;
                }
                Element e = source.createElement("mtd");
                _convert((LmElement)node, e, source, root);
                if(parente.getChildNodes().getLength()==0){
                    parente.appendChild(e);
                }else{
                    parente.insertBefore(e, parente.getFirstChild());
                }
                continue;
            }
            LmAttr 
            subflag = node.getAttr("subflag"),
            supflag = node.getAttr("supflag"),
            muoflag = node.getAttr("muoflag");
            boolean sub=b(subflag),sup=b(supflag),muo = b(muoflag);
            
            //^や_の処理
            if(sub&&sup){
                System.err.println("一つのノードにsubflagとsupflag属性が両方とも定義されています。");
            }else if(sup||sub){
                if(i+1 <len){
                    LmNode next = list.get(i+1);
                    subflag = next.getAttr("subflag");
                    supflag = next.getAttr("supflag");
                    boolean nsub = b(subflag);
                    boolean nsup = b(supflag);
                    if(nsub&&nsup){
                        System.err.println("一つのノードにsubflagとsupflag属性が両方とも定義されています。");
                    }else if((sub && sub==nsub) ||(sup && sup==nsup)){ 
                        if(sub)System.err.println("\\subが連続して使われています");
                        else System.err.println("\\supが連続して使われています");
                    }else{
                        if((nsup||nsub)&&i+2 >= len){
                            System.err.println("subflag属性またはsupflag属性が定義されていますが、次の要素が見つかりません。");
                        }else{
                            //ようやく個々で処理開始
                            boolean b = sub||nsub;
                            boolean p = sup||nsup;
                            Element e;
                            Node nn = _convert_node(node, source, root);
                            boolean nnnull=nn==null;
                            if(nnnull){
                                nn = source.createElement("mi");//ダミーを入れりゃいいのかね？
                            }
                            if(b&&p){
                                LmNode nnext = list.get(i+2);
                                Node supnode;
                                Node subnode;
                                if(sup){
                                    supnode = _convert_node(node, source, root);
                                }else{
                                    supnode=_convert_node(nnext, source, root);
                                }
                                if(sub){
                                    subnode = _convert_node(next, source, root);
                                }else{
                                    subnode = _convert_node(nnext, source, root);
                                }
                                boolean nsupnull=supnode!=null;//not sup null
                                boolean nsubnull=subnode!=null;//not sub null
                                if(nsupnull&&nsubnull){
                                    e = source.createElement(muo?"munderover":"msubsup");
                                    e.appendChild(nn);
                                    e.appendChild(subnode);
                                    e.appendChild(supnode);
                                    parente.appendChild(e);
                                }else if(nsubnull&&!nsupnull){
                                    e = source.createElement(muo?"munder":"msub");
                                    e.appendChild(nn);
                                    e.appendChild(subnode);
                                    parente.appendChild(e);
                                }else if(!nsubnull && nsupnull){
                                    e = source.createElement(muo?"mover":"msup");
                                    e.appendChild(nn);
                                    e.appendChild(supnode);
                                    parente.appendChild(e);
                                }else{
                                    if(!nnnull){
                                        parente.appendChild(nn);
                                    }
                                }
                                i=i+2;
                            }else{
                                if(b){
                                    e = source.createElement(muo?"munder":"msub");
                                }else{
                                    e = source.createElement(muo?"mover":"msup");
                                }
                                Node nnn = _convert_node(next, source, root);
                                if(nnn==null){
                                    if(!nnnull){//そもそも空っぽじゃねーか
                                        parente.appendChild(nn);
                                    }
                                }else{
                                    e.appendChild(nn);
                                    e.appendChild(nnn);
                                    parente.appendChild(e);
                                }
                                i++;
                            }
                            
                        }
                    }
                }else{
                    System.err.println("subflag属性またはsupflag属性が定義されていますが、次の要素が見つかりません。");
                }
            }//end sub sup
            else{//_や^じゃない場合
                Node n = _convert_node(node, source, root);
                if(n!=null){
                    parente.appendChild(n);
                }
            }
        }//end for
    }
  
}
