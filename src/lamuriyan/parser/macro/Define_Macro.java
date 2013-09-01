package lamuriyan.parser.macro;

import static lamuriyan.parser.token.TokenType.*;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import lamuriyan.parser.*;
import lamuriyan.parser.ExpandArea.ExpandAfter;
import lamuriyan.parser.io.LamuriyanFileUtilities;
import lamuriyan.parser.label.Label;
import lamuriyan.parser.label.RefTarget;
import lamuriyan.parser.node.LmAttr;
import lamuriyan.parser.node.LmElement;
import lamuriyan.parser.node.LmNode;
import lamuriyan.parser.node.env.Environment;
import lamuriyan.parser.node.env.MathEnvironment;
import lamuriyan.parser.node.env.MathEnvironment.MathTokenType;
import lamuriyan.parser.node.env.TextEnvironment;
import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;
import lamuriyan.parser.token.TokenPair;
import lamuriyan.parser.token.TokenType;
import nodamushi.fa.AutomatonFactory;
import nodamushi.fa.MoveFunction;
import nodamushi.number.NumberToString;


public class Define_Macro{
    
    public Macro ONVERBATIM;

    //\ifの実装だけど、\@ifnextcharでも使うことにしたので分離
    private static boolean iffunction(Token a,Token b,LamuriyanEngine engine){
        if(a.getType() != b.getType())return false;
        switch(a.getType()){
            case ESCAPE:
                return a.toString().equals(b.toString());
            default:
                return a.getChar()==b.getChar();
        }
    }
    
    private static String getCSName(Token t){
        Token a=null;
        switch(t.getType()){
            case ESCAPE:
                a = t;
                break;
            case __TOKEN__GROUP__:
                if(t.size()==1){
                    if(t.get(0).getType()==ESCAPE){
                        a=t;
                    }
                }
                break;
        }
        if(a==null){
            return null;
        }else return a.toString();
    }
    
    //コマンドをどんどん書いてくよ－
    private void init(){

        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //---------------------リスト-----------------------------
        //............________________________________............
        //何？TeXでリストは邪道？　うるせぇ！あんなキチ●イみたいなマクロかいてられっかよ！！！
        //Javaのリストを使えるようにしまぁすぅ。
        
        def("\\newlist","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                String name = getCSName(args.get(0));
                if(name==null){
                    printError("定義する名前はコントロールシーケンスである必要があります。");
                    return null;
                }
                
                TokenChainList list = new TokenChainList();
                Command c = new Command(name, list);
                return c;
            }
        });
        
        def("\\get","#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                
                String name = getCSName(args.get(0));
                if(name==null){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります");
                    return null;
                }
                Command c=engine.getCommand(name);
                if(c==null || !c.isList()){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります。"+name);
                    return null;
                }
                
                String number = engine.fullExpand(args.get(1),true).toString().trim();
                int n = Integer.parseInt(number);
                return c.getAsList().get(n);
            }
        });
        def("\\set","#3",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                
                String name = getCSName(args.get(0));
                if(name==null){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります");
                    return null;
                }
                Command c=engine.getCommand(name);
                if(c==null || !c.isList()){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります。"+name);
                    return null;
                }
                
                String number = engine.fullExpand(args.get(1),true).toString().trim();
                int n = Integer.parseInt(number);
                Token val = args.get(2);
                TokenChain tc ;
                if(val.getType() == __TOKEN__GROUP__){
                    tc = val.getTokenChain();
                }else{
                    tc = new TokenChain();
                    tc.add(val);
                }
                c.getAsList().set(n, tc);
                return null;
            }
        });
        def("\\pop","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                
                String name = getCSName(args.get(0));
                if(name==null){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります");
                    return null;
                }
                Command c=engine.getCommand(name);
                if(c==null || !c.isList()){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります。"+name);
                    return null;
                }
                
                return c.getAsList().pop();
            }
        });
        
        def("\\shift","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                
                String name = getCSName(args.get(0));
                if(name==null){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります");
                    return null;
                }
                Command c=engine.getCommand(name);
                if(c==null || !c.isList()){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります。"+name);
                    return null;
                }
                
                return c.getAsList().shift();
            }
        });
        
        def("\\length","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                
                String name = getCSName(args.get(0));
                if(name==null){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります");
                    return -1;
                }
                Command c=engine.getCommand(name);
                if(c==null || !c.isList()){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります。"+name);
                    return -1;
                }
                
                return c.getAsList().length();
            }
        });
        
        def("\\ifemptylist","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                
                String name = getCSName(args.get(0));
                if(name==null){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります");
                    return null;
                }
                Command c=engine.getCommand(name);
                if(c==null || !c.isList()){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります。"+name);
                    return null;
                }
                
                return c.getAsList().isEmpty();
            }
        });
        
        
        def("\\push","#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                
                String name = getCSName(args.get(0));
                if(name==null){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります");
                    return null;
                }
                Command c=engine.getCommand(name);
                if(c==null || !c.isList()){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります。"+name);
                    return null;
                }
                Token t = args.get(1);
                TokenChain tc ;
                if(t.getType()==__TOKEN__GROUP__){
                    tc = t.getTokenChain();
                }else{
                    tc = new TokenChain();
                    tc.add(t);
                }
                c.getAsList().push(tc);
                return null;
            }
        });
        def("\\unshift","#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                
                String name = getCSName(args.get(0));
                if(name==null){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります");
                    return null;
                }
                Command c=engine.getCommand(name);
                if(c==null || !c.isList()){
                    printError("第一引数はリストが入ったコントロールシーケンスである必要があります。"+name);
                    return null;
                }
                Token t = args.get(1);
                TokenChain tc ;
                if(t.getType()==__TOKEN__GROUP__){
                    tc = t.getTokenChain();
                }else{
                    tc = new TokenChain();
                    tc.add(t);
                }
                c.getAsList().unshift(tc);
                return null;
            }
        });
        def("\\java@foreach","#3",new JavaForeach());
        
        
        
        
        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //--------------------if----------------------------------
        //............________________________________............
        
        def("\\newif","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                String name = getCSName(args.get(0));
                if(name==null){
                    printError("定義する名前はコントロールシーケンスである必要があります。");
                    return null;
                }
                String _name = name.substring(1);//\以降
                //TeXの仕様だと、どうも2文字目以降を切り出すみたいだけど、
                //2文字以下とかのときどうしてんのかね？
                //いちおう、その仕様にのっとるけど………
                if(_name.length()<2){
                    return new Command(name,iffalse);
                }else{
                    _name ="\\"+ _name.substring(2);
                    ChangeIFValue totrue = new ChangeIFValue(name, true);
                    ChangeIFValue tofalse = new ChangeIFValue(name, false);
                    Command[] c = {
                        new Command(name,iffalse),
                        new Command(_name+"true",totrue),
                        new Command(_name+"false",tofalse)
                    };
                    return c;
                }
            }
        });
        
        //ifのマクロは必ずtrue か falseを返さなくてはならない。
        //エラーがあった場合は必ずfalse
        
        
        
        def("\\iftrue",iftrue);
        def("\\iffalse",iffalse);
        def("\\else",ELSE);
        def("\\fi",FI);
        defif("\\ifcontaincs","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token token = args.get(0);
                return containcs(token);
            }
        });
        
        defif("\\if","#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a = args.get(0),b=args.get(1);
                return iffunction(a, b, engine);
            }
        }).setUseBlock(false);
        
        defif("\\ifx","#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a = args.get(0),b=args.get(1);
                if(a.getType() != b.getType())return false;
                switch(a.getType()){
                    case ESCAPE:
                        boolean boo= a.toString().equals(b.toString());
                        if(boo)return true;
                        Command c1 = engine.getCommand(a.toString());
                        Command c2 = engine.getCommand(b.toString());
                        if(c1==null)return c2==null;
                        return c1.isSameCommand(c2);
                    default:
                        return a.getChar()==b.getChar();
                }
            }
        }).setUseBlock(false);

        defif("\\ifxusefirst","#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a = args.get(0),b=args.get(1);
                if(a.getType() == __TOKEN__GROUP__){
                    if(a.size()==0)return false;
                    a = a.get(0);
                }
                if(b.getType() == __TOKEN__GROUP__){
                    if(b.size()==0)return false;
                    b = b.get(0);
                }
                if(a.getType() != b.getType())return false;
                switch(a.getType()){
                    case ESCAPE:
                        boolean boo= a.toString().equals(b.toString());
                        if(boo)return true;
                        Command c1 = engine.getCommand(a.toString());
                        Command c2 = engine.getCommand(b.toString());
                        if(c1==null)return false;
                        return c1.isSameCommand(c2);
                    default:
                        return a.getChar()==b.getChar();
                }
            }
        });
        
        defif("\\ifempty","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a = args.get(0);
                if(a.getType()!=__TOKEN__GROUP__)return false;
                return a.size()==0;
            }
        });
        
        
        
        defif("\\ifnum","#3",new MacroProcess(){
            
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a1=args.get(0),a2 = args.get(1),a3 = args.get(2);
                if(!a1.isNumber() || !a3.isNumber()){
                    printError("ifnum:数値を見つけられませんでした。");
                    return false;
                }
                
                if(a2.getType()!=CHAR){
                    printError("第二引数が=,<,>ではありませんでした。");
                    return false;
                }
                int n = Integer.parseInt(a1.toString());
                int m = Integer.parseInt(a3.toString());
                switch(a2.getChar()){
                    case '=':
                        return n==m;
                    case '<':
                        return n<m;
                    case '>':
                        return n>m;
                }
                printError("第二引数が=,<,>ではありませんでした。");
                return false;
            }
        }).setUseNumber(true);
        
        defif("\\ifodd","#1",new MacroProcess(){
            
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a1=args.get(0);
                if(!a1.isNumber() ){
                    printError("ifodd:数値を見つけられませんでした。");
                    return false;
                }
                int n = Integer.parseInt(a1.toString());
                return (n &1) == 1;
            }
        }).setUseNumber(true);
        
        defif("\\ifm","",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                return engine.isMathMode();
            }
        });
        
        //defとexpandafterとかを駆使すれば出来るのかもしれないけど、
        //やりたくなかったのでネイティブ実装
        def("\\@ifnextchar","#3",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                final Token t1=args.get(0),t2=args.get(1),t3=args.get(2);
                Macro macro = new Macro("\\@ifnextchar", null, tc(arg1), new MacroProcess(){
                    protected Object _run(LamuriyanEngine engine ,List<Token> args)
                            throws Exception{
                        Token t4 = args.get(0);
                        TokenChain tc = new TokenChain();
                        if(iffunction(t1, t4, engine)){
                            if(t2.getType()==__TOKEN__GROUP__)
                                tc.addAll(t2.getTokenChain());
                            else
                                tc.add(t2);
                        }else{
                            if(t3.getType()==__TOKEN__GROUP__)
                                tc.addAll(t3.getTokenChain());
                            else
                                tc.add(t3);
                        }
                        tc.add(t4);
                        return tc;
                    }
                });
                macro.setUseBlock(false);
                return macro;
            }
        });
        //\@if\iftrue{～～～}{#####}という風に書くと
        //～～～だけ展開される
        //\iftrueか\iffalseだけ
        //\newifで作った物もこのどちらかなので、使えます。
        def("\\@if","#3",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t1=args.get(0),t2=args.get(1),t3=args.get(2);
                Command c = engine.getCommand(t1.toString());
                if(c==null || (c.get()!=iffalse && c.get()!=iftrue)){
                    printError("iftrueかiffalseに相当する物を第一引数に入れてください  "+t1);
                    return null;
                }
                TokenChain tc = new TokenChain();
                if(iftrue == c.get()){
                    if(t2.getType()==__TOKEN__GROUP__)
                        tc.addAll(t2.getTokenChain());
                    else
                        tc.add(t2);
                }else{
                    if(t3.getType()==__TOKEN__GROUP__)
                        tc.addAll(t3.getTokenChain());
                    else
                        tc.add(t3);
                }
                return tc;
            }
        });

        
        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //--------------------展開系------------------------------
        //............________________________________............

        
        
        def("\\expandafter","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                ExpandAfter ea = new ExpandAfter();
                TokenChain tc = new TokenChain();
                tc.add(args.get(0));
                ea.token=tc;
                return ea;
            }
        }).setUseBlock(false);
        //一個しか展開できないなんて嫌だー。TeXにこういう系統のがあるのか知らんけど、作ったれー
        def("\\expandafterall","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                ExpandAfter ea = new ExpandAfter();
                
                TokenChain tc;
                Token t = args.get(0);
                if(t.getType()==__TOKEN__GROUP__){
                    tc=t.getTokenChain();
                }else{
                    tc = new TokenChain();
                    tc.add(t);
                }
                ea.token=tc;
                return ea;
            }
        });
        
        MacroProcess noexpandprocess = new MacroProcess(){
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t = args.get(0);
                t.setProtect(true);
                return t;
            }
        };
        
        def("\\unexpanded","#1",noexpandprocess);
        def("\\noexpand","#1",noexpandprocess).setUseBlock(false);


        
        def("\\string","#1",new MacroProcess(){
           @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
               Token t = args.get(0);
               TokenChain tc;
               switch(t.getType()){
                   case CHAR:
                       tc = new TokenChain(1);
                       tc.add(t);
                       break;
                   default:
                       String str = t.toString();
                       tc = Token.toCharTokenChain(str);
               }
               return tc;
           } 
        }).setUseBlock(false);
        

        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //--------------------定義系------------------------------
        //............________________________________............
        
        def("\\global",DEFGLOBAL);
        
        def("\\edef","#2", new MacroProcess(){
            public Object _run(LamuriyanEngine engine ,List<Token> args) throws Exception{
                Token a1 = args.get(0),b = args.get(1);
                if(a1.getType()!=TokenType.ESCAPE){
                    printError("第一引数がコマンドシーケンスでないです。");
                    return null;
                }
                FullExpandArea a = engine.createFullExpandArea();
                a.setUseNoExpand();
                a.add(b);
                a.run();
                TokenChain tc=a.getTokens();
                return new Command(a1.toString(),tc);
            }
        });
        
        //後で読み取るプロパティとかがコマンドだと困るので、文字を定義する為のマクロ
        //基本的にはedefと同じで、文字列で登録するだけ。
        def("\\defstr","#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a1 = args.get(0),b = args.get(1);
                if(a1.getType()!=TokenType.ESCAPE){
                    printError("第一引数がコマンドシーケンスでないです。");
                    return null;
                }
                TokenChain tc=engine.fullExpand(b,false);
                return new Command(a1.toString(),tc.toString());
            }
        });
        
        
        
        def("\\let", "#1", tc(escape("\\@ifnextchar"),chart('='),
                bgroup(),escape("\\java@let"),arg1,egroup(),bgroup(),escape("\\java@let"),arg1,chart('='),egroup()))
        .setUseBlock(false);
        def("\\java@let","#1=#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a0 = args.get(0),a1 = args.get(1);
                if(a0.getType()!=TokenType.ESCAPE){
                    printError("letの第一引数はコマンドシーケンスです");
                    return null;
                }
                if(a1.getType()==TokenType.ESCAPE){
                    Command c = engine.getCommand(a1.toString());
                    if(c!=null){
                        return c.copyMacro(a0.toString());
                    }else{
                        engine.removeCommand(a0.toString());
                        return null;
                    }
                }
                return new Command(a0.toString(),a1);
            }
        }).setUseBlock(false);
        def("\\futurelet","#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a0 = args.get(0),a1 = args.get(1);
                if(a0.getType()!=TokenType.ESCAPE){
                    printError("letの第一引数はコマンドシーケンスです");
                    return null;
                }
                final Token cs=a0,after = a1;
                Macro macro = new Macro("", null, tc(arg1), new MacroProcess(){
                    protected Object _run(LamuriyanEngine engine ,List<Token> args)
                            throws Exception{
                        Token a1 = args.get(0);
                        TokenChain tc=tc(escape("\\let"),cs,a1);
                        if(after.getType()==TokenType.__TOKEN__GROUP__){
                            tc.addAll(after.getTokenChain());
                        }else tc.add(after);
                        return tc;
                    }
                });
                macro.setUseBlock(false);
                return macro;
            }
        });
        def("\\java@@newcommand","#3",new NoOptionNewCommand(true));
        def("\\java@@renewcommand","#3",new NoOptionNewCommand(false));

        def("\\java@@newopcommand","#1#2[#3]#4", new OptionNewCommand(true));
        def("\\java@@renewopcommand","#1#2[#3]#4", new OptionNewCommand(false));
        
        
