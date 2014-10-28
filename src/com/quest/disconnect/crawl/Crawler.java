
package com.quest.disconnect.crawl;

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
/**
 *
 * @author constant
 */
public class Crawler {
    private static volatile HashMap<Object, AbstractCrawlFilter> filters=new HashMap();
    private static volatile ExecutorService threadExecutor;
    private static volatile LinkedBlockingQueue validIPs=new LinkedBlockingQueue();
    private static volatile int validIpCount=0;
    private static volatile int finished=0;
    private static final int EXECUTION_PER_THREAD=25;
    public Crawler(String pathFrom,String pathTo, String userName, String pass,boolean copyOnFind) throws MalformedURLException, SmbException {
            jcifs.Config.setProperty("jcifs.smb.client.disablePlainTextPasswords","false");
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null,userName,pass);
            SmbFile dir = new SmbFile(pathFrom, auth);
            crawl(dir.listFiles(),pathTo,auth,copyOnFind);
       
    }
    
   public static void stopCrawl(){
      if(threadExecutor==null){
        return;  
      }
      threadExecutor.shutdownNow();
      threadExecutor=null;
      validIPs=new LinkedBlockingQueue();
      validIpCount=0;
      finished=0;
   }
    
    public static void startCrawl(String userName,String pass,String ext,String keys,String dir,boolean copyOnFind){
        threadExecutor=null;
        threadExecutor=Executors.newCachedThreadPool();
        HashMap props=new HashMap();
        if(userName.equals("!#none")){
         userName=null;   
        }
        props.put("username",userName);
    
       if(pass.equals("!#none")){
         pass=null;   
        }
       props.put("password",pass);
       
       String[] keyWords=toStringArray(keys); 
       props.put("keywords",keyWords);
       
      
       String [] extensions=toStringArray(ext);
       props.put("extensions",extensions);

      System.out.println("Started with below configuration :");
      System.out.println("username : "+userName);
      System.out.println("password : "+pass);
      System.out.println("keywords : "+Arrays.toString(keyWords));
      System.out.println("extensions : "+Arrays.toString(extensions));
      System.out.println("save directory : "+dir);
      System.out.println("copy on find: "+copyOnFind);
      
      if(extensions!=null && extensions.length!=0){
             filters.put(extensions,new ExtensionsCrawlFilter());
          }
       if(keyWords!=null && keyWords.length!=0){
             filters.put(keyWords,new KeywordsCrawlFilter());    
        }
      for(int y=1;y<=255; y=y+EXECUTION_PER_THREAD){
          if(y+EXECUTION_PER_THREAD>255){
              IPFinder finder1=new IPFinder(y+1, 255);
              threadExecutor.execute(finder1);
              break;
            }
            IPFinder finder=new IPFinder(y+1, y+EXECUTION_PER_THREAD);
            threadExecutor.execute(finder);
        }
    
       while(true){
          try {
             String ip="smb://"+(String)validIPs.take();
             Runner run=new Runner(ip, dir, userName, pass,copyOnFind);
             threadExecutor.execute(run);
             validIpCount--;
             if(threadExecutor.isShutdown()){
               break;
             }
             if(validIpCount==0 && finished==255){
               System.out.println();
               System.out.println("All computers found on the network have been searched");
               break;  
             }
          } 
          catch (Exception ex) {
               
          }
       }  
    }
   

  

  
  private static String[] toStringArray(String string){
    StringTokenizer st=new StringTokenizer(string, ",");
    String [] str=new String[st.countTokens()];
    for(int x=0; st.hasMoreTokens();x++){
      str[x]=st.nextToken();
    }
    String[] arr=new String[str.length];
    System.arraycopy(str, 0, arr, 0, str.length);
    return arr;  
  }
  
  

   


