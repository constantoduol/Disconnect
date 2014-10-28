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
public interface AbstractCrawlFilter {
    /**
     * 
     * @param file the file to check whether it meets the filter specs
     * @return true if the file qualifies, false if the file does not qualify
     */
    public boolean filter(SmbFile file, Object filterList);
}
