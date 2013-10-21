package lamuriyan.html;

import static java.util.Objects.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.*;

import lamuriyan.html.HTMLConvertSetting.*;
import lamuriyan.parser.node.LmAttr;
import lamuriyan.parser.node.LmElement;
import lamuriyan.parser.node.LmNode;
import lamuriyan.parser.node.LmTextNode;
import lamuriyan.parser.node.env.Environment;
import lamuriyan.parser.node.env.RootDocument;

import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.xerces.xni.*;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.cyberneko.html.filters.DefaultFilter;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;


public class HTMLConverter{

    public static final ArrayList<String> singleTags = new ArrayList<>();
    public static final ArrayList<String> noNewlineTags = new ArrayList<>();
    public static final ArrayList<String> noNewlineWhenStartChild=new ArrayList<>();
    
    
    static{
        Collections.addAll(singleTags 
                ,"img"
                ,"br"
                ,"input"
                ,"hr"
                ,"meta"
                ,"embed"
                ,"area"
                ,"base"
                ,"col"
                ,"keygen"
                ,"link"
                ,"param"
                ,"source"
                );
        Collections.addAll(noNewlineTags
                ,"span"
                ,"img"
                ,"em"
                ,"mi"
                ,"mo"
                ,"mn"
                ,"mtext","pre"
                );
        Collections.addAll(noNewlineWhenStartChild
                ,"p","pre"
                ,"h1","h2","h3"
                ,"h4","h5","h6"

                );
    }
    

    public static void toString(StringBuilder sb,Document d,String indent,boolean newline){
        toString(sb,d.getDocumentElement(),indent,0,newline?"\n":"");
    }


    public static void toString(StringBuilder sb,Node d,String indent,boolean newline){
        toString(sb,d,indent,0,newline?"\n":"");
    }

    public static void toString(StringBuilder sb,Node d,String indent,int currentindent,String newline){
        boolean hasChild = d.hasChildNodes();
        String tagname = d.getNodeName().toLowerCase();
        for(int i=0;i<currentindent;i++)sb.append(indent);
        if("#text".equals(tagname)){
            sb.append(d.getNodeValue());
            return;
        }
        boolean newlinetag =!noNewlineTags.contains(tagname); 
        boolean closetag = !singleTags.contains(tagname);
        sb.append("<").append(tagname);
        if(d.hasAttributes()){
            NamedNodeMap map = d.getAttributes();
            for(int i=0;i<map.getLength();i++){
                Node n=map.item(i);
                String atrname = n.getNodeName();
                String atrvalue = n.getNodeValue();
                sb.append(" ").append(atrname);
                if(atrvalue!=null){
                    sb.append("=\"").append(atrvalue).append("\"");
                }
            }
        }
        sb.append(">");

        if(closetag){
            if(hasChild){
                toStringChildNode(sb, d, indent, currentindent, newline);
            }
            sb.append("</").append(tagname).append(">");
        }
        if(newlinetag)sb.append(newline);
    }
    
    public static void toStringChildNode(StringBuilder sb,Node d,String indent,int currentindent,String newline){
        String tagname = d.getNodeName().toLowerCase();
        boolean newlinetag =!noNewlineTags.contains(tagname); 
        boolean pre = "pre".equals(tagname);
        boolean nonewline=noNewlineWhenStartChild.contains(tagname);
        if(newlinetag && !nonewline)sb.append(newline);
        NodeList list = d.getChildNodes();
        for(int i=0;i<list.getLength();i++){
            toString(sb,list.item(i),pre?"":indent,currentindent+1,pre?"":newline);
        }
        if(!pre)for(int i=0;i<currentindent;i++)sb.append(indent);
    }


