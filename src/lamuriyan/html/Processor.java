package lamuriyan.html;

import lamuriyan.parser.node.env.RootDocument;

import org.w3c.dom.Document;


/**
 * 前処理、後処理を定義するインターフェースです。<br>
 * このインターフェースを実装したクラスは無引数コンストラクタを持つ必要があります。
 * @author nodamushi
 *
 */
public interface Processor{
    /**
     * 前処理、後処理を行います。
     * @param texsource 変換元のTeXが作ったドキュメントです
     * @param document 変換結果を格納する、もしくは変換後のDocumentです。
     * @param converter 変換を行う呼び出し元のHTMLConverterオブジェクトです
     * @param isPreProcess この処理が前処理か、後処理かを示すフラグです。
     * @return 一般には同じdocumentを返してください。全く違うDocumentに作り替えた場合はそのDocumentを返してください。
     */
    public Document process(RootDocument texsource,Document document,HTMLConverter converter,final boolean isPreProcess)
    throws Exception;
}
