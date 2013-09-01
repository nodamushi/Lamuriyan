package lamuriyan.parser;

import static java.util.Objects.*;
import static lamuriyan.parser.CharCategory.*;

import java.util.ArrayDeque;
import java.util.HashMap;

/**
 * 文字がどのCharCategoryに登録されるのかを定義するクラス。<br>
 * 定義はスタック構造になっており、スタックの一番上の定義が有効になる。
 * @author nodamushi
 *
 */
public class CharCategoryDefine{
    
    /**
     * 実際に文字がどのCharCategoryに分類されるのかを定義するインターフェース
     * @author nodamushi
     *
     */
    public static interface CharCategoryDefiner extends Cloneable{
        /**
         * どのCharCategoryにcが分類されるか
         * @param c
         * @return
         */
        CharCategory get(char c);
        /**
         * cのCharCategoryをccに変更する。
         * @param c
         * @param cc
         */
        void setCharCategory(char c,CharCategory cc);
        /**
         * 現在の状態と同じCharCategoryDefinerを複製する。<br>
         * 複製されたオブジェクトはオリジナルから完全に独立した物である。（setCharCategoryの設定が連動したりしないという意味）
         * @return
         */
        public CharCategoryDefiner clone();
    }
    /**
     * CharCategoryDefinerの基本実装
     * @author nodamushi
     *
     */
    public static final class CCDImpl implements CharCategoryDefiner{
        private final HashMap<Character, CharCategory> map = new HashMap<>(1);
        @Override
        public void setCharCategory(char c ,CharCategory cc){
            if(cc==null)
                map.remove(c);
            else{
                CharCategory def =defaultCategory(c);
                if(def==cc){
                    map.remove(c);
                }else
                    map.put(c, cc);
            }
        }
        
        @Override
        public CharCategoryDefiner clone() {
            CCDImpl c = new CCDImpl();
            c.map.putAll(map);
            return c;
        }
        
        @Override
        public CharCategory get(char c){
            if(map.containsKey(c)){
                return map.get(c);
            }
            return defaultCategory(c);
        }
        
        private static CharCategory defaultCategory(char c){
            switch(c){
                case '^':
                    return UPPER;
                case '_':
                    return UNDERBAR;
                case '\\':
                    return ESCAPE;
                case '{':
                    return BEGINGROUP;
                case '}':
                    return ENDGROUP;
                case '$':
                    return DOLLAR;
                case '&':
                    return AND;
                case '#':
                    return PARAMETER;
                case ' ':case '\t':
                    return SPACE;
                case '%':
                    return COMMENT_START;
                case '\n':
                    return NEWLINE;
                case '\r':
                case 0:
                case 127:
                    return IGNORE;
                default:
                    if( (65 <= c && c<=90) || (97<=c && c<=122))return ALPHABET;
                    return OTHERCHARACTOR;
            }
        }
    };
    
    
    
    private CharCategoryDefiner def;
    //CharCategoryの定義はグループでプロトタイプ的な継承をする
    private ArrayDeque<CharCategoryDefiner> stack = new ArrayDeque<>();
    
    /**
     * 基本実装のCharCategoryDefinerを用いてCharCategoryDefineを作成します。
     */
    public CharCategoryDefine(){
        this(new CCDImpl());
    }
    public CharCategoryDefine(CharCategoryDefiner def){
        this.def = requireNonNull(def);
    }
    
    /**
     * スタックの一番上にdefを追加します。
     * @param def
     */
    public void pushDefine(CharCategoryDefiner def){
        if(def==null)return;
        stack.push(this.def);
        this.def = def;
    }
    
    /**
     * 現在のCharCategoryDefinerのスタック構造の一番上をdefで書き換えます
     * @param def
     */
    public void setDefine(CharCategoryDefiner def){
        if(def==null)return;
        this.def = def;
    }
    
    /**
     * スタックの一番上に現在の一番上の定義のクローンを追加します。
     */
    public void pushDefine(){
        pushDefine(def.clone());
    }
    
    /**
     * スタックの一番上を削除します。
     */
    public void pop(){
        if(stack.size()==0)return;
        def = stack.pop();
    }
    
    /**
     * スタックの一番上のCharCategoryDefinerに対して、文字chがccのカテゴリーになるよう設定を変更します。
     * @param ch
     * @param cc
     */
    public void setCharCategory(char ch,CharCategory cc){
        def.setCharCategory(ch, cc);
    }
    
    /**
     * 文字chのカテゴリーを返します。
     * @param ch
     * @return
     */
    public CharCategory get(char ch){
        return def.get(ch);
    }
}
