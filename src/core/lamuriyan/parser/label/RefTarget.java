package lamuriyan.parser.label;

import lamuriyan.parser.node.LmID;

public class RefTarget{
    public final LmID id;
    public final String refvalue;
    public final String labelName;
    public RefTarget(LmID id,String refvalue,String labelname){
        this.id = id;
        this.refvalue = refvalue;
        this.labelName = labelname;
    }
    
}
