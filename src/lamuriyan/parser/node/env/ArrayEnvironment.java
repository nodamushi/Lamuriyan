package lamuriyan.parser.node.env;

import java.util.*;
import java.util.Map.Entry;

import lamuriyan.parser.Command;
import lamuriyan.parser.EnvironmentConstructor;
import lamuriyan.parser.EnvironmentFactory;
import lamuriyan.parser.LamuriyanEngine;
import lamuriyan.parser.macro.Function;
import lamuriyan.parser.macro.Macro;
import lamuriyan.parser.macro.MacroProcess;
import lamuriyan.parser.node.LmAttr;
import lamuriyan.parser.node.LmElement;
import lamuriyan.parser.node.LmNode;
import lamuriyan.parser.token.Char;
import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;
import lamuriyan.parser.token.TokenType;



/**
 * 表を作る為の基本的な環境。環境名はbasetable、数式モードで使う場合はbasearrayである。<br>
 * 環境内部でTeXで処理をしている間はこの環境にはCellEnvironment以外一切のNodeを追加することは出来ない。<br>
 * また、実際にはCellEnvironmentも子として追加はなされていない。<br>
 * DOM構築はclose処理で行う。<br>
 * その際、セルのタグの名前は\cellnameを、行の名前は\rownameを用いる。未定義である場合はtd,trを利用する。<br>
 * セルに、cellnameという名前のプロパティが宣言されているときは、そちらの利用を優先する。thに変更したい場合はこれを用いる。<br><br>
 * 
 * \hlineは、現在の行にclass="bordertop"を付加し、その一つ前の行があれば、class="borderbottom"を付加する。<br>
 * \clineは、現在の行の該当セルにclass="bordertop"を付加し、その一つ前の行があれば、該当セルにclass="borderbottom"を付加する<br>
 * ただし、\clineは一つ前の行の該当セルにcol属性が設定されている場合はクラス名を付加しない。<br>
 * これらのボーダーの処理はCSSに一任する。<br><br>
 * \\は現在操作しているセルと行と閉じ、新たな行と、最初のセルを生成する。<br>
 * &は現在操作しているセルを閉じ、新たなセルを生成する。<br><br>
 * この環境が作成されたとき、自動で\\が発行され、最初の行とセルが作成される。<br>
 * この環境が終了するとき、自動でセルと行は閉じられる。<br>
 * 行が生成された後には\rowbeginが、閉じる前には\rowendコマンドが発行される。
 * セルが生成された後には\cellbeginがセルが終了する前には\cellendが<br>
 * セルとセルの間では\cellbetweenが発行される。<br>
 * 必要があれば、これらをフックとして利用する。<br><br>
 * 
 * セルに対して書き込みやノード追加処理などを行っているとき、環境はArrayEnvironmentではなく、CellEnvironmentという環境にある。<br>
 * この環境はTextEnvironmentを拡張した物であるが、setProperty、setAttrなど、全て親のArrayEnvironmentに追加される。<br> <br>
 * CellEnvironmentに属性やプロパティを設定したい場合は、setCellAttr,setCellPropertyメソッドを利用する。<br>
 * マクロは\cellattr、\cellpropである。<br><br>
 * また、行に関してはTeXで処理している間はその実態すらなく、環境が閉じられた後にはじめて生成される。<br>
 * 行に対して属性を設定したい場合は、setRowAttrを用いる。または\rowattrを用いる。<br><br><br>
 * 
 * セルを横方向に結合したい場合は\@multicolumn{正整数}を用いる。縦方向に結合したい場合は\@multirow{正整数}を用いる。<br>
 * \@multicolumnや\@multirowは単純にセルに対してcolとrow属性を付加するだけなので、TeXの様に\malticolumn{数}{書式}{内容}
 * の様に記述する必要性はない。セル内のどこで記述しても同じである。<br>
 * なお、縦方向に結合し、\hlineを書いたとしても、HTMLの仕様上、TeXの様にセルを分断するような線は出てこない。<br><br>
 * 
 * <br><br>
 * 最後の行において、セルが一つしか生成されておらず、そのセルには要素が一つも入っていないとき、この行を無視する。<br>
 * ただし、\hline,\clineについては処理が前の行に対してのみ適応される。<br><br><br>
 * この環境はclrなどと言った要素の配置位置などを必要とせず、&が発行される限り要素は追加される。<br>
 * この機能は新たにtabularを定義する予定である。たぶん。めいびー。<s>実はどういうマクロで書けるのか分かってな</s><br><br>
 * なお、LaTeXには区切りを線ではなく文字に返る@{}があるが、HTMLで区切りを変更することは出来ない為、文字を含んだセルとして
 * 定義するしかない。その際、\clineや\@multicolumnを用いると、このセルも一つと数える為整合性がとれなくなる。<br>
 * そのため、予めbasetable環境が定義される「前」に\tablecellnumbersに、セルの番号を\defstrで記述しておく。<br>
 * これは cccの様な場合、"1,2,3"という値を、cc@{,}cの様な場合@{,}で生成されるセルは一つ前のcに付属すると見なす場合は
 * "1,2,2,3"という値を設定する。<br>
 * これにより、実際は3番目のセルになるはずである@{,}を2番目のセルであると見なすことができ、
 * これに従って\clineや\@multicolumnの値を修正する。<br>
 * この値が設定されていない場合、もしくはこの設定を超える範囲は通常通りに番号が増えていく物と見なす。<br>
 * この番号の取得は、\cellnumberで取得可能である。また、\nextcellnumberで次に生成されるCellの番号を取得可能である。<br><br><br>
 * と、まぁ、ここまでは考えてArrayEnvironmentを実装したのはいいんだけど、実際にtabularをどう実装すればいいのかねぇ…<br><br>
 * とりあえず、最後に追加したセルと、次に入るセルに属性を設定する\setbothcellattr{最後のセルの属性}{次に入るセルの属性}
 * というマクロを定義した。これは、行にすでに一つ以上セルが追加されている場合、一つ目の引数で定義される属性を設定し、
 * もし、次にこの行にセルが来た場合、二つ目の引数で定義される属性を設定する。<br>
 * 中身はsetAttrsと同様属性名=値,属性名=値………である。
 * @author nodamushi
 *
 */
