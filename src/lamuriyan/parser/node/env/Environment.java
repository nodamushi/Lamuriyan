package lamuriyan.parser.node.env;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lamuriyan.parser.*;
import lamuriyan.parser.node.LmAttr;
import lamuriyan.parser.node.LmElement;
import lamuriyan.parser.node.LmNode;
import lamuriyan.parser.token.Char;
import lamuriyan.parser.token.Token;




//外に見せるaddはこのElementに対して直接add出来ないようにする。
//そのため内部からaddを呼ぶときはsuper.addを利用する事。


/**
 * 環境を表します。<br>
 * 環境はElementでありますが、addのルールが異なります。<br>
 * HTMLに変換する際、getNameの値ではなく、htmltagname属性の値を使います。getTagName()というショートカットメソッドがあります。
 * @author nodamushi
 *
 */
public abstract class Environment extends LmElement{
    public static final String SKIP_HTML_TAGNAME = "skiptag",
            TAGNAMECOMMAND = "htmltagname";
    
    protected Environment parent;
    protected HashMap<String, String> propertys = new HashMap<>();
    protected HashMap<String, List<String>> subcountermap = new HashMap<>();
    protected LamuriyanEngine engine;
    protected EnvironmentFactory factory;
    protected Mode mode=Mode.PLAIN;
    protected LmElement current=this;
    protected LmNode settingNode = this;
    
    public Environment(String name,LamuriyanEngine engine){
        super(name);
        super.setInline(false);
        setTagName(SKIP_HTML_TAGNAME);
        this.engine = engine;
    }
    
    
    public void setProperty(String str,String value){
        propertys.put(str,value);
    }
    
    public String getProperty(String name){
        return propertys.get(name);
    }
    
    public String getProperty(String name,String defaultValue){
        String ret = getProperty(name);
        if(ret==null)return defaultValue;
        else return ret;
    }
    /**
     * HTMLに変換する際のタグ名を設定します。<br>
     * skiptagという値にするとタグを生成しません。
     * @param str
     */
    protected void setTagName(String str){
        setProperty(TAGNAMECOMMAND, str);
//        Command namec = new Command(TAGNAMECOMMAND,str);
//        defineCommand(namec);
    }
    
    /**
     * HTMLに変換する際のタグ名を返します。
     * @return
     */
    public String getTagName(){
        return propertys.get(TAGNAMECOMMAND);
    }
    
//    public String getTagName(){
//        Command name = getCommand(TAGNAMECOMMAND);
//        return name.getAsString();
//    }
//    
    public void setEnvironmentFactory(EnvironmentFactory f){
        factory = f;
    }
    
    
    public EnvironmentFactory getEnvironementFactory(){
        return factory;
    }
    
    public boolean matchName(String name){
        return getName().equals(name);
    }
    
