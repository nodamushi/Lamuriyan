package lamuriyan.parser.node;

import static java.util.Objects.*;

import java.util.*;

import lamuriyan.parser.node.env.Environment;



/**
 * DOMにおいてNodeに相当するデータを表すクラス。<br>
 * （※実際のNodeとは全く違う、Lamuriyan内部で使うだけのデータ)<br><br>
 * テキストデータはLmTextNodeを、それ以外は基本的にはElementをつかう。
 * @author nodamushi
 *
 */
public class LmNode{
    /**
     * LmNodeの値を作成するファクトリー。<br>
     * LmNode.getValueが呼ばれたときに、LmNode.setValueによりnullでない値が設定されていない場合、
     * LmNodeはファクトリーメソッドのcreateValueを利用して値を生成し、設定します。
     */
    public static interface NodeValueFactory{
        /**
         * 値を生成します
         * @param node 値を設定するノード
         * @return 値
         */
        public String createValue(LmNode node);
    }
    
    private String value;
    private String name;
    private NodeValueFactory fact;
    private boolean inline = true;
    private LmElement parent;
    private boolean ignoreconvert = false;
    
    private LmID id;
    private HashMap<String, LmAttr> attrs = new HashMap<>();
    
    /**
     * 
     * @param name このノードの名前
     */
    public LmNode(String name){
        this.name =requireNonNull(name);
    }
    
    
    /**
     * ディープクローンを作ります。
     * @param n
     */
    protected LmNode(LmNode n){
        name =n.name;
        value = n.value;
        fact = n.fact;
        inline = n.inline;
        parent = n.parent;
        if(n.id!=null){
            id = n.id.clone();
            id.parent=this;
        }
        for(LmAttr attr:n.attrs.values()){
            attr = attr.clone();
            attr.parent=n;
            attrs.put(attr.name, attr);
        }
    }
    
    /**
     * ディープクローンを作りますが、名前だけ変えます。
     * @param n
     * @param name
     */
    protected LmNode(LmNode n,String name){
        this.name =name;
        value = n.value;
        fact = n.fact;
        inline = n.inline;
        parent = n.parent;
        if(n.id!=null){
            id = n.id.clone();
            id.parent=this;
        }
        for(LmAttr attr:n.attrs.values()){
            attr = attr.clone();
            attr.parent=n;
            attrs.put(attr.name, attr);
        }
    }
    
    @Override
    public LmNode clone(){
        return new LmNode(this);
    }
    
    /**
     * Converterが無視するかどうかの設定です。<br>
     * @param ignoreconvert 表示するかどうか
     */
    public void setIgnore(boolean b){
        this.ignoreconvert = b;
    }
    
    /**
     * Converterが無視するかどうかの設定です。<br>
     */
    public boolean isIgnore(){
        return ignoreconvert;
    }
    
    
    /**
     * このノードの親を設定します。
     * @param e
     */
    public void setParent(LmElement e){
        parent = e;
    }
    
    
    /**
     * このノードがEnvironmentを継承するクラスかどうかを返します。
     * @return this instanceof Environmentの値
     */
    public boolean isEnvironment(){
        return this instanceof Environment;
    }
    
    /**
     * このノードがElementを継承するクラスかどうかを返します。
     * @return this instanceof LmElement
     */
    public boolean isElement(){
        return this instanceof LmElement;
    }
    
    
    /**
     * 親を取得します
     * @return 親
     */
    public LmElement getParent(){
        return parent;
    }
    
    /**
     * 値を生成するファクトリーを設定します
     * @param f
     */
    public void setFactory(NodeValueFactory f){
        fact = f;
    }
    
    /**
     * 値を生成するファクトリーを返します。
     * @return
     */
    public NodeValueFactory getFactory(){
        return fact;
    }
    
    /**
     * このメソッドが返す名前とHTMLタグは違います。<br>
     * 利用しないでください。<br>
     * HTMLConverter.getHTMLTagName(TeXNode)を利用してください。
     * @return
     */
    public String getName(){
        return name;
    }
    
    /**
     * 値を返します。
     * @return
     */
    public String getValue(){
        if(value == null && fact!= null){
            value = fact.createValue(this);
        }
        return value;
    }
    /**
     * 値を設定します。
     * @param value
     */
    public void setValue(String value){
        this.value = value;
    }
    
