package lamuriyan.parser;

import static java.lang.String.*;
import static java.util.Objects.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import lamuriyan.parser.CharCategoryDefine.CharCategoryDefiner;
import lamuriyan.parser.io.*;
import lamuriyan.parser.label.Label;
import lamuriyan.parser.label.RefTarget;
import lamuriyan.parser.macro.Counter;
import lamuriyan.parser.macro.Define_EscapeString;
import lamuriyan.parser.macro.Define_Macro;
import lamuriyan.parser.macro.Macro;
import lamuriyan.parser.node.LmAttr;
import lamuriyan.parser.node.LmAttr.AttrValueFactory;
import lamuriyan.parser.node.LmID;
import lamuriyan.parser.node.LmNode;
import lamuriyan.parser.node.env.*;
import lamuriyan.parser.token.Char;
import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;
import lamuriyan.parser.token.TokenType;


/**
 * 処理のメインとなるクラス。<br>
 * エンジンがファイルを検索する際、基準ディレクトリと、検索に登録されたディレクトリからファイルを検索します。
 * @author nodamushi
 *
 */
public class LamuriyanEngine implements ExpandAreaOperator{

    
    private SourceStack sourcestack;
    private Source currentSource;

    private Path sourceFile;
    private Path auxFile;
    
    private Path baseDirectory;
   
    private List<Path> fileSearchDirectories=new ArrayList<>();

    
    private TokenFactory tokenfactory;
    private Map<String, LmID> idmap = new HashMap<>();
    private Map<Integer,IOContainer> iomap = new HashMap<>();
    private boolean ioImmediate=false;
    private RootDocument document;
    private Environment current;
    private ExpandArea expandarea;
    
    
    private CharCategoryDefine chardefine = new CharCategoryDefine();
    private HashMap<String, EnvironmentFactory> environmentFactoryMap = new HashMap<>();
    private String verbatimEnvironment=null;
    private ArrayDeque<Token> outputBuffer = new ArrayDeque<>();
    private boolean isMathMode=false;

    
    private StringBuilder auxValue=new StringBuilder();
    
    
    private long log_memory_at_start=usedMemory();
    private long log_max_usedMemory=log_memory_at_start;
    
    
    
    /**
     * 
     * @param file
     * @throws UnsupportedCharsetException
     * @throws FileNotFoundException
     * @throws UnDetectedCharCodeException
     * @throws IOException
     * @throws NullPointerException
     */
    public LamuriyanEngine(String file) 
            throws UnsupportedCharsetException, FileNotFoundException,
            IOException,NullPointerException{
        this(file,null,Paths.get("."));
    }
    
    public LamuriyanEngine(String file,Charset charset)
            throws UnsupportedCharsetException, FileNotFoundException,
            NullPointerException, IOException{
        this(file,charset,Paths.get("."));
    }
    
    /**
     * 
     * @param file
     * @param baseDirectory
     * @throws UnsupportedCharsetException
     * @throws FileNotFoundException
     * @throws UnDetectedCharCodeException
     * @throws IOException
     * @throws InvalidPathException
     * @throws NullPointerException
     */
    public LamuriyanEngine(String file,String baseDirectory) 
            throws UnsupportedCharsetException, FileNotFoundException,
             IOException,InvalidPathException,NullPointerException {
        this(file,null,Paths.get(baseDirectory));
    }
    
    public LamuriyanEngine(String file,Charset charset,String baseDirectory) 
            throws UnsupportedCharsetException, FileNotFoundException,
             IOException,InvalidPathException,NullPointerException {
        this(file,charset,Paths.get(baseDirectory));
    }

    public LamuriyanEngine(String file,Path baseDirectory) 
            throws UnsupportedCharsetException, FileNotFoundException, 
            IOException,NullPointerException{
        this(file,null,baseDirectory);
    }
    

    
    public LamuriyanEngine(String file,Charset charset,Path baseDirectory) 
            throws UnsupportedCharsetException, FileNotFoundException, 
            IOException,NullPointerException{
        this.baseDirectory = requireNonNull(baseDirectory);
        if(!Files.isDirectory(baseDirectory)){
            throw new FileNotFoundException(baseDirectory.toString()+"はディレクトリではありません。");
        }
        expandarea = new ExpandArea(null, this,this);
        sourceFile = baseDirectory.resolve(requireNonNull(file));
        String name = sourceFile.getFileName().toString();
        int l = name.lastIndexOf('.');
        if(l!=-1){
            name = name.substring(0, l);
        }
        auxFile = sourceFile.resolve(name+".aux");
        sourcestack = new SourceStack();
        tokenfactory = new TokenFactory(chardefine);
        document = new RootDocument(this);
        current = document;
        initCommands();
        initEnvironmentDefine();
        try {
            initsty();
        } catch (Exception e) {//本当はinit.styでエラー出ると困るんだけど。
            e.printStackTrace();
        }
        printlnAux("\\relax");
        currentSource=sourcestack.push(sourceFile,charset,file.toString());
    }
    
    
    private void initsty() throws Exception{
        sourcestack.push(Token.escape("makeatother"));
        if(Files.isReadable(auxFile)){
            currentSource = sourcestack.push(auxFile,auxFile.toString());
        }
        currentSource=sourcestack.push(LamuriyanEngine.class.getResourceAsStream("init.sty"), "init.sty");
        currentSource=sourcestack.push(Token.escape("makeatletter"));
        evaluate();
    }

    public Path getAuxFilePath(){
        return auxFile;
    }
    
