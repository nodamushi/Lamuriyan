package lamuriyan.parser.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;

import lamuriyan.parser.token.Token;
import lamuriyan.parser.token.TokenChain;



public class SourceStack implements Iterable<Source>{
    private ArrayDeque<Source> sources = new ArrayDeque<>();
    
    
    public boolean isEmpty(){
        return sources.isEmpty();
    }
    
    public int size(){
        return sources.size();
    }
    
    public Source peek(){
        if(sources.size()!=0)return sources.peek();
        return null;
    }
    
    public Source pop(){
        if(sources.size() !=0)return sources.pop();
        return null;
    }
    
    
    public Source push(Token... tokens){
        Source src = new InsertTokenSource(tokens);
        sources.push(src);
        return src;
    }
    
    
    public Source push(TokenChain chain){
        Source src = new InsertTokenSource(chain);
        sources.push(src);
        return src;
    }
    
    public Source push(Collection<Token> tokens){
        Source src = new InsertTokenSource(tokens);
        sources.push(src);
        return src;
    }
    
    public Source push(InputStream input,String info) throws IOException{
        Source src = new StringSource(input, info, 1000);
        sources.push(src);
        return src;
    }
    
    public Source push(InputStream input,String charset,String info) throws IOException{
        Source src = new StringSource(input,charset,info,1000);
        sources.push(src);
        return src;
    }
    public Source push(InputStream input,Charset charset,String info) throws IOException{
        Source src = new StringSource(input,charset,info,1000);
        sources.push(src);
        return src;
    }
    
    public StringSource push(String str,String info){
        StringSource src = new StringSource(str,info,1000);
        sources.push(src);
        return src;
    }
    
    public Source push(Reader reader,String info){
        Source src = new StringSource(reader,info,1000);
        sources.push(src);
        return src;
    }
    
    public Source push(Path file,String info) throws IOException{
        InputStream in = Files.newInputStream(file);
        return push(in,info);
    }
    
    
    public Source push(Path file,String charset,String info) throws IOException{
        InputStream in = Files.newInputStream(file);
        return push(in,charset,info);
    }
    
    public Source push(Path file,Charset charset,String info) throws IOException{
        InputStream in = Files.newInputStream(file);
        return push(in,charset,info);
    }
    
    public Source pushVerb(InputStream in,String info) throws IOException{
        Source src = new VerbinsertTokenSource(in, info);
        sources.push(src);
        return src;
    }
    
    public Source pushVerb(InputStream in,String charset,String info) throws IOException{
        Source src = new VerbinsertTokenSource(in, charset, info);
        sources.push(src);
        return src;
    }
    
    public Source pushVerb(Reader reader,String info){
        Source src = new VerbinsertTokenSource(reader, info);
        sources.push(src);
        return src;
    }
    public Source pushVerb(String source,String info){
        Source src = new VerbinsertTokenSource(source, info);
        sources.push(src);
        return src;
    }
    public Source pushVerb(Path file,String info) throws IOException{
        InputStream in = Files.newInputStream(file);
        return pushVerb(in,info);
    }
    
    public Source pushVerb(Path file,String charcode,String info) throws IOException{
        InputStream in = Files.newInputStream(file);
        return pushVerb(in,charcode,info);
    }

    @Override
    public Iterator<Source> iterator(){
        return sources.iterator();
    }
}