    /**
     * idも含めて全部のAttrを返します。
     * @return
     */
    public Collection<LmAttr> getAttrs(){
        Collection<LmAttr> a= attrs.values();
        List<LmAttr> list = new ArrayList<>(a);
        if(id!=null)list.add(id);
        return list;
    }
    /**
     * 属性を取得します。
     * @param name
     * @return
     */
    public LmAttr getAttr(String name){
        if(name.equals("id"))return id;
        return attrs.get(name);
    }
    
    /**
     * 属性を設定します。<br>
     * valueという名前にすると、setValueを呼び出して設定します。（なんでこんな仕様にしたのかは覚えてない）
     * @param name
     * @param value
     */
    public void setAttr(String name,String value){
        if(name==null)return;
        if("id".equals(name)){
            if(id==null){
                id = new LmID();
                id.parent(this);
            }
            id.setValue(value);
        }else if("value".equals(name)){
            if(value!=null){
                setValue(value);
            }
        }else {
            LmAttr a = new LmAttr(name);
            a.parent(this);
            a.setValue(value);
            setAttr(a);
        }
    }
    /**
     * 属性を設定します。<br>
     *  valueという名前にすると、setValueを呼び出して設定します。（なんでこんな仕様にしたのかは覚えてない）<br><br>
     * "class"属性は、元々属性が設定されていた場合
     * 「元の値+" "+新しい値」という値を設定します。<br>
     * "style"属性は、元々属性が設定されていた場合、
     * 「元の値+";"+新しい値」という値が設定されます。
     * 
     * @param attr
     */
    public void setAttr(LmAttr attr){
        if(attr==null)return;
        if(attr instanceof LmID){
            setID((LmID)attr);
        }else{
            String name =attr.getName();
            if("id".equals(name)){
                if(id==null){
                    id = new LmID();
                    id.parent(this);
                }
                id.setValue(attr.getValue());
            }else if("class".equals(name)){
                LmAttr a = attrs.get(name);
                if(a!=null){
                    a.setValue(a.getValue()+" "+attr.getValue());
                }else{
                    attrs.put(name,attr);
                    attr.parent(this);
                }
            }else if("style".equals(name)){
                LmAttr a = attrs.get(name);
                if(a!=null){
                    String v = a.getValue();
                    if(!v.endsWith(";")){
                        v+=";"+attr.getValue();
                    }else{
                        v+=attr.getValue();
                    }
                    a.setValue(v);
                }else{
                    attrs.put(name,attr);
                    attr.parent(this);
                }
            }else if("value".equals(name)){
                String v = attr.getValue();
                if(v!=null){
                    setValue(v);
                }
            }else{
                attrs.put(name,attr);
                attr.parent(this);
            }
        }
    }
    
    /**
     * 属性を削除します。
     * @param name
     */
    public void removeAttr(String name){
        if(name == null)return;
        if("id".equals(name)){
            if(id!=null)
                id.setValue(null);
            else
                id = null;
        }else{
            attrs.remove(name);
        }
    }
    
    /**
     * iDを設定します。
     * @param id
     */
    public void setID(LmID id){
        this.id = id;
        id.parent(this);
    }
    /**
     * idを取得します
     * @return
     */
    public LmID getID(){
        return id;
    }
    
    /**
     * このノードをインライン属性にします。
     * @param b
     */
    public void setInline(boolean b){
        inline = b;
    }
    
    /**
     * このノードがインライン属性かどうか返します。
     * @return
     */
    public boolean isInline(){
        return inline;
    }
    
    private void toString(StringBuilder sb,LmAttr a){
        sb.append(" ").append(a.getName());
        String val = a.getValue();
        if(val!=null && !val.isEmpty()){
            if(val.contains("'")){
                val = val.replace("'", "\\'");
            }
            sb.append("='").append(val).append("'");
        }
    }
    
    public void toString(StringBuilder sb){
        sb.append("<").append(getName());
        if(getID()!=null){
            toString(sb,getID());
        }
        
        for(String name:attrs.keySet()){
            LmAttr a = attrs.get(name);
            if(a!=null){
                toString(sb,a);
            }
        }
        if(!inline)
            sb.append(" display");
        String value =getValue();
        if(value!=null && !value.isEmpty())
            sb.append(" value='").append(value).append("'");
        if(!(this instanceof LmElement)){
            sb.append("/");
        }
        sb.append(">");
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }
    
    
    
}