public class ArrayEnvironment extends Environment{
    
    private ArrayList<ArrayRow> rows = new ArrayList<>();
    private ArrayRow currentrow;
    private Cell currentcell; 
    private int[] cellnumberslist = new int[0];
    private String cellname = TEXT_CELL_NAME;
    
    static final String TEXT_CELL_NAME = "\\{textcell}\\",//通常使う
            MATH_CELL_NAME = "\\{mathcell}\\";//数式モードで使う
    static final String TEXT_TABLE_NAME="basetable",MATH_TABLE_NAME="basearray";
    public static final EnvironmentFactory textCellFactory,mathCellFactory;
    static{
        EnvironmentConstructor ec = new EnvironmentConstructor(){
            @Override
            public Environment create(LamuriyanEngine engine ,EnvironmentFactory factory){
                Environment e = engine.getCurrentEnvironment();
                if(e instanceof ArrayEnvironment){
                    return new TextCell(engine, (ArrayEnvironment)e);
                }
                return null;
            }
        };
        TokenChain begin=new TokenChain(),end = new TokenChain();
        begin.addAll(Token.escape("\\cellbegin"),new Token(TokenType.ESCAPE,"cell is empty"));
        end.addAll(Token.escape("\\cellend"));
        EnvironmentFactory fact = new EnvironmentFactory(TEXT_CELL_NAME,ec, 0, begin, end, null);
        textCellFactory = fact;
        
        ec = new EnvironmentConstructor(){
            public Environment create(LamuriyanEngine engine ,EnvironmentFactory factory){
                Environment e = engine.getCurrentEnvironment();
                if(e instanceof ArrayEnvironment){
                    return new MathCell(engine, (ArrayEnvironment)e);
                }
                return null;
            }
        };
        fact = new EnvironmentFactory(MATH_CELL_NAME,ec, 0, begin, end, null);
        mathCellFactory = fact;
    }
    
    
    