//      //一応コマンド削除を実装してみた
//      def("\\@delcommand","#1", new TeXMacroProcess(){
//          
//          @Override
//          protected Object _run(TeXParserEngine engine ,List<Token> args)
//                  throws Exception{
//              Token a1 = args.get(0);
//              if(a1.getType()!=TokenType.Escape){
//                  boolean out = true;
//                  if(a1.getType() == TokenType.__TOKEN__LIST__){
//                      TokenChain tc = a1.getTokenChain();
//                      tc.trim();
//                      if(tc.size()==1 && tc.get(0).getType()==TokenType.Escape){
//                          a1 = tc.get(0);
//                          out =false;
//                      }
//                  }
//                  if(out){
//                      printError("delcommandの第一引数は" +
//                              "エスケープシーケンスである必要があります。");
//                      return null;
//                  }
//                  
//              }
//              engine.removeCommand(a1.toString());
//              return null;
//          }
//      });
        

        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //----------------------フック系--------------------------
        //............________________________________............
        
        
        def("\\aftergroup","#1", new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t = args.get(0);
                engine.setAfterBlock(t);
                return null;
            }
        });

        
        def("\\everybegingroup","=#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t=args.get(0);
                switch (t.getType()) {
                    case __TOKEN__GROUP__:
                        return new Command(Token.BEGINGROUP.toString(),t.getTokenChain());
                    default:
                        return new Command(Token.BEGINGROUP.toString(),t);
                }
            }
        });
        
        def("\\everypar", "=#1", new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t=args.get(0);
                switch (t.getType()) {
                    case __TOKEN__GROUP__:
                        return new Command(Token.NEWPARAGRAPH.toString(),t.getTokenChain());
                    default:
                        return new Command(Token.NEWPARAGRAPH.toString(),t);
                }
            }
        });
        
        //シンプルリスト記法が欲しいが為に作った
        def("\\everynewline","=#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t=args.get(0);
                switch (t.getType()) {
                    case __TOKEN__GROUP__:
                        return new Command(Token.NEWLINECOMMAND.toString(),t.getTokenChain());
                    default:
                        return new Command(Token.NEWLINECOMMAND.toString(),t);
                }
            }
        });
        
        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //--------------------カウンター系------------------------
        //............________________________________............
        
        //値取得系
        
        def("\\arabic", "#1", new GCV(){
            String convert(int n){
                return NumberToString.arabic(n);
            }
        });
        def("\\roman", "#1", new GCV(){
            String convert(int n){
                return NumberToString.roman(n);
            }
        });
        def("\\Roman", "#1", new GCV(){
            String convert(int n){
                return NumberToString.roman(n).toUpperCase();
            }
        });
        def("\\alph", "#1", new GCV(){
            String convert(int n){
                return NumberToString.alphabet(n);
            }
        });
        def("\\Alph", "#1", new GCV(){
            String convert(int n){
                return NumberToString.alphabet(n).toUpperCase();
            }
        });
        def("\\kanji", "#1", new GCV(){
            String convert(int n){
                return NumberToString.kanji(n, false);
            }
        });
        def("\\Kanji", "#1", new GCV(){
            String convert(int n){
                return NumberToString.kanji(n,true);
            }
        });
        
        def("\\daiji", "#1", new GCV(){
            String convert(int n){
                return NumberToString.daiji(n);
            }
        });
        
        def("\\uroman", "#1", new GCV(){
            String convert(int n){
                return NumberToString.unitroman(n);
            }
        });
        def("\\uRoman", "#1", new GCV(){
            String convert(int n){
                return NumberToString.unitRoman(n);
            }
        });
        
        //定義 実際のTeXは0～255のレジスタで定義しているみたいだが………
        def("newcount", "#1", new MacroProcess(){
            public Object _run(LamuriyanEngine doc ,List<Token> args) throws Exception{
                Token t = args.get(0);
                switch(t.getType()){
                    case ESCAPE:
                        break;
                    default:
                        printError("コマンドシーケンスが見つかりませんでした。"+t);
                        return null;
                }
                return new Command(t.toString(), 0);
            }
        });
        //\newcouanterはtexで定義する
        def("\\java@newcounter","#1[#2]", new MacroProcess(){
            public Object _run(LamuriyanEngine engine ,List<Token> args) throws Exception{
                Token t = args.get(0);
                String name = getCounterName(t, engine);
                if(name == null){
                    printError("名前が見つかりませんでした。"+t);
                    return null;
                }
                String original = name.substring(3);
                Token a2 = args.get(1);
                String supercounter = a2.toString().trim();
                final String thename = "\\the"+original;
                //\arabic{name}
                TokenChain tc = new TokenChain();
                tc.addAll(escape("\\arabic"),new Token(BEGINGROUP,'{'));
                tc.addAll(Token.toCharToken(original));
                tc.add(new Token(ENDGROUP,'}'));
                Macro macro = new Macro(thename, tc, null, null);
                Command tct = createCommand(macro);
                if(supercounter.isEmpty()){
                    engine.defineNewCounter(name);
                }else{
                    engine.defineNewCounter(name, "\\c@"+supercounter);
                }
                return tct;
            }
        });
        
        def("\\stepcounter","#1",new MacroProcess(){
            public Object _run(LamuriyanEngine engine ,List<Token> args) throws Exception{
                Token t = args.get(0);
                String name = getCounterName(t, engine);
                if(name==null){
                    printError("名前が見つかりませんでした。"+t);
                    return null;
                }
                Command c = engine.getCommand(name);
                if(c!=null && c.isCounter()){
                    c.getAsCounter().increase();
                }
                return null;
            }
        });
        
        def("\\setcounter", "#2", new SetProcess(false));
        def("\\addcounter", "#2", new SetProcess(true));
        
        
        
        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //--------------------環境系------------------------
        //............________________________________............
        
        
        def("\\begin","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a1 = args.get(0);
                String name=engine.fullExpand(a1,false).toString();
                if(engine.canCreateNewEnvironment(name)){
                    Token t = engine.createNewEnvironment(name);
                    return t;
                }else{
                    printError(name+"という名前の環境が定義されていません。");
                }
                return null;
            }
        });
        
        def("\\end","#1",  new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a1 = args.get(0);
                String name=engine.fullExpand(a1,false).toString();
                Token t = engine.getCloseEnvironmentToken(name);
                if(t==null){
                    printError(name+"という名前の環境が親にありません。");
                    return null;
                }
                TokenChain tc = new TokenChain();
                tc.add(t);
                tc.add(Token.escape("\\@end"));
                tc.add(a1);
                return tc;
            }
        });
        
        def("\\@end", "#1", new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a1 = args.get(0);
                String name=engine.fullExpand(a1,false).toString();
                if(engine.canCloseEnvironment(name)){
                    engine.closeEnvironment(name);
                }else{
                    printError(name+"という名前の環境を閉じることが出来ません。現在の環境名"+engine.getCurrentEnvironment().getName());
                    engine.canCloseEnvironment(name);
                }
                return null;
            }
        });
       
        //\newenvirontment,\newverbenvironmentの定義はinit.styにて行う。
        def("\\java@nooptnewenv", "#4", new NoOptionNewEnvironment(false));
        def("\\java@nooptnewvenv","#4",new NoOptionNewEnvironment(true));
        
        def("\\java@optnewenv","#1#2[#3]#4#5",new OptionNewEnvironment(false));
        def("\\java@optnewvenv","#1#2[#3]#4#5",new OptionNewEnvironment(true));
        
        def("\\verb", VERBEMODE);
  
        
        ONVERBATIM=def(Token.ONVERBATIM_TOKEN.toString(),"#1", new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t = args.get(0);
                String name=engine.fullExpand(t,false).toString();
                if(name == null || name.isEmpty())return null;
                return name;
            }
        });
        
        def("\\setprop", "#2", new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a1 = args.get(0),a2 = args.get(1);
                String name = engine.fullExpand(a1,false).toString();
                if(name.isEmpty())return null;
                
                String value = engine.fullExpand(a2,false).toString();
                engine.getCurrentEnvironment().setProperty(name, value);
                return null;
            }
        });

        def("\\setgprop", "#2", new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a1 = args.get(0),a2 = args.get(1);
                String name = engine.fullExpand(a1,false).toString();
                if(name.isEmpty())return null;
                
                String value = engine.fullExpand(a2,false).toString();
                engine.getDocument().setProperty(name, value);
                return null;
            }
        });
        
        
        
        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //--------------------ノード操作系------------------------
        //............________________________________............
        
        
        
        
        
        def("\\createBlockNode","#1", new CN(true,false,false));
        def("\\createInlineNode","#1", new CN(true,true,false));
        def("\\createBlockElement","#1", new CN(false,false,true));
        def("\\createInlineElement","#1", new CN(false,true,true));
        def("\\insertBlockElement","#1", new CN(false,false,false));
        def("\\insertInlineElement","#1", new CN(false,true,false));

        def("\\nodevalue","#1", new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a1 = args.get(0);
                FullExpandArea area =engine.createFullExpandArea();
                area.add(a1);
                area.run();
                String value = area.getTokens().toString();
                engine.getCurrentEnvironment().setValueToSettingTargetNode(value);
                return null;
            }
        });
        
        //----挿入ノードの移動系統-------
        
        //ノードの保存系
        def("\\currentmark","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t1 = args.get(0);
                switch(t1.getType()){
                    case ESCAPE:
                        break;
                    case __TOKEN__GROUP__:
                        if(t1.getTokenChain().size()==1)t1 = t1.getTokenChain().get(0);
                        else return null;
                        break;
                    default:
                        return null;
                }
                Environment e = engine.getCurrentEnvironment();
                LmElement ee = e.getCurrentElement();
                return new Command(t1.toString(), ee);
            }
        });
        def("\\parentmark","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t1 = args.get(0);
                switch(t1.getType()){
                    case ESCAPE:
                        break;
                    case __TOKEN__GROUP__:
                        if(t1.getTokenChain().size()==1)t1 = t1.getTokenChain().get(0);
                        else return null;
                        break;
                    default:
                        return null;
                }
                Environment e = engine.getCurrentEnvironment();
                LmElement ee = e.getParentOfCurrentElement();
                return new Command(t1.toString(), ee);
            }
        });
        
        def("\\settingmark","#1", new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t1 = args.get(0);
                switch(t1.getType()){
                    case ESCAPE:
                        break;
                    case __TOKEN__GROUP__:
                        if(t1.getTokenChain().size()==1)t1 = t1.getTokenChain().get(0);
                        else return null;
                        break;
                    default:
                        return null;
                }
                Environment e = engine.getCurrentEnvironment();
                LmNode ee = e.getSettingTargetNode();
                return new Command(t1.toString(), ee);
            }
        });
        //何の為か分からない
