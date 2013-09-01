package lamuriyan.parser.io;

import static java.util.Objects.*;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lamuriyan.parser.LamuriyanEngine;


/**
 * InputStreamとOutputStreamを一つのクラスで扱う為に作ったクラス。
 * \newread,\newwriteで生成される。<br><br>
 * InputStreamやOutputStreamを指定してオブジェクトを作成した場合、closeを呼び出しても
 * そのStream自体が閉じられることはありません。
 * 
 * @author nodamushi
 *
 */
public class IOContainer{

    private int number;
    private boolean isInputStream;
    private InputStream in;
    private Reader reader;
    private OutputStream out;
    private Writer writer;
    private String path;
    private boolean isOpen;
    private final boolean dontclose;
    
    
    public IOContainer(int number,InputStream in){
        this(number,in,new BufferedReader(new InputStreamReader(in)));
    }
    
    public IOContainer(int number,InputStream in,Reader reader){
        this.number = number;
        this.in = requireNonNull(in,"InputStream is null!");
        this.reader = requireNonNull(reader,"Reader is null!");
        dontclose = true;
        isOpen = true;
        isInputStream=true;
    }
    
    public IOContainer(int number,OutputStream out){
        this(number,out,new BufferedWriter(new OutputStreamWriter(out)));
    }
    
    public IOContainer(int number,OutputStream out,Writer writer){
        this.number = number;
        this.out = requireNonNull(out,"InputStream is null!");
        this.writer = requireNonNull(writer,"Reader is null!");
        dontclose = true;
        isOpen = true;
        isInputStream=false;
    }
    
    public IOContainer(int number,boolean isInputStream){
        this.number = number;
        this.isInputStream=isInputStream;
        this.dontclose = false;
    }
    
    public boolean isInputStream(){
        return isInputStream;
    }
    
    public boolean isOutputStream(){
        return !isInputStream;
    }
    
    public boolean isOpen(){
        return isOpen;
    }
    
    public int getNumber(){
        return number;
    }
    
    public String getPath(){
        return path;
    }
    
    public void open(String path,LamuriyanEngine engine)throws NullPointerException,IOException{
        if(isOpen){
            close();
            if(isOpen)return;
        }
        this.path = requireNonNull(path,"file name is null!");
        if(path.startsWith("http")){//URL
            URL url = new URL(path);
            //TODO プロキシとかの設定を外部から出来るようにした方が良いかな
            URLConnection con = url.openConnection();
            con.connect();
            
            if(isInputStream){
                in = con.getInputStream();
            }else{
                out = con.getOutputStream();
            }
        }else{
            Path p = engine.searchFile(path);
            if(isInputStream){
                in = Files.newInputStream(p);
            }else{
                out = Files.newOutputStream(p);
            }
        }
        
        if(isInputStream){
            reader = new BufferedReader(new InputStreamReader(in));
        }else{
            writer = new BufferedWriter(new OutputStreamWriter(out));
        }
        isOpen=true;
    }
    
    public void close(){
        if(isOpen && dontclose){
            isOpen =false;
            if(isInputStream){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                in = null;reader = null;
            }else{
                try{
                    writer.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                out = null;writer = null;
            }
        }
    }
    
    public void flush() throws IOException{
        if(isOpen&&!isInputStream){
            writer.flush();
        }
    }
    
    public String read() throws IOException{
        if(isOpen && isInputStream){
            //TODO 読み込みメンドクセ
            
            
        }
        return null;
    }
    
    public void writeln(String str) throws IOException{
        if(isOpen && !isInputStream){
            writer.write(str);
            writer.write("\n");
        }
    }
    
    public void write(String str) throws IOException{
        if(isOpen && !isInputStream){
            writer.write(str);
        }
    }
    
    
    
}
