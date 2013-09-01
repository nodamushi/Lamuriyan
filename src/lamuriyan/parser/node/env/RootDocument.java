package lamuriyan.parser.node.env;

import java.util.*;

import lamuriyan.parser.LamuriyanEngine;
import lamuriyan.parser.label.RefTarget;
import lamuriyan.parser.macro.IndexItem;


public class RootDocument extends TextEnvironment{
    
    private Map<String,RefTarget> refs = new HashMap<>();
    private List<IndexItem> items = new ArrayList<>(); 
    
    public RootDocument(LamuriyanEngine engine){
        super("root", engine);
    }

    public void addRefTarget(RefTarget ref){
        if(ref==null)return;
        refs.put(ref.labelName,ref);
    }
    
    public RefTarget getRefTarget(String labelName){
        return refs.get(labelName);
    }
    
    
    
    public void addIndexItem(IndexItem item){
        if(item!=null)items.add(item);
    }
    
    public List<IndexItem> getIndexItems(){
        return new ArrayList<IndexItem>(items);
    }
    
    public List<IndexItem> getIndexItems(String... types){
        List<String> ty = Arrays.asList(types);
        ArrayList<IndexItem> ret =new ArrayList<>();
        for(IndexItem i:items){
            if(ty.contains(i.type)){
                ret.add(i);
            }
        }
        return ret;
    }
    
}
