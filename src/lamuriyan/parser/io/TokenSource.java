package lamuriyan.parser.io;

import java.io.IOException;

import lamuriyan.parser.token.Token;



public interface TokenSource extends Source{
    public Token read() throws IOException;
    public Token preread()throws IOException;
}