    public static final EnvironmentFactory basetabeleFactory,basearrayFactory;
    static{
        EnvironmentConstructor ec = new EnvironmentConstructor(){
            public Environment create(LamuriyanEngine engine ,EnvironmentFactory factory){
                return new ArrayEnvironment(TEXT_TABLE_NAME,engine, TEXT_CELL_NAME);
            }
        };
        TokenChain begin=new TokenChain(),end = new TokenChain();

        begin.addAll(new Token(TokenType.ESCAPE,"newrow"),
                new Token(TokenType.ESCAPE,"\\rowbegin"),
                new Token(TokenType.ESCAPE,"\\cellbetween"),new Token(TokenType.ESCAPE,"createcell"));


        end.addAll(new Token(TokenType.ESCAPE,"closecell"),new Token(TokenType.ESCAPE,"\\cellbetween"));
        basetabeleFactory = new EnvironmentFactory(TEXT_TABLE_NAME, ec, 0,begin,end, null);
        
        EnvironmentConstructor mec = new EnvironmentConstructor(){
            public Environment create(LamuriyanEngine engine ,EnvironmentFactory factory){
                return new ArrayEnvironment(MATH_TABLE_NAME, engine, MATH_CELL_NAME);
            }
        };
        basearrayFactory = new EnvironmentFactory(MATH_TABLE_NAME, mec, 0,begin,end, null);
        
    }
    

    
    
    public ArrayEnvironment(String name, LamuriyanEngine engine,String cellenvname){
        super(name,  engine);
        Command c = engine.getCommand("\\tablecellnumbers");
        if(c!=null && c.isString()){
            setCellNumbersList(c.getAsString());
        }
        if(TEXT_CELL_NAME .equals(cellenvname)|| MATH_CELL_NAME.equals(cellenvname)){
            cellname = cellenvname;
        }
        engine.defineCommand(AndCommand);
        engine.defineCommand(NewRowCommand);
        engine.defineCommand(CreateCellCommand);
        engine.defineCommand(CloseCellCommand);
        engine.defineCommand(CellIsEmptyCommand);
        engine.defineCommand(YenYenCommand);
        engine.defineCommand(CellAttrCommand);
        engine.defineCommand(RowAttrCommand);
        engine.defineCommand(CellPropCommand);
        engine.defineCommand(AtMultiColumnCommand);
        engine.defineCommand(AtMultiRowCommand);
        engine.defineCommand(HLineCommand);
        engine.defineCommand(CLineCommand);
        engine.defineCommand(NextCellNumberCommand);
        engine.defineCommand(CellNumberCommand);
        engine.defineCommand(SETBOTHCELLATTRCommand);
    }
    
    
    public void setCellNumbersList(String str){
        String[] s = str.split(",");
        ArrayList<Integer> arr = new ArrayList<>();
        for(String ss:s){
            try{
                arr.add(Integer.parseInt(ss));
            }catch(Exception e){}
        }
        cellnumberslist = new int[arr.size()];
        for(int i=0;i<cellnumberslist.length;i++)cellnumberslist[i] = arr.get(i);
    }
    
    

    public int getCellNumber(int i){
        if(i<0)return i;
        if(i>cellnumberslist.length){
            int last = 0;
            if(cellnumberslist.length!=0)last = cellnumberslist[cellnumberslist.length-1];
            last += i-cellnumberslist.length;
            return last;
        }
        return cellnumberslist[i];
    }
    
    
    public void setMultiColumn(String number){
        try{
            setMultiColumn(Integer.parseInt(number));
        }catch (Exception e) {
        }
    }
    
    public void setMultiColumn(int col){
        if(col<=0)return;
        int x = currentcell.getX();
        int end = x+col;
        int join=1;
        int i=x+1;
        for(;getCellNumber(i)<end;i++){
            join++;
        }
        currentrow.countedCells=i-1;
        currentcell.setCellAttr("colspan", Integer.toString(join));
    }
    
