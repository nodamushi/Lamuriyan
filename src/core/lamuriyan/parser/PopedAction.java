package lamuriyan.parser;

import lamuriyan.parser.token.TokenChain;

public interface PopedAction{
    public void poped(LamuriyanEngine engine,Object option,TokenChain tc);
}