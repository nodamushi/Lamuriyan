package lamuriyan.parser.macro;

import lamuriyan.parser.node.LmNode;

/**
 * navなど目次を作成する際に利用するデータオブジェクト。<br>
 * 階層（depth）と、表示する番号(number)、内容(content)、目次タイプ(type)を保持する。<br>
 * これらをどう利用するかは利用側に全て一任し、IndexItem自体は何も定義しない。<br>
 * 参考までに、lamuriyan.html.NaviConverterではdepthはchapterを0,sectionを1、subsectionを2,subsubsectionを3,subsubsubsectionを4とし、
 * 設定された範囲のdepthでRootDocumentに登録されている順に目次を生成する。これらのtypeはchapterはchapter、section系はsectionとなっている。
 * 0でないchapterや、0のsection、その他の名前の目次タイプは無視される。
 * <br><br>
 * 登録には\@indexitem{ノード}{階層}{番号}{内容}{目次タイプ}を利用する。<br>
 * RootDocumentからの取得は、getIndexItemsを用いる。
 * 
 * @author nodamushi
 *
 */
public class IndexItem{

    public final int depth;
    public final String number,type;
    public final LmNode content;
    public final LmNode target;
    
    public IndexItem(int depth,String number,LmNode content,String type,LmNode target){
        this.depth = depth;
        this.number = number;
        this.content = content;
        this.type = type;
        this.target = target;
    }
    
    
}