    @Override
    public LmElement getCurrentElement(){
        if(currentcell==null)return currentrow;
        else return (LmElement)currentcell;
    }
    
    public int getNextCellNumber(){
        return currentrow.countedCells+1;//何故か先に足し算してから追加する仕様にしていた。わかりにくい(-_-)
    }
    
    public void setMultiRow(String number){
        try{
            setMultiRow(Integer.parseInt(number));
        }catch (Exception e) {
        }
    }
    public void setMultiRow(int row){
        if(row<=0)return;
        currentcell.setCellAttr("rowspan", Integer.toString(row));
    }
    
    private static String isNull(String a,String defaultvalue){
        return a!=null?a:defaultvalue;
    }
    @Override
    public void close(){
        //TODO multirowで結合されるセルの除去
        String rn=isNull(getProperty("rowname"),"tr"),cn=isNull(getProperty("cellname"),"td");
        
        int i=0;
        LmElement lastelement = null;
        ArrayRow lastrow = null;
        for(;i<rows.size()-1;i++){
            ArrayRow r = rows.get(i);
            LmElement e = new LmElement(rn);
            if(r.hline){
                e.setAttr("class", "bordertop");
                if(lastelement!=null)
                    lastelement.setAttr("class", "borderbottom");
            }
            
            for(LmAttr at:r.getAttrs()){
                e.setAttr(at);
            }
            
            for(Cell ce:r.cells){
                ce.off();
                LmNode c = (LmNode)ce;
                String cc = cn;
                if(ce.getProperty("cellname")!=null){
                    cc=ce.getProperty("cellname");
                }
                ce.setCellProperty(TAGNAMECOMMAND, cc);
                e.add(c);
                if(r.cline.contains(ce.getColumnNumber())){
                    ce.setAttr("class", "bordertop");
                }
            }
            
            if(lastrow!=null){
                for(Cell ce:lastrow.cells){
                    LmNode c = (LmNode)ce;
                    if(r.cline.contains(ce.getColumnNumber())){
                        if(c.getAttr("col")==null)//colが設定されている場合はclineは無視
                            ce.setAttr("class", "borderbottom");
                    }
                }
            }
            
            super.add(e,false);
            lastelement = e;
            lastrow = r;
        }
        
        //最後の行だけは単に\hlineを書く為だけって事が多分にあるので別処理
        ArrayRow r = rows.get(rows.size()-1);
        if(lastelement==null//行が1行しかなく、処理がされていないとき
                ||!r.isEmptyRow()){//空行でないとき
            //普通に処理
            LmElement e = new LmElement(rn);
            if(r.hline){
                e.setAttr("class", "bordertop");
            }
            
            for(LmAttr at:r.getAttrs()){
                e.setAttr(at);
            }
            
            for(Cell ce:r.cells){
                ce.off();
                LmNode c = (LmNode)ce;
                String cc = cn;
                if(ce.getProperty("cellname")!=null){
                    cc=ce.getProperty("cellname");
                }
                ce.setCellProperty(TAGNAMECOMMAND, cc);
                e.add(c);
                if(r.cline.contains(ce.getColumnNumber())){
                    ce.setAttr("class", "bordertop");
                }
            }
            super.add(e,false);
        }else{
            //\hline,\clineを書く為だけの行
            if(r.hline){
                lastelement.setAttr("class", "borderbottom");
            }
            for(Cell ce:lastrow.cells){
                LmNode c = (LmNode)ce;
                if(r.cline.contains(ce.getColumnNumber())){
                    if(c.getAttr("col")==null)//colが設定されている場合はclineは無視
                        ce.setAttr("class", "borderbottom");
                }
            }
        }
    }
    
    public void setHline(){
        currentrow.hline=true;
    }
    
    public void setCline(int start,int end){
        if(end<start){
            int i = start;
            start = end;
            end = i;
        }
        for(int i=start;i<=end;i++){
            currentrow.addCline(i);
        }
    }
    
    public Token createNewCell(){
        Environment e = engine.getCurrentEnvironment();
        if(e==this){
            return engine.createNewEnvironment(cellname);
        }
        return null;
    }
    
