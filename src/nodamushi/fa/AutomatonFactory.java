package nodamushi.fa;

import java.util.*;

public class AutomatonFactory<E>{
    
    
    private List<MoveFunction<E>>[] originalFunctionList;
    
    @SuppressWarnings("unchecked")
    public AutomatonFactory(int stateSize,int... endstates){
        if(stateSize < 1)throw new IllegalArgumentException("stateSize<1");
        stateLength = stateSize;
        
        if(endstates==null || endstates.length==0){
            this.endstates = new int[]{stateSize-1};
        }else{
            int[] imp = new int[endstates.length];
            int k=0;
            for(int i:endstates){
                if(i<stateSize){
                    imp[k++]=i;
                }
            }
            if(k==0){
                this.endstates = new int[]{stateSize-1}; 
            }else{
                this.endstates = new int[k];
                System.arraycopy(imp, 0, this.endstates, 0, k);
            }
        }
        
        
        originalFunctionList = new List[stateLength];
        for (int i = 0; i < originalFunctionList.length; i++) {
            originalFunctionList[i] = new ArrayList<>(4);
        }
    }
    
    public void setMoveFunction(int state,MoveFunction<E> m){
        if(state<0 || state>=stateLength)return;
        List<MoveFunction<E>> l = originalFunctionList[state];
        l.add(m);
    }
    
    /**
     * 決定性オートマトンを作成します。
     * @return
     */
    public DFA createDFA(){
        return new DFA();
    }
    
    /**
     * 非決定性オートマトンを作成します。
     * @return
     */
    public NFA createNFA(){
        return new NFA();
    }
    
    /**
     * オートマトンを作成します。dfaによって、決定性か非決定性かを決めれます。
     * @param dfa trueの場合決定性オートマトン、falseの場合非決定性オートマトンを作成します。
     * @return
     */
    public FA createFA(boolean dfa){
        return dfa? createDFA():createNFA();
    }
    
    private int stateLength;
    private int[] endstates;
    private static int[] toIntArray(Collection<Integer> c){
        int[] ret = new int[c.size()];
        int k=0;
        for(int i:c){
            ret[k++] = i;
        }
        return ret;
    }
    private static void addAll(Collection<Integer> c,int[] i){
        for(int in:i){
            if(!c.contains(in))
                c.add(in);
        }
    }
    private static void addAll(Collection<Integer> c,Collection<Integer> i){
        for(int in:i){
            if(!c.contains(in))
                c.add(in);
        }
    }
    private static boolean equal(Collection<Integer> a,Collection<Integer> b){
        if(a.size()!=b.size())return false;
        for(int i:a){
            if(!b.contains(i))return false;
        }
        return true;
    }
    public abstract class FA{
        protected List<MoveFunction<E>>[] funcs;
        //受け付けない入力を得た場合trueにする。
        //trueになるともうどうにもなりましぇん。
        protected boolean error=false;
        
        private FA(){
            funcs=originalFunctionList.clone();
            init();
        }
        
        /**
         * 現在状態を持っているかどうかを示します。<br>
         * 状態を持っていない場合、これ以上入力を受け付けません。
         * @return
         */
        public boolean hasState(){
            return !error;
        }
        
        /**
         * 終了状態の状態があるかどうか判断します。
         * @return
         */
        public boolean isEndState(){
            int[] st = getState();
            return isEndState(st);
        }
        
        private boolean isEndState(int[] st){
            for(int s:st){
                for(int e:endstates){
                    if(s==e)return true;
                }
            }
            return false;
        }
        /**
         * 現在持ちうる状態を返します。<br>
         * DFAの場合、配列長は常に1です。
         * @return
         */
        public abstract int[] getState();
        
        public abstract boolean isDFA();
        public boolean isNFA(){
            return !isDFA();
        }
        
        private ArrayList<Integer> _input(E input,int[] currents){
            ArrayList<Integer> imp = new ArrayList<>();
            for(int s:currents){
                List<MoveFunction<E>> ms = funcs[s];
                for(MoveFunction<E> m:ms){
                    int to;
                    try{
                        to = m.move(input, s);
                    }catch (NullPointerException e) {continue;}
                    if(-1<to && to < stateLength){
                        if(!imp.contains(to)){
                            imp.add(to);
                        }
                    }
                }
            }//end for
            return imp;
        }
        private ArrayList<Integer> _input(E input,ArrayList<Integer> currents){
            ArrayList<Integer> imp = new ArrayList<>();
            for(int s:currents){
                List<MoveFunction<E>> ms = funcs[s];
                for(MoveFunction<E> m:ms){
                    int to;
                    try{
                        to = m.move(input, s);
                    }catch (NullPointerException e) {continue;}
                    if(-1<to && to < stateLength){
                        if(!imp.contains(to)){
                            imp.add(to);
                        }
                    }
                }
            }//end for
            return imp;
        }
        
