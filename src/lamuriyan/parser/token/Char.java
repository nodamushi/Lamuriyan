package lamuriyan.parser.token;

import lamuriyan.parser.Command;
import lamuriyan.parser.ExpandArea;
import lamuriyan.parser.macro.MathEscape;

/**
 * 環境に追加する文字を表す。<br>
 * 追加するcharの値とフォントの設定を持つ。<br>
 * 例外としてcharの代わりにMathEscapeの値を持つこともある。<br>
 * CharのタイプはCHAR（文字）、SPACE（空白）,MATHESCAPE（数式の回避文字）
 * @author nodamushi
 *
 */
public class Char{
    public static enum CharType{
        CHAR,SPACE,MATHESCAPE
    }
    public static final String normal = "normal";
    public static final String FONTSIZE="\\fontsize";
    public static final String FONTFAMILY="\\fontfamily";
    public static final String FONTSERIES="\\fontseries";
    public static final String FONTSHAPE="\\fontshape";
    public static final String FONTVARIANT="\\fontvariant";
    public static final String FONTCOLOR = "\\fontcolor";
    public static final String FONTDECORATION="\\fontdecoration";
    public static boolean isFontPropName(String s){
        switch(s){
            case FONTFAMILY:
            case FONTSERIES:
            case FONTSHAPE:
            case FONTSIZE:
            case FONTVARIANT:
                return true;
        }
        return false;
    }
    
    
    
    public char ch;
    public MathEscape mathescape;//タイプがmathescapeの時のみ
    public CharType type;
    public String fontsize,fontfamily,fontseries,fontshape,fontvariant,fontcolor,fontdecoration;
    public Char(){
        
    }
    
    public void init(char ch,CharType type,MathEscape escape,ExpandArea e){
        this.ch = ch;
        this.type = type;
        this.mathescape = escape;
        setFont(e);
    }
    public Char(char ch,CharType type){
        this.ch = ch;
        this.type = type;
        fontsize = fontfamily=fontseries=fontshape=fontvariant=fontcolor=fontdecoration=normal;
    }
    
    public Char(MathEscape str){
        type= CharType.MATHESCAPE;
        mathescape=str;
    }
    
    private String getFontValue(ExpandArea e,String t){
        Command com = e.getCommand(t);
        if(com!=null)return com.getAsString();
        return normal;
    }
    
    public void setFont(ExpandArea e){
        fontsize = getFontValue(e,FONTSIZE);
        fontfamily = getFontValue(e,FONTFAMILY);
        fontseries = getFontValue(e,FONTSERIES);
        fontshape = getFontValue(e,FONTSHAPE);
        fontvariant=getFontValue(e,FONTVARIANT);
        fontcolor = getFontValue(e,FONTCOLOR);
        fontdecoration=getFontValue(e, FONTDECORATION);
    }
    
    public boolean hasFont(){
        return !normal.equals(fontsize)||!normal.equals(fontfamily)||
                !normal.equals(fontseries)||!normal.equals(fontshape)||
                !normal.equals(fontvariant)||!normal.equals(fontcolor)||!normal.equals(fontdecoration);
                
    }
    
}
