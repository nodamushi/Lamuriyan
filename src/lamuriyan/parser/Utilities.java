package lamuriyan.parser;

public class Utilities{
    


    /**
     * typeについて。<br>
     * typeに関係なく、strの頭に#か*が付いている場合はそれをそのまま返します。<br><br>
     * #rgb形式,*rgb形式：rgbをFFFなどのように16進数表記します。strに#を付けて返します。<br>
     * rgb形式：r,g,bをそれぞれ0～255の数値で表し、カンマで区切る<br>
     * argb形式：r,g,bをそれぞれ0～255の数値で表す。aは小数点を含む場合は0～1の範囲でしてされている物と見なし、
     * 整数なら0～255で指定されている物と見なす。ただし、1の場合は0～1の範囲で指定された物と見なす。(1/255にはならない。)<br>
     * 
     * a#rgb形式,a*rgb形式：aはargb形式と同様に判断するが、rgbは#rgb形式とみなす。a#rgbもしくは、a,rgbもしくは、a,#rgbの形式で入力する<br><br>
     * 
     * 分からないときはとりあえずnullを返す。<br>
     * @param str
     * @param type
     * @return
     */
    public static String color(String str,String type){
        str =str.trim();
        //typeとか関係なく結果を返す場合
        if(str.startsWith("#"))return str;//#FFFとかの形式
        if(str.startsWith("*"))return "#"+str.substring(1);//同上だけど、#は打つのが面倒くさいので*も許可
        String[] sp = str.split(",");
        String splitstr=null;
        char splitchar =0;
        switch(type.toLowerCase()){
            case "name":
                return str;
            case "#rgb":
            case "*rgb":
                return "#"+str;
            
                //255,255,255の様に0～255の数字で記述
            case "rgb":{
                if(sp.length<3)return null;
                int r,g,b;
                String R,G,B;
                try{
                    r = Integer.parseInt(sp[0]);
                    g = Integer.parseInt(sp[1]);
                    b = Integer.parseInt(sp[2]);
                    if(r > 255)r = 255;
                    else if(r<0)r=0;
                    if(g > 255)g = 255;
                    else if(g<0)g=0;
                    if(b> 255)b = 255;
                    else if(b<0)b=0;
                    
                    R = Integer.toHexString(r);
                    G = Integer.toHexString(g);
                    B = Integer.toHexString(b);
                    if(R.length()==1)R="0"+R;
                    if(G.length()==1)G="0"+G;
                    if(B.length()==1)B="0"+B;
                    return "#"+R+G+B;
                }catch(NullPointerException e){
                    return null;
                }
            }
            //aだけ数値で指定して、後は#FFFの様に指定します。
            case "a#rgb":
                splitstr="#";
                splitchar='#';
            case "a*rgb":{
                if(splitstr==null){
                    splitstr="*";
                    splitchar='*';
                }
                String A,rgb;
                if(sp.length==1){
                    sp = str.split(splitstr);
                    A = sp[0];
                    rgb=sp[1];
                }else{
                    A=sp[0].trim();
                    rgb=sp[1].trim();
                    if(rgb.charAt(0)==splitchar){
                        rgb = rgb.substring(1);
                    }
                }
                double a;
                if(A.contains(".")){
                    try{
                        a = Double.parseDouble(A);
                    }catch(NumberFormatException e){
                        a = 1;
                    }
                }else{
                    try{
                        int aa = Integer.parseInt(A);
                        if(aa==1)a=1;
                        else{
                            a = aa/255d;
                        }
                    }catch(NullPointerException e){
                        a = 1;
                    }
                }
                if(a>1)a=1;
                else if(a<0)a=0;
                
                if(rgb.length()==3){
                    int r = getHexValue(rgb.charAt(0));
                    int g = getHexValue(rgb.charAt(1));
                    int b = getHexValue(rgb.charAt(2));
                    return String.format("rgba(%d,%d,%d,%.3f)", r,g,b,a);
                }else if(rgb.length()>=6){
                    int r = getHexValue(rgb.substring(0,2));
                    int g = getHexValue(rgb.substring(2,4));
                    int b = getHexValue(rgb.substring(4,6));
                    return String.format("rgba(%d,%d,%d,%.3f)", r,g,b,a);
                }else return null;
            }
            
            //argb形式。aは1の場合や、小数点を含む場合は0～1の範囲で指定する物と見なし、
            //1でない整数の場合は0～255の範囲で表す物と見なす。
            case "argb":{
                if(sp.length<4)return null;
                int r,g,b;
                double a;
                String A=sp[0];
                if(A.contains(".")){
                    try{
                        a = Double.parseDouble(A);
                    }catch(NumberFormatException e){
                        a = 1;
                    }
                }else{
                    try{
                        int aa = Integer.parseInt(A);
                        if(aa==1)a=1;
                        else{
                            a = aa/255d;
                        }
                    }catch(NullPointerException e){
                        a = 1;
                    }
                }
                if(a>1)a=1;
                else if(a<0)a=0;
                A = String.format("%.3f", a);
                
                try{
                    r = Integer.parseInt(sp[1]);
                    g = Integer.parseInt(sp[2]);
                    b = Integer.parseInt(sp[3]);
                    if(r > 255)r = 255;
                    else if(r<0)r=0;
                    if(g > 255)g = 255;
                    else if(g<0)g=0;
                    if(b> 255)b = 255;
                    else if(b<0)b=0;
                    return "rgba("+r+","+g+","+b+","+A+")";
                }catch(NullPointerException e){
                    return null;
                }
            }
        }
        
        return null;
    }
    
    
    
    
    private static int getHexValue(char c){
        switch(c){
            case '0':return 0;
            case '1':return 17;
            case '2':return 34;
            case '3':return 3*16+3;
            case '4':return 4*16+4;
            case '5':return 5*16+5;
            case '6':return 6*16+6;
            case '7':return 7*16+7;
            case '8':return 8*16+8;
            case '9':return 9*16+9;
            case 'a':
            case 'A':return 170;
            case 'b':
            case 'B':return 187;
            case 'c':
            case 'C':return 204;
            case 'd':
            case 'D':return 221;
            case 'e':
            case 'E':return 238;
            case 'f':
            case 'F':return 255;
        }
        return 0;
    }
    
    private static int getHexValue(String str){
        int l=str.length();
        int ret=0;
        for(int i=0;i<l;i++){
            ret = ret*16+_hex(str.charAt(i));
        }
        return ret;
    }
    private static int _hex(char c){
        switch(c){
            case '0':return 0;
            case '1':return 1;
            case '2':return 2;
            case '3':return 3;
            case '4':return 4;
            case '5':return 5;
            case '6':return 6;
            case '7':return 7;
            case '8':return 8;
            case '9':return 9;
            case 'a':
            case 'A':return 10;
            case 'b':
            case 'B':return 11;
            case 'c':
            case 'C':return 12;
            case 'd':
            case 'D':return 13;
            case 'e':
            case 'E':return 14;
            case 'f':
            case 'F':return 15;
        }
        return 0;
    }
    
    
    
    
    
    
    
    private Utilities(){}
}
