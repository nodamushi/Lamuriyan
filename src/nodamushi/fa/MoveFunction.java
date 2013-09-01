package nodamushi.fa;

public interface MoveFunction<E>{
    /**
     * 状態遷移を行う関数です。<br>
     * FAは現在の状態で登録されているMoveFunctionを全て実行し、-1でない結果が得られた場合、
     * その遷移先に状態を遷移します。<br>
     * 複数の遷移先が見つかった場合は非決定性オートマトンとして動いている場合は、全ての状態を保存します。<br>
     * 決定性オートマトンとして動いている場合は、プログラムエラーとしてDFAStateExceptionを発生させます。<br>
     * 非決定性オートマトンとして動く場合、イプシロン遷移として、inputにnullが渡されるます。
     * @param input マシンに入力された値です。
     * @param currentstate この関数が実行された際の状態です。
     * @return 状態遷移先がある場合は、その遷移先を返します。<br>
     * @throws NullPointerException イプシロン遷移を考慮せずにNullPointerExceptionが発生した場合はAutomaton側で対処します。<br>
     * throwsを付け忘れないで下さい。
     * 遷移先がない場合は-1を返して下さい。
     */
    public int move(E input,int currentstate) throws NullPointerException;
}
