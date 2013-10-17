package lamuriyan.parser.node;

import static java.util.Objects.*;

import org.w3c.dom.Attr;

public class LmAttr implements Cloneable{
    protected String name;
    protected String value;
    protected LmNode parent;
    protected AttrValueFactory fact;
    
    
    private Attr converteddom;
    
    
    public LmAttr(String name){
        this.name = requireNonNull(name);
    }
    
    public LmAttr(String name,String value){
        this.name = requireNonNull(name);
        this.value =value; 
    }
    
    public LmAttr(String name,LmNode parent,AttrValueFactory fact){
        this.name = requireNonNull(name);
        this.parent = parent;
        this.fact = fact;
    }
    
    @Override
    public LmAttr clone(){
        LmAttr a = new LmAttr(name);
        a.value = value;
        a.parent=parent;
        a.fact=fact;
        return a;
    }
    public void parent(LmNode o){
        parent = o;
    }
    
    public LmNode parent(){
        return parent;
    }
    public String getValue(){
        if(value == null && fact != null){
            value = fact.createValue(this);
        }
        return value;
    }
    public void setValue(String value){
        this.value = value;
    }
    public String getName(){
        return name;
    }
    
    public void copy(LmAttr attr){
        name = attr.name;
        value = attr.value;
        fact = attr.fact;
        parent = attr.parent;
    }
    
    public static interface AttrValueFactory{
        public String createValue(LmAttr id);
    }
    
    
    /**
     * HTMLConverterで変換したDOMを保持します。
     * @param attr
     */
    public void setConvertedDOM(Attr attr){
        converteddom=attr;
    }
    
    /**
     * HTMLConverterで変換した結果のDOMが登録されていれば、それを返します。
     */
    public Attr getConvertedDOM(){return converteddom;}
    
}
