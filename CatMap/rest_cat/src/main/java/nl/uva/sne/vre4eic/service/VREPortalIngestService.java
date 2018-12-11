/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.uva.sne.vre4eic.util.Util;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author S. Koulouzis
 */
@Service
public class VREPortalIngestService {

    @Autowired
    MeterRegistry meterRegistry;

    @Value("${node.service.url:http://v4e-lab.isti.cnr.it:8080/NodeService/}")
    private String nodeServiceURL;

    private static String JSESSIONID;
    private String exportID;
    private String mappingName;

    public JSONObject ingest(JSONObject requestParams) throws IOException, MalformedURLException, ParseException {
        Timer.Sample ingestTimer = Timer.start(meterRegistry);

        String sourceRecords = (String) requestParams.get("sourceRecURL");
        String[] parts = sourceRecords.split("/");
        exportID = parts[parts.length - 1];
        mappingName = parts[parts.length - 2];

        String ingestCatURL = (String) requestParams.get("ingestCatalogueURL");
        String token = (String) requestParams.get("token");
        String username = (String) requestParams.get("username");
        String namedGraphLabelParam = (String) requestParams.get("namedGraphLabelParam");

        JSONObject response = new JSONObject();

        JSONObject inputParams = initInputParams(token, username, namedGraphLabelParam);

        JSONObject createGraphMetadataInput = getCreateGraphMetadataInput(inputParams);
        JSONObject resp = createGraphMetadata(ingestCatURL, createGraphMetadataInput, token);

        response.putAll(resp);
        if (!(Boolean) resp.get("success")) {
            return response;
        }
        String namedGraphIdParam = (String) resp.get("namedGraphIdParam");
        inputParams.put("namedGraphIdParam", namedGraphIdParam);
        JSONObject insertUserProfileMetadataInput = getInsertUserProfileMetadataInput(inputParams);
        resp = insertUserProfileMetadata(ingestCatURL, insertUserProfileMetadataInput, token);
        response.putAll(resp);
        if (!(Boolean) resp.get("success")) {
            return response;
        }
        String linkingUpdateQuery = (String) resp.get("linkingUpdateQuery");
        JSONObject uploadInput = getUploadInput(inputParams, token, linkingUpdateQuery);

        String path = Util.downloadRecords(sourceRecords);
        resp = upload(ingestCatURL, uploadInput, path);
        response.putAll(resp);
        if (!(Boolean) resp.get("success")) {
            return response;
        }

        JSONObject afterUploadProcessInput = getAfterUploadProcessInput(inputParams, (String) resp.get("linkingUpdateQuery"));
        resp = afterUploadProcess(ingestCatURL, afterUploadProcessInput, token);
        response.putAll(resp);

        ingestTimer.stop(meterRegistry.timer("ingest." + this.getClass().getName(), "target.metadata.catalogue", ingestCatURL, "mapping.name", mappingName, "exportID", exportID));

        return response;

    }

    private JSONObject initInputParams(String token, String username, String namedGraphLabelParam) throws IOException, MalformedURLException, ParseException {
        JSONObject userProfileMetadata = getUserProfileMetadata(nodeServiceURL + "/user/getprofile?token=" + token + "&userLogin=" + username);

        JSONObject requestParams = new JSONObject();
        requestParams.put("namedGraphLabelParam", namedGraphLabelParam);
        requestParams.put("selectedCategoryLabel", "RIs");
        requestParams.put("selectedCategoryId", 2);
        requestParams.put("userProfileMetadata", userProfileMetadata);
        return requestParams;
    }

    private JSONObject getUserProfileMetadata(String service) throws MalformedURLException, IOException, ParseException {
        URL url = new URL(service);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", con.getClass().getName());
        int responseCode = con.getResponseCode();
        StringBuilder response;
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(response.toString());
        return json;
    }

