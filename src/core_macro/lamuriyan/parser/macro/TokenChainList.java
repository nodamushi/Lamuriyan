package lamuriyan.parser.macro;

import java.util.ArrayList;
import java.util.Iterator;

import lamuriyan.parser.token.TokenChain;




/**
 * マクロで使うリスト。<br><br>
 * 挿入<br>
 * unshift→ 0..[list]..n ←push<br><br>
 * 取り出し<br>
 * shift  ← 0..[list]..n →pop
 * 
 * @author nodamushi
 *
 */
public class TokenChainList implements Iterable<TokenChain>{
    //ArrayDequeだとget(i)が使えない
    ArrayList<TokenChain> list = new ArrayList<>();
    
    public boolean isEmpty(){
        return list.isEmpty();
    }
    public int length(){
        return list.size();
    }
    
    public void push(TokenChain t){
        list.add(t);
    }
    
    public TokenChain pop(){
        if(list.size()==0)return null;
        int i = list.size()-1;
        TokenChain t = list.get(i);
        list.remove(i);
        return t;
    }
    
    
    public void unshift(TokenChain t){
        list.add(0, t);
    }
    
    public TokenChain shift(){
        if(list.size()==0)return null;
        TokenChain t = list.get(0);
        list.remove(0);
        return t;
    }
    
    public TokenChain get(int i){
        if(i>=list.size() || i<0)return null;
        return list.get(i);
    }
    
    public void set(int i,TokenChain t){
        if(i<0)return;
        if(i>=list.size()){
            while(list.size()!=i){
                list.add(new TokenChain());
            }
            list.add(t);
        }else{
            list.set(i, t);
        }
        
    }
    
    
    public TokenChain peek(){
        return get(0);
    }
    
    public TokenChain first(){
        return get(0);
    }
    
    public TokenChain last(){
        return get(list.size()-1);
    }
    
    @Override
    public Iterator<TokenChain> iterator(){
        return list.iterator();
    }
}
