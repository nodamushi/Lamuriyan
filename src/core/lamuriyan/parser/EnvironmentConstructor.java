package lamuriyan.parser;

import lamuriyan.parser.node.env.Environment;

public interface EnvironmentConstructor{
    /**
     * 環境のオブジェクトを作成します。<br>
     * また、必要ならば、TeXParserEngineに対してコマンドの定義なども行ったりします。
     * @param engine
     * @param factory
     * @return
     */
    public Environment create(LamuriyanEngine engine,EnvironmentFactory factory);
}