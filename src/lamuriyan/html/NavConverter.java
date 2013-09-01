package lamuriyan.html;

import java.util.*;

import lamuriyan.parser.macro.IndexItem;
import lamuriyan.parser.node.LmAttr;
import lamuriyan.parser.node.LmElement;
import lamuriyan.parser.node.LmID;
import lamuriyan.parser.node.LmNode;
import lamuriyan.parser.node.env.RootDocument;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class NavConverter implements Converter{
    
    

    @Override
    public boolean acceptable(LmNode node ,HTMLConverter source ,
            RootDocument root ,String property){
        LmAttr
        type = node.getAttr("type"),
        depth = node.getAttr("depth"),
        scope = node.getAttr("scope");
        return type!=null && depth!=null && scope != null;
    }

    @Override
    public Node convert(LmNode node ,HTMLConverter source ,
            RootDocument root ,String property) throws Exception{
        LmAttr
        typea = node.getAttr("type"),
        deptha = node.getAttr("depth"),
        scopea = node.getAttr("scope"),
        titlea = node.getAttr("title")
//        column = node.getAttr("column")
        ;
        String
        typestr = typea.getValue(),
        depthstr =deptha.getValue(),
        scope = scopea.getValue();
        boolean all = !scope.equals("here");
        String[] type = typestr.split(",");
        String[] sp = depthstr.split("-",2);
        ArrayList<Integer> depth = new ArrayList<>();
        if(sp.length==1){
            depth.add(Integer.parseInt(depthstr));
        }else{
            int s = Integer.parseInt(sp[0]);
            int e =Integer.parseInt(sp[1]);
            if(e<s){
                int i = s;
                s= e;
                e = i;
            }
            for(int i=s;i<=e;i++){
                depth.add(i);
            }
        }
        List<IndexItem> items;
        if(all){
            items = _convert_all(node, source, root,type,depth);
        }else{
            items = _convert_here(node, source, root, type, depth);
        }
        
        
        Element nav = source.createElement("nav");
        if(titlea!=null){
            String title = titlea.getValue();
            Element header = source.createElement("header");
            Element h1 = source.createElement("h1");
            header.appendChild(h1);
            Text t = source.createText(title);
            h1.appendChild(t);
            nav.appendChild(header);
        }
//        int col = 1;
//        if(column!=null){
//            try{
//                col = Integer.parseInt(column.toString());
//            }catch(Exception e){}
//        }
        
        Element ol = source.createElement("ul");
        nav.appendChild(ol);
        
        ArrayDeque<Element> parent = new ArrayDeque<>();
        ArrayDeque<Integer> depths = new ArrayDeque<>();
        parent.push(ol);
        depths.push(Integer.MIN_VALUE);
        
        for (int index = 0,e=items.size(); index < e; index++) {
            IndexItem i=items.get(index);
            Element li = source.createElement("li");
            
            Element a = source.createElement("a");
            LmID lid=i.target.getID();
            Attr id = source.createAttr(lid);
            Attr href = source.createAttr("href", "#"+id.getValue());
            PageLinkObject o = new PageLinkObject(id, href, lid);
            source.addRefLink(o);
            a.setAttributeNode(href);
            a.setAttribute("class", "ref");
            
            li.appendChild(a);
            Element span = source.createElement("span");
            span.setAttribute("class", "itemnumber");
            Text num = source.createText(i.number);
            span.appendChild(num);
            Element span2 = source.createElement("span");
            span2.setAttribute("class", "itemtitle");
            LmElement  content = (LmElement)i.content.clone();//TODO cloneがなくても動くようにするべき。
            source.convertToDOM(content, span2);
            a.appendChild(span);
            a.appendChild(span2);
            parent.peek().appendChild(li);
            
            
            
            if(index+1<e){
                IndexItem ii = items.get(index+1);
                if(ii.depth>i.depth){
                    Element ool = source.createElement("ul");
                    li.appendChild(ool);
                    parent.push(ool);
                    depths.push(i.depth);
                }else if(ii.depth < i.depth){
                    while(depths.peek()>=ii.depth){
                        parent.pop();
                        depths.pop();
                    }
                }
                
            }
            
        }
        return nav;
    }
    
    
    private List<IndexItem> _convert_all(LmNode node,HTMLConverter source,
            RootDocument root,String[] type,List<Integer> depth)throws Exception{
        List<IndexItem> allitems=root.getIndexItems(type);
        ArrayList<IndexItem> items =new ArrayList<>(); 
        for(IndexItem i:allitems){
            if(depth.contains(i.depth)){
                items.add(i);
            }
        }
        return items;
    }
    private List<IndexItem> _convert_here(LmNode node,HTMLConverter source,
            RootDocument root,String[] type,List<Integer> depth)throws Exception{
        
        String[] str = new String[type.length+1];
        System.arraycopy(type, 0, str, 1, type.length);
        str[0] = "nav";
        List<IndexItem> allitems = root.getIndexItems(str);
        ArrayList<IndexItem> items =new ArrayList<>(); 
        int ndepth=-1;
        boolean find = false;
        for(IndexItem i:allitems){
            if(find)
            {
                if(i.depth<=ndepth)break;
                if(i.type.equals("nav"))continue;
                if(depth.contains(i.depth))
                    items.add(i);
            }
            else
            {
                if(i.target==node){
                    find =true;
                }else{
                    ndepth = i.depth;
                }
            }
            
            
        }
        return items;
    }
}
