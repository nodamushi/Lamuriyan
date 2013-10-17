package lamuriyan.html;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLConvertSetting{
    
    private static int commentPos(String str){
        int c=str.indexOf('%');
        if(c<=0)return c;
        char bc = str.charAt(c-1);
        if(bc=='\\'){
            boolean escape = true;
            if(c==1)escape=true;
            else{
                char bbc = str.charAt(c-2);
                if(bbc=='\\'){
                    if(c==2)escape=false;
                    else{
                        char bbbc=str.charAt(c-3);
                        escape = bbbc=='\\';
                    }
                }else
                    escape=true;
            }
            if(!escape)return c;
            c = str.indexOf('%',c+1);
            if(c==2||c==-1){
                return c;
            }
            
            while(true){
                bc = str.charAt(c-1);
                if(bc=='\\'){
                    char bbc =str.charAt(c-2);
                    if(bbc=='\\'){
                        char bbbc = str.charAt(c-3);
                        if(bbbc!='\\')break;
                    }
                }else break;
                c = str.indexOf('%',c+1);
                if(c==-1)break;
            }
        }
        
        return c;
    }
    
    private static String trim(String str){
        int c = commentPos(str);
        if(c>=0){
            str = str.substring(0,c);
        }
        return str.trim();
    }
    private int y=0;
    private String fname;
    private String state="plain";
    
     HTMLConvertSetting(Path path) throws FileNotFoundException{
        fname = path.toFile().getName();
        Scanner scan = new Scanner(path.toFile());
        String str;
        
        
        while(scan.hasNext()){
            str=scan.nextLine();y++;
            str = trim(str);//コメントと空白除去
            if(str.isEmpty())continue;
            int p = str.indexOf(':');
            if(p!=-1){//block:とかconvert:とかの定義の開始合図
                String envname = str.substring(0,p);
                if(envname.isEmpty()){
                    error("無効な文字列です");
                    continue;
                }
                state = envname;
                switch(envname){
                    case "block":
                        readBlockSection(scan);
                        break;
                    case "convert":
                        readConvertSection(scan);
                        break;
                    case "skipenvironment":
                        readSkipEnvironment(scan);
                        break;
                    case "ignorenode":
                        readIgnoreNode(scan);
                        break;
                    case "attribute":
                        readAttrribute(scan);
                        break;
                    case "renameclass":
                        readRenameclass(scan);
                        break;
                    case "ignoreclass":
                        readIgnoreClass(scan);
                        break;
                    case "postprocess":
                        readAfter(scan);
                        break;
                    case "preprocess":
                        readBefore(scan);
                        break;
                    case "end":
                        state="plain";
                        break;
                    default:
                        otherRule(scan, envname);
                }
                state = "plain";
            }
        }
        
    }
    
 
    
    private HashMap<String, StringBuilder> map = new HashMap<>();
    //置換文字列用
    private static final String yen="\\", YEN = yen+yen,YP=YEN+"%",YYP=YEN+YEN+"%";
    
    
     String getRule(String name){
        StringBuilder sb = map.get(name);
        if(sb==null){return "";}
        else return sb.toString();
    }
    
    private void otherRule(Scanner scan,String name){
        StringBuilder sb = map.get(name);
        if(sb==null){
            sb=new StringBuilder();
            map.put(name, sb);
        }
        state=name;
        String str;
        while(scan.hasNext()){
            str=scan.nextLine();y++;
            str = trim(str);//コメントと空白除去
            if(str.isEmpty())continue;
            if(str.equals("end:"))break;
            int c = str.charAt('%');
            if(c!=-1){
                str = str.replaceAll(YP, "%").replaceAll(YYP, YP);
            }
            sb.append(str).append("\n");
        }
    }
    
    
    private void error(String message){
        System.err.println("error:["+state+"] "+message+"   ,file:"+fname+" "+y+"行目");
    }

     static class ProcessRule{
         String classname;
         Class<?> clasz;
         Processor instance;
        boolean load() throws ClassNotFoundException, InstantiationException, IllegalAccessException{
            clasz = Class.forName(classname);
            if(!Processor.class.isAssignableFrom(clasz)){
                return false;
            }
            instance = (Processor)clasz.newInstance();
            return true;
        }
    }
    
    
    private ArrayList<ProcessRule> afterrules = new ArrayList<>();
     List<ProcessRule> afterRules(){
        return afterrules;
    }
    private ArrayList<ProcessRule> beforerules = new ArrayList<>();
     List<ProcessRule> beforeRules(){
        return beforerules;
    }
    private void readBefore(Scanner scan){
        String str;
        while(scan.hasNext()){
            str=scan.nextLine();y++;
            str = trim(str);//コメントと空白除去
            if(str.isEmpty())continue;
            if(str.equals("end:"))break;
            ProcessRule r = new ProcessRule();
            r.classname = str;
            try {
                if(r.load()){
                    beforerules.add(r);
                }else{
                    error(str+"はtex.html.Processorを実装していません。");
                }
            } catch (ClassNotFoundException e) {
                error(str+"が見つかりませんでした。");
            } catch (InstantiationException|IllegalAccessException e) {
                error(str+"のインスタンスを生成できませんでした。");
            }
        }
    }
    
    private void readAfter(Scanner scan){
        String str;
        while(scan.hasNext()){
            str=scan.nextLine();y++;
            str = trim(str);//コメントと空白除去
            if(str.isEmpty())continue;
            if(str.equals("end:"))break;
            ProcessRule r = new ProcessRule();
            r.classname = str;
            try {
                if(r.load()){
                    afterrules.add(r);
                }else{
                    error(str+"はtex.html.Processorを実装していません。");
                }
            } catch (ClassNotFoundException e) {
                error(str+"が見つかりませんでした。");
            } catch (InstantiationException|IllegalAccessException e) {
                error(str+"のインスタンスを生成できませんでした。");
            }
        }
    }
    
    
    private ArrayList<String> ignoreclassrules = new ArrayList<>();
    List<String> ignoreClassRules(){
        return ignoreclassrules;
    }
    private void readIgnoreClass(Scanner scan){
        String str;
        while(scan.hasNext()){
            str=scan.nextLine();y++;
            str = trim(str);//コメントと空白除去
            if(str.isEmpty())continue;
            if(str.equals("end:"))break;
            String[] ss = str.split(",");
            for(String s:ss){
                s = s.trim();
                if(s.isEmpty())continue;
                ignoreclassrules.add(s);
            }
        }
    }
    
     static class RenameClassRule{
         String nodename;
         private String[] classes;
         private String afterName;
         String rename(String nodename,String classValue){
            if(nodename!=null&&nodename.equals(this.nodename))return classValue;
            String[] ss = classValue.split(" ");
            //ルールを満たしているかどうかのフラグ
            boolean[] bb=new boolean[classes.length];
            
            for(int i=0;i<ss.length;i++){
                String s=ss[i];
                //クラス名に対して該当するルールがあるかどうか調べる
                for(int k=0;k<classes.length;k++){
                    String cl = classes[k];
                    if(s.equals(cl)){
                        ss[i]="";
                        bb[k]=true;
                        break;
                    }
                }
            }
            //全部のルールを満たしていない場合は変換しない
            for(boolean b:bb){
                if(!b)return classValue;
            }
            StringBuilder sb = new StringBuilder(afterName);
            for(String s:ss){
                if(!s.isEmpty())
                    sb.append(" ").append(s);
            }
            return sb.toString().trim();
        }
    }
    

    private ArrayList<RenameClassRule> renameclassrules = new ArrayList<>();
     List<RenameClassRule> renameClassRules(){
        return renameclassrules;
    }
    
    private void readRenameclass(Scanner scan){
        String str;
        while(scan.hasNext()){
            str=scan.nextLine();y++;
            str = trim(str);//コメントと空白除去
            if(str.isEmpty())continue;
            if(str.equals("end:"))break;
            if(str.contains(">")){
                String[] ss=str.split(">",2);
                ss[0] = ss[0].trim();//ルール部分
                ss[1] = ss[1].trim();//変換後のクラス名
                //ルールの最初にnodename.rule1などと書いてあると、nodenameにだけ適応する
                String nodename = null;
                int dotp = ss[0].indexOf('.');
                if(dotp!=-1){
                    nodename = ss[0].substring(0,dotp).toLowerCase();
                    if(dotp+1!=ss[0].length())
                        ss[0] = ss[0].substring(dotp+1);
                    else{
                        continue;
                    }
                }
                String[] classes = ss[0].split("&");
                int length = classes.length;
                for (int i = 0; i < classes.length; i++) {
                    String string = classes[i].trim();
                    if(string.isEmpty()){//空の物は後で削除する。
                        classes[i]=null;
                        length--;
                    }else classes[i]=string;
                }
                if(length==0)continue;//ルールが記述されていない場合はスキップ。
                if(length!=classes.length){//空文字があった場合の処理。
                    String[] cs = new String[length];
                    int kkk=0;
                    for(int i=0;i<classes.length;i++){
                        if(classes[i]!=null){
                            cs[kkk++]=classes[i];
                        }
                    }
                    classes=cs;
                }
                RenameClassRule r = new RenameClassRule();
                r.nodename=nodename;
                r.classes=classes;
                r.afterName=ss[1];
                renameclassrules.add(r);
            }else{
                error("不明な文字列:"+str);
            }
            
        }
    }
    
    static class AttributeRule{
         String attrName;
        private Pattern pattern;
        /**domに残すかどうか*/
         boolean isAccept=true;
         boolean hasValue=true;
         boolean isMatch(String nodename){
            return pattern==null?true:pattern.matcher(nodename).find();
        }
    }
    
    private  ArrayList<AttributeRule> attrrules=new ArrayList<>();
    List<AttributeRule> getAttributeRules(){
        return attrrules;
    }
    
    HashMap<String, List<AttributeRule>> getCreateAttributeMap(){
        HashMap<String, List<AttributeRule>> map = new HashMap<>();
        for(AttributeRule ar:attrrules){
            if(map.containsKey(ar.attrName)){
                map.get(ar.attrName).add(0,ar);
            }else{
                ArrayList<AttributeRule> list = new ArrayList<>(1);
                list.add(ar);
                map.put(ar.attrName, list);
            }
        }
        return map;
    }
    
//    ArrayList<String> fullAccepts(){
//        ArrayList<String> full = new ArrayList<>();
//        ArrayList<String> notfull=new ArrayList<>();
//        
//        for(AttributeRule ar:attrrules){
//            if(ar.isAccept && ar.pattern==null){
//                if( !notfull.contains(ar.attrName)&&!full.contains(ar.attrName)){
//                    full.add(ar.attrName);
//                }
//            }else{
//                if(full.contains(ar.attrName)){
//                    full.remove(ar.attrName);
//                }
//                if(!notfull.contains(ar.attrName)){
//                    notfull.add(ar.attrName);
//                }
//            }
//        }
//        return full;
//    }
    
    private void readAttrribute(Scanner scan){
        String str;
        while(scan.hasNext()){
            str=scan.nextLine();y++;
            str = trim(str);//コメントと空白除去
            if(str.isEmpty())continue;
            if(str.equals("end:"))break;
            if(str.contains("<")){
                String[] ss=str.split("<",2);
                boolean b=true;
                boolean v=true;
                ss[0] = ss[0].trim().toLowerCase();
                ss[1] = ss[1].trim().toLowerCase();
                if(ss[0].startsWith("none:")){
                    b=false;
                    ss[0] = ss[0].replace("none:", "").trim();
                }
                if(ss[0].endsWith("!")){
                    v=false;
                    ss[0] = ss[0].substring(0,ss[0].length()-1).trim();
                }
                if(ss[0].isEmpty())continue;
                if(ss[1].equals("*")){
                    AttributeRule ar = new AttributeRule();
                    ar.attrName = ss[0];
                    ar.isAccept = b;
                    ar.hasValue=v;
                    attrrules.add(ar);
                }else{
                    String sss=ss[1].replaceAll(" ", "");
                    if(sss.endsWith(","))sss = sss.substring(0,sss.length()-1);
                    String pat = "\\A("+ss[1].replaceAll(",", ")|(")+")\\z";
                    AttributeRule ar = new AttributeRule();
                    ar.attrName = ss[0];
                    ar.isAccept = b;
                    ar.hasValue=v;
                    ar.pattern=Pattern.compile(pat,Pattern.CASE_INSENSITIVE);
                    attrrules.add(ar);
                }
            }else if(str.contains(",")){//jsの為のルール
                boolean b=true;
                if(str.startsWith("none:")){
                    b=false;
                    str = str.replace("none:", "").trim();
                }
                String[] ss = str.split(",");
                for(String name:ss){
                    name = name.trim();
                    boolean v = true;
                    if(name.endsWith("!")){
                        v=false;
                        name = name.substring(0,name.length()-1).trim();
                    }
                    if(name.isEmpty())continue;
                    AttributeRule ar = new AttributeRule();
                    ar.attrName = name;
                    ar.isAccept = b;
                    ar.hasValue=v;
                    attrrules.add(ar);
                }
            }else{
                boolean b=true;
                
                boolean v=true;
                if(str.endsWith("!")){
                    v=false;
                    str = str.substring(0,str.length()-1).trim();
                }
                if(str.startsWith("none:")){
                    b=false;
                    str = str.replace("none:", "").trim();
                }
                if(str.isEmpty())continue;
                AttributeRule ar = new AttributeRule();
                ar.attrName = str;
                ar.isAccept = b;
                ar.hasValue=v;
                attrrules.add(ar);
            }
            
        }
    }
    private ArrayList<String> ignorenoderule = new ArrayList<>();
    List<String> getIgnoreRules(){return ignorenoderule;}
    
    private void readIgnoreNode(Scanner scan){
        String str;
        while(scan.hasNext()){
            str=scan.nextLine();y++;
            str = trim(str);//コメントと空白除去
            if(str.isEmpty())continue;
            if(str.equals("end:"))break;
            ignorenoderule.add(str);
        }
    }
    
    private ArrayList<String> skipenvrules = new ArrayList<>();
    List<String> getSkipRules(){return skipenvrules;}
    
    

    private void readSkipEnvironment(Scanner scan){
        String str;
        while(scan.hasNext()){
            str=scan.nextLine();y++;
            str = trim(str);//コメントと空白除去
            if(str.isEmpty())continue;
            if(str.equals("end:"))break;
            skipenvrules.add(str);
            
        }
    }

    static class ConvertRule{
        String tagname;
        String classname;
        Converter instance;
        Class<Converter> clasz;
        String constructproperty;
        String property;
        
        @SuppressWarnings("unchecked")
        boolean load() throws ClassNotFoundException, InstantiationException, IllegalAccessException, 
        SecurityException, IllegalArgumentException, InvocationTargetException,Exception{
            Class<?> claz = Class.forName(classname);
            if(!Converter.class.isAssignableFrom(claz)){
                return false;
            }
            clasz = (Class<Converter>)claz;
            try {
                Constructor<Converter> constractor=clasz.getConstructor(String.class);
                instance = constractor.newInstance(constructproperty);
            } catch (NoSuchMethodException e) {
                instance = clasz.newInstance();
            }
            return true;
        }
        
    }
    
    
    private ArrayList<ConvertRule> convrules =  new ArrayList<>();
     List<ConvertRule> getConvertRules(){
        return convrules;
    }
    private void readConvertSection(Scanner scan){
        String str;
        while(scan.hasNext()){
            str=scan.nextLine();y++;
            str = trim(str);//コメントと空白除去
            if(str.isEmpty())continue;
            if(str.equals("end:"))break;
            
            if(str.contains(">")){
                String[] ss=str.split(">",2);
                ss[0] = ss[0].trim();
                ss[1] = ss[1].trim();
                ConvertRule r = new ConvertRule();
                r.tagname = ss[0];
                ss = ss[1].split("\\?",2);
                if(ss.length==1)r.property="";
                else r.property=ss[1];
                ss =  ss[0].split(":",2);
                r.classname=ss[0];
                if(ss.length==1)r.constructproperty="";
                else r.constructproperty=ss[1];
                try {
                    if(r.load()){
                        convrules.add(r);
                    }else{
                        error(r.classname+"はtex.html.Converterを実装していません。");
                    }
                } catch (ClassNotFoundException e){
                    error(e.getMessage());
                    error(r.classname+"をロードすることが出来ませんでした。");
                    
                }catch(InstantiationException
                        | IllegalAccessException e) {
                    error(r.classname+"のインスタンスを生成できませんでした。" +
                    		"コンストラクタはStringを受け取るもしくは引数を浮けとらないのコンストラクタがある必要があります。");
                } catch (SecurityException|IllegalArgumentException|InvocationTargetException e) {
                    error(r.classname+"のインスタンスを生成できませんでした");
                } catch(Exception e){
                    e.printStackTrace();
                }
            }else{
                error("不明な文字列:"+str);
            }
        }
    }

    
    private static Pattern
    groupstart=Pattern.compile("group\\{(.*)\\}");
    
    static class BlockElement{
         String name;
         int depth;
         String tagname;
        private BlockElement parent;
        int calcdepth(int max){
            return calcdepth(0,max);
        }
        
        int calcdepth(int i,int max){
            if(i>max){
                return -1;
            }
            if(parent == null)return 0;
            return parent.calcdepth(i+1,max)+1;
        }
        
        
        @Override //list.containsで判断する為。面倒くさかったんじゃ
        public boolean equals(Object obj){
            if(obj instanceof BlockElement){
                return name.equals(((BlockElement) obj).name);
            }else return false;
        }
        
    }
    static class BlockRule{
        private BlockElement[] bs;
        @Override
         public String toString(){
            StringBuilder sb=new StringBuilder();
            for(BlockElement b:bs){
                sb.append(b.name).append("  depth:").append(b.depth)
                .append("  convert tagname:").append(b.tagname).append("\n");
            }
            return sb.toString();
        }
         boolean hasElement(String name){
            if(name.startsWith("block-")){
                int p = name.indexOf('-');
                name = name.substring(p+1);
                if(name.startsWith("end-")){
                    p = name.indexOf('-');
                    name = name.substring(p+1);
                }
            }
            BlockElement e = get(name);
            return e!=null;
        }
        
         BlockElement get(String name){
            if(name.startsWith("block-")){
                int p = name.indexOf('-');
                name = name.substring(p+1);
                if(name.startsWith("end-")){
                    p = name.indexOf('-');
                    name = name.substring(p+1);
                }
            }
            for(BlockElement b:bs){
                if(b.name.equals(name))return b;
            }
            return null;
        }
        
        boolean calcdepth(){
            for(BlockElement b:bs){
                b.depth = b.calcdepth(bs.length+1);
                if(b.depth==-1)return false;
            }
            return true;
        }
    }
    private ArrayList<BlockRule> blocks=new ArrayList<>();
     List<BlockRule> getBlockRoles(){
        return blocks;
    }
    private void readBlockSection(Scanner scan){
        String str;
        BlockRule current=null;
        while(scan.hasNext()){
            str=scan.nextLine();y++;
            str = trim(str);//コメントと空白除去
            if(str.isEmpty())continue;
            if(str.equals("end:"))break;
            if(current==null){
                Matcher m = groupstart.matcher(str);
                if(m.find()){
                    String ss=m.group(1);
                    String[] names = ss.split(",");
                    if(names.length<1)continue;
                    ArrayList<BlockElement> bb=new ArrayList<>();
                    for(int i=0;i<names.length;i++){
                        BlockElement be = new BlockElement();
                        be.name = names[i].trim();
                        if(bb.contains(be))continue;
                        if(!be.name.isEmpty())bb.add(be);
                    }
                    current = new BlockRule();
                    current.bs = bb.toArray(new BlockElement[bb.size()]);
                    if(current.bs.length<1){
                        current = null;
                        continue;
                    }
                    blocks.add(current);
                }
            }else{
                if(str.equals("endgroup")){
                    if(!current.calcdepth()){
                        error("blockのグループの階層構造を定義できませんでした。ループになっています。");
                        blocks.remove(current);
                    }
                    current =null;
                    continue;
                }
                
                if(str.contains(">")){
                    String[] ss=str.split(">",2);
                    ss[0] = ss[0].trim();
                    ss[1] = ss[1].trim();
                    BlockElement b = current.get(ss[0]);
                    if(b!=null)b.tagname=ss[1];
                    else error(ss[0]+"はブロックのグループで未定義です");
                }else if(str.contains("<")){
                    String[] ss=str.split("<",2);
                    ss[0] = ss[0].trim();
                    ss[1] = ss[1].trim();
                    BlockElement b = current.get(ss[0]);
                    if(b==null) {
                        error(ss[0]+"はブロックのグループで未定義です");
                        continue;
                    }
                    BlockElement bb = current.get(ss[1]);
                    if(bb==null){
                        error(ss[1]+"はブロックのグループで未定義です");
                        continue;
                    }
                    if(b==bb)continue;
                    b.parent=bb;
                }else{
                    error("不明な文字列:"+str);
                }
            }
            
        }
        if(current!=null&&!current.calcdepth()){
            error("blockのグループの階層構造を定義できませんでした。ループになっています。");
            blocks.remove(current);
        }
    }
    
    
}
