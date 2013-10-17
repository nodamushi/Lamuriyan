import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.*;

import lamuriyan.html.HTMLConverter;
import lamuriyan.html.PageLinkObject;
import lamuriyan.parser.LamuriyanEngine;
import lamuriyan.parser.node.env.RootDocument;



/**
 * Lamuriyanを起動する為のランチャー的クラス
 * @author nodamushi
 *
 */
public class Main{
    
    
    public static void main(String[] args) throws Exception{
        CommandLineOption op = new CommandLineOption(args);
        CommandLineOption.Option[] ops = op.getOptions();
        
        String fn=null,output=null,baseDir=".",inits=null,setting=null;
        for(CommandLineOption.Option o:ops){
            if(o.name==null){
                fn = o.value;
            }else{
                switch(o.name){
                    case "-o":
                    case "--output":
                        output = o.value;
                        break;
                    case "-d":
                    case "--directory":
                        baseDir=o.value;
                        break;
                    case "-s":
                    case "--setting":
                        setting=o.value;
                        break;
                    case "-i":
                    case "--init":
                        inits = o.value;
                        break;
                    case "-h":
                    case "--help":
                        System.out.println("[-OPTIONS] -s settingfile1[,settigfile2] input file name");
                        System.out.println("    -d,--direcoty:作業ディレクトリを指定します。指定しない場合は弦座のディレクトリになります。");
                        System.out.println("    -i,--init    :ファイルを読み込む前に実行するマクロの初期化ファイルです。必須オプションです。");
                        System.out.println("    -o,--output  :出力ファイル名を指定してください");
                        System.out.println("    -s,--setting :RootDocumentをHTMLDOMに変換する為の設定ファイルです。必須オプションです。");
                        System.out.println("    -h,--help    :このヘルプを出力します");
                        return;
                }
            }
        }
        
        if(fn==null){
            System.out.println("開発のテストモードで起動します。");
            testMain();
            return;
//            System.out.println("入力ファイルを指定してください");
//            return;
        }
        if(inits==null){
            System.err.println("マクロ初期化ファイルが指定されていません。");
            return;
        }
        
        if(setting==null){
            System.err.println("設定ファイルが指定されていません。");
            return;
        }
        
        Path base = Paths.get(baseDir).toAbsolutePath();
        Path file = base.resolve(fn);
        if(!Files.isReadable(file)){
            System.err.println("ファイルを読み込むことが出来ません。"+file.toString());
            return;
        }
        LamuriyanEngine engine = new LamuriyanEngine(fn, base);
        String[] s = inits.split(",");
        ArrayList<Path> paths = new ArrayList<>();
        for(String ss:s){
            paths.add(Paths.get(ss));
        }
        engine.setInitFiles(paths);
        
        engine.evaluate();
        
        RootDocument root = engine.getDocument();
        HTMLConverter converter =  new HTMLConverter(root, Paths.get(setting));
        converter.convert();
        
        if(output==null){
            System.out.println(converter.toHTML());
        }else{
            Path out = Paths.get(output);
            if(!Files.exists(out))
                Files.createFile(out);
            FileWriter fw = new FileWriter(out.toFile());
            fw.write(converter.toHTML());
        }
    }
    
    
    
    

    //主にnodamushiがEclipseから起動してなんやかんややってるときのメソッド。
    private static void testMain() throws Exception{
        String file = "testdataMain.tex";
        LamuriyanEngine e = new LamuriyanEngine(file, "testdata");
        e.setFileSearchDirectories(Paths.get("."));
        e.setInitFiles(Paths.get("testdata\\_init_test.tex"));
        e.evaluate();
        //Lamuriyanの変換完了
        RootDocument root = e.getDocument();
        //コンバート
        HTMLConverter hcon =new HTMLConverter(root,new File("testdata\\convsetting.hcv").toPath());
        hcon.convert();
        List<PageLinkObject> plos=hcon.refs();
        Document document = hcon.getDocument();
        
        NodeList chapters = document.getElementsByTagName("chapter");
        ArrayList<Element> chapterlist = new ArrayList<>();
        for(int i=chapters.getLength()-1;i>=0;i--){
            Node node = chapters.item(i);
            node.getParentNode().removeChild(node);
            chapterlist.add(0,(Element)node);
            
            Element  el = (Element)node;
            hcon.fixLink(el, "index_chapter"+(i+1)+".html");
            
        }
        
        NodeList headcontentlist = document.getElementsByTagName("headcontent");
        
        
        
        StringBuilder headcontent=new StringBuilder();
        for(int i=0,end=headcontentlist.getLength();i<end;i++){
            Node node = headcontentlist.item(i);
            HTMLConverter.toStringChildNode(headcontent, node, "", 0,"\n");
            Attr numbera=((Element)node).getAttributeNode("number");
        }
        StringBuilder contents = new StringBuilder();
        Node documentcontent = document.getElementById("document");
        
        HTMLConverter.toStringChildNode(contents, documentcontent, "", 0, "\n");
        
        //テンプレートファイル読み込み
        String templatefile = "testdata\\template.html";
        List<String> template = Files.readAllLines(Paths.get(templatefile), Charset.forName("utf-8"));
        
        String title = root.getProperty("title");
        if(title==null){
            title="";
        }
        
        
        
        
        ArrayList<String> lis =new ArrayList<>(template.size());
        
        
        File write = new File("testdata\\output\\index.html");
        //        System.out.println(e.current);
        PrintStream s = new PrintStream(write, "UTF-8");
//        s.print(hcon.toHTML());
        for(String str:template){
            if(str.equals("<!--{PAGETITLE}-->")){
//                lis.add(title);
//                System.out.println(title);
                s.println(title);
            }else if(str.equals("<!--{HEADCONTENT}-->")){
//                lis.add(headcontent.toString());
//                System.out.println(headcontent.toString());
                s.println(headcontent);
            }else if(str.equals("<!--{CONTENTS}-->")){
//                lis.add(contents.toString());
//                System.out.println(contents);
                s.println(contents);
            }
            
            else{
//                lis.add(str);
                s.println(str);
            }
        }
        s.close();
        for(int i=0,end=chapterlist.size();i<end;i++){
            Element node = chapterlist.get(i);
            write = new File("testdata\\output\\index_chapter"+(i+1)+".html");
            //        System.out.println(e.current);
            s = new PrintStream(write, "UTF-8");
            contents.setLength(0);
            HTMLConverter.toStringChildNode(contents, node, "", 0, "\n");
            Attr numbera=node.getAttributeNode("number");
            Attr titla =node.getAttributeNode("title");
//            System.out.println(contents);
            title = numbera.getValue()+
                    "　　"+titla.getValue();
            for(String str:template){
                if(str.equals("<!--{PAGETITLE}-->")){
//                    lis.add(title);
//                    System.out.println(title);
                    s.println(title);
                }else if(str.equals("<!--{HEADCONTENT}-->")){
//                    lis.add(headcontent.toString());
//                    System.out.println(headcontent.toString());
                    s.println(headcontent);
                }else if(str.equals("<!--{CONTENTS}-->")){
//                    lis.add(contents.toString());
//                    System.out.println(contents);
                    s.println(contents);
                }
                
                else{
//                    lis.add(str);
                    s.println(str);
                }
            }
            s.close();
        }
        
    }
    
    
    
    private Main(){};
}
