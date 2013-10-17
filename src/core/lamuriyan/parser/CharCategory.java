package lamuriyan.parser;

/**
 * TeXのCharCategoryの番号に相当するenum型<br>
 * Invalid(15)はLamuriyanでは扱わない物とし、無視した。
 * @author nodamushi
 *
 */
public enum CharCategory{
    ESCAPE(0),BEGINGROUP(1),ENDGROUP(2),DOLLAR(3),AND(4),NEWLINE(5),PARAMETER(6),UPPER(7),UNDERBAR(8),IGNORE(9),SPACE(10),
    ALPHABET(11),OTHERCHARACTOR(12),ACTIVE(13),COMMENT_START(14),//Invalid 標準入力は想定しなくていいじゃね
    ;
    /**
     * TeXのCharCategoryに対応する番号
     */
    public final int number;
    /**
     * TeXのCharCategoryの番号から対応するenum型を取得
     * @param charcategorynumber
     * @return
     */
    public static CharCategory getCharCategory(int charcategorynumber){
        if(charcategorynumber<0 || charcategorynumber > 14)return null;
        switch (charcategorynumber) {
            case 0:
                return ESCAPE;
            case 1:
                return BEGINGROUP;
            case 2:
                return ENDGROUP;
            case 3:
                return DOLLAR;
            case 4:
                return AND;
            case 5:
                return NEWLINE;
            case 6:
                return PARAMETER;
            case 7:
                return UPPER;
            case 8:
                return UNDERBAR;
            case 9:
                return IGNORE;
            case 10:
                return SPACE;
            case 11:
                return ALPHABET;
            case 12:
                return OTHERCHARACTOR;
            case 13:
                return ACTIVE;
            default:
                return COMMENT_START;
        }
    }
    
    
    private CharCategory(int num){number = num;}
}