    public void close(){}
    
//    @Override
//    public Command getCommand(String name){
//        if(commandList.containsKey(name)){
//            return commandList.get(name);
//        }
//        if(staticList!=null && staticList.containsKey(name)){
//            return staticList.get(name);
//        }
//        if(parent!=null)return parent.getCommand(name);
//        return null;
//    }
//    
    /**
     * この環境を内包する環境を得ます
     * @return
     */
    public Environment getParentEnvironment(){
        return parent;
    }
    /**
     * この環境を内包する環境を設定します。
     * @param e
     */
    public void setParentEnvironment(Environment e){
        parent = e;
    }
    
//    public void defineCommand(Command c){
//        if(TAGNAMECOMMAND.equals(c.getName())){
//            if(!c.isString()){
//                return;
//            }
//        }
//        commandList.put(c.getName(), c);
//    }
    
//    public void removeCommand(String name){
//        if(TAGNAMECOMMAND.equals(name)){
//            return;
//        }
//        if(commandList.containsKey(name))
//            commandList.remove(name);
////        else if(parent!=null)
////            parent.removeCommand(name);
//    }
    /**
     * この環境のモードを返します
     * @return
     */
    public Mode getMode(){
        return mode;
    }
    /**
     * この環境のモードを設定します
     * @param m
     */
    public void setMode(Mode m){
        mode = m;
    }
    /**
     * 現在addで追加するノードの親となるElementを返します。
     * @return
     */
    public LmElement getCurrentElement(){
        return current;
    }
    /**
     * getCurrentElement()で得られるElementの親を返します。<br><br>
     * 
     * getCurrentElementを親に設定したい場合などに使う目的でつくられました。<br>
     * そのため、getCurrentElement()で得られるElementがこの環境である場合、
     * 親ではなく、この環境自身が返ります。（環境外のElementを設定できない為）<br>
     * @return
     */
    public LmElement getParentOfCurrentElement(){
        if(current==this)return this;
        return current.getParent();
    }
    /**
     * 現在addで追加するノードの親となるElementを設定します。
     * @param e この環境下にあるElementか、この環境そのもの
     */
    public void setCurrentElement(LmElement e){
        if(this==e){
            current = e;
            currentChanged();
        }else if(containsInHierarchy(e)){
            current = e;
            currentChanged();
        }
    }
    
    
    /**
     * getCurrentElement()で返るElementを、現在のgetCurrentElement()のElementの親に設定します。<br>
     * ただし、ElementがこのEnvironmentの場合は何もしません。
     */
    public void setCurrent_Parent(){
        if(current == this)return;
        current = current.getParent();
        currentChanged();
    }
    
    /**
     * getCurrentElementで得るElementを、現在のgetCurrentElementのElementの一つ前のElementに設定します。<br>
     */
    public void setCurrent_Before(){
        if(current == this)return;
        LmElement p = current.getParent();
        ArrayList<LmNode> c = p.getChildren();
        LmNode n =null;
        for(int i=c.indexOf(current)-1;i>=0;i--){
            LmNode l = c.get(i);
            if(l.isElement()){
                n=l;
                break;
            }
        }
        if(n!=null && n!=current){
            current = (LmElement)n;
            currentChanged();
        }
    }
    
    /**
     * getCurrentElementで得るElementを、現在のgetCurrentElementのElementの一つ前のElementに設定します。<br>
     * 一つ前がElementでない場合は変更されません。
     */
    public void setCurrent_After(){
        if(current == this)return;
        LmElement p = current.getParent();
        ArrayList<LmNode> c = p.getChildren();
        LmNode n =null;
        for(int i=0,e=c.indexOf(current);i<e;i++){
            LmNode l = c.get(i);
            if(l.isElement()){
                n=l;
                break;
            }
        }
        if(n!=null && n!=current){
            current = (LmElement)n;
            currentChanged();
        }
    }
    
    /**
     * getCurrentElement()で返るElementを、このEnvirnmentに設定します。
     */
    public void setCurrent_Top(){
        if(current==this)return;
        current = this;
        currentChanged();
    }
    
    public void setCurrent_LastSibling(){
        if(current == this)return;
        LmElement p = current.getParent();
        ArrayList<LmNode> c = p.getChildren();
        LmNode n =null;
        for(int i=c.size()-1;i>=0;i--){
            LmNode l = c.get(i);
            if(l.isElement()){
                n=l;
                break;
            }
        }
        if(n!=null && n!=current){
            current = (LmElement)n;
            currentChanged();
        }
    }
    
    public void setCurrent_FirstSibling(){
        if(current == this)return;
        LmElement p = current.getParent();
        ArrayList<LmNode> c = p.getChildren();
        LmNode n =null;
        for(int i=0,e=c.size();i<e;i++){
            LmNode l = c.get(i);
            if(l.isElement()){
                n=l;
                break;
            }
        }
        if(n!=null && n!=current){
            current = (LmElement)n;
            currentChanged();
        }
    }
    
    /**
     * サブクラスがElementとしてのaddを使いたいときはこのメソッドを利用してください。
     * @param n
     */
    protected void superadd(LmNode n){
        super.add(n);
    }
    
