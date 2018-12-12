/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.benchmarkcat;

import io.micrometer.core.instrument.Tag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author S. Koulouzis
 */
public class Main {

    private static final String CAT_BASE_URL = "http://localhost:8083/catalogue_mapper/";
    private static final String D4SCIENEC_CKAN = "https://ckan-d4s.d4science.org/";
    private static final String[] MAPPING_115 = new String[]{"https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/Mapping115.x3ml", "https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/CERIF-generator-policy-v5___21-08-2018124405___12069.xml"};
    private static final int LIMIT = 10;
    private static final String QUEUE_NAME = "metadata_records";
    private static final String RABBIT_API_URL = "http://localhost:15672/api/consumers/%2F";
    private static final String RABBIT_USER = "guest";
    private static final String RABBIT_PASS = "guest";

    public static void main(String[] args) throws InterruptedException {
        try {
            benchmarkConversion(D4SCIENEC_CKAN, MAPPING_115, UUID.randomUUID().toString());
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void benchmarkConversion(String catalogueURL, String[] mapping, String exportID) throws IOException, InterruptedException {
        String mappingName = mapping[0].substring(mapping[0].lastIndexOf("/") + 1, mapping[0].lastIndexOf("."));
        Collection<Tag> tags = new ArrayList<>();
        tags.add(Tag.of("source", catalogueURL));
        tags.add(Tag.of("mapping.name", mappingName));
        tags.add(Tag.of("exportID", exportID));
        int numOfConsumers = Util.getNumberOfConsumers(QUEUE_NAME, RABBIT_API_URL, RABBIT_USER, RABBIT_PASS);
        tags.add(Tag.of("num.of.consumers", String.valueOf(numOfConsumers)));
        tags.add(Tag.of("records.size", String.valueOf(LIMIT)));

        ConvertControllerClient convertClient = new ConvertControllerClient(CAT_BASE_URL);
        String resp = convertClient.convert(catalogueURL, mapping[0], mapping[1], String.valueOf(LIMIT), exportID);
        System.err.println(resp);

        String folderName = mappingName + "/" + exportID;
        JSONArray res = convertClient.listResults(folderName);
        System.err.println(res);
        System.err.println(res.length());
        while (((res.length()-1) / 2) < LIMIT) {
            res = convertClient.listResults(folderName);
            System.err.println(((res.length()-1) / 2));
            Thread.sleep(500);
        }
//        System.err.println(((res.length()-1) / 2));
    }

}
