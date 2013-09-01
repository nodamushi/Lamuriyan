package lamuriyan.parser.io;

import java.io.*;
import java.nio.charset.Charset;


public class StringSource implements Source{
    private String infomation;
    private Reader source;
    private int currentPosition;//改行を含めた現在の位置
    private int line;//行番号
    private int x;//改行からの位置
    private boolean endOfStream=false;
    char[] buffer;//読み込みの一時バッファ
    int bufferSize;//bufferのlength
    int breaded;//bufferに入っているデータ長
    int bpos;//bufferの次に読み込むポインタ
    /**
     * 
     * @param str ソースとなる文字列
     * @param inf ソースの情報
     * @param bufferSize バッファサイズ
     */
    public StringSource(String str,String inf,int bufferSize){
        StringReader read = new StringReader(str);
        source = read;
        this.infomation = inf;
        if(bufferSize<16)bufferSize=16;
        this.bufferSize = bufferSize;
        buffer = new char[bufferSize];
    }
    
    public int getLine(){
        return line;
    }
    public int getX(){
        return x;
    }
    
    public String getInfomation(){
        return infomation;
    }
    
    public int getCaretPosition(){
        return currentPosition;
    }
    
    public String getBufferValue(){
        int st = bpos-10;
        int en = bpos+10;
        if(st<0)st = 0;
        if(en>buffer.length)en=buffer.length;
        char[] c = new char[en-st];
        System.arraycopy(buffer, st, c, 0, c.length);
        String s = new String(c);
        return s;
    }
    
    /**
     * 文字コードは自動で判別します
     * @param in ソースとなる入力
     * @param inf ソースの情報
     * @param bufferSize バッファサイズ
     * @throws IOException
     */
    public StringSource(InputStream in,String inf,int bufferSize) throws IOException{
        this(in,(Charset)null,inf,bufferSize);
    }
    
    
    private static Charset getCharset(String charset){
        return Charset.isSupported(charset)?Charset.forName(charset):null;
    }
    /**
     * 
     * @param in ソースとなる入力
     * @param charset ソースの文字コード
     * @param inf ソースの情報
     * @param bufferSize バッファサイズ
     * @throws IOException
     */
    public StringSource(InputStream in,String charset,String inf,int bufferSize) throws IOException{
        this(in,getCharset(charset),inf,bufferSize);
    }
    
    /**
     * 
     * @param in ソースとなる入力
     * @param charset ソースの文字コード
     * @param inf ソースの情報
     * @param bufferSize バッファサイズ
     * @throws IOException
     */
    public StringSource(InputStream in,Charset charset,String inf,int bufferSize) throws IOException{
        if(charset==null){
            if(!in.markSupported()){
                in = new BufferedInputStream(in, bufferSize);
            }
            charset = LamuriyanFileUtilities.getInputStreamCharset(in,bufferSize);
            if(charset==null)charset = LamuriyanFileUtilities.getDefaultCharset();
        }
        InputStreamReader r = new InputStreamReader(in,charset);
        source = r;
        this.infomation = inf;
        if(bufferSize<16)bufferSize=16;
        this.bufferSize = bufferSize;
        buffer = new char[bufferSize];
    }
    
    /**
     * 
     * @param r ソースとなるリーダー
     * @param inf ソースの情報
     * @param bufferSize バッファサイズ
     */
    public StringSource(Reader r,String inf,int bufferSize){
        source = r;
        this.infomation = inf;
        if(bufferSize<16)bufferSize=16;
        this.bufferSize = bufferSize;
        buffer = new char[bufferSize];
    }
    
    private boolean shoudRead(){
        return breaded <= bpos+4;
    }
    
    
    private void _read() throws IOException{
        if(endOfStream || !shoudRead())return;
        if(bufferSize -breaded > 100){
            int readable = bufferSize-breaded;
            int read = source.read(buffer, breaded, readable);
            if(read==-1){
                endOfStream=true;
                return;
            }
            breaded+=read;
            return;
        }
        int cl;
        if(breaded>8){
            int next =bpos< 4?bpos:4;
            cl = breaded-bpos+next;
            for(int i=0;i<cl;i++)
                buffer[i] = buffer[breaded-cl+i];
            bpos = next;
            breaded = cl;
        }else{ 
            cl=breaded;
            
        }
        int read=source.read(buffer,cl,bufferSize-cl);
        if(read==-1){
            endOfStream = true;
            return;
        }
        breaded += read;
    }
    
    public boolean isEnd(){
        return endOfStream && bpos == breaded;
    }
    
    public char before(){
        if(bpos==0)return 0;
        return buffer[bpos-1];
    }
    public char read() throws IOException{
        _read();
        if(isEnd())return 0;
        char ret = buffer[bpos++];
        if(ret == '\r'){
            ret = '\n';
            buffer[bpos-1]=ret;
            char next = _preread();
            if(next == '\n'){
                bpos++;
            }
        }
        currentPosition++;
        x++;
        if(ret== '\n'){
            line++;
            x=0;
        }
        
        return ret;
    }
    
    private char _preread() throws IOException{
        _read();
        if(isEnd())return 0;
        return buffer[bpos];
    }
    
    /**
     * readと返す値は同じだが、bposの位置を移動しない
     * @return
     * @throws IOException 
     */
    public char preread() throws IOException{
        _read();
        if(isEnd())return 0;
        char ret = buffer[bpos];
        if(ret == '\r'){
            ret = '\n';
        }
        return ret;
    }
    
    public void close(){
        try {
            source.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
