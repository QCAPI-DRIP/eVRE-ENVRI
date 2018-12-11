/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.benchmarkcat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Base64;
import java.util.Iterator;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;

/**
 *
 * @author S. Koulouzis
 */
public class Util {

    private static Object JSESSIONID;

    public static String post(String serviceURL, JSONObject jsonParams) throws MalformedURLException, ProtocolException, IOException {
        HttpPost request = new HttpPost(serviceURL);
        StringEntity params = new StringEntity(jsonParams.toString());
        request.addHeader("Content-Type", "application/json");
        if (JSESSIONID != null) {
            request.setHeader("Cookie", "JSESSIONID=" + JSESSIONID);
        }

//        request.addHeader("Authorization", token);
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

    public static String get(String serviceURL) throws MalformedURLException, ProtocolException, IOException {
        HttpGet request = new HttpGet(serviceURL);
        request.addHeader("Content-Type", "application/json");
        if (JSESSIONID != null) {
            request.setHeader("Cookie", "JSESSIONID=" + JSESSIONID);
        }

//        request.addHeader("Authorization", token);
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

    public static int getNumberOfConsumers(String taskQeueName, String rabbitAPIURL, String username, String password) throws MalformedURLException, IOException {
        StringBuilder result = new StringBuilder();
        URL url = new URL(rabbitAPIURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        String userpassword = username + ":" + password;

        byte[] encodedBytes = Base64.getEncoder().encode(userpassword.getBytes());
        conn.setRequestProperty("Authorization", "Basic "
                + new String(encodedBytes));

        try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        }
        int consumerCount = 0;
        JSONArray resp = new JSONArray(result.toString());
        Iterator<Object> iter = resp.iterator();
        while (iter.hasNext()) {
            JSONObject obj = (JSONObject) iter.next();
            if (obj.getJSONObject("queue").getString("name").equals(taskQeueName)) {
                consumerCount++;
            }
        }
        return consumerCount;
    }
}