    private DOMFragmentParser parser=new DOMFragmentParser();
    private Document document;
    private RootDocument rootdoc;
    private HTMLConvertSetting setting;
    private List<String> skip,ignore;
    private List<PageLinkObject> reflink = new ArrayList<>();
    public HTMLConverter(RootDocument rootDocument,Path settingfile) throws FileNotFoundException{
        this(rootDocument, new HTMLConvertSetting(settingfile));
    }
    
    public HTMLConverter(RootDocument rootDocument,HTMLConvertSetting setting){
        this.setting = setting;
        document = new HTMLDocumentImpl();
        XMLDocumentFilter[] filters = {new NbspRemover()};
        try {
            parser.setFeature(
                    "http://cyberneko.org/html/features/balance-tags/document-fragment",
                    true);
            parser.setFeature(
                    "http://cyberneko.org/html/features/scanner/notify-builtin-refs",
                    true);
            parser.setProperty(
                    "http://cyberneko.org/html/properties/default-encoding", "UTF-8");
            parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            e.printStackTrace();
        }
        rootdoc = requireNonNull(rootDocument,"RootDocument is null!");
        skip = setting.getSkipRules();
        if(!skip.contains("skiptag")){
            skip.add("skiptag");
        }
        ignore = setting.getIgnoreRules();
    }


    public void convert(){
        preprocess();
        makeMarkerBlock();
        convertToDOM(rootdoc, document.getDocumentElement());
        removeAttribute();
//        speedTest();
        postprocess();
    }


    public void addRefLink(PageLinkObject obj){
        reflink.add(obj);
    }
    
    public List<PageLinkObject> refs(){
        return reflink;
    }
    
    /**
     * Elementを別ページに内容を移動させることで変化するページ内リンクの変更処理をします。
     * @param e 別ページに移動させる要素
     * @param url 別ページのURL
     */
    public void fixLink(Element e,String url){
        for(PageLinkObject p:reflink){
            if(e == p.id.getOwnerElement()){
                p.href.setValue(url);
            }else if(isContainNode(e, p.id)){
                if(!isContainNode(e, p.href)){
                    p.href.setValue(url+p.href.getValue());
                }
            }
        }
    }
    
    //ハッシュマップとリストどっちが検索が早いのか試してみたんだけど、
    //実行時によって割と結果が全然違うんだよね………　リストの方が早かったりマップの方が早かったり。
//    private void speedTest(){
//        List<String> full = setting.fullAccepts();
//        HashMap<String, AttributeRule> rules = setting.getMap();
//        long time = System.currentTimeMillis();
//        int k=0;
//        for(int i=0;i<10000;i++){
//            if(full.contains("onvolumechange")){
//                k++;
//            }
//        }
//        long tim2 = System.currentTimeMillis();
//        int l=0;
//        for(int i=0;i<10000;i++){
//            AttributeRule rule = rules.get("onvolumechange");
//            if(rule.isMatch("div")){
//                l++;
//            }
//        }
//        long time3 = System.currentTimeMillis();
//
//        System.out.println("リストでかかった時間は"+(tim2-time)+"ミリ秒　　"+k+"回マッチ");
//        System.out.println("マップでかかった時間は"+(time3-tim2)+"ミリ秒　　"+l+"回マッチ");
//
//    }

    /**
     * hcvファイルの基本で定義されている以外のユーザー定義のルールを返します。<br>
     * この文字列は、コメントや空行、最初の空白、タブなどは除去されています。
     * @param rulename
     * @return
     */
    public String getRule(String rulename){
        return setting.getRule(rulename);
    }

    public Document getDocument(){
        return document;
    }

    public String toHTML(){
        StringBuilder sb = new StringBuilder();
        toString(sb, document, "", true);
        return sb.toString();
    }

