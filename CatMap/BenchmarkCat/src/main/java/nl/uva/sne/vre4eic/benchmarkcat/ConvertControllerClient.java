/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.benchmarkcat;

import java.io.IOException;
import java.net.ProtocolException;
import org.json.JSONArray;

/**
 *
 * @author S. Koulouzis
 */
public class ConvertControllerClient {
//http://localhost:8080/rest/list_records/?catalogue_url=https://ckan-d4s.d4science.org/

    private final String baseURL;

    public ConvertControllerClient(String baseURL) {
        this.baseURL = baseURL;
    }

    public String convert(String catalogueURL, String mappingURL, String generatorURL, String limit, String exportID) throws ProtocolException, IOException {
        String resp = Util.get(baseURL + "/convert?catalogue_url=" + catalogueURL
                + "&mapping_url=" + mappingURL + "&generator_url=" + generatorURL + "&limit=" + limit + "&export_id=" + exportID);
        return resp;
    }

    public JSONArray listResults(String folderName) throws ProtocolException, IOException {
        String resp = Util.get(baseURL + "/list_results/?folder_name=" + folderName);
        return new JSONArray(resp);
    }
}
