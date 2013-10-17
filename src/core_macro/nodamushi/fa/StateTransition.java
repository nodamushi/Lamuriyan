package nodamushi.fa;

import java.util.ArrayList;

/**
 * 状態遷移の履歴を保持する
 * @author nodamushi
 *
 */
public class StateTransition implements Cloneable{
    
    
    public static int[] getCurrents(StateTransition[] st){
        int[] ret = new int[st.length];
        for(int i=0;i<st.length;i++){
            ret[i] = st[i].current;
        }
        return ret;
    }
    
    int current;
    ArrayList<Integer> list = new ArrayList<>(4);
    public StateTransition(){
        current = 0;
        list.add(0);
    }
    
    public StateTransition(int first){
        current = first;
        list.add(first);
    }
    
    //クローン用ダミー
    private StateTransition(String s){}
    
    public StateTransition cloneAndAdd(int state){
        StateTransition s = new StateTransition("");
        s.current = state;
        s.list = new ArrayList<>(list);
        s.list.add(state);
        return s;
    }
    
    public void add(int state){
        current = state;
    }
    
    public StateTransition clone(){
        StateTransition s = new StateTransition("");
        s.current = current;
        s.list = new ArrayList<>(list);
        return s;
    }
    
}
