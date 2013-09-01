package lamuriyan.parser.macro;

import lamuriyan.parser.Command;
import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenType;

public class Define_EscapeString{
    public static Command[] createEscapeStringCommands(){
        Command[] com = {
                c("\\iexcl","&iexcl;"),
                c("\\iquest","&iquest;"),
                c("\\@argsone",Token.Args1),
                c("\\@argstwo",Token.Args2),
                c("\\@argsthree",Token.Args3),
                c("\\@argsfour",Token.Args4),
                c("\\@argsfive",Token.Args5),
                c("\\@argssix",Token.Args6),
                c("\\@argsseven",Token.Args7),
                c("\\@argseight",Token.Args8),
                c("\\@argsnine",Token.Args9),
                c("\\,"," "),
                c("\\rdquo","&rdquo;"),
                c("\\ldquo","&ldquo;"),
                c("\\ ", "&nbsp;"),
                c("\\&", "&amp;"),
                c("\\#", "#"),
                c("\\yen", "&yen;"),
                c("\\YEN","\\"),
                c("\\(", new Token(TokenType.ESCAPE,"$")),
                c("\\)", new Token(TokenType.ESCAPE,"$")),
                c("\\[",new Token(TokenType.ESCAPE,"$$")),
                c("\\]",new Token(TokenType.ESCAPE,"$$")),
                c("\\{", "{"),
                c("\\<","&lt;"),
                c("\\>","&gt;"),
                c("\\}", "}"),
                c("\\%", "%"),
                c("\\$", "$"),
                c("\\^", "^"),
                c("\\_", "_"),
                c("\\~", "~"),
                c("\\'","'"),
                c("\\escapesquo","&#039;"),
                c("\\escapedquo","&quot;"),
                c("\\escaperdquo","&rdquo;"),
                c("\\escapeldquo","&ldquo;"),
                c("\\`","`"),
                c("\\textbar", "|"),
                c("\\textless", "&lt;"),
                c("\\textgreater", "&gt;"),

                c("\\dag", "&#8224;"),
                c("\\dagger","&nbsp;&#8224;&bpsp;"),
                c("\\ddag", "&#8225;"),
                c("\\ddagger","&nbsp;&#8225,&bpsp;"),
                c("\\dots", "&#8230;"),

                
                c("&",new Token('&')),
                c("^",new Token('^')),
                c("_",new Token('_')),
                c("\\prime",new Token('\'')),
        };
        return com;
    }
    
    private Define_EscapeString(){};
    private static Command c(String value,Object replace){
        return new Command(value,replace);
    }
}
