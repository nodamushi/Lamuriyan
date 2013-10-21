package lamuriyan.parser.node;

import java.util.ArrayList;

import lamuriyan.parser.node.env.Environment;

/**
 * 子を持つことが出来るNodeを表す。<br>
 * DOMのElementに対応する。（※実装は全く異なるLamuriyan処理系だけのデータ）
 * @author nodamushi
 *
 */
public class LmElement extends LmNode{
    
    private ArrayList<LmNode> nodelist = new ArrayList<>();
    protected boolean acceptDisplayNode=true;
    private boolean flagmentNode=false;
    
    
    public LmElement(String name){
        super(name);
    }
    
    /**
     * 子要素以外はディープコピーする
     * @param e
     */
    protected LmElement(LmElement e){
        super(e);
        acceptDisplayNode=e.acceptDisplayNode;
        flagmentNode=e.flagmentNode;
        for(LmNode n:e.nodelist){
            n = n.clone();
            add(n);
        }
    }
    
    public boolean isFlagmentNode(){
        return flagmentNode;
    }
    
    public void setFlagmentNode(boolean b){
        flagmentNode=b; 
    }
    
    /**
     * Environmentのclone用コンストラクタ用のコンストラクタ<br>
     * 子要素以外はディープコピー
     * @param e
     */
    protected LmElement(Environment e){
        super(e,e.getTagName());
        
        acceptDisplayNode=e.acceptDisplayNode;
        for(LmNode n:((LmElement)e).nodelist){
            n = n.clone();
            add(n);
        }
    }
    
    @Override
    public LmElement clone(){
        if(this instanceof Environment){
            return new LmElement((Environment)this);
        }
        return new LmElement(this);
    }
    /**
     * このElementにディスプレイノードを入れることが出来るかどうか。
     * @return
     */
    public boolean isAcceptDisplayNode(){
        return isFlagmentNode()||!isInline()&&acceptDisplayNode;
    }
    /**
     * このElementにディスプレイノードを入れることが出来るかどうかを設定します。<br>
     * ただし、isInline()がtrueの時、この設定は無視されます
     * @param b
     */
    public void setAcceptDisplayNode(boolean b){
        acceptDisplayNode = b;
    }
    
    public ArrayList<LmNode> getChildren(){
        return nodelist;
    }
    /**子要素が空かどうか*/
    public boolean isEmpty(){
        return nodelist.isEmpty();
    }
    
    /**
     * このElementの直下の子要素にnがあるかどうか調べます。
     * @param n
     * @return
     */
    public boolean contains(LmNode n){
        return nodelist.contains(n);
    }
    
    public boolean add(LmNode n){
        if(nodelist.contains(n))return false;
        if(!isFlagmentNode()&&!acceptDisplayNode && !n.isInline())return false;
        nodelist.add(n);
        n.setParent(this);
        return true;
    }
    
    public void remove(LmNode n){
        nodelist.remove(n);
    }
    
    public int getChildLength(){
        return nodelist.size();
    }
    public LmNode getLastChild(){
        if(getChildLength()==0)return null;
        return nodelist.get(nodelist.size()-1);
    }
    public LmNode getFirstChild(){
        if(getChildLength()==0)return null;
        return nodelist.get(0);
    }
    
    public LmNode getChild(int index){
        if(nodelist.size()<=index)return null;
        return nodelist.get(index);
    }
    
    /**
     * 子要素にnが含まれているかどうかを再帰的に調べます。<br>
     * 
     * @param n
     * @return
     */
    public boolean containsInHierarchy(LmNode n){
        for(LmNode tn:nodelist){
            if(tn == n)return true;
            if(tn.isElement()){
                boolean b = ((LmElement)tn).containsInHierarchy(n);
                if(b)return true;
            }
        }
        return false;
    }
    
    
    @Override
    public void toString(StringBuilder sb){
        super.toString(sb);
        sb.append("\n");
        for(LmNode n:nodelist){
            n.toString(sb);
            sb.append("\n");
        }
        sb.append("</").append(getName()).append(">");
    }
    
}
