package org.jtaccuino.app.studio.util;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * An Utility class.
 * 
 * @author ikost
 */
public final class Util {

    private Util() {}
    
    /**
     * Extract the file name from a filesystem path or a URL.
     * 
     * @param filePath a filesystem path or a URL e.g. {@code /Users/ikost/Documents/xchart.ipynb}.
     * @return the filename + extension, e.g. ({@code xchart.ipynb})
     */
    public static String getFileNamePartOf(String filePath) {
        int slashIndex = filePath.lastIndexOf('/');
        if (slashIndex == -1) {
            return filePath;
        }
        return filePath.substring(slashIndex + 1);
    }
    
        /** 
     * Check if the URL is valid (protocol, structure)
     * 
     * @param url the remote file's URL
     * @return {@code true} if this is a valid URL
     */ 
    public static boolean isValidUrl(String url) {
        try {
            URI uri = new URI(url);
            // URI must have scheme and host for remote files
            return !(uri.getScheme() == null || uri.getHost() == null);
        } catch (URISyntaxException e) {
            return false;
        }
    }
     
}
