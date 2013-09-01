package lamuriyan.parser.node;

import lamuriyan.parser.token.Char;

/**
 * フォントに関する設定を反映させる為に作ったノード。
 * @author nodamushi
 *
 */
public class SpanNode extends LmElement{

    public SpanNode(String name){
        super(name);
        setInline(true);
    }

    private SpanNode(SpanNode s){
        super(s);
        fontsize=s.fontsize;
        fontfamily=s.fontfamily;
        fontseries=s.fontseries;
        fontshape=s.fontshape;
        fontvariant=s.fontvariant;
        fontcolor=s.fontcolor;
        fontdecoration=s.fontdecoration;
    }
    
    public String fontsize=Char.normal,fontfamily=Char.normal,fontseries=Char.normal,
            fontshape=Char.normal,fontvariant=Char.normal,fontcolor=Char.normal,fontdecoration=Char.normal;
    
    public boolean isSameClassName(Char ch){
        return fontsize.equals(ch.fontsize)&&fontfamily.equals(ch.fontfamily)&&
                fontseries.equals(ch.fontseries)&&fontshape.equals(ch.fontshape)&&
                fontvariant.equals(ch.fontvariant)&&fontcolor.equals(ch.fontcolor)&&fontdecoration.equals(ch.fontdecoration);
    }
    
    public void setFontProperty(Char ch){
        fontsize =ch.fontsize;
        fontfamily = ch.fontfamily;
        fontseries = ch.fontseries;
        fontshape = ch.fontshape;
        fontvariant = ch.fontvariant;
        fontcolor = ch.fontcolor;
        fontdecoration = ch.fontdecoration;
        
        StringBuilder sb = new StringBuilder();
        boolean before = false;
        if(!fontsize.equals(Char.normal)&&!fontsize.isEmpty()){
            sb.append(fontsize);
            before = true;
        }
        
        if(!fontfamily.equals(Char.normal)&&!fontfamily.isEmpty()){
            if(before)sb.append(" ");
            sb.append(fontfamily);
            before = true;
        }
        
        if(!fontseries.equals(Char.normal)&&!fontseries.isEmpty()){
            if(before)sb.append(" ");
            sb.append(fontseries);
            before = true;
        }
        
        if(!fontshape.equals(Char.normal)&&!fontshape.isEmpty()){
            if(before)sb.append(" ");
            sb.append(fontshape);
            before = true;
        }
        
        if(!fontvariant.equals(Char.normal)&&!fontvariant.isEmpty()){
            if(before)sb.append(" ");
            sb.append(fontvariant);
            before = true;
        }
        if(sb.length()!=0){
            setAttr("class", sb.toString());
        }
        sb.setLength(0);
        if(!fontcolor.equals(Char.normal)&& !fontcolor.isEmpty()){
            sb.append("color:").append(fontcolor).append(";");
        }
        
        if(!fontdecoration.equals(Char.normal)&& !fontdecoration.isEmpty()){
            sb.append("text-decoration:").append(fontdecoration).append(";");
        }
        
        
        if(sb.length()!=0){
            setAttr("style",sb.toString());
        }
        
    }
    
    
    
    
    
    
}
