package lamuriyan.parser;

import lamuriyan.parser.node.LmNode;

public interface IDNameFactory{
    /**
     * node,labelnameからnodeに付加するidの名前を生成します。<br>
     * このメソッドはnodeに対して一切変更を加えないでください。<br>
     * なお、すでに登録されている名前が返ってきた場合は、LamuriyanEngineが返ってきた結果の後ろに適当なアルファベットを付加します。
     * @param refvalue
     * @param node
     * @param labelname
     * @return
     */
    public String getIDName(LmNode node,String labelname);
}
