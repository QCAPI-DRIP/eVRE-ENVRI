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

    private static String baseURL = "http://localhost:8080/rest/";
    private static String d4ScienceCKAN = "https://ckan-d4s.d4science.org/";
    private static String[] mapping115 = new String[]{"https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/Mapping115.x3ml", "https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/CERIF-generator-policy-v5___21-08-2018124405___12069.xml"};
    private static int limit = 10;
    private static String queueName = "metadata_records";
    private static String rabbitAPIURL = "http://localhost:15672/api/consumers/%2F";
    private static String rabbitUser = "guest";
    private static String rabbitPass = "guest";

    public static void main(String[] args) {
        try {
            benchmarkConversion(d4ScienceCKAN, mapping115, UUID.randomUUID().toString());
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void benchmarkConversion(String catalogueURL, String[] mapping, String exportID) throws IOException {
        String mappingName = mapping[0].substring(mapping[0].lastIndexOf("/") + 1, mapping[0].lastIndexOf("."));
        Collection<Tag> tags = new ArrayList<>();
        tags.add(Tag.of("source", catalogueURL));

        tags.add(Tag.of("mapping.name", mappingName));

        tags.add(Tag.of("exportID", exportID));
        int numOfConsumers = Util.getNumberOfConsumers(queueName, rabbitAPIURL, rabbitUser, rabbitPass);
        tags.add(Tag.of("num.of.consumers", String.valueOf(numOfConsumers)));
        tags.add(Tag.of("records.size", String.valueOf(limit)));
        String folderName = mappingName + "/" + exportID;

        ConvertControllerClient convertClient = new ConvertControllerClient(baseURL);
        String resp = convertClient.convert(catalogueURL, mapping[0], mapping[1], String.valueOf(limit), exportID);
        System.err.println(resp);

        JSONArray res = convertClient.listResults(folderName);
        System.err.println(res);
        System.err.println(res.length());
    }

}