    public void printlnAux(String str){
        auxValue.append(str).append("\n");
    }
    public void printAux(String str){
        auxValue.append(str);
    }
    
    public StringBuilder getAuxContents(){
        return auxValue;
    }
    
    public void saveAux() throws IOException{
        
        try(PrintWriter pw = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(auxFile.toFile())), "UTF-8"))){
            pw.print(auxValue.toString());
        }
    }
    
    
    /**
     * ファイルを読み込む前に、先に読み込む設定ファイルを指定します。<br>
     * このメソッドを何度か呼ぶ場合、バッファは先入れ後出しのスタック構造なので、後に指定したファイルが先に読み込まれます。<br>
     * initfilesは渡された順番で読み込みます。<br>
     * このメソッドが呼ばれた時点で読み込み不可能なファイルは無視されます。<br>
     * @param initfiles
     * @throws IOException
     */
    public void setInitFiles(Collection<Path> initfiles) throws IOException{
        sourcestack.push(Token.escape("makeatother"));

        List<Path> l = new ArrayList<>(initfiles);
        Collections.reverse(l);
        for(Path p:l){
            Path read = searchFile(p);
            if(read==null)continue;
            if(Files.isReadable(read))
                sourcestack.push(read, "initFile "+read.toString());
        }
        currentSource=sourcestack.push(Token.escape("makeatletter"));
    }
    /**
     * ファイルを読み込む前に、先に読み込む設定ファイルを指定します。<br>
     * このメソッドを何度か呼ぶ場合、バッファは先入れ後出しのスタック構造なので、後に指定したファイルが先に読み込まれます。<br>
     * initfilesは渡された順番で読み込みます。<br>
     * このメソッドが呼ばれた時点で読み込み不可能なファイルは無視されます。
     * @param initfiles
     * @throws IOException
     */
    public void setInitFiles(Path... initfiles) throws IOException{
        sourcestack.push(Token.escape("makeatother"));

        List<Path> l = new ArrayList<>(initfiles.length);
        Collections.addAll(l, initfiles);
        Collections.reverse(l);
        for(Path p:l){
            Path read = searchFile(p);
            if(read==null){
                System.err.println(p.toString()+"が見つかりません。");
                continue;
            }
            if(Files.isReadable(read))
                sourcestack.push(read, "initFile "+read.toString());
            else
                System.err.println(p.toString()+"はファイルではありません");
        }
        currentSource=sourcestack.push(Token.escape("makeatletter"));
    }


    /**
     * ファイルを検索するディレクトリを指定します。<br>
     * 検索順番は、コンストラクタで指定した基本ディレクトリをまず検索し、
     * setFileSearchDirectoryで登録された順に検索します。
     * @param directories
     */
    public void setFileSearchDirectories(Collection<Path> directories){
        for(Path p:directories){
            if(Files.isDirectory(p)){
                fileSearchDirectories.add(p);
            }
        }
    }
    /**
     * ファイルを検索するディレクトリを指定します。<br>
     * 検索順番は、コンストラクタで指定した基本ディレクトリをまず検索し、
     * setFileSearchDirectoryで登録された順に検索します。
     * @param directories
     */
    public void setFileSearchDirectories(Path... directories){
        for(Path p:directories){
            if(Files.isDirectory(p)){
                fileSearchDirectories.add(p);
            }
        }
    }
    /**
     * ファイルを検索するディレクトリを指定します。<br>
     * 検索順番は、コンストラクタで指定した基本ディレクトリをまず検索し、
     * setFileSearchDirectoryで登録された順に検索します。
     * @param directory
     */
    public void setFileSearchDirectory(Path directory){
        if(Files.isDirectory(directory)){
            fileSearchDirectories.add(directory);
        }
    }
    
    /**
     * 読み込むファイルを検索します<br>
     * @param file
     * @return
     */
    public Path searchFile(Path file){
        if(file.isAbsolute()){
            if(Files.isReadable(file))return file;
            else return null;
        }
        Path p = baseDirectory.resolve(file);
        if(Files.isReadable(p))return p;
        for(Path dir:fileSearchDirectories){
            p = dir.resolve(file);
            if(Files.isReadable(p))return p;
        }
        return null;      
        
    }
    
    /**
     * 読み込むファイルを検索します<br>
     * @param file
     * @return
     */
    public Path searchFile(String filename){
        Path file = Paths.get(filename);
        return searchFile(file);
    }

    
    /**
     * 新しいStreamの番号を割り当てます。
     * @param isIn
     * @return
     */
    public int createNewIO(boolean isIn){
        int i=0;
        while(true){
            IOContainer io = iomap.get(i);
            if(io==null){
                io = new IOContainer(i, isIn);
                iomap.put(i,io);
                return i;
            }
            i++;
        }
    }
    
    /**
     * 新しい書き込みStreamの番号を割り当てます。
     * @return
     */
    public int createNewOut(){
        return createNewIO(false);
    }
    
    /**
     * 新しい読み込みStreamの番号を割り当てます。
     * @return
     */
    public int createNewIn(){
        return createNewIO(true);
    }
    
    /**
     * numberのStreamが割り当てられている場合、このStreamを使ってpathのStreamを開きます。
     * @param number
     * @param path
     * @throws NullPointerException pathがnull
     * @throws IOException 
     */
    public void openIO(int number,String path) 
            throws NullPointerException, IOException{
        IOContainer io = iomap.get(number);
        if(io==null)return;
        io.open(path,this);
    }
    
    public boolean isInputStream(int number){
        IOContainer io = iomap.get(number);
        if(io==null)return false;
        return io.isInputStream();
    }
    
    public boolean isOutputStream(int number){
        IOContainer io = iomap.get(number);
        if(io==null)return false;
        return !io.isInputStream();
    }
    
    /**
     * numberのStreamを閉じます。
     * @param number
     */
    public void closeIO(int number){
        IOContainer io = iomap.get(number);
        if(io==null)return;
        io.close();
    }
    
    /**
     * バッファの内容をフラッシュします
     * @param number
     * @throws IOException
     */
    public void flashIO(int number) throws IOException{
        IOContainer io = iomap.get(number);
        if(io==null)return;
        io.flush();
    }
    
    /**
     * Streamに文字列を書き込みます。
     * @param number
     * @param value
     * @throws IOException
     */
    public void write(int number,String value) throws IOException{
        IOContainer io = iomap.get(number);
        if(io==null)return;
        io.write(value);
        if(ioImmediate)io.flush();
        ioImmediate=false;
    }
    
    public void read(String toSC){
        //TODO 読み込み処理。\def\toSC{読み込み内容}をFullExpandAreaで評価する
        // そのためには、読み込み処理のやり方変えないといけないな…
        ioImmediate=false;
    }
    
    /**
     * \immediateの実装がよくわからんので、Lamuriyan処理系では
     * immediateフラグがオンになっていると<br>
     * write→書き込んだ後、バッファをフラッシュする<br>
     * read→トークン分割する前に全てのデータを読み込む（この事に意味があるのかは分からない）<br>
     * とする。write,readが呼ばれるとこのフラグはoffになる。
     */
    public void immediateFlag(){
        ioImmediate = true;
    }
    
    
    public void defineNewCounter(String name){
        Counter ct = new Counter();
        Command c = new Command(name, ct);
        expandarea.setGlobalCommand(c);
    }

    public void defineNewCounter(String name,String supercounter){
        Counter ct = new Counter();
        Command c = new Command(name, ct);
        expandarea.setGlobalCommand(c);
        Command sp = expandarea.getCommand(supercounter);
        if(sp!=null && sp.isCounter()){
            sp.getAsCounter().addSubCounter(ct);
        }
    }

    private String getCurrentSourcePosition(){
        Source source=currentSource;
        if(!(source instanceof StringSource))for(Source s:sourcestack){
            if(s instanceof StringSource){
                source = s;
                break;
            }
        }

        if(source instanceof StringSource){
            StringSource ss = (StringSource)source;
            StringBuilder sb = new StringBuilder();
            sb.append("読み込み："+ss.getInfomation()).append("\n");
            sb.append(ss.getLine()).append("行目").append(ss.getX()).append("　　全体で").append(ss.getCaretPosition());
            sb.append("を読み込んでいます。").append("\nバッファーの状態は[").append(ss.getBufferValue()).append("]になっています。");
            return sb.toString();
        }else{
            return "ファイルの読み込みは終わっています。";
        }
    }



    private void initEnvironmentDefine(){
        defineNewEnvironment(ListEnvironment.factory);
        defineNewEnvironment(ArrayEnvironment.textCellFactory);
        defineNewEnvironment(ArrayEnvironment.basetabeleFactory);
        defineNewEnvironment(ArrayEnvironment.mathCellFactory);
        defineNewEnvironment(ArrayEnvironment.basearrayFactory);
        defineNewEnvironment(MathEnvironment.inlineFactory);
        defineNewEnvironment(MathEnvironment.blockFactory);
        defineNewEnvironment(NHLightEnvironment.factory);
    }

    private void insertToken(Collection<Token> tokens){
        if(tokens==null)return;
        currentSource = sourcestack.push(tokens);
    }

    private void insertToken(TokenChain t){
        if(t==null)return;
        currentSource = sourcestack.push(t);
    }


    private void insertToken(Token... t){
        if(t==null)return;
        ArrayList<Token> tt = new ArrayList<>(t.length);
        Collections.addAll(tt, t);
        insertToken(tt);
    }

    public void insertString(String str){
        if(str==null)return;
        currentSource=sourcestack.push(str, "insert string");

    }

    public void insertFile(String filename) throws IOException{
        if(filename==null)return;
        Path p = searchFile(filename);
        if(p!=null){
            currentSource = sourcestack.push(p,"insert file "+p.toString());
        }
    }
    
    public void usePackage(String name) throws IOException{
        if(name==null)return;
        String fname = name.endsWith(".sty")?name:name+".sty";
        Path p = searchFile(fname);
        if(p!=null){
            CharCategory c=chardefine.get('@');
            if(c!=CharCategory.ALPHABET){
                String str = "\\relax\\catcode '@ ="+c.number+"\\relax";
                sourcestack.push(str, "reset catcode");
                sourcestack.push(p,"package "+p.toString());
                currentSource=sourcestack.push(Token.escape("makeatletter"));
            }else{
                currentSource = sourcestack.push(p,"package "+p.toString());
            }
        }
    }
    
    
    public void verbInsertFile(String filename) throws IOException{
        if(filename == null)return;
        Path p = searchFile(filename);
        if(p!=null){
            currentSource = sourcestack.pushVerb(p,"insert file "+p.toString());
        }
    }
    
    public void verbInsertString(String str){
        if(str==null)return;
        currentSource=sourcestack.pushVerb(str, "verb insert string");
    }
    
    /**
     * ファイルを全て実行し、DOMを生成します。<br>
     * 全て処理し終わるまでスレッドを占領します。Thread.interruptedで中断することは出来ますが、
     * 中断機構を作りたい場合はprocessTokenを自分で呼び出してください。<br><br>
     * evaluate()の実装は以下の様になっています
<code><pre>
while(processToken())
  if(Thread.interrupted())
    throw new InterruptedException();
</pre></code>
     * @throws Exception
     */
    public void evaluate() throws Exception{
        while(processToken()){
            if(Thread.interrupted()){
                throw new InterruptedException();
            }
        }
    }
    
    /**
     * トークンを一つ読み込み、処理をします。<br>
     * 返値がtrueである間はLamuriyanの処理が終わっていないことを表します。
     * @return まだこのメソッドを呼ぶ必要があるかどうか。
     * @throws Exception
     */
    public boolean processToken()
            throws Exception
    {log_max_usedMemory=Math.max(log_max_usedMemory, usedMemory());while(true){
        //↑無駄に関数スタックを消費しない為のループとメモリのログ
        
        Token t = readToken();
        if(t==null)return false;//処理が終了
        
        //\begin{～}ではじまり\end{～}で終わる中身をそのまま出力する環境の処理
        if(tokenfactory.isVerbatimeMode()){
            if(t.getType() == TokenType.ESCAPE){
                Command ct = expandarea.getCommand(t.toString());
                
                if(ct==null){//ただの文字
                    insertToken(Token.toCharToken(t.toString()+(t.getTokenEndChar()!=0?t.getTokenEndChar():"")));
//                    return parseNextToken();
                    continue;
                }
                
                if(ct.isMacro()){
                    Macro macro = ct.getAsMacro();
                    String name = macro.getName();
                    if(name.equals("\\end")){
                        tokenfactory.setVerbatimeMode_findGroupMode(true);
                        Token tt = readToken();
                        tokenfactory.setVerbatimeMode_findGroupMode(false);
                        if(tt==null){//なんかよ～わからん形でファイルが終了した場合
                            System.err.println("vervatimモードが終了していません");
                            return false;
                        }
                        
                        if(tt.getType()==TokenType.UNDEFINED){
                            //ただの文字扱い
                            //insettTokenメソッドじゃないのは気まぐれか？
                            currentSource=sourcestack.pushVerb(t.toString()+tt.toString(), "");
//                            return parseNextToken();
                            continue;
                        }else{
                            //\end{～}の～の部分
                            String endname = tt.getTokenChain().toString();
                            TokenChain in = new TokenChain();//\end,{,～,} というトークン列
                            in.addAll(t,new Token(TokenType.BEGINGROUP));
                            in.addAll(Token.toCharToken(endname));
                            in.addAll(new Token(TokenType.ENDGROUP));
                            //名前が一致したらExpandAreaにinを展開
                            if(endname.equals(verbatimEnvironment)){
                                endVerbatim();
                                for(Token to:in){
                                    expandarea.add(to);
                                }

                                TokenChain tc = expandarea.getTokens();
                                insertToken(tc);
                                expandarea.stateClear();
                                return true;
                            }else{//そうじゃなければただの文字
                                currentSource=sourcestack.pushVerb(
                                        t.toString()+
                                        (t.getTokenEndChar()!=0?t.getTokenEndChar():"")+
                                        tt.getStringProperty(), "");
//                                return parseNextToken();
                                continue;
                            }
                        }
                    }
                }

                //どれでもなかったら、やっぱりただの文字
                currentSource=sourcestack.pushVerb(
                        t.toString()+
                        (t.getTokenEndChar()!=0?t.getTokenEndChar():"")
                        , "");
//                return parseNextToken();
                continue;
            }//end if Escape
            
        }//end if verbatimemode

        //基本処理はExpandAreaに入力して、出力があればバッファに保存し、それを環境か入力に入力する。
        if(expandarea.add(t)){
            expandarea.getTokens().pushTo(outputBuffer);//出力をバッファoutputBufferに保存
            expandarea.stateClear();//ExpandAreaのバッファをクリア
            output();//環境に入力
        }
        
        return true;
        
    }}//end parseNextToken()

    
    private void output(){
        if(!tdefblockmode)outputToEnvironment();
        else outputToTDefBlock();
    }
    
    
    private TDefBlock tdefblock;
    private boolean tdefblockmode=false;
    private String tdefname = null;
    private boolean tdefglobal = false;
    void startTDefBlockMode(String name,boolean global){
        if(tdefblockmode){
            printError("\\tdefは入れ子関係には出来ません。");
            return;
        }
        tdefblockmode = true;
        tdefname = name;
        tdefglobal = global;
    }
    
     boolean shouldCreateTDefBlock(){
        return tdefblockmode && tdefblock==null;
    }
    
    
    boolean setTDefBlock(TDefBlock tblock){
        if(tdefblockmode && tdefblock==null){
            this.tdefblock = tblock;
            tblock.setName(tdefname);
            tblock.setGlobal(tdefglobal);
            return true;
        }
        return false;
    }
    
    void endTDefBlockMode(TDefBlock tblock){
        if(this.tdefblock==tblock ){
           tdefblock = null;
           tdefblockmode=false;
        }
    }
    
    
    private void outputToTDefBlock(){
        while(!outputBuffer.isEmpty()){
            Token t = outputBuffer.pop();
            Token insertToken=null;//入力に戻すトークン
            if(t.isProtected()){
                if(tdefblock==null){
                    tdefblockmode =false;
                    outputBuffer.push(t);
                    printError("\\tdefの次には{が来なくてはなりません。"+t);
                    outputToEnvironment();
                    return;
                }
                //protectedな物は突っ込む
                t.setProtect(false);
                tdefblock.appendToken(t);
                continue;
            }

            switch(t.getType()){
                case ARGUMENT:
                    insertToken = new Token(TokenType.__TOKEN__GROUP__);
                    TokenChain tc=new TokenChain();
                    tc.addAll(Token.toCharToken(t.toString()));
                    insertToken.setTokenChain(tc);
                    break;
                case CHAR:
                case SPACE:
                case MATHESCAPE:
                    if(tdefblock==null){
                        tdefblockmode =false;
                        outputBuffer.push(t);
                        printError("\\tdefの次には{が来なくてはなりません。"+t);
                        outputToEnvironment();
                        
                        return;
                    }
                    tdefblock.appendToken(t);
                    break;
                case NEWLINE:
                    Command c = getCommand("\\@newlineflag");
                    if(c!=null && "on".equals(c.getAsString())){
                        if(tdefblock==null){
                            tdefblockmode =false;
                            outputBuffer.push(t);
                            printError("\\tdefの次には{が来なくてはなりません。"+t);
                            outputToEnvironment();
                            return;
                        }
                        tdefblock.appendToken(Token.NEWLINECOMMAND);
                    }
                    break;
                default:
                    insertToken = t;
            }
            if(insertToken!=null){
                //outputBufferが残っている場合は
                //insertToken→outputBufferの順に処理するように割り込ませる。
                if(!outputBuffer.isEmpty())
                    insertToken(outputBuffer);//先入れ後出しなので先にoutputBufferを挿入
                outputBuffer.clear();
                if(insertToken.getType() == TokenType.__TOKEN__GROUP__){
                    insertToken(insertToken.getTokenChain());
                }else{
                    insertToken(insertToken);
                }
            }
        }
    }
    
    //環境か入力にバッファに保存されたトークンを入力する。
    private void outputToEnvironment(){
        while(!outputBuffer.isEmpty()){
            Token t = outputBuffer.pop();
            Char chr;
            Token insertToken=null;//入力に戻すトークン
            if(t.isProtected()){//protectedな物は無視すればいいっぽい。
                t.setProtect(false);
                continue;
            }
            switch(t.getType()){
                case ARGUMENT:
                    insertToken = new Token(TokenType.__TOKEN__GROUP__);
                    TokenChain tc=new TokenChain();
                    tc.addAll(Token.toCharToken(t.toString()));
                    insertToken.setTokenChain(tc);
                    break;
                case CHAR:
                    chr = new Char(t.getChar(),Char.CharType.CHAR);
                    chr.setFont(expandarea);
                    insertToken =current.input(chr);
                    break;
                case SPACE:
                    chr = new Char(t.getChar(),Char.CharType.SPACE);
                    chr.setFont(expandarea);
                    insertToken=current.input(chr);
                    break;
                case NEWLINE:
                    Command c = getCommand("\\@newlineflag");
                    if(c!=null && "on".equals(c.getAsString())){
                        insertToken = Token.NEWLINECOMMAND;
                    }
                    break;
//                case __NOEXPAND_TOKEN__:
//                    insertToken = new Token(TokenType.ESCAPE,t.toString());
//                    break;
//                case __UNEXPAND_TOKENS__:
//                    insertToken = new Token(TokenType.__TOKEN__GROUP__);
//                    insertToken.setTokenChain(t.getTokenChain());
//                    break;
                case MATHESCAPE:
                    if(isMathMode){
                        chr = new Char(t.getMathEscape());
                        chr.setFont(expandarea);
                        insertToken = current.input(chr);
                        break;
                    }else{
                        printError("数式モードでしか利用できません。"+t.getMathEscape().value);
                        break;
                    }
                default:
                    insertToken = t;
            }
            if(insertToken!=null){
                //outputBufferが残っている場合は
                //insertToken→outputBufferの順に処理するように割り込ませる。
                if(!outputBuffer.isEmpty())
                    insertToken(outputBuffer);//先入れ後出しなので先にoutputBufferを挿入
                outputBuffer.clear();
                if(insertToken.getType() == TokenType.__TOKEN__GROUP__){
                    insertToken(insertToken.getTokenChain());
                }else{
                    insertToken(insertToken);
                }
            }
        }
    }




    @Override
    public boolean canUseVarb(){
        return true;
    }
    @Override
    public void endVarb(){
        tokenfactory.endVerbMode();
    }
    @Override
    public void setVarb(){
        tokenfactory.setVerbMode();
    }

    @Override
    public void setVarb(char ch){
        tokenfactory.setVerbMode(ch);
    }

    @Override
    public void endVerbatim(){
        tokenfactory.endVerbatimMode();
    }
    @Override
    public void setVerbatim(String name){
        if(name == null || name.isEmpty())return;
        tokenfactory.setVerbatimMode();
        verbatimEnvironment=name;
    }

    //トークン分割のメイン処理
    private Token readToken() throws IOException{
        if(currentSource==null)return tokenfactory.eof();
        while(currentSource.isEnd()){
            sourcestack.pop();
            currentSource =sourcestack.peek();
            if(currentSource==null)return tokenfactory.eof();
        }
        if(currentSource instanceof TokenSource){
            TokenSource source = (TokenSource)currentSource;
            //            Token t= tokenbuffer.poll();
            Token t = source.read();
            if(tokenfactory.isVerbMode() && !tokenfactory.isDefinedVerbStartChar()){
                switch(t.getType()){
                    case SPACE:
                    case CHAR:
                        char c =t.getChar();
                        tokenfactory.append(c);
                        return readToken();
                    case ESCAPE:
                        String s = t.toString().substring(1);
                        if(s.length()==1){
                            tokenfactory.append(s.charAt(0));
                            return readToken();
                        }
                    default:
                        System.err.println("verbの開始終了文字が定義できません。");
                        tokenfactory.endVerbMode();
                        return t;
                }
            }else return t;
        }else{
            //        if(filebuffer.isEnd()){
            //            return tokenfactory.eof();
            //        }
            StringSource source = (StringSource)currentSource;
            //            char c = filebuffer.read();
            char c = source.read();
            Token t;
            t = tokenfactory.append(c);
            int i=0;
            while(t==null){
                while(currentSource.isEnd()){
                    sourcestack.pop();
                    currentSource =sourcestack.peek();
                    if(currentSource==null)return tokenfactory.eof();
                }
                if(currentSource instanceof TokenSource){
                    return tokenfactory.eof();
                }
                source = (StringSource)currentSource;
                c =source.preread();
                t = tokenfactory.append(c);
                if(tokenfactory.isConsumedLastChar()){
                    source.read();
                    i=0;
                }else{
                    i++;
                    if(i>100000){
                        throw new RuntimeException("無限ループバグっぽいぞ。\nchar="+c+" \nfactorystate:"+tokenfactory.debag());
                    }
                }
            }
            if(t!=null){
                if(t.getType()==TokenType.__TOKEN__GROUP__){
                    TokenChain tc = t.getTokenChain();
                    insertToken(tc);
                    return readToken();
                }
            }
            return t;
        }
    }


    /**
     * spaceやコメントでないトークンの先読みを行います。<br> 
     * \@ifnextcharを勘違いしていた頃の諸悪の根源。いつかこのメソッド削除しないと………。
     * だが面倒
     * @return
     * @throws IOException
     */
    public Token preGetNextToken_without_SPACE_and_COMMENT() throws IOException{
        TokenChain tc = new TokenChain();
        Token t = readToken();
        if(t!=null){
            switch(t.getType()){
                case SPACE:
                case COMMENT:
                    tc.add(t);
                    boolean endwhile=false;
                    while((t=readToken())!=null){
                        tc.add(t);
                        switch(t.getType()){
                            case SPACE:
                            case COMMENT:
                                break;
                            default:
                                endwhile=true;
                                break;
                        }
                        if(endwhile)break;
                    }
                    insertToken(tc);
                    return t;
                default:
                    insertToken(t);
                    return t;
            }
        }
        return null;
    }

    public static final ExpandAreaOperator DefaultExpandAreaOperator=new ExpandAreaOperator() {
        @Override
        public boolean canUseVarb(){
            return false;
        }
        @Override
        public void setVarb(char ch){
        }
        @Override
        public void endVarb(){
        }
        @Override
        public void setVarb(){
        }
        @Override
        public void endVerbatim(){
        }
        @Override
        public void setVerbatim(String name){
        }
    };



    public void mathMode(boolean block){
        String envname = block?MathEnvironment.blockFactory.getName():
            MathEnvironment.inlineFactory.getName();

        if(isMathMode){
            if(canCloseEnvironment(envname)){
                closeEnvironment(envname);
                isMathMode = false;
                tokenfactory.setMathMode(false);
                //TODO 数式モード終了後の処理

            }else{
                printError("数式モードを閉じることが出来ません。");
            }
        }else{
            createNewEnvironment(envname);
            isMathMode = true;
            tokenfactory.setMathMode(true);
            //TODO 数式モード始まりの処理
        }

    }


    public boolean canCreateNewEnvironment(String name){
        EnvironmentFactory f = environmentFactoryMap.get(name);
        return f!=null;
    }

    public Token createNewEnvironment(String name){
        EnvironmentFactory f = environmentFactoryMap.get(name);
        if( f!=null){
            if(f!=ArrayEnvironment.mathCellFactory && f!=ArrayEnvironment.textCellFactory)
                expandarea.pushBlock(CommandMap.ENVIRONMENTBLOCK);
            Environment e = f.create(this);
            addEnvironment(e);
            return f.getBeginToken();
        }
        return null;
    }

    public void addEnvironment(Environment e){
        if(e!=null){
            //            expandarea.pushBlock(CommandMap.ENVIRONMENTBLOCK);
            e.setParent(current);
            e.setParentEnvironment(current);
            current.add(e);
            current = e;
        }
    }

    public boolean canCloseEnvironment(String name){
        return current!=document&&current.matchName(name);
    }

    public Token getCloseEnvironmentToken(String name){
        Environment e = current;
        while(e!=null&&!e.matchName(name)){
            e=e.getParentEnvironment();
        }
        if(e==null)return null;
        return e.getEnvironementFactory().getEndToken();

    }

    public void closeEnvironment(String name){
        if(!canCloseEnvironment(name))return;
        current.close();
        EnvironmentFactory f = current.getEnvironementFactory();
        current =current.getParentEnvironment();
        while(true)
            try{
                if(f!=ArrayEnvironment.mathCellFactory && f!=ArrayEnvironment.textCellFactory)
                    expandarea.popBlock(CommandMap.ENVIRONMENTBLOCK);
                break;
            }catch(Exception e){//環境の前にブロックがきちんと閉じてない。
                e.printStackTrace();
                expandarea.popBlock(CommandMap.CHARBLCOK);
            }
    }

    /**
     * 現在がどんな環境であろうと強制的に現在の環境を終了します。<br>
     * \endタグによる終了とは別物と見なし、
     * \endタグが本来置き換えられるべきトークンなどは無視されます。<br>
     * 本来はcanCloseEnvironment(String)→getCloseEnvironmentToken(String)→closeEnvironment(String)を使うべきです。
     */
    public void closeEnvironmentNow(){
        if(current==document)return;
        current.close();
        current = current.getParentEnvironment();
        while(true)
            try{
                expandarea.popBlock(CommandMap.ENVIRONMENTBLOCK);
                break;
            }catch(Exception e){//環境の前にブロックがきちんと閉じてない。
                expandarea.popBlock(CommandMap.CHARBLCOK);
            }
    }

    public void defineNewEnvironment(String name,int beginMacroArgumentSize,TokenChain begin,TokenChain end,TokenChain option){
        try{
            EnvironmentFactory factory = new EnvironmentFactory(name, beginMacroArgumentSize, begin, end,option);
            defineNewEnvironment(factory);
        }catch(Exception e){
            System.err.println("環境"+name+"を定義できませんでした。");
            e.printStackTrace();
        }
    }


    public void defineNewEnvironment(EnvironmentFactory factory){
        environmentFactoryMap.put(factory.getName(), factory);
        Command beginC = factory.getBeginCommand();
        Command endC = factory.getEndCommand();
        Command b2C = factory.getBeginSecondCommand();
        defineGlobalCommand(beginC);
        defineGlobalCommand(endC);
        if(b2C!=null){
            defineGlobalCommand(b2C);
            defineGlobalCommand(factory.getBeginLastCommand());
        }
    }

    public Environment getCurrentEnvironment(){
        return current;
    }

    public RootDocument getDocument(){
        return document;
    }

    private AttrValueFactory labelID = new AttrValueFactory(){
        @Override
        public String createValue(LmAttr id){
            // TODO 仕様書いてないからここがなんだったかあんまり覚えてない！たぶんlabelとrefの問題
            return null;
        }
    };

    public AttrValueFactory getLabelIDFactory(){
        return labelID;
    }


    public LmID getLabelID(String labelName){
        return idmap.get(labelName);
    }

    public void putLabelID(String labelName ,LmID id){
        idmap.put(labelName, id);
    }

    /**
     * 
     * @param ch
     * @param cc nullの時はremoveになります。
     */
    public void setCharCategory(char ch,CharCategory cc){
        chardefine.setCharCategory(ch, cc);
    }

    private Macro ONVERBATIM;
    public Macro getOnVerbatimMacro(){
        return ONVERBATIM;
    }
    public void initCommands() {
        Define_Macro defm = new Define_Macro();

        for(Command c:defm.commands){
            expandarea.setCommand(c);
        }
        for(Command c:Define_EscapeString.createEscapeStringCommands()){
            expandarea.setCommand(c);
        }

        ONVERBATIM=defm.ONVERBATIM;

        //日付
        GregorianCalendar calendar = new GregorianCalendar();
        int year = calendar.get(Calendar.YEAR);
        int date = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH)+1;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int HOUR = calendar.get(Calendar.HOUR);
        int min = calendar.get(Calendar.MINUTE);
        int sec = calendar.get(Calendar.SECOND);
        int week = calendar.get(Calendar.DAY_OF_WEEK)-Calendar.SUNDAY;
        int ampm = hour>12?1:0;
        defineCommand(new Command("\\year",Integer.toString(year)));
        defineCommand(new Command("\\date",Integer.toString(date)));
        defineCommand(new Command("\\month",Integer.toString(month)));
        defineCommand(new Command("\\hour",Integer.toString(hour)));
        defineCommand(new Command("\\HOUR",Integer.toString(HOUR)));
        defineCommand(new Command("\\minute",Integer.toString(min)));
        defineCommand(new Command("\\second",Integer.toString(sec)));
        defineCommand(new Command("\\week",Integer.toString(week)));
        defineCommand(new Command("\\ampm",Integer.toString(ampm)));
        
        //ファイル名系
        String file;
        String filename = sourceFile.getFileName().toString();
        int l = filename.lastIndexOf('.');
        if(l!=-1){
            filename = filename.substring(0, l);
        }
        String bd;//basedirectory
        try{
            file = baseDirectory.relativize(sourceFile).toString();
            bd=baseDirectory.toAbsolutePath().toString();
        }catch(Exception e){
            bd="";
            file="";
        }
        defineCommand(new Command("\\filepath",file));//\directoryからの相対パス
        defineCommand(new Command("\\filename",filename));//拡張子を除いたファイル名
        defineCommand(new Command("\\directory",bd));//ディレクトリ
        
        
    }

    public FullExpandArea createFullExpandArea(){
        return new FullExpandArea(expandarea);
    }

    public TokenChain fullExpand(Token t) throws Exception{
        FullExpandArea area = new FullExpandArea(expandarea);
        area.add(t);
        area.run();
        return area.getTokens();
    }

    public TokenChain fullExpand(Token t,boolean useNumber) throws Exception{
        FullExpandArea area = new FullExpandArea(expandarea);
        area.add(t);
        area.setUseNumber();
        area.run();
        return area.getTokens();
    }

    public OneTimeExpandArea createOneTimeExpandArea(){
        return new OneTimeExpandArea(expandarea);
    }

    public TokenChain onetimeExpand(Token t) throws Exception{
        OneTimeExpandArea a = createOneTimeExpandArea();
        a.add(t);
        a.run();
        return a.getTokens();
    }

    public ExpandArea createExpandArea(){
        return new ExpandArea(expandarea, this, this);
    }

    public void defineCommand(Command com){
        expandarea.setCommand(com);
    }

    public void defineCommandForMacro(Command com){
        expandarea.setCommand(com);
    }

    public void defineGlobalCommand(Command com){
        expandarea.setGlobalCommand(com);
    }

    public void defineGlobalCommandForMacro(Command com){
        expandarea.setGlobalCommand(com);
    }
    public Command getCommand(String name){
        return expandarea.getCommand(name);
    }

    public void removeCommand(String name){
        expandarea.removeCommand(name);
    }

    public void setAfterBlock(Token t){
        expandarea.setAfterBlock(t);
    }

    public void pushNewCharCategoryBlock(){
        chardefine.pushDefine();
    }

    public void pushNewCharCategoryDefiner(CharCategoryDefiner def){
        chardefine.pushDefine(def);
    }

    public void setNewCharCategoryDefiner(CharCategoryDefiner def){
        chardefine.pushDefine(def);
    }

    public void popCharCategoryBlock(){
        chardefine.pop();
    }

    public void setStartlineActiveChar(char ch ,String string){
        tokenfactory.setLineStartActiveChar(ch, string);
    }

    public int getFileLine(){
        Source source=currentSource;

        if(!(source instanceof StringSource))for(Source s:sourcestack){
            if(s instanceof StringSource){
                source = s;
                break;
            }
        }

        if(source instanceof StringSource){
            return ((StringSource) source).getLine();
        }
        return -1;
    }

    public boolean isMathMode(){
        return isMathMode;
    }



    public static boolean isSizeUnit(String str){
        return str.endsWith("px")||
                str.endsWith("em")||str.endsWith("ex")||
                str.endsWith("mm")||str.endsWith("pt")||str.endsWith("cm")||
                str.endsWith("pc");
    }

    private PrintStream errstream = System.err;
    private PrintStream outputstream = System.out;
    public void printError(String message){
        errstream.println(message);
        errstream.println(getCurrentSourcePosition());
    }

    public void printMessage(String message){
        outputstream.println(message);
    }

    public void setErrorPrintStream(PrintStream stream){
        if(stream==null)return;
        errstream=stream;
    }

    public void setPrintStream(PrintStream stream){
        if(stream == null)return;
        outputstream = stream;
    }
    
    private static final IDNameFactory DefaultIDFactory = new IDNameFactory(){
        private int image=1,table=1,def=1,section=1;
        
        
        public String getIDName(LmNode node ,String labelname){
            String name = (node instanceof Environment)?((Environment)node).getTagName():node.getName();
            if(name.contains("img")){
                return format("__fig:%03d", image++);
            }
            if(name.contains("table")){
                return format("__table:%03d",table++);
            }
            if(name.contains("section")||name.contains("chapter")){
                return format("__section:%03d",section++);
            }
            return format("__label:%03d",def++);
        }
    };
    
    private IDNameFactory idFactory=DefaultIDFactory;
    
    public void setIDNameFactory(IDNameFactory idnameFactory){
        if(idnameFactory==null)return;
        this.idFactory=idnameFactory;
    }
    
    private Map<String,String> idlabelmap = new HashMap<>();
    
    public boolean isDefinedLabelID(String idname){
        return idlabelmap.containsValue(idname);
    }
    
    private static String appendIDString(int i){
        String ret="";
        while(i>25){
            ret=Character.toString((char)('a'+i%26))+ret;
            i/=26;
        }
        ret=Character.toString((char)('a'+i))+ret;
        return ret;
    }
    
    public RefTarget createRefTarget(String labelname,Label label){
        LmNode node = label.node;
        String refValue = label.refvalue;
        setID(node, labelname);
        return new RefTarget(node.getID(), refValue, labelname);
    }
    
    
    public void setIdMap(String idname,String labelname){
        if(idname==null || labelname==null)return;
        idlabelmap.put(labelname, idname);
    }
    
    /**
     * システムで管理できる上でユニークなIDを生成し、設定します。
     * @param node
     * @param mapKey
     */
    public void setID(LmNode node,String mapKey){
        if(node.getID()==null){
            String idname;
            if(mapKey!=null && idlabelmap.containsKey(mapKey)){
                idname=idlabelmap.get(mapKey);
            }else{
                idname = idFactory.getIDName(node, mapKey);
                if(isDefinedLabelID(idname)){//すでに存在している場合は適当に末尾にアルファベットをくっつける。
                    //ただし、aaaaaaaaaaみたいにどんどん伸びるのは見た目がよろしくないので
                    //a～zを使ってみて、だめだったらaa,ab,ac......,ba,bb………という風にした。
                    //まず二文字より伸びることはない。
                    String ss=idname+appendIDString(0);
                    int i=1;
                    while(isDefinedLabelID(ss)){
                        ss=idname+appendIDString(i++);
                    }
                    idname = ss;
                }
            }
            node.setAttr("id", idname);
            if(mapKey!=null){
                printAux("\\prenewlabel{");
                printAux(mapKey);
                printAux("}{");
                printAux(idname);
                printlnAux("}");
            }
        }
    }
    
    
    
    //メモリ監視デバッグ用
    private static long usedMemory(){
        Runtime r = Runtime.getRuntime();
        return r.totalMemory()-r.freeMemory();
    }
    
}
