package lamuriyan.parser.token;

import java.util.*;


public class TokenChain implements Iterable<Token>{
    
    private List<Token> list;
    
    
    public TokenChain(){
        list = new ArrayList<>();
    }
    
    public TokenChain(int cap){
        list = new ArrayList<>(cap);
    }
    private TokenChain(boolean d){}
    
    public TokenChain subChain(int a,int b){
        if(a==b)return new TokenChain(1);
        if(a>b)return null;
        if(b>list.size())b=list.size();
        if(a<0)a=0;
        int s = b-a;
        ArrayList<Token> l = new ArrayList<>(s);
        for(int i=a;i<b;i++){
            l.add(list.get(i));
        }
        TokenChain sc = new TokenChain(false);
        sc.list = l;
        return sc;
    }
    
    public void pushTo(Collection<Token> collection){
        collection.addAll(list);
    }
    
    /**
     * このTokenChainが持つ要素を変更不可能なリストとして返します。
     * @return
     */
    public List<Token> getTokens(){
        return Collections.unmodifiableList(list);
    }

    /**
     * 前後にあるSPACEを全て消し、COMMENTとNEWLINEは全て消します。
     */
    public void trim(){
        for(int i=list.size()-1;i>-1;i--){
            Token t = list.get(i);
            if(t.getType() == TokenType.SPACE){
                list.remove(i);
            }else break;
        }
        while(list.size()!=0){
            Token t = list.get(0);
            if(t.getType()==TokenType.SPACE){
                list.remove(0);
            }else break;
        }
        for(int i=list.size()-1;i>-1;i--){
            Token t = list.get(i);
            if(t.getType() == TokenType.COMMENT || t.getType()==TokenType.NEWLINE){
                list.remove(i);
            }
        }
    }
    
    
    public void add(Token str){
        if(str!=null)list.add(str);
    }
    
    public void add(Token token,int index){
        list.add(index, token);
    }
    
    public void addAll(Token... strs){
        Collections.addAll(list, strs);
    }
    public void addAll(TokenChain tc){
        list.addAll(tc.list);
    }
    
    public void addAll(TokenChain tc,int index){
        list.addAll(index, tc.list);
    }
    
    public void addAll(Token[] tc,int index){
        ArrayList<Token> tokens = new ArrayList<>(tc.length);
        Collections.addAll(tokens, tc);
        list.addAll(index, tokens);
    }
    
    public void removeLast(){
        if(list.size()!=0){
            list.remove(list.size()-1);
        }
    }
    
    public void removeFirst(){
        if(list.size()!=0){
            list.remove(0);
        }
    }
    
    public void remove(Token str){
        list.remove(str);
    }
    
    public TokenChain removeComment(){
        TokenChain s = new TokenChain();
        List<Token> ts = s.list;
        for(Token t:this){
            if(t.getType()!=TokenType.COMMENT){
                ts.add(t);
            }
        }
        return s;
    }
    
    public int size(){
        return list.size();
    }
    
    public Token get(int i){
        return list.get(i);
    }
    
    public void toString(StringBuilder sb){
        for(Token t:this){
            sb.append(t.toString());
        }
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    @Override
    public Iterator<Token> iterator(){
        return list.iterator();
    }
    
    public static interface PaierIterator extends Iterator<TokenPair>{
        public TokenPair getPrePair();
    }
    
    public PaierIterator pairs(){
        return new PaierIterator(){
            boolean inited=false;
            int n=0;
            Token before;
            
            TokenPair pre=new TokenPair(),nextpair=new TokenPair();
            
            @Override
            public TokenPair getPrePair(){
                Token next;
                if(n>=list.size()){
                    next = null;
                }else{
                    next = list.get(n);
                }
                pre.init(before, next);
                return pre;
            }
            
            private void init(){
                if(list.size()==0)return;
                n=1;
                inited = true;
                before = list.get(0);
            }

            @Override
            public void remove(){

            }

            @Override
            public TokenPair next(){
                Token next;
                if(n>=list.size()){
                    next=null;
                    n++;
                }else{
                    next = list.get(n);
                    n++;
                }
                nextpair.init(before,next);
                before = next;
                return nextpair;
            }

            @Override
            public boolean hasNext(){
                if(!inited){
                    init();
                    if(!inited){
                        return false;
                    }
                }
                return n <= list.size();
            }
        };
    }

    public void clear(){
        list.clear();
    }

}
