package lamuriyan.parser.io;

import java.awt.Dimension;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.mozilla.universalchardet.UniversalDetector;

public enum LamuriyanFileUtilities{
    ;
    //Sytemのデフォルト値を書き換えたくない為
    //Lamuriyanではデフォルトの文字エンコードこの値を用いる
    private static Charset defaultCharset = Charset.forName("UTF-8");
    /**
     * Lamuriyan処理系が使うデフォルトのエンコードを指定します。<br>
     * Javaのデフォルトのエンコードには関係ありません。
     * @param charset
     */
    public static void setDefaultCharset(String charset){
        if(Charset.isSupported(charset)){
            defaultCharset=Charset.forName(charset);
        }
    }
    /**
     * Lamuriyan処理系が使うデフォルトのエンコードを指定します。<br>
     * Javaのデフォルトのエンコードには関係ありません。
     * @param charset
     */
    public static void setDefaultCharset(Charset set){
        if(set!=null){
            defaultCharset = set;
        }
    }
    
    /**
     * Lamuriyan処理系のデフォルトのエンコードを返します。<br>
     * Javaのデフォルトのエンコードには関係ありません。
     * 初期値はUTF-8です。
     * @return
     */
    public static Charset getDefaultCharset(){
        return defaultCharset;
    }
    
    /**
     * 文章ファイルを全て読み込みます。
     * @param f 読み込むファイル
     * @param charset 文字コード、nullまたは空文字列の場合自動判別します
     * @return ファイルの内容
     * @throws FileNotFoundException ファイルが見つからない
     * @throws IOException 読み込みエラー
     * @throws UnsupportedCharsetException charcodeに渡された文字列が定義されない文字コードだった場合
     */
    public static String read(File f,String charset) throws FileNotFoundException,
        IOException,UnsupportedCharsetException{
        if(charset!=null && Charset.isSupported(charset)){
            Charset c = Charset.forName(charset);
            return _read(f, c);
        }
        Charset cs = getFileCharset(f);
        if(cs==null)cs = defaultCharset;
        return _read(f, cs);
    }
    
    /**
     * ファイルのCharsetを判断します
     * @param f
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static Charset getFileCharset(File f) throws FileNotFoundException, IOException{
        try(FileInputStream fin = new FileInputStream(f);BufferedInputStream bin=new BufferedInputStream(fin)){
            return getInputStreamCharset(bin);
        }
    }
    
    /**
     * InputStreamから得られる文字列のエンコードを判別します。<br>
     * このメソッドは
     * getInputStreamCharset(InputStream in,int readlimit)を10000を最大読み込み容量として呼び出します。
     * @param in markSupportedがtrueであるInputStream
     * @return 判定できなかったときはnull
     * @throws IOException
     */
    public static Charset getInputStreamCharset(InputStream in) throws IOException {
        return getInputStreamCharset(in, 10000);
    }
    /**
     * InputStreamから得られる文字列のエンコードを判別します。<br>
     * 必ずmark,resetをサポートしているInputStreamを利用してください。<br>
     * 利用できない場合はIOExceptionが発生します。<br><br>
     * 判定できなかった時はnullが返りますが、英字のみのファイルなどでも判別不可能であり、
     * かならずしも読み込みに失敗するとは限りません。
     * @param in markSupportedがtrueであるInputStream
     * @param readlimit 判別に読み込んでもよい最大容量
     * @return 判定できなかったときはnull。
     * @throws IOException InputStreamがmark,resetをサポートしていない場合、その他IOExceptionが発生した場合
     */
    public static Charset getInputStreamCharset(InputStream in,int readlimit) throws IOException {
        if(in.markSupported()){
            int read;
            UniversalDetector detector = new UniversalDetector(null);
            in.mark(readlimit);
            final int buffersize=1000;
            byte[] bbuffer = new byte[buffersize];
            int readedsize=0;
            while(
                    readlimit-readedsize>0&&
                    (read=in.read(bbuffer, 0,
                            readlimit-readedsize<buffersize?readlimit-readedsize:buffersize))!=-1 
                    && !detector.isDone())
            {
                detector.handleData(bbuffer, 0, read);
                readedsize+=read;
            }
            detector.dataEnd();
            in.reset();
            String name= detector.getDetectedCharset();
            Charset ret = Charset.isSupported(name)?Charset.forName(name):null;
            return ret;
        }else{
            throw new IOException("InputStreamがmark、resetをサポートしていません。");
        }
    }
    
    //charsetが指定されているとき
    private static String _read(File f,Charset charset) throws IOException{
        try(BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(f),charset))){
            StringBuilder sb = new StringBuilder();
            final int buffersize=1000;
            char[] buffer = new char[buffersize];
            int read;
            while((read=r.read(buffer, 0, buffersize))!=-1){
                sb.append(buffer,0,read);
            }
            return sb.toString();
        }
    }
    
    
    
    /**
     * 拡張子を得ます
     * @param path
     * @return
     */
    public static String getSuffix(String path){
        Path p = Paths.get(path);
        return getSuffix(p);
    }
    /**
     * 拡張子を得ます
     * @param file
     * @return
     */
    public static String getSuffix(File file){
        return getSuffix(file.getPath());
    }
    
    /**
     * 拡張子を得ます
     * @param path
     * @return
     */
    public static String getSuffix(Path path){
        String fname = path.getFileName().toString();
        int point = fname.lastIndexOf('.');
        if(point == -1)return fname;
        return fname.substring(point+1);
    }
    

    /**
     * 画像のサイズを返します
     * @param file
     * @return
     * @throws IOException 
     */
    public static Dimension getImageSize(File file) throws IOException{
        String suf = getSuffix(file);
        Iterator<ImageReader> ir =  ImageIO.getImageReadersBySuffix(suf);
        while(ir.hasNext()){
            ImageReader r = ir.next();
            ImageInputStream stream = ImageIO.createImageInputStream(file);
            r.setInput(stream);
            int w = r.getWidth(r.getMinIndex());
            int h = r.getHeight(r.getMinIndex());
            return new Dimension(w, h);
        }
        return null;
    }
    
    public static Dimension getImageSize(String path) throws IOException{
        return getImageSize(new File(path));
    }
    
    public static Dimension getImageSize(Path path) throws IOException{
        return getImageSize(path.toFile());
    }
    
    
    
    
    
}