    private void preprocess(){
        for(ProcessRule p:setting.beforeRules()){
            Document d;
            try {
                d = p.instance.process(rootdoc, document, this, true);
                if(d!=null){
                    document = d;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void postprocess(){
        for(ProcessRule p:setting.afterRules()){
            Document d;
            try {
                d = p.instance.process(rootdoc, document, this, false);
                if(d!=null){
                    document = d;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public boolean isSkipNode(LmNode n){
        return skip.contains(getHTMLTagName(n));
    }
    
    public boolean isIgnore(LmNode n){
        return ignore.contains(getHTMLTagName(n));
    }
    
    private void removeAttribute(){
        NodeList all = document.getElementsByTagName("*");
        HashMap<String, List<AttributeRule>> rule=setting.getCreateAttributeMap();
        for(int i=0,e=all.getLength();i<e;i++){
            Node n = all.item(i);
            if(n instanceof Element){
                _removeAttribute((Element)n,rule);
            }
        }
    }
    
    
    private void _removeAttribute(Element node,Map<String, List<AttributeRule>> rule){
        NamedNodeMap attrs = node.getAttributes();
        //*が選言されている場合、ブラックリストではなく、ホワイトリストとみなす。
        //すなわち、ルールが選言されていない属性は残すようになる。
        final boolean isblacklist=!rule.containsKey("*");
        String nodename = node.getNodeName();
        for(int i=attrs.getLength()-1;i>-1;i--){
            Attr attr = (Attr)attrs.item(i);
            String name = attr.getName().toLowerCase();
            if(name.startsWith("data-"))continue;
            if(rule.containsKey(name)){
                List<AttributeRule> rs = rule.get(name);
                boolean b=isblacklist;//属性に対してマッチするルールが無かったどうかのフラグ。
                //ルールは後から選言した物を優先する。
                for(int li=rs.size()-1;i>-1;i--){
                    AttributeRule r = rs.get(li);
                    if(r!=null && r.isMatch(nodename)){
                        b=false;//ルールが見つかったフラグを残しておく。
                        if(r.isAccept){
                            if(!r.hasValue){
                                attr.setValue(null);
                            }
                        }else{
                            node.removeAttributeNode(attr);
                        }
                        break;//ルールが見つかったので終了。
                    }
                }
                if(b){//ブラックリストで、かつ、どのルールも満たさなかった場合は属性を削除。
                    node.removeAttributeNode(attr);
                }
            }else{
                node.removeAttributeNode(attr);
            }
        }
        
    }
    

    /**
     * 渡されたparentの子要素をNodeに変換し、putParentにappendしていきます。parent自体は変換しません。
     * @param parent この要素の子要素をDOMに変換します。nullの場合は処理をしません。
     * @param putParent 生成したNodeをappendする対象です。nullの場合処理をしません。
     */
    public void convertToDOM(LmElement parent,Node putParent){
        if(parent==null || putParent==null)return;
        for(LmNode n:parent.getChildren()){
            String tname = getHTMLTagName(n);
            if(n.isIgnore()||isIgnore(n)){
                continue;
            }
            

            if(isSkipNode(n)){
                if(n.isElement()){
                    DocumentFragment df = createDocumentFragment();
                    convertToDOM((LmElement)n, df);
                    putParent.appendChild(df);
                }
            }else{
                List<ConvertRule> crule = setting.getConvertRules();
                boolean bb=false;
                for(ConvertRule cc:crule){
                    if(cc.tagname.equals(tname)){
                        if(cc.instance.acceptable(n, this, rootdoc,cc.property)){
                            bb=true;
                            try {
                                Node node=cc.instance.convert(n, this, rootdoc,cc.property);
                                if(node!=null)
                                    putParent.appendChild(node);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                }
                if(bb)continue;
                Node nn=createNode(n);
                putParent.appendChild(nn);
                if(n.isElement()){
                    putParent.appendChild(nn);
                    convertToDOM((LmElement)n, nn);
                }
            }
        }
    }

    private void makeMarkerBlock(){
        List<BlockRule> blockrules = setting.getBlockRoles();
        for(BlockRule rule:blockrules){
            _makeMarkerBlock(rule, rootdoc, -1);
        }
    }
    private void _makeMarkerBlock(BlockRule rule,LmElement parent,int depth){
        LmElement e=null;
        int d=depth;
        Stack<Integer> stack = new Stack<>();
        Stack<LmElement> estack = new Stack<>();
        ArrayList<LmNode> newchildlist = new ArrayList<>();
        for(LmNode node:parent.getChildren()){
            String name = getHTMLTagName(node);
            if(!node.isElement()&&name.startsWith("block-"))IF:{
                name = name.substring(6);
                boolean end = false;
                if(name.startsWith("end-")){
                    name = name.substring(4);
                    end = true;
                }
                BlockElement el = rule.get(name);
                if(el==null)break IF;
                if(!end){
                    if(el.depth<=depth){
                        System.err.println("階層構造が間違っています："+node.toString());
                        continue;
                    }

                    if(d>el.depth){
                        while(d>el.depth){
                            d = stack.pop();
                            e = estack.pop();
                        }
                    }

                    if(d<el.depth){
                        estack.push(e);
                        stack.push(d);
//                        TeXElement m=e ;
                        e= new LmElement(el.tagname);
                        d = el.depth;
                    }else{
                        e = new LmElement(el.tagname);
                        d = el.depth;
                    }

                    if(estack.size()==1){
                        e.setParent(parent);
                        newchildlist.add(e);
                    }else{
                        estack.peek().add(e);
                    }

                    Collection<LmAttr> attrs = node.getAttrs();
                    for(LmAttr ta:attrs){
                        e.setAttr(ta);
                    }
                }else{
                    if(depth<el.depth && estack.size()!=0 && (d==el.depth || stack.contains(el.depth))){
                        while(d>=el.depth){
                            e = estack.pop();
                            d = stack.pop();
                        }
                    }else{
                        System.err.println("階層終了タグの構造が間違っています："+node.toString());
                    }
                }
                continue;
            }

            if(e!=null){
                e.add(node);
            }else{
                newchildlist.add(node);
            }

            if(node.isElement()){
                LmElement te = (LmElement)node;
                _makeMarkerBlock(rule, te, d);
            }
        }
        parent.getChildren().clear();
        parent.getChildren().addAll(newchildlist);
    }


    /**
     * ノードにノードリストを全て追加します。
     * @param e
     * @param append
     */
    public static void appendAllNode(Node e,NodeList append){
        for(int i=0;i<append.getLength();i++){
            e.appendChild(append.item(i));
        }
    }
    /**
     * ノードにリストのノードを全て追加します。
     * @param e
     * @param append
     */
    public static void appendAllNode(Node e,List<Node> append){
        for(Node n:append){
            e.appendChild(n);
        }
    }

    /**
     * ノードの子要素をリストに追加して返します。
     * @param node
     * @return
     */
    public static List<Node> getChildNodes(Node node){
        ArrayList<Node> n = new ArrayList<>();
        Node f = node.getFirstChild();
        if(f!=null){
            while(f!=null){
                n.add(f);
                f=f.getNextSibling();
            }
        }
        return n;
    }

    /**
     * 文字列となっているHTMLソースコードからDOMFragmentを作成します。<br>
     * 例外が発生した場合はnullが返ります。
     * @param html
     * @return
     */
    public DocumentFragment parseHTML(String html){
        InputSource source = new InputSource(new StringReader(html));
        try {
            DocumentFragment fragment = createDocumentFragment();
            parser.parse(source, fragment);
            return fragment;
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * TeXNodeからHTMLタグの名前を取得します。<br>
     * EnvironmentはgetNameと変換されるHTMLの名前が異なります。<br>
     * TeXNode.getName()を利用しないでください。<br>
     * この取得される名前は変換ルールによって変換されていない、TeXで生成された名前です。<br>
     * @param e
     * @return
     */
    public String getHTMLTagName(LmNode e){
        if(e instanceof Environment){
            return ((Environment) e).getTagName();
        }else return e.getName();
    }

    /**
     * TextTeXNodeをNodeに変換します。<br>
     * このTextTeXNodeに属性が定義されていない場合はTextノードを作成して返します。<br>
     * 属性が定義されている場合は、spanエレメントでTextノードを囲い、そのspanエレメントに属性を定義します。
     * @param node
     * @return
     */
    public Node createText(LmTextNode node){
        Text text= createText(node.getValue());
        if(!LmTextNode.TEXT_NODE_NAME.equals(node.getName())){//#textでない場合はそのタグで囲う
            Element e = createElement(node.getName());
            setAttrs(e,node);
            e.appendChild(text);
            return e;
        }
        if(node.getAttrs().size()==0){//ただのテキスト
            return text;
        }else{//spanで囲う
            Element span = createElement("span");
            setAttrs(span, node);
            span.appendChild(text);
            return span;
        }
    }

    /**
     * TeXNodeの持つHTMLタグの名前でエレメントを作成し、TeXNodeの持つ属性を設定して返します。<br>
     * ただし、TeXTextNodeの場合のみノードになります。<br>
     * TeXTextNode以外は必ずNodeはElementになります。
     * 子要素は作成しません。
     * @param t
     * @return
     */
    public Node createNode(LmNode t){
        if(t instanceof LmTextNode){
            return createText((LmTextNode)t);
        }
        Element e = createElement(getHTMLTagName(t));
        setAttrs(e,t);
        return e;
    }
    

    /**
     * DocumentFragmentを作成して返します。<br>
     * 親を持たない兄弟を返したいときなどに利用します。
     * @return
     */
    public DocumentFragment createDocumentFragment(){
        return document.createDocumentFragment();
    }

    /**
     * TeXNodeに設定されているAttrを全てElementに設定します。
     * @param e
     * @param t
     */
    public void setAttrs(Element e,LmNode t){
        for(LmAttr at:t.getAttrs()){
            e.setAttributeNode(createAttr(at));
        }
    }

    /**
     * TeXAttrをAttrに変換します。<br>
     * 変換したAttrはTeXAttrのsetConvertedDOMに登録します。<br>
     * 変換処理はなるべくこれを用いてください。
     * @param ta
     * @return
     */
    public Attr createAttr(LmAttr ta){
        if(ta.getConvertedDOM()!=null)return ta.getConvertedDOM();
        Attr ret= createAttr(ta.getName(),ta.getValue());
        ta.setConvertedDOM(ret);
        return ret;
    }

    /**
     * org.w3c.dom.Elementを生成します
     * @param tagname
     * @return
     */
    public Element createElement(String tagname){
        return document.createElement(tagname);
    }

    /**
     * org.w3c.dom.Textを生成します
     * @param text
     * @return
     */
    public Text createText(String text){
        return document.createTextNode(text);
    }

    /**
     * org.w3c.dom.Commentを生成します
     * @param text
     * @return
     */
    public Comment createComment(String text){
        return document.createComment(text);
    }

    /**
     * org.w3c.dom.Attrを生成します
     * @param name
     * @return
     */
    public Attr createAttr(String name){
        return document.createAttribute(name);
    }
    /**
     * org.w3c.dom.Attrを生成します
     * @param name
     * @return
     */
    public Attr createAttr(String name,String value){
        Attr attr = document.createAttribute(name);
        if(value!=null)
            attr.setValue(value);
        return attr;
        
    }

    public static void addAll(Collection<Node> collection,NodeList nodelist){
        for(int i=0,e=nodelist.getLength();i<e;i++){
            collection.add(nodelist.item(i));
        }
    }
    
    /**
     * 子要素の子要素まで辿り、eの中にnodeがあるかどうか検索をする。<br>
     * nodeがAttrの場合はその親を使います。
     * @param e
     * @param node
     * @return
     */
    public boolean isContainNode(Element e,Node node){
        if(node instanceof Attr){
            node = ((Attr) node).getOwnerElement();
        }
        List<Node> a=new ArrayList<>(),b=new ArrayList<>(),imp;
        addAll(b, e.getChildNodes());
        while(!b.isEmpty()){
            imp = a;
            a=b;
            b=imp;
            b.clear();
            for(Node n:a){
                if(n == node)return true;
                else{
                    if(n.hasChildNodes()){
                        Element el =(Element)n;
                        addAll(b,el.getChildNodes());
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 直下の子要素にnodeが含まれるかどうかを返します。<br>
     * nodeがAttrの場合はその親を使います。
     * @param e
     * @param node
     * @return
     */
    public boolean hasChildNode(Element e,Node node){
        if(node instanceof Attr){
            node = ((Attr) node).getOwnerElement();
        }
        NodeList nodelist = e.getChildNodes();
        for(int i=0,l=nodelist.getLength();i<l;i++){
            Node n = nodelist.item(i);
            if(node==n)return true;
        }
        
        return false;
    }
    


    //デフォルト状態だと私がせっっっっっっっっっっっっっっかく生成した&nbsp;を置換しやがる糞仕様なので、
    //置換させないようにする
    //
    //内容は以下のURLから丸コピ 
    //  http://gwt-test-utils.googlecode.com/svn-history/r1354/src/framework/branches/
    //    gwt-test-utils-0.22-branch/gwt-test-utils/src/main/java/com/octo/gwt/test/internal/utils/
    //     GwtHtmlParser.java
    //何やってるのかは全く知らんヽ( ・∀・)ﾉ

    private static class NbspRemover extends DefaultFilter {

        private static final String NBSP_ENTITY_NAME = "nbsp";
        private static final String AND_ENTITY_NAME  ="amp";
        private static final String LT_ENTITY_NAME = "lt";
        private static final String GT_ENTITY_NAME = "gt";
        boolean inEntityRef;

        XMLString nbspXMLString,andXMLString,ltXMLString,gtXMLString;

        private NbspRemover() {
            nbspXMLString = new XMLString();
            char[] c = {'&', 'n', 'b', 's', 'p', ';'};
            nbspXMLString.setValues(c, 0, 6);
            char[] amp = {'&','a','m','p',';'};
            andXMLString=new XMLString();
            andXMLString.setValues(amp, 0, 5);
            ltXMLString=new XMLString();
            char[] lt = {'&','l','t',';'};
            ltXMLString.setValues(lt,0,4);
            gtXMLString=new XMLString();
            char[] gt = {'&','g','t',';'};
            gtXMLString.setValues(gt,0,4);
        }

        @Override
        public void characters(XMLString text, Augmentations augs)
                throws XNIException {

            if (!inEntityRef) {
                super.characters(text, augs);
            }
        }

        @Override
        public void endGeneralEntity(String name, Augmentations augs)
                throws XNIException {

            inEntityRef = false;
        }

        @Override
        public void startDocument(XMLLocator locator, String encoding,
                Augmentations augs) throws XNIException {

            super.startDocument(locator, encoding, augs);
            inEntityRef = false;
        }

        @Override
        public void startGeneralEntity(String name, XMLResourceIdentifier id,
                String encoding, Augmentations augs) throws XNIException {
            if (NBSP_ENTITY_NAME.equals(name)) {
                inEntityRef = true;
                super.characters(nbspXMLString, augs);
            } else if(AND_ENTITY_NAME.equals(name)){
                inEntityRef=true;
                super.characters(andXMLString, augs);
            }else if(LT_ENTITY_NAME.equals(name)){
                inEntityRef=true;
                super.characters(ltXMLString, augs);
            }else if(GT_ENTITY_NAME.equals(name)){
                inEntityRef=true;
                super.characters(gtXMLString, augs);
            }else {
                super.startGeneralEntity(name, id, encoding, augs);
            }
        }
    }
    
}
