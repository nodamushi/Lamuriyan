package lamuriyan.parser.macro;

/**
 * 数式モードの実体参照を定義するクラス。<br>
 * 仮に、\sumを\def\sum{\@startchargroup o&0x3a3;\@endchargroup}と定義した場合、
 * 新たに\sumを利用してグループを定義したい場合、\@startchargroup i\sum\sum\@endchargroupの様に書くと、仕様上エラーとなってしまう。<br>
 * グループに上書きを許可したとしても、\sumはmiにはならない。&lt;mi>&lt;mo>Σ&lt;/mo>&lt;mo>Σ&lt;/mo>&lt;/mi>となるだけである。<br>
 * また、\sumの\sp,\sbはデフォルトでは上下に付くが、これを変更したいときにも困るであろう。<br><br>
 * これらを避ける為にエスケープ文字は\@startchargroupを用いなくてもmi,mo,mn,mtextに分けられなくてはならない。<br><br>
 * そのために、このクラスは作られた。
 */
public class MathEscape{
    public final String value;//&ApplyFunction;とか
    //以下の二つは\@stargchargroupが宣言されている間は無視される。
    public final String type;//mo mi mn mtextとか
    public final boolean isOverUnder;//sb,spでmundr,moverになるかどうかのフラグ。
    public final String mathvariantAttr;
    
    
    public MathEscape(String value,String type,boolean isOverUnder,String mathvaliant){
        this.value = value;
        this.type = type;
        this.isOverUnder=isOverUnder;
        mathvariantAttr = mathvaliant;
    }
}
