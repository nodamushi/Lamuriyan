package lamuriyan.html;

import lamuriyan.parser.node.LmNode;
import lamuriyan.parser.node.env.RootDocument;

import org.w3c.dom.Node;


/**
 * Converterを実装するクラスはStringを一つ受け取るコンストラクターを実装してください。 
 */
public interface Converter{
    
    /**
     * nodeを変換する作業を請け負うことが出来るかどうかを返してください。<br>
     * nodeに変更処理は加えないでください。
     * @param node
     * @param source
     * @param root
     * @param property 
     * @return
     */
    public boolean acceptable(LmNode node,HTMLConverter source,RootDocument root,String property);
    
    /**
     * 変換処理を行います。ConverterはNodeの子要素に対しても変換処理の義務があります。<br>
     * ElementやNode、Attrなどの生成にはHTMLConverterの各メソッドを利用してください。<br>
     * 子要素に対して変換処理をHTMLConverterに任せたい場合はconvertToDOMメソッドに委譲してください<br>
     * 渡されたノードの子要素全ての変換処理を行い、結果を返すメソッドです。
     * @param convertnode 変換するノード
     * @param source 変換処理を行っているHTMLConverter
     * @param root
     * @param proerty
     * @return 変換後の結果
     */
    public Node convert(LmNode convertnode,HTMLConverter source,RootDocument root,String property)throws Exception;
}
