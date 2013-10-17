package lamuriyan.parser.io.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * htmlを読み込んでページのtitleを取得する。
 */
public class PageTitle{
    
    private volatile String title="";
    private final URL url;
    private ExecutorService pool = Executors.newSingleThreadExecutor();
    public PageTitle(String url){
        URL u;
        try {
            u = new URL(url);
            
        } catch (MalformedURLException e) {
            u = null;
        }
        this.url = u;
    }
    
    private volatile boolean loaded = false;
    private static final Pattern
    titlepattern = Pattern.compile("<\\s*(title|TITLE)\\s*>(.*?)<\\s*/\\s*(title|TITLE)\\s*>",
            Pattern.MULTILINE&Pattern.DOTALL&Pattern.CASE_INSENSITIVE),
    headend = Pattern.compile("<\\s*/\\s*head\\s*>",Pattern.CASE_INSENSITIVE);
    private Future<String> imf = new Future<String>(){
        
        @Override
        public boolean isDone(){
            return true;
        }
        
        @Override
        public boolean isCancelled(){
            return true;
        }
        
        @Override
        public String get(long timeout ,TimeUnit unit) throws InterruptedException,
                ExecutionException,TimeoutException{
            return title;
        }
        
        @Override
        public String get() throws InterruptedException,ExecutionException{
            return title;
        }
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning){
            return false;
        }
    };
    
    public synchronized Future<String> load(){
        if(loaded)return imf;
        loaded=true;
        if(url == null){
            pool.shutdown();
            return imf;
        }
        Future<String> f = pool.submit(new Callable<String>(){
            @Override
            public String call() throws Exception{
                String result = "";
                try(BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()))){
                    String line;
                    StringBuilder data = new StringBuilder();
                    while((line=read.readLine())!=null){
                        data.append(line);
                        Matcher m = titlepattern.matcher(data);
                        if(m.find()){
                            String t = m.group(2);
                            result = t.trim();
                            break;
                        }
                        m = headend.matcher(data);
                        if(m.find())break;
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
                title = result;
                pool.shutdown();
                return result;
            }
        });
        
        return f;
    }
    
    public String getTitle(){
        return title;
    }
}
