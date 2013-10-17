package lamuriyan.parser;

/**
 * ExpandAreaを操作するオブジェクトを定義する………とか思ってたんだけど、むしろExpandAreaに操作されるインターフェースになってた
 *
 */
public interface ExpandAreaOperator{
    public boolean canUseVarb();
    public void setVarb();
    public void setVarb(char ch);
    public void setVerbatim(String verbEnvironmentName);
    public void endVerbatim();
    public void endVarb();
}