    public Token getCloseCellToken(){
        Environment e = engine.getCurrentEnvironment();
        if(e==currentcell){
            return engine.getCloseEnvironmentToken(cellname);
        }
        return null;
    }
    
    public void closeCell(){
        Environment e = engine.getCurrentEnvironment();
        if(e==currentcell){
            engine.closeEnvironment(cellname);
            currentcell = null;
        }
    }
    
    
    public void createNewRow(){
        ArrayRow row = new ArrayRow();
        rows.add(row);
        currentrow = row;
        currentcell = null;
    }
    
    public void setCellAttr(String name,String value){
        currentcell.setCellAttr(name, value);
    }
    
    public void setCellAttr(LmAttr attr){
        currentcell.setCellAttr(attr);
    }
    
    public void setRowAttr(String name,String value){
        currentrow.setAttr(name, value);
    }
    
    @Override
    public boolean add(LmNode n ,boolean moveCurrent){
        if( n instanceof Environment &&n instanceof Cell ){
            currentcell = (Cell)n;
            currentrow.add(currentcell);
            return true;
        }else return false;//Cell以外は面倒くさいので全部無視。　rowもどうせ後で名前を置換しないと行けないので、ArrayEnvironmentには追加しない。
    }

    @Override
    public Token input(Char t){
        return null;
    }

    @Override
    protected void currentChanged(){
        
    }
    
    @Override
    public LmNode getSettingTargetNode(){
        if(currentcell==null)return currentrow;
        return super.getSettingTargetNode();
    }

    public void setBothCellAttr(Map<String, String> lmap ,
            Map<String, String> rmap){
        currentrow.setBothCellAttr(lmap, rmap);
    }
    
    class ArrayRow extends LmElement{
        public ArrayRow(){
            super("row");
        }

        List<Cell> cells=new ArrayList<>();
        List<Integer> cline = new ArrayList<>();
        boolean hline=false;
        int countedCells=0;
        Map<String, String> nextcellattr;
        
        boolean isEmptyRow(){
            if(countedCells>1)return false;
            Cell c = cells.get(0);
            return c.isEmpty();
        }
        
        
//        
//        void setAttr(String name,String value){
//            attribute.put(name, value);
//        }
        
        public void setBothCellAttr(Map<String, String> lmap ,
                Map<String, String> rmap){
            nextcellattr = rmap;
            if(countedCells!=0){
                Cell c = cells.get(cells.size()-1);
                for(Entry<String, String> en:lmap.entrySet()){
                    c.setCellAttr(en.getKey(), en.getValue());
                }
            }
        }
        
        void add(Cell c){
            countedCells++;
            cells.add(c);
            c.setColumnNumber(getCellNumber(countedCells));
            c.setX(countedCells);
            if(nextcellattr!=null){
                for(Entry<String, String> en:nextcellattr.entrySet()){
                    c.setCellAttr(en.getKey(), en.getValue());
                }
                nextcellattr=null;
            }
        }
        
