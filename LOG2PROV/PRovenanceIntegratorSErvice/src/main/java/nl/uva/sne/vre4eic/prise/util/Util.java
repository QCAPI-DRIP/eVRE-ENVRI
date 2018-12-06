/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.prise.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author S. Koulouzis
 */
public class Util {

    public static File convert(MultipartFile file) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir") + File.separator + file.getOriginalFilename());
        convFile.createNewFile();
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }

    public static boolean urlExists(String URLName) {
//        try {
//            HttpURLConnection.setFollowRedirects(true);
//            //        HttpURLConnection.setInstanceFollowRedirects(false)
//            HttpURLConnection con
//                    = (HttpURLConnection) new URL(URLName).openConnection();
//            con.setInstanceFollowRedirects(true);
//            con.setRequestMethod("HEAD");
//            con.setConnectTimeout(6000);
//            int code = con.getResponseCode();
//
//            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
//                Logger.getLogger(Util.class.getName()).log(Level.INFO, "URLName: " + URLName + " exitsts: false");
//                return false;
//            }
//
//        } catch (MalformedURLException ex) {
//            Logger.getLogger(Util.class.getName()).log(Level.INFO, "URLName: " + URLName + " exitsts: false");
//            return false;
//        } catch (IOException ex) {
//            Logger.getLogger(Util.class.getName()).log(Level.INFO, "URLName: " + URLName + " exitsts: false");
//            return false;
//        }
        Logger.getLogger(Util.class.getName()).log(Level.INFO, "URLName: " + URLName + " exitsts: true");
        return true;
    }

    
    
    
    public static void generateVis(){
        
    }
    
}
