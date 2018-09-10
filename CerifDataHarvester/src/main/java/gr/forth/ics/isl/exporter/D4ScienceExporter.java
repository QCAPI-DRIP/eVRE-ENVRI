package gr.forth.ics.isl.exporter;

import gr.forth.ics.isl.common.D4ScienceResources;
import gr.forth.ics.isl.exception.GenericException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Yannis Marketakis (marketak 'at' ics 'dot' forth 'dot' gr)
 */
public class D4ScienceExporter implements CatalogueExporter {

    private final String endpointUrl;

    public D4ScienceExporter(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    @Override
    public void exportAll(String outputPath) throws GenericException {
        try {
            Collection<String> allResourceIDs = this.fetchAllDatasetUUIDs();
//        log.info("Found " + allResourceIDs.size() + " resources");
            for (String resourceId : allResourceIDs) {

//                log.debug("resourceId: " + resourceId);
                JSONObject resource = this.exportResource(resourceId);
                String xml = this.transformToXml(resource);
                this.exportToXml(xml, new File(outputPath + File.separator + resourceId + D4ScienceResources.EXTENSION_XML));
            }
        } catch (IOException | ParserConfigurationException | SAXException | TransformerException ex) {
            Logger.getLogger(D4ScienceExporter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Collection<String> fetchAllDatasetUUIDs() throws MalformedURLException, IOException {
//        log.info("Fetching the list of Resource IDs from the resource catalog");
        Set<String> retCollection = new HashSet<>();
        String ckanPath = null;
        for (String path : D4ScienceResources.ALL_RESOURCES_ENDPOINT) {
            if (urlExists(this.endpointUrl + path)) {
                ckanPath = path;
                break;
            }
        }
        HttpURLConnection conn = (HttpURLConnection) new URL(this.endpointUrl + ckanPath).openConnection();
        StringBuilder sb;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        JSONObject jsonResultObject = new JSONObject(sb.toString());
        JSONArray jsonResults;
        try {

            jsonResults = jsonResultObject.getJSONArray(D4ScienceResources.RESULT);
        } catch (JSONException ex) {
            jsonResults = jsonResultObject.getJSONObject(D4ScienceResources.RESULT).getJSONArray("results");

        }
        for (Object res : jsonResults) {
            if (res instanceof String) {
                retCollection.add((String) res);
            } else if (res instanceof JSONObject) {
                if (((JSONObject) res).has("id")) {
                    retCollection.add(((JSONObject) res).getString("id"));
                }

            }
        }
        return retCollection;
    }

    private boolean urlExists(String URLName) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            //        HttpURLConnection.setInstanceFollowRedirects(false)
            HttpURLConnection con
                    = (HttpURLConnection) new URL(URLName).openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (MalformedURLException ex) {
            return false;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public JSONObject exportResource(String resourceId) throws MalformedURLException, IOException {

        String resourceUrl = this.endpointUrl + D4ScienceResources.RESOURCE_ENDPOINT + "?" + D4ScienceResources.ID_PARAMETER + "=" + resourceId;
        HttpURLConnection conn = (HttpURLConnection) new URL(resourceUrl).openConnection();
        StringBuilder sb;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return new JSONObject(sb.toString());

    }

    @Override
    public String transformToXml(JSONObject jsonObject) {
        JSONObject realObject = jsonObject.getJSONObject("result");
        if (realObject.has("extras")) {
            JSONArray extrasArray = realObject.getJSONArray("extras");
            if (extrasArray != null) {
                for (int i = 0; i < extrasArray.length(); i++) {
                    if (extrasArray.getJSONObject(i).getString("key").equalsIgnoreCase("responsible-party")) {
                        realObject.put("responsible_party", new JSONArray(extrasArray.getJSONObject(i).getString("value")));
                    } else if (extrasArray.getJSONObject(i).getString("key").equalsIgnoreCase("dataset-reference-date")) {
                        realObject.put("dataset-reference-date", new JSONArray(extrasArray.getJSONObject(i).getString("value")));
                    }
                }
            }
        }
        return D4ScienceResources.RESOURCE_ELEMENT_OPEN + "\n"
                + XML.toString(jsonObject) + "\n"
                + D4ScienceResources.RESOURCE_ELEMENT_CLOSE + "\n";
    }

    public void exportToXml(String xmlContents, File xmlFile) throws GenericException, ParserConfigurationException, UnsupportedEncodingException, SAXException, IOException, TransformerConfigurationException, TransformerException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(new InputSource(new ByteArrayInputStream(xmlContents.getBytes("UTF-8"))));
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(xmlFile);
        transformer.transform(source, result);

    }

    public static void main(String[] args) throws GenericException {
        new D4ScienceExporter(D4ScienceResources.D4SCIENCE_CATALOG_URL).exportAll("output");
    }
}