        void addCline(int i){
            if(i<0)return;
            if(!cline.contains(i))cline.add(i);
        }
        
        
        
    }
    
    
    
    
    ////テーブル用マクロ-----------------------------------------------------------
    
    
    private static final Function newrow,closecell,createcell,cellisempty,nextcellnumber,cellnumber;
    private static final Macro and,yenyen,cellattr,cellprop,rowattr,atmulticolumn,atmultirow,hline,cline,setbothcellAttr;
    static{
        newrow = new Function(){
            public Object run(LamuriyanEngine engine) throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof ArrayEnvironment) {
                    ArrayEnvironment ae = (ArrayEnvironment) e;
                    ae.createNewRow();
                }
                return null;
            }
        };
        
        nextcellnumber = new Function(){
            
            @Override
            public Object run(LamuriyanEngine engine) throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof ArrayEnvironment) {
                    ArrayEnvironment ae = (ArrayEnvironment) e;
                    return Integer.toString(ae.getNextCellNumber());
                }else if(e instanceof Cell){
                    Cell cell = (Cell)e;
                    return Integer.toString(cell.getParentArray().getNextCellNumber());
                }
                return null;
            }
        };
        
        cellnumber = new Function(){
            public Object run(LamuriyanEngine engine) throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if(e instanceof Cell){
                    Cell cell = (Cell)e;
                    return Integer.toString(cell.getColumnNumber());
                }else if (e instanceof ArrayEnvironment) {
                    ArrayEnvironment ar = (ArrayEnvironment) e;
                    return Integer.toString(ar.currentrow.countedCells);
                }
                return null;
            }
        };
        
        TokenChain andp = new TokenChain();
        andp.addAll(new Token(TokenType.ESCAPE,"closecell"),new Token(TokenType.ESCAPE,"\\cellbetween")
                ,new Token(TokenType.ESCAPE,"createcell"));
        and = new Macro("&", andp, null, null);
        TokenChain yenyenp = new TokenChain();
        yenyenp.addAll(new Token(TokenType.ESCAPE,"\\rowend"),new Token(TokenType.ESCAPE,"closecell"),new Token(TokenType.ESCAPE,"\\cellbetween"),
                new Token(TokenType.ESCAPE,"newrow"),new Token(TokenType.ESCAPE,"\\rowbegin"),
                new Token(TokenType.ESCAPE,"\\cellbetween"),new Token(TokenType.ESCAPE,"createcell"));
        //本当は\\[1em]みたいにするべきナンだろうけどねーめんどーくせー
        yenyen = new Macro("\\\\", yenyenp, null, null);
        closecell = new Function(){
            public Object run(LamuriyanEngine engine){
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof Cell) {
                    Cell cell = (Cell) e;
                    cell.getParentArray().closeCell();
                }
                return null;
            }
        };
        createcell = new Function(){
            public Object run(LamuriyanEngine engine)
                    throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof ArrayEnvironment) {
                    ArrayEnvironment ae = (ArrayEnvironment) e;
                    return ae.createNewCell();
                }
                return null;
            }
        };
        cellisempty = new Function(){
            public Object run(LamuriyanEngine engine)
                    throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof Cell) {
                    Cell cell = (Cell) e;
                    cell.setStillEmpty();
                }
                return null;
            }
        };
        
        TokenChain args = new TokenChain();
        args.addAll(Token.Args1,Token.Args2);
        setbothcellAttr = new Macro("\\setbothcellattr", null, args, new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Environment e = engine.getCurrentEnvironment();
                ArrayEnvironment ae=null;
                if (e instanceof Cell) {
                    ae = ((Cell) e).getParentArray();
                }else if (e instanceof ArrayEnvironment) {
                    ae = (ArrayEnvironment) e;
                }
                
                if(ae==null){
                    printError("現在テーブルの環境ではありません");
                    return null;
                }
                String left = engine.fullExpand(args.get(0)).toString();
                String right = engine.fullExpand(args.get(1)).toString();
                String[] lsp = left.split(",");
                String[] rsp = right.split(",");
                Map<String, String> 
                lmap = new HashMap<>(),
                rmap = new HashMap<>();
                for(String str:lsp){
                    String[] ss = str.split("=",2);
                    if(ss.length==2){
                        lmap.put(ss[0],ss[1]);
                    }
                }
                        
                for(String str:rsp){
                    String[] ss = str.split("=",2);
                    if(ss.length==2){
                        rmap.put(ss[0],ss[1]);
                    }
                }
                
                ae.setBothCellAttr(lmap,rmap);
                
                return null;
            }
        });
        
        cellattr = new Macro("\\cellattr", null, args, new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof Cell) {
                    Cell cell = (Cell) e;
                    Token a1 = args.get(0),a2 = args.get(1);
                    String name = engine.fullExpand(a1).toString();
                    if(name.isEmpty())return null;
                    
                    String value = engine.fullExpand(a2).toString();
                    cell.setCellAttr(name, value);
                }else{
                    printError("環境がCellではありません。");
                }
                return null;
            }
        });
        cellprop = new Macro("\\cellprop", null, args, new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof Cell) {
                    Cell cell = (Cell) e;
                    Token a1 = args.get(0),a2 = args.get(1);
                    String name = engine.fullExpand(a1).toString();
                    if(name.isEmpty())return null;
                    
                    String value = engine.fullExpand(a2).toString();
                    cell.setCellProperty(name, value);
                }else{
                    printError("環境がCellではありません。");
                }
                return null;
            }
        });
        
        rowattr = new Macro("\\rowattr", null, args, new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof Cell) {
                    Cell cell = (Cell) e;
                    Token a1 = args.get(0),a2 = args.get(1);
                    String name = engine.fullExpand(a1).toString();
                    if(name.isEmpty())return null;
                    
                    String value = engine.fullExpand(a2).toString();
                    cell.getParentArray().setRowAttr(name, value);
                }else{
                    printError("環境がCellではありません。");
                }
                return null;
            }
        });
        args = new TokenChain();
        args.add(Token.Args1);
        atmulticolumn = new Macro("\\@multicolumn",null,args, new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof Cell) {
                    Cell cell = (Cell) e;
                    Token t1 = args.get(0);
                    String v = engine.fullExpand(t1).toString();
                    cell.getParentArray().setMultiColumn(v);
                }else{
                    printError("環境がCellではありません。");
                }
                return null;
            }
        });
        
        atmultirow = new Macro("\\@multirow",null,args, new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof Cell) {
                    Cell cell = (Cell) e;
                    Token t1 = args.get(0);
                    String v = engine.fullExpand(t1).toString();
                    cell.getParentArray().setMultiRow(v);
                }else{
                    printError("環境がCellではありません。");
                }
                return null;
            }
        });
        cline =new Macro("\\cline", null, args, new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof Cell) {
                    Cell cell = (Cell) e;
                    Token t = args.get(0);
                    String v = engine.fullExpand(t).toString();
                    String[] ss = v.split("-",2);
                    if(ss.length!=2){
                        printError("数値-数値　で範囲を指定してください");
                    }
                    int st = Integer.parseInt(ss[0]);
                    int en = Integer.parseInt(ss[1]);
                    cell.getParentArray().setCline(st, en);
                }else{
                    printError("環境がCellではありません。");
                }
                return null;
            }
        });
        
        hline = new Macro("\\hline", null, null, new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof Cell) {
                    Cell cell = (Cell) e;
                    cell.getParentArray().setHline();
                }else{
                    printError("環境がCellではありません。");
                }
                return null;
            }
        });
        
    }
    private static Command AndCommand=new Command("&", and),
            NewRowCommand=new Command("newrow", newrow),
            CreateCellCommand=new Command("createcell",createcell),
            CloseCellCommand=new Command("closecell", closecell),
            CellIsEmptyCommand = new Command("cell is empty",cellisempty),
            YenYenCommand=new Command(yenyen.getName(),yenyen),
            CellAttrCommand=new Command(cellattr.getName(),cellattr),
            RowAttrCommand=new Command(rowattr.getName(),rowattr),
            CellPropCommand=new Command(cellprop.getName(),cellprop),
            AtMultiColumnCommand = new Command(atmulticolumn.getName(), atmulticolumn),
            AtMultiRowCommand = new Command(atmultirow.getName(), atmultirow),
            HLineCommand = new Command(hline.getName(),hline),
            CLineCommand = new Command(cline.getName(), cline),
            NextCellNumberCommand=new Command("\\nextcellnumber",nextcellnumber),
            CellNumberCommand=new Command("\\cellnumber", cellnumber),
            SETBOTHCELLATTRCommand=new Command(setbothcellAttr.getName(), setbothcellAttr);
    
    
    
    
    
    
    
    
}