        private ArrayList<Integer> epsilon(int[] currents){
            ArrayList<Integer> imp=new ArrayList<>(),before;
            //イプシロン遷移を試みたリスト
            ArrayList<Integer> didlist = new ArrayList<>();
            addAll(imp, currents);
            while(!imp.isEmpty()){
                before = imp;
                imp = _input(null, imp);
                addAll(didlist,before);
                imp.removeAll(didlist);
            }
            return didlist;
        }
        
        /**
         * 
         * @param input
         * @return trueの場合、入力を受け付けた。falseの場合、入力を受け付けることが出来ず、状態遷移が失敗した。
         */
        public boolean input(E input) throws DFAStateException{
            if(!hasState())return false;
            //決定性オートマトンはイプシロン遷移しない
            if(input==null && isDFA())return false;
            int[] currents = getState();
            ArrayList<Integer> imp=null;
            
            if(isNFA()){
                //イプシロン遷移を試みる。
                imp = epsilon(currents);
                currents = toIntArray(imp);
            }
            
            //inputがnullの時はNFAの場合、無駄なので実行しない。
            if(input!=null)
                imp=_input(input, currents);
            
            if(imp.size()==0){
                error=true;
                return false;
            }
            
            if(isDFA()){
                if(imp.size()>1){
                    throw new DFAStateException(currents[0],input,imp);
                }
                setState(toIntArray(imp));
            }else{
                //再度イプシロン遷移を試みる。
                currents = toIntArray(imp);
                imp = epsilon(currents);
                
                setState(toIntArray(imp));
            }
            return true;
        }
        
        /**
         * インプットを試してみるメソッド。遷移はしないが、この入力で終了状態に辿り着ける場合trueを返す。
         * @param input
         * @return
         */
        public boolean preinput(E input){
            if(!hasState())return false;
            //決定性オートマトンはイプシロン遷移しない
            if(input==null && isDFA())return false;
            int[] currents = getState();
            ArrayList<Integer> imp=null;
            
            if(isNFA()){
                //イプシロン遷移を試みる。
                imp = epsilon(currents);
                currents = toIntArray(imp);
            }
            
            //inputがnullの時はNFAの場合、無駄なので実行しない。
            if(input!=null)
                imp=_input(input, currents);
            
            if(imp.size()==0){
                return false;
            }
            
            if(isDFA()){
                if(imp.size()>1){
                    return false;
                }
                return isEndState(toIntArray(imp));
            }else{
                //再度イプシロン遷移を試みる。
                currents = toIntArray(imp);
                imp = epsilon(currents);
                
                return isEndState(toIntArray(imp));
            }
        }
        
        /**
         * デバッグ用に公開関数にしています。<br>
         * 実際には呼ばないでね。
         * @param state
         */
        public abstract void setState(int[] state);
        
        /**
         * 初期化します。
         */
        public abstract void init();
    }
    
    
    //決定性オートマトン
    public class DFA extends FA{
        private int currentState;

        private ArrayList<Integer> history=new ArrayList<>();
        {history.add(0);}
        @Override
        public int[] getState(){
            return new int[]{currentState};
        }
        
        public int[] getStateHistory(){
            return toIntArray(history);
        }
        
        @Override
        public boolean isDFA(){
            return true;
        }

        @Override
        public void setState(int[] state){
            currentState=state[0];
            history.add(currentState);
        }
        @Override
        public void init(){
            error = false;
            currentState=0;
        }
        
        @Override
        public String toString(){
            return "DFA:state   "+currentState;
        }
    }
    
    //非決定性オートマトン
    public class NFA extends FA{
        private int[] states;

        @Override
        public int[] getState(){
            return states.clone();
        }

        @Override
        public boolean isDFA(){
            return false;
        }

        @Override
        public void setState(int[] state){
            states = state;
        }
        
        @Override
        public void init(){
            states = new int[]{0};
            error = false;
        }
        @Override
        public String toString(){
            return "NFA:state  "+Arrays.toString(states);
        }
    }

}
