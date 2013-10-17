package lamuriyan.parser.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import lamuriyan.parser.token.Token;



public class VerbinsertTokenSource implements TokenSource{

    private StringSource source;
    
    
    public VerbinsertTokenSource(Reader r,String infomation){
        source = new StringSource(r, infomation, 1000);
    }
    
    public VerbinsertTokenSource(InputStream in,String infomation) throws IOException{
        source = new StringSource(in, infomation, 1000);
    }
    
    public VerbinsertTokenSource(String str,String infomation){
        source = new StringSource(str, infomation, 1000);
    }
    
    public VerbinsertTokenSource(InputStream in,String charset,String infomation) throws IOException{
        source = new StringSource(in, charset, infomation, 1000);
    }
    
    @Override
    public Token read() throws IOException{
        char c = source.read();
        return new Token(c);
    }

    @Override
    public Token preread() throws IOException{
        char c = source.preread();
        return new Token(c);
    }

    @Override
    public boolean isEnd(){
        return source.isEnd();
    }
    
    @Override
    public void close(){
        source.close();
    }

}
