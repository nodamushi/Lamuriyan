package lamuriyan.parser.label;

import lamuriyan.parser.node.LmNode;

public class Label{
    public final LmNode node;
    public final String refvalue;
    /**
     * 
     * @param node 対象ノード
     * @param value \refで参照されたときに表示する文字
     */
    public Label(LmNode node,String value){
        this.node = node;
        this.refvalue = value;
    }
}