    @Override
    /**
     * この環境にNodeを追加します。<br>
     * 追加先はこのEnvironment直下とは限らず、getCurrentElementで得られるElementに追加されます。
     * nがElementであり、かつ、Environmentでないとき、<br>
     * 次にNodeをaddしたときに追加するElementをこのnにします。
     * @param n 追加するノード。nullの場合は無視します。
     */
    public boolean add(LmNode n){
        return add(n,true);
    }
    /**
     * この環境にNodeを追加します。<br>
     * 追加先はこのEnvironment直下とは限らず、getCurrentElementで得られるElementに追加されます。
     * nがElementであり、かつ、Environmentでないとき、<br>
     * moveCurrentがtrueの場合に限り、次にNodeをaddしたときに追加するElementをこのnにします。<br>
     * また、getSettingTargetNodeのNodeがnになります。<br>
     * getCurrentElementがinline要素しか受け付けなく、nがblock要素であるときは、nを追加することが出来るElementまで<br>
     * 親を辿っていきます。そのさい、getCurrentElementの位置は変化します。
     * @param n 追加するノード。nullの場合は無視します。
     * @param moveCurrent nがElementの時、currentElementをnにするかどうかのフラグ。
     */
    public boolean add(LmNode n,boolean moveCurrent){
        if(n==null)return false;
        if(!n.isInline() && !current.isAcceptDisplayNode()){
            while(!current.isAcceptDisplayNode()){
                setCurrent_Parent();
            }
        }
        if(current == this){
            super.add(n);
        }else{
            current.add(n);
        }
        settingNode = n;
        if(moveCurrent &&n instanceof LmElement && !(n instanceof Environment)){
            current = ((LmElement)n);
            currentChanged();
        }
        return true;
    }
    
    /**
     * getSettingTargetNodeで得られるNodeに対してAttrを設定します。
     * @param attr
     */
    public void setAttrToSettingNode(LmAttr attr){
        settingNode.setAttr(attr);
    }
    
    /**
     * 当初は最後に追加したノードにだけ属性とか設定できればいいだろう、というもくろみで作っていたから、
     * LastSetって名前だったんだけど、変更できるようにしたので、名前を変えた。<br>
     * 前の名前の影響でlastnodeとかいう名前が残ってる可能性大
     * @param node
     */
    public void setSettingTargetNode(LmNode node){
        settingNode=node;
    }
    
    
    public LmNode getSettingTargetNode(){
        return settingNode;
    }
    
    /**
     * 現在の追加先のElementに対してアトリビュートを設定します
     * @param attr
     */
    public void setAttrToCurrentElement(LmAttr attr){
        current.setAttr(attr);
    }
    
    /**
     * 現在の追加先のElementの中で最後の兄弟に対して属性を設定します。
     * @param attr
     */
    public void setAttrToLastSibling(LmAttr attr){
        LmNode n;
        if((n=current.getLastChild())!=null)n.setAttr(attr);
    }
    
    /**
     * 現在の追加先Current「の親」に属性を追加します。<br>
     * currentが環境であった場合は何もしません。
     * @param attr
     */
    public void setAttrToParent(LmAttr attr){
        if(current!=this){
            current.getParent().setAttr(attr);
        }
    }
    
    /**
     * 現在の追加先Current「の親」に属性を追加します。<br>
     * currentが環境であっても、親に追加します。
     * @param attr
     */
    public void setAttrToParentUnSafe(LmAttr attr){
        if(current.getParent()!=null){
            current.getParent().setAttr(attr);
        }
    }
    
    
    public void setValueToSettingTargetNode(String value){
        settingNode.setValue(value);
    }
    
    /**
     * この環境に文字tを追加します。
     * @param t
     * @return エンジンに出力するトークン。何も出力する物がない場合はnull
     */
    abstract public Token input(Char t);
    
    /**
     * getCurrentElementが変更されたときに呼び出されます。
     */
    abstract protected void currentChanged();
    
}