//        def("\\settingmarkg","#1", new MacroProcess(){
//            protected Object _run(LamuriyanEngine engine ,List<Token> args)
//                    throws Exception{
//                Token t1 = args.get(0);
//                switch(t1.getType()){
//                    case ESCAPE:
//                        break;
//                    case __TOKEN__GROUP__:
//                        if(t1.getTokenChain().size()==1)t1 = t1.getTokenChain().get(0);
//                        else return null;
//                        break;
//                    default:
//                        return null;
//                }
//                Environment e = engine.getCurrentEnvironment();
//                LmNode ee = e.getSettingTargetNode();
//                return new Command(t1.toString(), ee);
//            }
//        });
        def("\\lastsiblingmark","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t1 = args.get(0);
                switch(t1.getType()){
                    case ESCAPE:
                        break;
                    case __TOKEN__GROUP__:
                        if(t1.getTokenChain().size()==1)t1 = t1.getTokenChain().get(0);
                        else return null;
                        break;
                    default:
                        return null;
                }
                Environment e = engine.getCurrentEnvironment();
                LmNode ee = e.getCurrentElement().getLastChild();
                return new Command(t1.toString(), ee);
            }
        });
        
        
        //設定系
        def("\\hreset","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t1 = args.get(0);
                switch(t1.getType()){
                    case ESCAPE:
                        break;
                    case __TOKEN__GROUP__:
                        if(t1.getTokenChain().size()==1)t1 = t1.getTokenChain().get(0);
                        else return null;
                        break;
                    default:
                        return null;
                }
                Command c = engine.getCommand(t1.toString());
                if(c==null ||!( c.get() instanceof LmElement))return null;
                LmElement te = (LmElement)c.get();
                Environment e = engine.getCurrentEnvironment();
                e.setCurrentElement(te);
                return null;
            }
        });
        
        
        def("\\settingnode","#1", new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t1 = args.get(0);
                switch(t1.getType()){
                    case ESCAPE:
                        break;
                    case __TOKEN__GROUP__:
                        if(t1.getTokenChain().size()==1)t1 = t1.getTokenChain().get(0);
                        else return null;
                        break;
                    default:
                        return null;
                }
                Command c = engine.getCommand(t1.toString());
                if(c==null ||!( c.get() instanceof LmNode))return null;
                LmNode te = (LmNode)c.get();
                Environment e = engine.getCurrentEnvironment();
                e.setSettingTargetNode(te);
                return null;
            }
        });
        //何の為にあるのか分からない
