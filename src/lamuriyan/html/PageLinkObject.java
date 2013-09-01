package lamuriyan.html;

import lamuriyan.parser.node.LmID;

import org.w3c.dom.Attr;

public class PageLinkObject{
    /**
     * リンク先のid
     */
    public final Attr id;
    /**
     * リンクの属性
     */
    public final Attr href;
    /**
     * idの変換元
     */
    public final LmID texid;
    public PageLinkObject(Attr id,Attr href,LmID texid){
        this.id = id;
        this.href = href;
        this.texid =texid;
    }
}
