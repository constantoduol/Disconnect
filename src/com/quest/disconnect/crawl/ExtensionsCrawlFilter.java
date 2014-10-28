/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.quest.disconnect.crawl;

import jcifs.smb.SmbFile;

/**
 *
 * @author constant
 */
public class ExtensionsCrawlFilter implements AbstractCrawlFilter{
    @Override
    public boolean filter(SmbFile file,Object filterList) {
        String [] extensions=(String[])filterList;
        for(String ext: extensions){
          String exte=getExtension(file);
           if(exte==null){
              return false;      
              }
            else if(exte.equalsIgnoreCase(ext)){
                return true;
            }
        }
       return false;
       
    }
    
    
 private static String getExtension(SmbFile f) {
    String ext = null;
    String s = f.getName();
    int i = s.lastIndexOf('.');

    if (i > 0 &&  i < s.length() - 1) {
        ext = s.substring(i+1).toLowerCase();
    }
    return ext;
}
    
}


//    copyFiles(file,new File(pathTo+"/"+file.getName()),auth);