interface Cell{
    
    public void setProperty(String str ,String value);
    public void off();
    
    public void setCellProperty(String str,String value);
    public void setColumnNumber(int n);
    public int getColumnNumber();
    public void setX(int x);
    public int getX();
    public void setAttr(String name ,String value);
    
    public void setCellAttr(String name,String value);
    
    public void setCellAttr(LmAttr attr);
    public boolean isEmpty();
    public String getProperty(String name);
    public void setStillEmpty();
    public ArrayEnvironment getParentArray();
    
}

class MathCell extends MathEnvironment implements Cell{
    
    int x;
    int number;
    ArrayEnvironment arrayparent;
    boolean on = true;
    boolean sempty = true;
    
    protected MathCell( LamuriyanEngine engine,ArrayEnvironment parent){
        super(ArrayEnvironment.MATH_CELL_NAME, engine);
        arrayparent = parent;
    }
    @Override
    public void off(){
        on = false;
    }
    

    @Override
    public void setCellProperty(String str ,String value){
        super.setProperty(str,value);
    }

    @Override
    public void setColumnNumber(int n){
        number = n;
    }

    @Override
    public void setX(int x){
        this.x = x;
    }

    public void setCellAttr(String name ,String value){
        LmAttr attr = new LmAttr(name);
        attr.setValue(value);
        setCellAttr(attr);
    }