private void crawl(SmbFile[] files,String pathTo,NtlmPasswordAuthentication auth,boolean copyOnFind) {
     if(files!=null){
        for (SmbFile file : files) {
                try {
                    if (file.isDirectory()) {
                         crawl(file.listFiles(),pathTo,auth,copyOnFind); // Calls same method again.
                    } else {
                      // copy file
                      Iterator iter=filters.keySet().iterator();
                      while(iter.hasNext()){
                         Object filterList = iter.next();
                         AbstractCrawlFilter filter = filters.get(filterList);
                         boolean passed = filter.filter(file,filterList);
                         if(passed && iter.hasNext()==false){
                            //no more filters so just copy the file 
                           if(copyOnFind){
                            System.out.println();
                            System.out.println("COPYING "+file.getName());
                            File destFile=new File(pathTo+"/"+file.getName());
                            UniqueRandom rand=new UniqueRandom(8);
                            if(destFile.exists()){
                               destFile=new File(pathTo+"/"+rand.nextRandom()+"_"+file.getName());
                            }
                            copyFile(file,destFile,auth);
                           }
                           else{
                             System.out.println();
                             System.out.println("HIT: "+file.getName()+" ||  LOCATION: "+file.getPath());
                           }
                         }
                         else if(!passed){
                            // skip this file one of the filters failed 
                            System.out.print("-");
                            break; //one filter already failed dont try the others
                         }
                      }
                    }
                } catch (Exception ex) {
                   
        }
    }
  }  
}

private static void copyFile(SmbFile sourceFile, File destFile,NtlmPasswordAuthentication auth)
		throws IOException {
        
	if (!sourceFile.exists()) {
		return;
	}
	if (!destFile.exists()) {
		destFile.createNewFile();
	}
       
        SmbFile sos = new SmbFile(sourceFile.getPath(), auth);
        SmbFile dest = new SmbFile (destFile.getPath(),auth);
        System.out.println();
        System.out.println("Destination "+dest);
        System.out.println("Source "+sos);
        System.out.println();
        
	//SmbFileOutputStream sfos = new SmbFileOutputStream(sos);
        InputStream input = new SmbFileInputStream(sos);
        OutputStream output = new FileOutputStream(destFile);
        WritableByteChannel outputChannel = null;
        try  {
            ReadableByteChannel inputChannel = Channels.newChannel(input);
            outputChannel = Channels.newChannel(output);
            fastChannelCopy(inputChannel, outputChannel);
        }
        catch(Exception e){
          
        }
        finally {
          outputChannel.close();
        }
}


private static void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest) throws IOException {
    final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
    while (src.read(buffer) != -1) {
      // prepare the buffer to be drained
      buffer.flip();
      // write to the channel, may block
      dest.write(buffer);
      // If partial transfer, shift remainder down
      // If buffer is empty, same as doing clear()
      buffer.compact();
    }
    // EOF will leave buffer in fill state
    buffer.flip();
    // make sure the buffer is fully drained.
    while (buffer.hasRemaining()) {
      dest.write(buffer);
    }
  }

private static class IPFinder implements Runnable {
        private int stop;
        private int start;
        public IPFinder(int start,int stop){
           this.stop=stop; 
           this.start=start;
        }
        @Override
        public void run() {
          try {
           InetAddress localhost=InetAddress.getLocalHost();
           byte[] ip=localhost.getAddress();
           for(int i=start; i<=stop; i++){
               
               ip[3]=(byte)i;
               InetAddress address=InetAddress.getByAddress(ip);
               if(address.isReachable(1000)){
                   Crawler.validIPs.put(address.getHostAddress());
                   validIpCount++;
               }
               else if(!address.getHostAddress().equals(address.getHostName())){
                  System.out.println();
                  System.out.println("Found: "+address.getHostName()+"/"+address.getHostAddress());
                  Crawler.validIPs.put(address.getHostAddress());
                  validIpCount++;
               }
               else {
                  System.out.print(".");
               }
               if(i==stop){
                 addFinished(stop-start);
               }
           }
        } catch (Exception ex) {
           
        }
      }
        
        
    private synchronized void addFinished(int add){
       finished=finished+add;
    }
    
}

private static class Runner implements Runnable {
    private String ip;
    private String dir;
    private String userName;
    private String pass;
    private String id;
    private boolean copyOnFind;
    public Runner(String ip,String dir,String userName,String pass,boolean copyOnFind){
       this.ip=ip;
       this.dir=dir;
       this.userName=userName;
       this.pass=pass;
       this.id=new UniqueRandom(8).nextRandom();
       this.copyOnFind=copyOnFind;
    }

        @Override
        public void run() {
            try {
                Crawler cl=new Crawler(ip,dir,userName,pass,copyOnFind);
            } catch (Exception ex) {
               
            }
        }
       
        public String getID(){
            return id;
        }
    }

}

