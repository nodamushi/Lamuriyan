package lamuriyan.parser.token;

public enum TokenType{
    CHAR,COMMENT,ESCAPE,ARGUMENT,SPACE,NEWLINE
    ,BEGINGROUP,ENDGROUP,
    
    /**{～}の間のトークン*/
    __TOKEN__GROUP__,
    //以下は特殊な場合でしか生成されない
    /**数値を必要とするマクロの引数を探しているときに生成される*/
    __NUMBER__,
    /**パターンマッチ列*/
    __PATTERN__GROUP__MATCH__,
    __VERB__,
//    /**FullExpandAreaでの展開時に、マクロ展開が無視されるトークン。<br>
//     * 無視された後はESCAPEトークンに変換すること*/
//    __NOEXPAND_TOKEN__,
//    /**\\unexpandedとかいうので展開しなかったトークン列*/
//    __UNEXPAND_TOKENS__,
    /**NUMBERを探しているときにフラグとして使う*/
    __EXPANDMARKER__,
    /**特に何でもいいけど、フラグとして欲しいとき*/
    UNDEFINED,
    
    BEGINMATHMODEGROUP,//mathモードで{}を単にスコープとして使いたいときに、無限ループに陥るのを防ぐ為のグループ開始トークン
    ENDMATHMODEGROUP,//BEGINMATHMODEGROUPに対応するグループ
    MATHESCAPE//MathEscapeを格納したトークン　数式モードでのみ有効
    
}
