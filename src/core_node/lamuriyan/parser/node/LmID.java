package lamuriyan.parser.node;

/**
 * LamuriyanNodeのid属性を表すクラス。<br>
 * idの値を書き換えてもオブジェクトは変更しないようにする為にidだけは別扱いにした。
 * @author nodamushi
 *
 */
public class LmID extends LmAttr{

    
    
    public LmID(){
        super("id");
    }

    public LmID(LmNode parent,AttrValueFactory fact){
        super("id", parent,fact);
    }
    
    @Override
    public LmID clone(){
        LmID a = new LmID();
        a.value = value;
        a.parent=parent;
        a.fact=fact;
        return a;
    }
    
    
}
