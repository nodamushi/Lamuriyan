package nodamushi.number;

public class NumberToString{
    private NumberToString(){}
    
    
    public static String arabic(int i){
        return Integer.toString(i);
    }
    
    public static String arabic(double d){
        return Double.toString(d);
    }
    
    private static final String[]
            kanji_1    ={"","一","二","三","四","五","六","七","八","九"},
            kanji_2    ={"","","二","三","四","五","六","七","八","九"},
            daiji_1    ={"","壱","弐","参","四","五","六","七","八","九"};
    
    private static void _daiji(int i,StringBuilder sb){
        int t = i/1000;
        if(t!=0){
            sb.append(daiji_1[t]).append("千");
            i%=1000;
        }
        t = i/100;
        if(t!=0){
            sb.append(daiji_1[t]).append("百");
            i%=100;
        }
        t = i/10;
        if(t!=0){
            sb.append(daiji_1[t]).append("拾");
        }
        t = i%10;
        sb.append(daiji_1[t]);
    }
    
    /**
     * 大字で数を表現します。負数には接頭辞としてマイナスがつきます。
     * @param i
     * @return
     */
    public static String daiji(int i){
        if(i==0)return "零";
        StringBuilder sb = new StringBuilder();
        if(i<0){
            sb.append("マイナス");
            i=-i;
        }
        int t = i/100000000;
        if(t!=0){
            _daiji(t, sb);
            sb.append("億");
            i%=100000000;
        }
        t = i/10000;
        if(t!=0){
            _daiji(t, sb);
            sb.append("萬");
            i%=10000;
        }
        _daiji(i,sb);
        return sb.toString();
    }
    
    
    
    private static void _kanji(int i,StringBuilder sb,boolean nijuu_sannju){
        int t = i/1000;
        if(t!=0){
            sb.append(kanji_2[t]).append("千");
            i%=1000;
        }
        t = i/100;
        if(t!=0){
            sb.append(kanji_2[t]).append("百");
            i%=100;
        }
        t = i/10;
        if(t!=0){
            if(nijuu_sannju){
                if(t==2)
                    sb.append("廿");
                else if(t==3)
                    sb.append("卅");
            }else
                sb.append(kanji_2[t]).append("十");
        }
        t = i%10;
        sb.append(kanji_1[t]);
    }
    
    /**
     * 漢数字で数を表します。負数の場合は接頭辞としてマイナスがつきます。
     * @param i
     * @param nijuu_sannju 20を廿、30を卅と表記するか否か
     * @return
     */
    public static String kanji(int i,boolean nijuu_sannju){
        if(i==0)return "零";
        StringBuilder sb = new StringBuilder();
        if(i<0){
            sb.append("マイナス");
            i=-i;
        }
        int t = i/100000000;
        if(t!=0){
            _kanji(t, sb,nijuu_sannju);
            sb.append("億");
            i%=100000000;
        }
        t = i/10000;
        if(t!=0){
            _kanji(t, sb,nijuu_sannju);
            sb.append("万");
            i%=10000;
        }
        _kanji(i,sb,nijuu_sannju);
        return sb.toString();
    }
    
    /**
     * 1～26の数をa～zに変換します。それ以外の数はアラビアン数字になります。
     * @param i
     * @return
     */
    public static String alphabet(int i){
        if(i<=0 || i>26)return arabic(i);
        i +=96;
        return Character.toString((char)i);
    }
    
    private static final String[]
            roman_1 = {"","i","ii","iii","iv","v","vi","vii","viii","ix"},
            roman_10= {"","x","xx","xxx","xl","l","lx","lxx","lxxx","xc"},
            roman_100={"","c","cc","ccc","cd","d","dc","dcc","dccc","cm"},
            roman_1000={"","m","mm","mmm"};
    