    @Override
    public void setCellAttr(LmAttr attr){
        super.setAttr(attr);
    }
    @Override
    public int getColumnNumber(){
        return number;
    }
    
    @Override
    public void setProperty(String str ,String value){
        if(on)
            arrayparent.setProperty(str, value);
        else
            super.setProperty(str, value);
    }
    
    @Override
    public int getX(){
        return x;
    }
    
    @Override
    public void setAttr(String name ,String value){
        if(on)
            arrayparent.setAttr(name, value);
        else
            super.setAttr(name, value);
    }
    @Override
    public void setAttr(LmAttr attr){
        if(on)
            arrayparent.setAttr(attr);
        else
            super.setAttr(attr);
    }

    @Override
    public void setStillEmpty(){
        sempty =true;
    }
    
    @Override
    public boolean add(LmNode n ,boolean moveCurrent){
        sempty=false;
        return super.add(n, moveCurrent);
    }
    
    @Override
    public boolean isEmpty(){
        return sempty||super.isEmpty();
    }


   
    @Override
    public ArrayEnvironment getParentArray(){
        return arrayparent;
    }
}

class TextCell extends TextEnvironment implements Cell{

    int x;
    int number;
    ArrayEnvironment arrayparent;
    boolean on = true;
    boolean sempty = true;
    
    public TextCell(LamuriyanEngine engine,ArrayEnvironment parent){
        super(ArrayEnvironment.TEXT_CELL_NAME,  engine);
        arrayparent = parent;
    }
    @Override
    public void off(){
        on = false;
    }
    

    @Override
    public void setCellProperty(String str ,String value){
        super.setProperty(str,value);
    }

    @Override
    public void setColumnNumber(int n){
        number = n;
    }

    @Override
    public void setX(int x){
        this.x = x;
    }

    @Override
    public void setCellAttr(String name ,String value){
        LmAttr attr = new LmAttr(name);
        attr.setValue(value);
        setCellAttr(attr);
    }

    @Override
    public void setCellAttr(LmAttr attr){
        super.setAttr(attr);
    }
    @Override
    public int getColumnNumber(){
        return number;
    }
    
    @Override
    public void setProperty(String str ,String value){
        if(on)
            arrayparent.setProperty(str, value);
        else
            super.setProperty(str, value);
    }
    
    @Override
    public int getX(){
        return x;
    }
    
    @Override
    public void setAttr(String name ,String value){
        if(on)
            arrayparent.setAttr(name, value);
        else
            super.setAttr(name, value);
    }
    
    @Override
    public void setAttr(LmAttr attr){
        if(on)
            arrayparent.setAttr(attr);
        else
            super.setAttr(attr);
    }
    @Override
    public void setStillEmpty(){
        sempty =true;
    }
    
    @Override
    public boolean add(LmNode n ,boolean moveCurrent){
        sempty=false;
        return super.add(n, moveCurrent);
    }
    
    @Override
    public boolean isEmpty(){
        return sempty||super.isEmpty();
    }


   
    @Override
    public ArrayEnvironment getParentArray(){
        return arrayparent;
    }
}