    private JSONObject getCreateGraphMetadataInput(JSONObject requestParams) {
        JSONObject createGraphMetadataInput = new JSONObject();
        createGraphMetadataInput.put("namedGraphLabelParam", (String) requestParams.get("namedGraphLabelParam"));
        createGraphMetadataInput.put("selectedCategoryLabel", (String) requestParams.get("selectedCategoryLabel"));
        createGraphMetadataInput.put("selectedCategoryId", (int) requestParams.get("selectedCategoryId"));
        return createGraphMetadataInput;
    }

    private JSONObject createGraphMetadata(String vreportal, JSONObject createGraphMetadataInput, String token) throws IOException, ParseException {
        Timer.Sample createGraphMetadataTimer = Timer.start(meterRegistry);
        String response = post(vreportal + "/createGraphMetadata", createGraphMetadataInput, token);
        JSONParser parser = new JSONParser();
        createGraphMetadataTimer.stop(meterRegistry.timer("createGraphMetadata." + this.getClass().getName(), "target.metadata.catalogue", vreportal, "mapping.name", mappingName, "exportID", exportID));
        return (JSONObject) parser.parse(response);

    }

    private String post(String service, JSONObject createGraphMetadataInput, String token) throws MalformedURLException, ProtocolException, IOException {
        HttpPost request = new HttpPost(service);
        StringEntity params = new StringEntity(createGraphMetadataInput.toJSONString());
        request.addHeader("Content-Type", "application/json");
        if (JSESSIONID != null) {
            request.setHeader("Cookie", "JSESSIONID=" + JSESSIONID);
        }

        request.addHeader("Authorization", token);
        request.setEntity(params);

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse response = httpClient.execute(request);
        Header[] headers = response.getAllHeaders();
        for (Header h : headers) {
            if (h.getName().equals("Set-Cookie")) {
                JSESSIONID = h.getValue().split("=")[1].split(";")[0];
                break;
            }
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(
                (response.getEntity().getContent())));
        String output;
        StringBuilder sb = new StringBuilder();
        while ((output = br.readLine()) != null) {
            sb.append(output);
        }
        return sb.toString();
    }

    private JSONObject getInsertUserProfileMetadataInput(JSONObject requestParams) {

        JSONObject insertUserProfileMetadataInput = new JSONObject();
        JSONObject userProfileMetadata = (JSONObject) requestParams.get("userProfileMetadata");
        insertUserProfileMetadataInput.put("namedGraphLabel", (String) requestParams.get("namedGraphLabelParam"));
        insertUserProfileMetadataInput.put("role", (String) userProfileMetadata.get("role"));
        insertUserProfileMetadataInput.put("namedGraphId", (String) requestParams.get("namedGraphIdParam"));
        insertUserProfileMetadataInput.put("organization", (String) userProfileMetadata.get("organization"));
        insertUserProfileMetadataInput.put("name", (String) userProfileMetadata.get("name"));
        insertUserProfileMetadataInput.put("organizationURL", (String) userProfileMetadata.get("organizationURL"));
        insertUserProfileMetadataInput.put("userId", (String) userProfileMetadata.get("userId"));
        insertUserProfileMetadataInput.put("email", (String) userProfileMetadata.get("email"));
        return insertUserProfileMetadataInput;
    }

    private JSONObject insertUserProfileMetadata(String vreportal, JSONObject createGraphMetadataInput, String token) throws ProtocolException, IOException, ParseException {
        Timer.Sample insertUserProfileMetadataTimer = Timer.start(meterRegistry);
        String response = post(vreportal + "/insertUserProfileMetadata", createGraphMetadataInput, token);
        JSONParser parser = new JSONParser();
        insertUserProfileMetadataTimer.stop(meterRegistry.timer("insertUserProfileMetadata." + this.getClass().getName(), "target.metadata.catalogue", vreportal, "mapping.name", mappingName, "exportID", exportID));
        return (JSONObject) parser.parse(response);
    }