//        def("\\settingnodeg","#1", new MacroProcess(){
//            protected Object _run(LamuriyanEngine engine ,List<Token> args)
//                    throws Exception{
//                Token t1 = args.get(0);
//                switch(t1.getType()){
//                    case ESCAPE:
//                        break;
//                    case __TOKEN__GROUP__:
//                        if(t1.getTokenChain().size()==1)t1 = t1.getTokenChain().get(0);
//                        else return null;
//                        break;
//                    default:
//                        return null;
//                }
//                Command c = engine.getCommand(t1.toString());
//                if(c==null ||!( c.get() instanceof LmNode))return null;
//                LmNode te = (LmNode)c.get();
//                Environment e = engine.getCurrentEnvironment();
//                e.setSettingTargetNode(te);
//                return null;
//            }
//        });
        
        def("\\@setAttr","#3", new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a1 = args.get(1),a2 = args.get(2),target=args.get(0);
                FullExpandArea area = engine.createFullExpandArea();
                FullExpandArea area2 = engine.createFullExpandArea();
                area.add(a1);
                area2.add(a2);
                area.run();
                area2.run();
                
                String name = area.getTokens().toString();
                if(name.isEmpty()){
                    printError("属性の名前が分かりませんでした。");
                    return null;
                }
                String t = target.toString();
                
                
                String value = area2.getTokens().toString();
                LmAttr attr = new LmAttr(name, value);
                Environment e = engine.getCurrentEnvironment();
                switch(t){
                    case "c":
                        e.setAttrToCurrentElement(attr);
                        break;
                    case "s":
                        e.setAttrToLastSibling(attr);
                        break;
                    case "p":
                        e.setAttrToParent(attr);
                        break;
                    case "P":
                        e.setAttrToParentUnSafe(attr);
                        break;
                    case "e":
                        e.setAttr(attr);
                        break;
                    default:
                        e.setAttrToSettingNode(attr);
                        break;                        
                }
                return null;
            }
        });
        
        
        def("\\hclimb", new Function(){
            public Object run(LamuriyanEngine doc) throws Exception{
                doc.getCurrentEnvironment().setCurrent_Parent();
                return null;
            }
        });
        
        def("\\hclimbtop", new Function(){
            public Object run(LamuriyanEngine doc) throws Exception{
                doc.getCurrentEnvironment().setCurrent_Top();
                return null;
            }
        });
        
        def("\\hshiftbefore", new Function(){
            public Object run(LamuriyanEngine doc) throws Exception{
                doc.getCurrentEnvironment().setCurrent_Before();
                return null;
            }
        });
        
        def("\\hfirstsibling",new Function(){
            public Object run(LamuriyanEngine engine) throws Exception{
                engine.getCurrentEnvironment().setCurrent_FirstSibling();
                return null;
            }
        });
        def("\\hlastsibling",new Function(){
            public Object run(LamuriyanEngine engine) throws Exception{
                engine.getCurrentEnvironment().setCurrent_LastSibling();
                return null;
            }
        });
        
        def("\\hshiftafter",new Function(){
            public Object run(LamuriyanEngine engine) throws Exception{
                engine.getCurrentEnvironment().setCurrent_After();
                return null;
            }
        });

        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //--------------------カテゴリコード----------------------
        //............________________________________............
        
        
        
        //設定ファイルを呼ぶ時にこれを使ってるから、どうしても最初に必要
        def("\\makeatletter",new Function(){
            public Object run(LamuriyanEngine doc) throws Exception{
                doc.setCharCategory('@', CharCategory.ALPHABET);
                return null;
            }
        });
        
        def("\\makeatother", new Function(){
            public Object run(LamuriyanEngine doc) throws Exception{
                doc.setCharCategory('@', null);
                return null;
            }
        });
        
        
        def("\\catcode","#1=#2",new MacroProcess(){
            private void error(){
                printError("Charactorが見つかりませんでした。");
            }
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t = args.get(0),t2 = args.get(1);
                if(!t2.isNumber()){
                    printError("カテゴリーコードは数字で指定してください");
                    return null;
                }
                int catcode = Integer.parseInt(t2.toString());
                CharCategory cc;
                if(catcode == -1)
                    cc=null;
                else{
                    cc = CharCategory.getCharCategory(catcode);
                    if(cc==null){
                        printError(catcode+"番のカテゴリーコードは定義されていません");
                        return null;
                    }
                }
                int ch = -1;
                //catcode 123
                //とか
                //catcode 'あ
                //とか
                //catcode '\{ とかからcharを取得
                //`は引数展開時に無視されるのでなくなっている。
                switch(t.getType()){
                    case CHAR:
                        if(t.isNumber())
                            ch = Integer.parseInt(t.toString());
                        break;
                    case ESCAPE:
                        String str=t.toString();
                        if(str.length()!=2){
                            error();
                            return null;
                        }
                        ch = str.charAt(1);
                        break;
                }
                
                if(ch <=0 || ch > Character.MAX_VALUE){
                    error();
                    return null;
                }
                engine.setCharCategory((char)ch, cc);
                return null;
            }
        }).setUseNumber(true);

        
        
        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //--------------------数式モード--------------------------
        //............________________________________............
        
        def("$",new Function(){
            public Object run(LamuriyanEngine engine) throws Exception{
                engine.mathMode(false);
                return null;
            }});
        def("$$",new Function(){
            public Object run(LamuriyanEngine engine) throws Exception{
                engine.mathMode(true);
                return null;
            }});

        def("\\@mathbgroup",new Function(){
            public Object run(LamuriyanEngine engine) throws Exception{
                return new Token(TokenType.BEGINMATHMODEGROUP);
            }
        });
        
        def("\\@mathegroup",new Function(){
           @Override
            public Object run(LamuriyanEngine engine) throws Exception{
               return new Token(TokenType.ENDMATHMODEGROUP);
            } 
        });
        
        
        
        //\mathescape\sum &0x3a3;,Oと書く。
        //第三引数はi,n,o,t,I,N,O,Tで、大文字だと、isUnderOver=trueになる。
        //さらに、iの場合、mathvariant属性をnormalにしたい場合は、a,Aを用いることする。
        def("\\mathescape",tc(arg1,arg2,chart(','),arg3),new MacroProcess(){
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t1 = args.get(0),t2=args.get(1),t3=args.get(2);
                if(t1.getType()!=ESCAPE){
                    printError("\\mathescapeの第一引数はコマンドシーケンスです");
                    return null;
                }
                if(t3.getType()!=CHAR){
                    printError("\\mathescapeの第三引数Char一文字です。");
                    return null;
                }
                boolean isUO=false;
                String type=null;
                String attr = null;
                switch(t3.getChar()){
                    case 'I':
                        isUO=true;
                    case 'i':
                        type = "mi";
                        break;
                    case 'A':
                        isUO = true;
                    case 'a':
                        attr = "normal";
                        type="mi";
                        break;
                    case 'O':
                        isUO=true;
                    case 'o':
                        type ="mo";
                        break;
                    case 'N':
                        isUO=true;
                    case 'n':
                        type="mn";
                        break;
                    case 'T':
                        isUO=true;
                    case 't':
                        type="mtext";
                        break;
                }
                if(type==null){
                    printError("\\mathescapeの第三引数はi,n,o,t,I,N,O,Tのどれかです。");
                    return null;
                }
                String value = engine.fullExpand(t2).toString().trim();
                MathEscape me = new MathEscape(value, type, isUO,attr);
                return new Command(t1.toString(),me);
            }
        });

        def("\\@underoverflag","",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof MathEnvironment) {
                    ((MathEnvironment) e).underoverFlag();
                }else{
                    printError("数式モードではありません。");
                }
                return null;
            }
            
        });
        
        
        def("\\@startchargroup","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t= args.get(0);
                if(t.getType()==__TOKEN__GROUP__ && t.size()==1 && t.get(0).getType()==CHAR){
                    t = t.get(0);
                }else if( t.getType()!=CHAR){
                    printError("引数は　o i n tのどれかの一文字である必要があります");
                    return null;
                }
                char c = t.getChar();
                MathTokenType type;
                switch(c){
                    case 'o':
                        type = MathTokenType.OPERATER;
                        break;
                    case 'i':
                        type = MathTokenType.IDENTIFIER;
                        break;
                    case 'n':
                        type = MathTokenType.NUMBER;
                        break;
                    case 't':
                        type = MathTokenType.TEXT;
                        break;
                    default:
                        printError("引数は　o i n tのどれかの一文字である必要があります");
                        return null;
                }
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof MathEnvironment) {
                    ((MathEnvironment) e).startGroupType(type);
                }else{
                    printError("数式モードではありません。");
                }
                return null;
            }
        });
        def("\\@endchargroup","",new MacroProcess(){
            
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof MathEnvironment) {
                    ((MathEnvironment) e).endGroupType();
                }else{
                    printError("数式モードではありません。");
                }
                return null;
            }
        });
        
        
        
        
        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //--------------------label,ref---------------------------
        //............________________________________............
        
        def("\\@labelable","#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args) throws Exception{
                Token t1 = args.get(0),t2=args.get(1);
                LmNode target=null;
                if(t1.getType()==__TOKEN__GROUP__){
                    if(t1.size()==1 && t1.get(0).getType()==ESCAPE){
                        t1 = t1.get(0);
                    }
                }
                
                
                if(t1.getType() == ESCAPE){
                    Command c = engine.getCommand(t1.toString());
                    if(c!=null && c.get() instanceof LmNode){
                        target = (LmNode)c.get();
                    }else{
                        printError("引数に渡されたコマンドシーケンスはノードを表していません");
                        return null;
                    }
                }
                
                if(target==null){
                    target = engine.getCurrentEnvironment().getSettingTargetNode();
                }
                
                String value = engine.fullExpand(t2).toString();
                Label label = new Label(target, value);
                return new Command("labelable",label);
            }
        });
        
        
        def("\\label","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args) throws Exception{
                Command c = engine.getCommand("labelable");
                String value = engine.fullExpand(args.get(0)).toString();
                if(c==null || !(c.get() instanceof Label)){
                    printError("labelを付加可能な対象が登録されていません。labelname:"+value);
                    return null;
                }
                Label l = (Label)c.get();
                RefTarget ref = engine.createRefTarget(value, l);
                engine.getDocument().addRefTarget(ref);
                
                return null;
            }
        });
        
        def("\\prenewlabel","#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args) throws Exception{
                String lname = engine.fullExpand(args.get(0)).toString();
                String idname = engine.fullExpand(args.get(1)).toString();
                engine.setIdMap(idname, lname);
                return null;
            }
        });
        
        
        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //---------------------画像とか---------------------------
        //............________________________________............
        
        
        def("\\@imagestyle","#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                String str = engine.fullExpand(args.get(0)).toString();
                String filename = engine.fullExpand(args.get(1)).toString();
                String[] spl = str.split(",");
                String width=null,height=null,scale=null;
                StringBuilder sb = new StringBuilder();
                int i=0;
                boolean keepasp=false,usezoom=false;
                for(String s:spl){
                    String[] sp = s.split("=",2);
                    if(sp.length==2){
                        String ss=sp[0].trim();
                        String s1 =sp[1].trim();
                        switch(ss){
                            case "width":
                                width = s1;
                                break;
                            case "height":
                                height=s1;
                                break;
                            case "scale":
                                scale=s1;
                                break;
                            default:
                                if(i!=0){
                                    sb.append(",");
                                }
                                i++;
                                sb.append(s);
                        }
                    }else{
                        switch(s.trim()){
                            case "keepaspectratio":
                                keepasp=true;
                                break;
                            case "usezoom":
                                usezoom=true;
                                break;
                        }
                    }
                }
                if(scale!=null){
                    double d = 1;
                    try{
                        d = Double.parseDouble(scale);
                    }catch (Exception e) {
                        return "";
                    }

                    if(!usezoom)IF:{
                        Path p = engine.searchFile(filename);
                        if(p!=null){
                            Dimension dim=null;
                            try{
                                dim = LamuriyanFileUtilities.getImageSize(p);
                            }catch(IOException e){
                                printError("\\@imagesize:画像の大きさを取得中にエラーが発生しました。message:"
                                        +e.getMessage());
                            }
                            if(dim==null){//どーしょーもないねー
                                break IF;
                            }
                            
                            int w = (int)(dim.getWidth()*d);
                            int h = (int)(dim.getHeight()*d);
                            if(sb.length()!=0)
                                return Token.toCharTokenChain(sb.append(",").append(String.format("style=width:%dpx;height:%dpx;", w,h)).toString());
                            else
                                return Token.toCharTokenChain(String.format("style=width:%dpx;height:%dpx;", w,h));
                        }
                    }
                    TokenChain tc;
                    if(sb.length()!=0)
                        tc=Token.toCharTokenChain(sb.append(",").toString());
                    else
                        tc =tc();
                    tc.addAll(Token.toCharToken("style="));
                    tc.add(escape("\\@zoomstyle"));
                    Token arg1 = new Token(__TOKEN__GROUP__);
                    arg1.setTokenChain(Token.toCharTokenChain(scale));
                    tc.add(arg1);
                    int D = (int)d*100;
                    Token arg2=new Token(__TOKEN__GROUP__);
                    arg2.setTokenChain(Token.toCharTokenChain(D+"%"));
                    tc.add(arg2);
                    return tc;
                }
                
                if(width!=null){
                    if(height!=null){
                        if(keepasp){
                            if(sb.length()!=0)
                                return Token.toCharTokenChain(sb.append(",").append(String.format(
                                        "style=max-width:%s;max-height:%s;", width,height)).toString());
                            else
                                return Token.toCharTokenChain(String.format(
                                        "style=max-width:%s;max-height:%s;", width,height));
                        }else{
                            if(sb.length()!=0)
                                return Token.toCharTokenChain(sb.append(",").append(String.format(
                                        "style=width:%s;height:%s;", width,height)).toString());
                            else
                                return Token.toCharTokenChain(String.format(
                                        "style=width:%s;height:%s;", width,height));
                        }
                    }
                    if(sb.length()!=0)
                        return Token.toCharTokenChain(sb.append(",").append(String.format(
                                "style=width:%s;", width)).toString());
                    else
                        return Token.toCharTokenChain(String.format(
                                "style=width:%s;", width));
                }else if(height!=null){
                    if(sb.length()!=0)
                        return Token.toCharTokenChain(sb.append(",").append(String.format(
                                "style=height:%s;", height)).toString());
                    else
                        return Token.toCharTokenChain(String.format(
                                "style=height:%s;", height));
                }
                if(sb.length()!=0){
                    return Token.toCharTokenChain(sb.toString());
                }
                
                return null;
            }
        });
        
        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //---------------------目次とか---------------------------
        //............________________________________............        
        
        def("\\@indexitem","#5",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t1 =args.get(0),t2=args.get(1),t3=args.get(2),t4=args.get(3),t5=args.get(4);
                if(t1.getType()==__TOKEN__GROUP__&&t1.size()==1){
                    t1 = t1.get(0);
                }
                final LmNode  node;
                if(t1.getType()==ESCAPE){
                    Command c = engine.getCommand(t1.toString());
                    if(c==null || !(c.get()instanceof LmNode)){
                        printError(t1.toString()+"にはNodeが登録されていません。");
                        return null;
                    }
                    node = (LmNode)c.get();
                }else{
                    node = engine.getCurrentEnvironment().getSettingTargetNode();
                }
                
                int depth;
                String DEP = engine.fullExpand(t2).toString();
                try{
                    depth = Integer.parseInt(DEP);
                }catch(Exception e){
                    printError("第二引数「"+t2.toString()+"」が数字ではありません。");
                    return null;
                }
                
                String number = engine.fullExpand(t3).toString();
                final LmNode content;
                if(t1.getType()==ESCAPE){
                    Command c = engine.getCommand(t4.toString());
                    if(c==null || !(c.get()instanceof LmNode)){
                        printError("第四引数"+t4.toString()+"にはNodeが登録されていません。");
                        return null;
                    }
                    content = (LmNode)c.get();
                }else{
                    content = engine.getCurrentEnvironment().getSettingTargetNode();
                }
                String type = engine.fullExpand(t5).toString();
                
                IndexItem item = new IndexItem(depth, number, content, type, node);
                engine.getDocument().addIndexItem(item);
                engine.setID(node, type+":"+content);
                return null;
            }
        });
        
        def("\\createIDfromHashKey","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                LmNode node = engine.getCurrentEnvironment().getSettingTargetNode();
                String key;
                LmAttr attr = node.getAttr("idhash");
                if(attr!=null){
                    key = attr.getValue()+"??( ・∀・)??"+engine.fullExpand(args.get(0)).toString();
                }else
                    key = engine.fullExpand(args.get(0)).toString();
                engine.setID(node, key);
                return null;
            }
        });
        
        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //---------------------IO関連----------------------------
        //............________________________________............
        
        
        def("\\openout","#1",new MacroProcess(){
            Token atwrite = new Token(ESCAPE, "@@openout@@");
            Token eq = new Token('=');
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                TokenChain tc = new TokenChain();
                tc.addAll(atwrite,eq,BGROUP);
                Token t = args.get(0);
                if(!t.isNumber()){
                    printError("\\openout:Streamを指定する数値を見つけられませんでした。");
                    return null;
                }
                tc.addAll(Token.toCharToken(t.toString()));
                tc.add(EGROUP);
                return tc;
            }
        }).setUseNumber(true);
        //\openoutの実際の処理
        def("@@openout@@","#1=#2",new MacroProcess(){
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t1 = args.get(0),t2 =args.get(1);
                int stream;
                try{
                    stream = Integer.parseInt(t1.toString());
                }catch(NumberFormatException e){
                    printError("\\openout:Streamを指定する数値を見つけられませんでした。");
                    return null;
                }
                if(!engine.isOutputStream(stream))return null;
                String filename = engine.fullExpand(t2).toString();
                try{
                    engine.openIO(stream, filename);
                }catch(IOException e){
                    printError("\\openout:ファイルオープン時にエラーが発生しました。message:"+e.getMessage());
                }
                return null;
            }
        });
        
        def("\\openin","#1",new MacroProcess(){
            Token atwrite = new Token(ESCAPE, "@@openin@@");
            Token eq = new Token('=');
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                TokenChain tc = new TokenChain();
                tc.addAll(atwrite,eq,BGROUP);
                Token t = args.get(0);
                if(!t.isNumber()){
                    printError("\\openin:Streamを指定する数値を見つけられませんでした。");
                    return null;
                }
                tc.addAll(Token.toCharToken(t.toString()));
                tc.add(EGROUP);
                return tc;
            }
        }).setUseNumber(true);
        //\openoutの実際の処理
        def("@@openin@@","#1=#2",new MacroProcess(){
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t1 = args.get(0),t2 =args.get(1);
                int stream;
                try{
                    stream = Integer.parseInt(t1.toString());
                }catch(NumberFormatException e){
                    printError("\\openin:Streamを指定する数値を見つけられませんでした。");
                    return null;
                }
                if(!engine.isInputStream(stream))return null;
                String filename = engine.fullExpand(t2).toString();
                try{
                    engine.openIO(stream, filename);
                }catch(IOException e){
                    printError("\\openin:ファイルオープン時にエラーが発生しました。message:"+e.getMessage());
                }
                return null;
            }
        });
        
        def("\\closeout","#1",new MacroProcess(){
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t = args.get(0);
                if(!t.isNumber()){
                    printError("\\closeout:Streamを指定する数値を見つけられませんでした。");
                    return null;
                }
                int i=Integer.parseInt(t.toString());
                if(engine.isOutputStream(i))
                    engine.closeIO(i);
                return null;
            }
        }).setUseNumber(true);
        def("\\closein","#1",new MacroProcess(){
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t = args.get(0);
                if(!t.isNumber()){
                    printError("\\closein:Streamを指定する数値を見つけられませんでした。");
                    return null;
                }
                int i=Integer.parseInt(t.toString());
                if(engine.isInputStream(i))
                    engine.closeIO(i);
                return null;
            }
        }).setUseNumber(true);
        
        
        
        def("\\write","#1",new MacroProcess(){
            Token atwrite = new Token(ESCAPE, "@@write@@");
            Token eq = new Token('=');
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                TokenChain tc = new TokenChain();
                tc.addAll(atwrite,eq,BGROUP);
                Token t = args.get(0);
                if(!t.isNumber()){
                    printError("\\write:Streamを指定する数値を見つけられませんでした。");
                    return null;
                }
                tc.addAll(Token.toCharToken(t.toString()));
                tc.add(EGROUP);
                return tc;
            }
        }).setUseNumber(true);
        //\writeの実際の処理
        def("@@write@@","#2",new MacroProcess(){
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token t1 = args.get(0),t2 =args.get(1);
                int stream;
                try{
                    stream = Integer.parseInt(t1.toString());
                }catch(NumberFormatException e){
                    printError("\\openout:Streamを指定する数値を見つけられませんでした。");
                    return null;
                }
                if(!engine.isOutputStream(stream))return null;
                String value = engine.fullExpand(t2).toString();
                try{
                    engine.write(stream, value);
                }catch(IOException e){
                    printError("\\write:ファイル書き込み時にエラーが発生しました。message:"+e.getMessage());
                }
                return null;
            }
        });
        
        def("\\immediate",  new Function(){
            @Override
            public Object run(LamuriyanEngine engine) throws Exception{
                engine.immediateFlag();
                return null;
            }
        });
        
        //TODO /read #1 to #2の実装
        
        
        //............~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~............
        //---------------------その他-----------------------------
        //............________________________________............
        Function relax;
        def("\\relax",relax=new Function(){
            public Object run(LamuriyanEngine doc) throws Exception{
                return null;
            }
        });
        def("\\protect",relax);
        def(Token.NEWLINECOMMAND.toString(),relax);
        def(Token.NEWPARAGRAPH.toString(),relax);
        def(Token.BEGINGROUP.toString(),relax);
        def("\\enableparagraph",new Function(){
            public Object run(LamuriyanEngine doc) throws Exception{
                Environment e=doc.getCurrentEnvironment();
                if(e instanceof TextEnvironment){
                    ((TextEnvironment) e).setEnableParagraph(true);
                }
                return new Command("\\useparagraph","t");
            }
        });
        
        
        def("\\disableparagraph",new Function(){
            public Object run(LamuriyanEngine doc) throws Exception{
                Environment e=doc.getCurrentEnvironment();
                if(e instanceof TextEnvironment){
                    ((TextEnvironment) e).setEnableParagraph(false);
                }
                return new Command("\\useparagraph","f");
            }
        });
        
        def("\\@newline","[#1]",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a = args.get(0);
                Environment e = engine.getCurrentEnvironment();
                if (e instanceof TextEnvironment) {
                    TextEnvironment te = (TextEnvironment) e;
                    String str = engine.fullExpand(a,true).toString();
                    if(str.isEmpty()){
                        str = null;
                    }else
                        if(!LamuriyanEngine.isSizeUnit(str)){
                            try{
                                Double.parseDouble(str);
                                str += "px";
                            }catch(Exception ex){
                                str = null;
                            }
                        }
                    te.addBR(str);
                }
                return null;
            }
        });
        def("\\newline", "", tc(escape("\\@ifnextchar"),chart('['),bgroup(),
                escape("\\@newline"),egroup(),
                bgroup(),escape("\\@newline"),chart('['),chart(']'),egroup()));
        Macro parmacro=def("\\par", "", new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Environment e = engine.getCurrentEnvironment();
                if(e instanceof TextEnvironment){
                    ((TextEnvironment) e).newParagraphFrag();
                }
                return null;
            }
        });
        def("\n\n",parmacro);
        
        //for文
        def("\\java@for","#3", new JavaFor(false));
        def("\\java@tfor","#3", new JavaFor(true));

        
        def("\\csname",tc(arg1,escape("\\endcsname")), new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a1 = args.get(0);
                FullExpandArea area = engine.createFullExpandArea();
                area.add(a1);area.run();
                String name = area.getTokens().toString().trim();
                if(name.isEmpty()){
                    printError("空文字のコマンドシーケンスは作れません。");
                    return null;
                }
                return escape("\\"+name);
            }
        });
        //マクロデバッグ用
        def("\\error","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a1 = args.get(0);
                printError(a1.toString());
                return null;
            }
        });
        
        
        //+や-による簡易リスト記法が欲しいが為に作った
        //変態フック
        def("\\startlineactivechar","#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a = args.get(0),b=args.get(1);
                if(a.getType()!=TokenType.CHAR){
                    printError("startlineactivecharの第一引数は文字でなくてはいけません");
                    return null;
                }
                if(b.getType()!=TokenType.ESCAPE){
                    printError("startlineactivecharの第一引数はコマンドシーケンスでなくてはいけません");
                    return null;
                }
                
                engine.setStartlineActiveChar(a.getChar(),b.toString());
                return null;
            }
        });
        def("\\undefstartlineactivechar","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a = args.get(0);
                if(a.getType()!=TokenType.CHAR){
                    printError("startlineactivecharの第一引数は文字でなくてはいけません");
                    return null;
                }
                
                engine.setStartlineActiveChar(a.getChar(),null);
                return null;
            }
        });
        //現在読み込んでいるファイルの行番号を得る。
        def("\\fileline", "",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                return Integer.toString(engine.getFileLine());
            }
        });
        
        
        def("\\printAUX","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                FullExpandArea area = engine.createFullExpandArea();
                area.setUseNoExpand();
                area.add(args.get(0));
                area.run();
                String v = area.toString();
                engine.printAux(v);
                return null;
            }
        });
        
        def("\\printlnAUX","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                FullExpandArea area = engine.createFullExpandArea();
                area.setUseNoExpand();
                area.add(args.get(0));
                area.run();
                String v = area.toString();
                engine.printlnAux(v);
                return null;
            }
        });
        
        def("\\varb",new Function(){
            public Object run(LamuriyanEngine engine) throws Exception{
                
                engine.setVarb();
                return null;
            }
        });
        
        def("\\input","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args) throws Exception{
                String fname = engine.fullExpand(args.get(0)).toString();
                engine.insertFile(fname);
                return null;
            }
        });
        
        def("\\verbinput","#1",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args) throws Exception{
                String fname = engine.fullExpand(args.get(0)).toString();
                engine.verbInsertFile(fname);
                return null;
            }
        });
        
        
        
        
        def("\\torgb","#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                String type = engine.fullExpand(args.get(0)).toString();
                String value = engine.fullExpand(args.get(1)).toString();
                String color=Utilities.color(value, type);
                if(color==null)color = "normal";
                return Token.toCharTokenChain(color);
            }
        });
        
        
        //確実に二つに分割する為 分割結果は\@divideresultone \@divideresulttwoに格納される。
        def("\\@divide","#2",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                String div = engine.fullExpand(args.get(0)).toString();
                String str = engine.fullExpand(args.get(1)).toString();
                String[] sp = str.split(div,2);
                Command a = new Command("\\@divideresultone", sp[0]);
                Command b = new Command("\\@divideresulttwo",sp.length==2?sp[1]:"");
                return new Command[]{a,b};
            }
        });
        
        
        def("\\debug","",new MacroProcess(){
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                return null;
            }
        });
        
    }
    
    
    
    
    
    
    //**--------------------------------------------------------------------------------------**//
    //------------------------------------------------------------------------------------------//
    //------------------------------------------------------------------------------------------//
    //-----------------------------static classとか---------------------------------------------//
    //------------------------------------------------------------------------------------------//
    //------------------------------------------------------------------------------------------//
    //**--------------------------------------------------------------------------------------**//
    
    private static String getCounterName(Token t,LamuriyanEngine engine) throws Exception{
        TokenChain tc = engine.fullExpand(t,false);
        String name = tc.toString();
        if(name.isEmpty())return null;
        name = "\\c@"+name;
        return name;
    }
    
    //GetCounterValue
    private static abstract class GCV extends MacroProcess{
        protected Object _run(LamuriyanEngine engine ,List<Token> args) throws Exception{
            Token t = args.get(0);
            String name = getCounterName(t, engine);
            if(name == null){
                printError("名前が見つかりませんでした。"+t);
                return 0;
            }
            Command ct = engine.getCommand(name);
            if(ct==null || !ct.isCounter()){
                printError("カウンターが見つかりませんでした。"+t);
                return 0;
            }
            int n = ct.getAsCounter().get();
            String value = convert(n);
            return Token.toCharTokenChain(value);
        }
        abstract String convert(int n);
    };

    class SetProcess extends MacroProcess{
        boolean add;
        public SetProcess(boolean add){
            this.add = add;
        }
        @Override
        public Object _run(LamuriyanEngine engine ,List<Token> args) throws Exception{
            Token t = args.get(0);
            String name = getCounterName(t, engine);
            if(name == null){
                printError("名前が見つかりませんでした。"+t);
                return null;
            }
            Token t2 = args.get(1);
            String value=engine.fullExpand(t2,true).toString();
            int v ;
            try{
                v = Integer.parseInt(value);
            }catch(NumberFormatException e){
                printError("数値のパースエラーが起こりました。"+t2);
                return null;
            }
            Command ct=engine.getCommand(name);
            if(ct!=null){
                Counter cc = ct.getAsCounter();
                if(add){
                    cc.set(cc.get()+v);
                }
                else
                    cc.set(v);
            }
            return null;
        }
    };

    static private class OptionNewCommand extends MacroProcess{
        private boolean notreplace = true;
        public OptionNewCommand(boolean notreplace){
            this.notreplace = notreplace;
        }
        @Override
        protected Object _run(LamuriyanEngine engine ,List<Token> args)
                throws Exception{
            Token a0 = args.get(0),a1 = args.get(1),a2 = args.get(2),a3 = args.get(3);
            Token name = getCommandName(a0);
            if(name==null){
                printError("新しいコマンド名が正しくありません。"+a0);
                return null;
            }
            Command c = engine.getCommand(name.toString());
            if(notreplace){
                if(c!=null){
                    printError("すでにコマンド"+name+"は定義されています。");
                    return null;
                }
            }else{
                if(c==null){
                    printError("コマンド"+name+"が元々定義されていません。");
                    return null;
                }
            }
            int m;
            try{
                m = Integer.parseInt(engine.fullExpand(a1,true).toString());
            }catch (Exception e) {
                printError(name+"の定義でエラー。数値ではありません"+a1);
                return null;
            }
            if(m < 0){
                printError(name+"の定義でエラー。引数は0～9個です");
                return null;
            }
            TokenChain tc;
            if(m!=0){
                tc = tc(chart('['),arg1,chart(']'));
                for(int i=1;i<m;i++)tc.add(Token.getArgumentToken(i+1));
            }else{
                tc = new TokenChain();
                for(int i=0;i<m;i++)tc.add(Token.getArgumentToken(i+1));
            }
            TokenChain program;
            if(a3.getType()!=TokenType.__TOKEN__GROUP__){
                program = tc(a3);
            }else program = a3.getTokenChain();
            if(m!=0){
                final Token name2 =new Token(ESCAPE,"expandcommand@"+name.toString().substring(1));
                Macro macro = new Macro(name2, program, tc, null);
                Command[] com =new Command[2];
                com[0]=createCommand(macro);
                final Token op = a2;
                MacroProcess process = new MacroProcess(){
                    protected Object _run(LamuriyanEngine engine ,List<Token> args)
                            throws Exception{
                        return tc(escape("\\@calloptionmacro"),name2,op);
                    }
                };
                macro = new Macro(name, null, null, process);
                com[1]=createCommand(macro);
                return com;
            }else{
                Macro macro = new Macro(name,program,tc,null);
                return createCommand(macro);
            }
        }
    }
    
    static private class NoOptionNewCommand extends MacroProcess{
        private boolean notreplace = true;
        
        public NoOptionNewCommand(boolean notreplace){
            this.notreplace = notreplace;
        }
        protected Object _run(LamuriyanEngine engine ,List<Token> args)
                throws Exception{
            Token a0 = args.get(0),a1 = args.get(1),a2 = args.get(2);
            
            Token name = getCommandName(a0);
            if(name==null){
                printError("新しいコマンド名が正しくありません。"+a0);
                return null;
            }
            Command c = engine.getCommand(name.toString());
            if(notreplace){
                if(c!=null){
                    printError("すでにコマンド"+name+"は定義されています。");
                    return null;
                }
            }else{
                if(c==null){
                    printError("コマンド"+name+"が元々定義されていません。");
                    return null;
                }
            }

            int m;
            try{
                m = Integer.parseInt(engine.fullExpand(a1,true).toString());
            }catch (Exception e) {
                printError(name+"の定義でエラー。数値ではありません"+a1);
                return null;
            }
            if(m < 0){
                printError(name+"の定義でエラー。引数は0～9個です");
                return null;
            }
            String arg = "#"+m;
            TokenChain tc =new TokenChain();
            for(int i=0;i<m;i++)tc.add(Token.getArgumentToken(i+1));
            if(a2.getType()!=TokenType.__TOKEN__GROUP__){
                tc.add(a2);
            }else tc = a2.getTokenChain();
            Macro macro = new Macro(name, tc,Define_Macro.args.get(arg) , null);
            return createCommand(macro);
        }
    }


    static private class NoOptionNewEnvironment extends MacroProcess{
        boolean verb;
        public NoOptionNewEnvironment(boolean verbatim){
            verb = verbatim;
        }
        
        @Override
        protected Object _run(LamuriyanEngine engine ,List<Token> args)
                throws Exception{
            Token a0=args.get(0),a1=args.get(1),a2 = args.get(2),a3=args.get(3);
            String name = engine.fullExpand(a0,false).toString();
            if(name.isEmpty()){
                printError("新しい環境名が正しくありません。");
                return null;
            }
            String n = engine.fullExpand(a1,true).toString();
            int m=0;
            try{
                m = Integer.parseInt(n);
            }catch(Exception e){
            }
            if(m>9 || m <0){
                printError("引数は最大で9の正数です");
                return null;
            }
            TokenChain beginProgram;
            if(a2.getType()!=TokenType.__TOKEN__GROUP__){
                beginProgram = new TokenChain();
                beginProgram.add(a2);
            }else beginProgram = a2.getTokenChain();
            
            if(verb){
                beginProgram.addAll(Token.relax9());//何が何でもONVERBATIME_TOKENを実行させたい
                beginProgram.addAll(Token.ONVERBATIM_TOKEN,new Token(BEGINGROUP,'{'));
                beginProgram.addAll(Token.toCharToken(name));
                beginProgram.add(new Token(ENDGROUP,'}'));
            }
            
            TokenChain endProgram;
            if(a3.getType()!=TokenType.__TOKEN__GROUP__){
                endProgram = new TokenChain();
                endProgram.add(a3);
            }else endProgram = a3.getTokenChain();
            
            engine.defineNewEnvironment(name, m, beginProgram, endProgram,null);
            
            return null;
        }
    }
    
    static private class OptionNewEnvironment extends MacroProcess{
        boolean verb;
        public OptionNewEnvironment(boolean verbatim){
            verb = verbatim;
        }
        @Override
        protected Object _run(LamuriyanEngine engine ,List<Token> args)
                throws Exception{
            Token a0=args.get(0),a1=args.get(1),a2 = args.get(2),a3 = args.get(3),a4=args.get(4);
            String name = engine.fullExpand(a0,false).toString();
            if(name.isEmpty()){
                printError("新しい環境名が正しくありません。");
                return null;
            }
            TokenChain option = new TokenChain();
            option.addAll(engine.fullExpand(a2,false));
            String n = engine.fullExpand(a1,true).toString();
            int m=0;
            try{
                m = Integer.parseInt(n);
            }catch(Exception e){
            }
            if(m>9 || m <0){
                printError("引数は最大で9の正数です");
                return null;
            }
            TokenChain beginProgram;
            if(a3.getType()!=TokenType.__TOKEN__GROUP__){
                beginProgram = new TokenChain();
                beginProgram.add(a3);
            }else beginProgram = a3.getTokenChain();
            
            if(verb){
                beginProgram.addAll(Token.relax9());//何が何でもONVERBATIME_TOKENを実行させたい
                beginProgram.addAll(Token.ONVERBATIM_TOKEN,new Token(BEGINGROUP,'{'));
                beginProgram.addAll(Token.toCharToken(name));
                beginProgram.add(new Token(ENDGROUP,'}'));
            }
            
            TokenChain endProgram;
            if(a4.getType()!=TokenType.__TOKEN__GROUP__){
                endProgram = new TokenChain();
                endProgram.add(a4);
            }else endProgram = a4.getTokenChain();
            
            engine.defineNewEnvironment(name, m, beginProgram, endProgram,option);
            
            return null;
        }
    }



    //CreateNode
    private static class CN extends MacroProcess{ 
        boolean n,i,m;
        public CN(boolean isnode,boolean isinline,boolean move){
            n = isnode;
            i = isinline;
            m = move;
        }
        
        @Override
        protected Object _run(LamuriyanEngine engine ,List<Token> args)
                throws Exception{
            Token a1 = args.get(0);
            String name = engine.fullExpand(a1,false).toString();
            LmNode node;
            if(n){
                node = new LmNode(name);
            }else{
                node = new LmElement(name);
            }
            node.setInline(i);
            engine.getCurrentEnvironment()
            .add(node, m);
            return null;
        }
    }
    

    //aftergroupとかで実装するのが正しいのかねぇ？
    private static class JavaFor extends MacroProcess{
        boolean splitToken;
        public JavaFor(boolean b){
            splitToken = b;
        }
        @Override
        protected Object _run(LamuriyanEngine engine ,List<Token> args)
                throws Exception{
            Token a1 = args.get(0),a2=args.get(1),a3=args.get(2);
            if(a1.getType()!=TokenType.ESCAPE){
                boolean out = true;
                if(a1.getType() == TokenType.__TOKEN__GROUP__){
                    TokenChain tc = a1.getTokenChain();
                    tc.trim();
                    if(tc.size()==1 && tc.get(0).getType()==TokenType.ESCAPE){
                        a1 = tc.get(0);
                        out =false;
                    }
                }
                if(out){
                    printError("\\@forまたは@tforの第一引数は内容が書き換えられても良い" +
                            "エスケープシーケンスである必要があります。");
                    return null;
                }
                
            }
            
            
            List<TokenChain> tclist;
            if(splitToken){
                TokenChain inputs;
                if(a2.getType()!=TokenType.__TOKEN__GROUP__){
                    inputs = new TokenChain();
                    inputs.add(a2);
                }else{
                    inputs = a2.getTokenChain();
                }
                tclist = new ArrayList<>();
                int groupdepth = 0;
                TokenChain group=null;
                for(Token t:inputs.getTokens()){
                    switch (t.getType()) {
                        case BEGINGROUP:
                            if(groupdepth==0){
                                group = new TokenChain();
                                tclist.add(group);
                            }else{
                                group.add(t);
                            }
                            groupdepth++;
                            break;
                        case ENDGROUP:
                            if(groupdepth==1){
                                group = null;
                            }else group.add(t);
                            if(groupdepth!=0){
                                groupdepth--;
                            }
                            break;
                        default:
                            if(groupdepth!=0){
                                group.add(t);
                            }else{
                                TokenChain tc = new TokenChain(1);
                                tc.add(t);
                                tclist.add(tc);
                            }
                            break;
                    }
                }
            }else{
                TokenChain inputs;
                ArrayList<TokenChain> list= new ArrayList<>();
                tclist = list;
                TokenChain tc=new TokenChain();
                list.add(tc);
                inputs = engine.onetimeExpand(a2);
                
                for(Token t:inputs){
                    if(t.getType()==TokenType.CHAR && t.getChar() == ','){
                        tc=new TokenChain();
                        list.add(tc);
                    }else{
                        tc.add(t);
                    }
                }
                if(tc.size()==0){
                    list.remove(tc);
                }
            }
            if(tclist.size()==0)return null;
            
            int i=0;
            String name = "java@forimpfunction";
            while(engine.getCommand(name+i)!=null){
                i++;
            }
            name = name+i;
            Token defname = new Token(ESCAPE,name);
            MoreFor mfor = new MoreFor();
            mfor.defname=defname;
            mfor.targetName=a1;
            mfor.obj = tclist;
            mfor.program = a3;
            engine.defineCommand( new Command(name,mfor));
            return defname;
        }
    }
    
    private static class JavaForeach extends MacroProcess{
        @Override
        protected Object _run(LamuriyanEngine engine ,List<Token> args)
                throws Exception{
            Token a1 = args.get(0),a2=args.get(1),a3=args.get(2);
            if(a1.getType()!=TokenType.ESCAPE){
                boolean out = true;
                if(a1.getType() == TokenType.__TOKEN__GROUP__){
                    TokenChain tc = a1.getTokenChain();
                    tc.trim();
                    if(tc.size()==1 && tc.get(0).getType()==TokenType.ESCAPE){
                        a1 = tc.get(0);
                        out =false;
                    }
                }
                if(out){
                    printError("\\@foreachの第一引数は内容が書き換えられても良い" +
                            "エスケープシーケンスである必要があります。");
                    return null;
                }
                
            }
            
            String csname = getCSName(a2);
            Command c = engine.getCommand(csname);
            if(c==null || !c.isList()){
                printError("\\@foreachにはリストを用います。"+a2);
                return null;
            }
            List<TokenChain> tclist = c.getAsList().list;
            if(tclist.size()==0)return null;
            
            int i=0;
            String name = "java@foreachimpfunction";
            while(engine.getCommand(name+i)!=null){
                i++;
            }
            name = name+i;
            Token defname = new Token(ESCAPE,name);
            MoreFor mfor = new MoreFor();
            mfor.defname=defname;
            mfor.targetName=a1;
            mfor.obj = tclist;
            mfor.program = a3;
            engine.defineCommand( new Command(name,mfor));
            return defname;
        }
    }
    
    private static class MoreFor implements Function{
        List<TokenChain> obj;
        int i=0;
        Token defname;
        Token targetName;
        Token program;
        
        public Object run(LamuriyanEngine doc) throws Exception{
            TokenChain tc=obj.get(i++);
            //メモリ登録
            doc.defineCommand(new Command(targetName.toString(),tc));
            tc = new TokenChain();
            tc.add(program);
            if(i==obj.size()){
                //一応削除
                doc.removeCommand(defname.toString());
            }else{
                //ループする為に再度追加
                tc.add(defname);
            }
//            tc.addAll(Token.escape(deletecommandMacro.getName()),targetName);
            return tc;
        }
    }

    private static final IFMacro iftrue = new IFMacro("iftrue", null, null, new MacroProcess(){
        protected Object _run(LamuriyanEngine engine ,List<Token> args){
            return true;
        }
    });
    
    private static IFMacro iffalse = new IFMacro("iffalse", null, null, new MacroProcess(){
        protected Object _run(LamuriyanEngine engine ,List<Token> args){
            return false;
        }
    });

    
    
    private static class ChangeIFValue implements Function{
        String ifname;
        IFMacro ifmac;
        public ChangeIFValue(String ifname,boolean b){
            this.ifname = ifname;
            ifmac = b?iftrue:iffalse;
        }
        @Override
        public Object run(LamuriyanEngine doc) throws Exception{
            return new Command(ifname,ifmac);
        }
    }















    private static HashMap<String, TokenChain> args = new HashMap<>();
    private static TokenChain tc(Token... ts){
        TokenChain tc = new TokenChain();
        if(ts!=null && ts.length!=0)
            tc.addAll(ts);
        return tc;
    }
    private static Token escape(String str){
        return new Token(TokenType.ESCAPE,str);
    }
    private static Token chart(char c){
        return new Token(c);
    }
    
    private static Token BGROUP = new Token(BEGINGROUP,'{');
    private static Token bgroup(){
        return BGROUP;
    }
    private static Token EGROUP = new Token(ENDGROUP,'}');
    private static Token egroup(){
        return EGROUP;
    }
    private static Token arg1=Token.Args1,arg2=Token.Args2,arg3=Token.Args3,
            arg4 = Token.Args4,arg5=Token.Args5,arg6=Token.Args6,arg7=Token.Args7,
            arg8=Token.Args8,arg9=Token.Args9;
    private static void put(String str,Token... ts){
        args.put(str, tc(ts));
    }
    private static TokenChain argsget(String str){
        if(args.containsKey(str))return args.get(str);
        throw new RuntimeException(str+"引数が未定義");
    }
    static{
        put("");
        put("#1",arg1);
        put("#2",arg1,arg2);
        put("#3",arg1,arg2,arg3);
        put("#4",arg1,arg2,arg3,arg4);
        put("#5",arg1,arg2,arg3,arg4,arg5);
        put("#6",arg1,arg2,arg3,arg4,arg5,arg6);
        put("#7",arg1,arg2,arg3,arg4,arg5,arg6,arg7);
        put("#8",arg1,arg2,arg3,arg4,arg5,arg6,arg7,arg8);
        put("#9",arg1,arg2,arg3,arg4,arg5,arg6,arg7,arg8,arg9);
        put("[#1]",chart('['),arg1,chart(']'));
        put("[#1]#2",chart('['),arg1,chart(']'),arg2);
        put("#1[#2]",arg1,chart('['),arg2,chart(']'));
        put("[#1][#2]",chart('['),arg1,chart(']'),chart('['),arg2,chart(']'));
        put("#1=#2",arg1,chart('='),arg2);
        put("=#1",chart('='),arg1);
        put("#1#2[#3]#4",arg1,arg2,chart('['),arg3,chart(']'),arg4);
        put("#1#2[#3]#4#5",arg1,arg2,chart('['),arg3,chart(']'),arg4,arg5);
    }
    public Define_Macro(){
        def();
        init();
    };
    

    public final ArrayList<Command> commands=new ArrayList<>();
    private Macro def(String name,TokenChain args,MacroProcess process){
        Macro macro = new Macro(name, null, args, process);
        Command c = new Command(name,macro);
        commands.add(c);
        return macro;
    }
    private Macro def(String name,String argstr,MacroProcess process){
        Macro macro = new Macro(name, null, argsget(argstr), process);
        Command c = new Command(name,macro);
        commands.add(c);
        return macro;
    }
    private Macro def(String name,String argstr,TokenChain program){
        Macro macro = new Macro(name, program, argsget(argstr),  null);
        Command c = new Command(name,macro);
        commands.add(c);
        return macro;
    }

    private void def(String name,Object o){
        Command c = new Command(name,o);
        commands.add(c);
    }
    
    private Macro defif(String name,String argstr,MacroProcess process){
        Macro macro = new IFMacro(name, null, argsget(argstr), process);
        Command c = new Command(name,macro);
        commands.add(c);
        return macro;
    }
    

    private static Command createCommand(Macro m){
        return new Command(m.getName(),m);
    }
    
    
   

    public static final Object
    FI = new IFCommandObject(),ELSE = new IFCommandObject()
    ,VERBEMODE=new Object(),DEFGLOBAL = new Object();
    
    
    public static class TDef{
        public final String name;
        public TDef(String name){
            this.name = name;
        }
    }

    //最も重要な\defの定義
    private void def(){
        AutomatonFactory<TokenPair> automaton =new AutomatonFactory<>(4);

        automaton.setMoveFunction(0, new UndefinedMove());

        automaton.setMoveFunction(1, new MoveFunction<TokenPair>(){
            public int move(TokenPair input ,int currentstate)
                    throws NullPointerException{
                if(input == null)return -1;
                Token t=input.before();
                switch(t.getType()){
                    case ESCAPE:
                        return 2;
                    case __TOKEN__GROUP__:
                        TokenChain sc = t.getTokenChain();
                        Token escape=null;
                        for(Token to:sc){
                            switch(to.getType()){
                                case SPACE:
                                    break;
                                case ESCAPE:
                                    if(escape == null){
                                        escape = to;
                                        break;
                                    }
                                default:
                                    return -1;
                            }
                        }
                        if(escape==null)return -1;
                        return 2;
                    case SPACE:
                    case COMMENT:
                    case NEWLINE:
                        return 1;
                }
                return -1;
            }
        });
        automaton.setMoveFunction(2, new MoveFunction<TokenPair>(){
            @Override
            public int move(TokenPair input ,int currentstate)
                    throws NullPointerException{
                if(input == null)return -1;
                Token t=input.before();
                switch(t.getType()){
                    case __TOKEN__GROUP__:
                        return 3;
                }
                return 2;
            }
        });
        MacroProcess process = new MacroProcess(){
            @Override
            public Object _run(LamuriyanEngine doc ,List<Token> args){
                Token a1 = args.get(0);
                Token a2 = args.get(1);
                TokenChain b2 = a2.getTokenChain();
                Token block = b2.get(b2.size()-1);
                TokenChain tc = null;
                if(b2.size()!=1){
                    tc = b2.subChain(0, b2.size()-1);
                }
                Macro macro = new Macro(a1, block.getTokenChain(), tc, null);
                return new Command(macro.getName(),macro);
            }
        };
        Macro def = new Macro("\\def", automaton, null,process, new int[]{1,2},new int[]{0,Macro.defParam});
        commands.add(new Command(def.getName(),def));
        
        
        def("\\tdef", "#1", new MacroProcess(){
            
            @Override
            protected Object _run(LamuriyanEngine engine ,List<Token> args)
                    throws Exception{
                Token a1 = args.get(0);
                if(a1.getType()!=TokenType.ESCAPE){
                    printError("\\tdefの引数はエスケープシーケンスでなくてはなりません。");
                    return null;
                }
                TDef t = new TDef(a1.toString());
                return t;
            }
        });
        
        
    }
    
    public static class IFMacro extends Macro implements IFCommand{

        public IFMacro(Token name, TokenChain block, TokenChain argspattern,
                MacroProcess func){
            super(name, block, argspattern, func);
        }

        public IFMacro(String name, TokenChain block, TokenChain argspattern,
                MacroProcess func){
            super(name, block, argspattern, func);
        }

        public IFMacro(String name, AutomatonFactory<TokenPair> automaton,
                TokenChain blockvalue, MacroProcess process, int[] argstate,
                int[] argtype){
            super(name, automaton, blockvalue, process, argstate, argtype);
        }
    }
    
    private static class IFCommandObject implements IFCommand{
        
    }
    
    private static boolean containcs(Token token){
        if(token.getType()!=TokenType.__TOKEN__GROUP__)return false;
        ArrayDeque<TokenChain> stack = new ArrayDeque<>();
        stack.add(token.getTokenChain());
        while(!stack.isEmpty()){
            TokenChain tc = stack.pop();
            for(Token t:tc){
                switch(t.getType()){
                    case ESCAPE:
                        return true;
                    case __TOKEN__GROUP__:
                        stack.push(t.getTokenChain());
                        break;
                }
            }
        }
        return false;
    }
    
    private static Token getCommandName(Token t){
        switch(t.getType()){
            case ESCAPE:
                return t;
            case __TOKEN__GROUP__:
                t.getTokenChain().trim();
                if(t.getTokenChain().size()==1){
                    return getCommandName(t.getTokenChain().get(0));
                }
        }
        return null;
    }
    

  static class UndefinedMove implements MoveFunction<TokenPair>{
      
      @Override
      public int move(TokenPair input ,int currentstate)
              throws NullPointerException{
          if(input == null)return -1;
          Token t = input.before();
          if(t==null){
              return -1;
          }
          switch(t.getType()){
              case COMMENT:
              case NEWLINE:
                  return currentstate;
          }
          if(t.getType()==TokenType.UNDEFINED)return currentstate+1;
          return -1;
      }
  }


}