    /**
     * i,v,x,l,c,mを利用してローマ数字に変換します。<br>
     * 0以下、4000以上の数はアラビアン数字になります。<br>
     * 大文字にする場合はtoUpperCase()を利用してください。
     * @param i
     * @return
     */
    public static String roman(int i){
        if(i<=0 || i >=4000){
            return arabic(i);
        }
        StringBuilder sb = new StringBuilder();
        
        int n = i/1000;
        sb.append(roman_1000[n]);
        i=i%1000;
        n = i/100;
        sb.append(roman_100[n]);
        i=i%100;
        n = i/10;
        sb.append(roman_10[n]);
        n = i%10;
        sb.append(roman_1[n]);
        return sb.toString();
    }
    
    private static final String[]
            roman_unit_1 = {"","ⅰ","ⅱ","ⅲ","ⅳ","ⅴ","ⅵ","ⅶ","ⅷ","ⅸ"},
            roman_unit_10= {"","ⅹ","ⅹⅹ","ⅹⅹⅹ","ⅹⅼ","ⅼ","ⅼⅹ","ⅼⅹⅹ","ⅼⅹⅹⅹ","ⅹⅽ"},
            roman_unit_100={"","ⅽ","ⅽⅽ","ⅽⅽⅽ","ⅽⅾ","ⅾ","ⅾⅽ","ⅾⅽⅽ","ⅾⅽⅽⅽ","ⅽⅿ"},
            roman_unit_1000={"","ⅿ","ⅿⅿ","ⅿⅿⅿ"};
    
    /**
     * Unicode,JISで定義されているローマ数字の小文字を利用して文字を出力します。<br>
     * 11以上の数にはフォントが対応していない場合があります。<br>
     * 0以下、4000以上の数はアラビアン数字になります。
     * @param i
     * @return
     */
    public static String unitroman(int i){
        if(i<=0 || i >=4000){
            return arabic(i);
        }
        StringBuilder sb = new StringBuilder();
        
        int n = i/1000;
        sb.append(roman_unit_1000[n]);
        i=i%1000;
        n = i/100;
        sb.append(roman_unit_100[n]);
        i=i%100;
        if(i==11){
            sb.append("ⅺ");
        }else if(i==12){
            sb.append("ⅻ");
        }else{
            n = i/10;
            sb.append(roman_unit_10[n]);
            n = i%10;
            sb.append(roman_unit_1[n]);
        }
        return sb.toString();
    }
    
    private static final String[]
            Roman_unit_1 = {"","Ⅰ","Ⅱ","Ⅲ","Ⅳ","Ⅴ","Ⅵ","Ⅶ","Ⅷ","Ⅸ"},
            Roman_unit_10= {"","Ⅹ","ⅩⅩ","ⅩⅩⅩ","ⅩⅬ","Ⅼ","ⅬⅩ","ⅬⅩⅩ","ⅬⅩⅩⅩ","ⅩⅭ"},
            Roman_unit_100={"","Ⅽ","ⅭⅭ","ⅭⅭⅭ","ⅭⅮ","Ⅾ","ⅮⅭ","ⅮⅭⅭ","ⅮⅭⅭⅭ","ⅭⅯ"},
            Roman_unit_1000={"","Ⅿ","ⅯⅯ","ⅯⅯⅯ"};
    /**
     * Unicode,JISで定義されているローマ数字の大文字を利用して文字を出力します。<br>
     * 11以上の数にはフォントが対応していない場合があります。<br>
     * 0以下、4000以上の数はアラビアン数字になります。
     * @param i
     * @return
     */
    public static String unitRoman(int i){
        if(i<=0 || i >=4000){
            return arabic(i);
        }
        StringBuilder sb = new StringBuilder();
        
        int n = i/1000;
        sb.append(Roman_unit_1000[n]);
        i=i%1000;
        n = i/100;
        sb.append(Roman_unit_100[n]);
        i=i%100;
        if(i==11){
            sb.append("Ⅺ");
        }else if(i==12){
            sb.append("Ⅻ");
        }else{
            n = i/10;
            sb.append(Roman_unit_10[n]);
            n = i%10;
            sb.append(Roman_unit_1[n]);
        }
        return sb.toString();
    }
    
}