    private JSONObject getUploadInput(JSONObject requestParams, String token, String linkingUpdateQuery) {
        JSONObject uploadInput = new JSONObject();
        uploadInput.put("namedGraphLabelParam", (String) requestParams.get("namedGraphLabelParam"));
        uploadInput.put("selectedCategoryLabel", (String) requestParams.get("selectedCategoryLabel"));
        uploadInput.put("namedGraphIdParam", (String) requestParams.get("namedGraphIdParam"));
        uploadInput.put("selectedCategoryId", (int) requestParams.get("selectedCategoryId"));
        uploadInput.put("authorizationParam", token);
        uploadInput.put("linkingUpdateQuery", linkingUpdateQuery);
        uploadInput.put("contentTypeParam", "application/x-turtle");
        return uploadInput;
    }

    private JSONObject upload(String vreportal, JSONObject uploadnput, String filesPath) throws ProtocolException, IOException, ParseException {
        Timer.Sample uploadTimer = Timer.start(meterRegistry);
        String endpoint = vreportal + "/upload";
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost uploadFile = new HttpPost(endpoint);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        Set keys = uploadnput.keySet();
        for (Object k : keys) {
            String key = (String) k;
            Object value = uploadnput.get(key);
            if (value instanceof String) {
                builder.addTextBody(key, ((String) value), ContentType.TEXT_PLAIN);
            } else if (value instanceof Integer) {
                builder.addTextBody(key, String.valueOf(((Integer) value)), ContentType.TEXT_PLAIN);
            }

        }

        File[] files = new File(filesPath).listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".ttl")) {
                builder.addBinaryBody(
                        f.getName(),
                        new FileInputStream(f),
                        ContentType.create((String) uploadnput.get("contentTypeParam")),
                        f.getName()
                );
            }
        }

        HttpEntity multipart = builder.build();
        uploadFile.setEntity(multipart);
        CloseableHttpResponse response = httpClient.execute(uploadFile);
        HttpEntity responseEntity = response.getEntity();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                (responseEntity.getContent())));

        String output;
        StringBuilder sb = new StringBuilder();
        while ((output = br.readLine()) != null) {
//            System.out.println(output);
            sb.append(output);
        }
        Pattern p = Pattern.compile("\"([^response_status\"]*),\"");
        Matcher m = p.matcher(sb.toString());
        String resp = null;
        while (m.find()) {
            String pat = m.group(1);
            pat = pat.replaceAll(":", "");
            resp = sb.toString().replaceAll(pat, "\"" + pat + "\"");
        }

        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(resp);
        String response_status = ((String) json.get("response_status")).toLowerCase();
        json.remove("response_status");
        if (response_status.equals("succeed")) {
            json.put("success", true);
        } else {
            json.put("success", false);
        }
        uploadTimer.stop(meterRegistry.timer("upload." + this.getClass().getName(), "target.metadata.catalogue", vreportal, "mapping.name", mappingName, "exportID", exportID));
        return json;
    }

    private JSONObject getAfterUploadProcessInput(JSONObject requestParams, String linkingUpdateQuery) {
        JSONObject afterUploadProcessInput = new JSONObject();
        afterUploadProcessInput.put("namedGraphLabel", (String) requestParams.get("namedGraphLabelParam"));
        afterUploadProcessInput.put("namedGraphIdParam", (String) requestParams.get("namedGraphIdParam"));
        afterUploadProcessInput.put("linkingUpdateQuery", linkingUpdateQuery);
        afterUploadProcessInput.put("namedGraphIdParam", (String) requestParams.get("namedGraphIdParam"));
        return afterUploadProcessInput;
    }

    private JSONObject afterUploadProcess(String vreportal, JSONObject afterUploadProcessInput, String token) throws ProtocolException, IOException, ParseException {
        Timer.Sample afterUploadProcessTimer = Timer.start(meterRegistry);
        String resp = post(vreportal + "/after_upload_process", afterUploadProcessInput, token);
        JSONParser parser = new JSONParser();
        afterUploadProcessTimer.stop(meterRegistry.timer("afterUploadProcess." + this.getClass().getName(), "target.metadata.catalogue", vreportal, "mapping.name", mappingName, "exportID", exportID));
        return (JSONObject) parser.parse(resp);
    }
}
