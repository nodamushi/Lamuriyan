package lamuriyan.parser.node;



public class LmTextNode extends LmNode{
    public static final String TEXT_NODE_NAME="#text";
    private CharSequence sb;
    private boolean isStringBuilder=true;
    
    
    /**
     * 通常StringBuilderでデータを持っていますが、
     * valueをそのまま保持する。<br>
     * append等のメソッドは使えなくなります。
     * @param value 保持するデータ
     * @param dumy ダミー用
     */
    public LmTextNode(String value,boolean dumy){
        super(TEXT_NODE_NAME);
        sb=value;
        isStringBuilder=false;
    }
    
    public LmTextNode(){
        super(TEXT_NODE_NAME);
        sb=new StringBuilder();
    }
    public LmTextNode(String name){
        super(name);
        sb=new StringBuilder();
    }
    public LmTextNode(String name,int cap){
        super(name);
        sb=new StringBuilder(cap);
    }
    
    protected LmTextNode(LmTextNode n){
        super(n);
        sb = new StringBuilder(n.sb);
    }
    
    @Override
    public LmTextNode clone(){
        return new LmTextNode(this);
    }
    
    public void append(char c){
        if(isStringBuilder)
            ((StringBuilder)sb).append(c);
    }
    
    public void append(String s){
        if(isStringBuilder)
            ((StringBuilder)sb).append(s);
    }
    
    @Override
    public void setValue(String value){
        if(isStringBuilder){
            ((StringBuilder)sb).setLength(0);
            ((StringBuilder)sb).append(value);
        }
    }
    
    
    @Override
    public String getValue(){
        return sb.toString();
    }


    public int textsize(){
        return sb.length();
    }
    

    
    
    
}
