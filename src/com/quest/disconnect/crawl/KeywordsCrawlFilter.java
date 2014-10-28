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
public class KeywordsCrawlFilter implements AbstractCrawlFilter {
    @Override
    public boolean filter(SmbFile file, Object filterList) {
        String [] keyWords=(String[])filterList;
         for(String keyWord : keyWords){
               if(file.getName().indexOf(keyWord)>-1){
                     return true;
               } 
          }
        return false;
    }
    
}
