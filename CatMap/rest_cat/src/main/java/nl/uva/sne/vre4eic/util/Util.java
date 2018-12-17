/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.util;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import gr.forth.ics.isl.exporter.CatalogueExporter;
import gr.forth.ics.isl.exporter.D4ScienceExporter;
import gr.forth.ics.isl.exporter.OGCCSWExporter;
import gr.forth.ics.isl.exporter.RDFExporter;
import gr.forth.ics.isl.util.XML;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.compress.utils.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 *
 * @author S. Koulouzis
 */
public class Util {

    private static Map<String, String> CACHE = new HashMap<>();

    public static boolean isCKAN(String url) {
        String type = CACHE.get(url);
        if (type != null && type.equals("CKAN")) {
            return true;
        }
        try {

            if (!url.contains("/api/action/help_show?name=tag_list")) {
                url += "/api/action/help_show?name=tag_list";
            }

            JSONObject json = readJsonFromUrl(url);
            CACHE.put(url, "CKAN");
            return json.getBoolean("success");
        } catch (IOException | JSONException ex) {
            return false;
        }
    }

    public static boolean isCSW(String url) {
        String type = CACHE.get(url);
        if (type != null && type.equals("CSW")) {
            return true;
        }
        try {
            if (!url.contains("/csw?REQUEST=GetCapabilities&SERVICE=CSW&VERSION=2.0.2&constraintLanguage=CQL_TEXT&constraint_language_version=1.1.0")) {
                url += "/csw?REQUEST=GetCapabilities&SERVICE=CSW&VERSION=2.0.2&constraintLanguage=CQL_TEXT&constraint_language_version=1.1.0";
            }

            DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
            fac.setNamespaceAware(true);
            Document doc = fac.newDocumentBuilder().parse(new URL(url).openStream());

            Node capabilities = XML.getNode(doc.getFirstChild(), "Capabilities");
            if (capabilities == null) {
                return false;
            }
        } catch (SAXException | ParserConfigurationException | IOException ex) {
            return false;
        }
        CACHE.put(url, "CSW");
        return true;
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        JSONObject json = new JSONObject(readPage(url));
        return json;
    }

    public static String readPage(String url) throws MalformedURLException, IOException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            return sb.toString();
        }

    }

    public static File downloadFile(DavResource resource, Sardine sardine, String webDAVURL, File output) {

        InputStream in = null;
        File file = null;
        String webdavFile = webDAVURL + "/" + resource.getPath();
        try {
            in = sardine.get(webdavFile);

            file = new File(output, resource.getName());
            try (OutputStream out = new FileOutputStream(file.getAbsoluteFile())) {
                IOUtils.copy(in, out);
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return file;
    }

    public static String zipRecords(String webDAVURL) throws IOException {
        try {
            Sardine sardine = SardineFactory.begin();

            List<DavResource> resources = sardine.list(webDAVURL);
            URL url = new URL(webDAVURL);
            String base = url.getProtocol() + "://" + url.getHost();
            if (url.getPort() > -1) {
                base += ":" + url.getPort();
            }

            File output = new File(System.getProperty("java.io.tmpdir") + File.separator + "records");
            if (output.exists()) {
                output.delete();
            }
            output.mkdirs();

            for (DavResource res : resources) {
                if (!res.isDirectory()) {
                    File file = Util.downloadFile(res, sardine, base, output);
                }
            }

            zipFolder(Paths.get(output.getAbsolutePath()), Paths.get(output.getAbsolutePath() + ".zip"));
            return output.getAbsolutePath() + ".zip";
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static String downloadRecords(String webDAVURL) throws IOException {
        try {
            Sardine sardine = SardineFactory.begin();

            List<DavResource> resources = sardine.list(webDAVURL);
            URL url = new URL(webDAVURL);
            String base = url.getProtocol() + "://" + url.getHost();
            if (url.getPort() > -1) {
                base += ":" + url.getPort();
            }

            File output = new File(System.getProperty("java.io.tmpdir") + File.separator + "records");
            if (output.exists()) {
                output.delete();
            }
            output.mkdirs();

            for (DavResource res : resources) {
                if (!res.isDirectory()) {
                    File file = Util.downloadFile(res, sardine, base, output);
                }
            }

            return output.getAbsolutePath();
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private static void zipFolder(Path sourceFolderPath, Path zipPath) throws Exception {
        if (zipPath.toFile().exists()) {
            zipPath.toFile().delete();
        }
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            Files.walkFileTree(sourceFolderPath, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                    zos.putNextEntry(new ZipEntry(sourceFolderPath.relativize(file).toString()));

                    Files.copy(file, zos);

                    zos.closeEntry();

                    return FileVisitResult.CONTINUE;

                }

            });
        }

    }

    public static InputStream getWebDavInputStream(DavResource resource, Sardine sardine, String webDAVURL) throws IOException {
        String webdavFile = webDAVURL + "/" + resource.getPath();
        return sardine.get(webdavFile);
    }

    public static boolean urlExists(String URLName) {
        try {
            HttpURLConnection.setFollowRedirects(true);
            //        HttpURLConnection.setInstanceFollowRedirects(false)
            HttpURLConnection con
                    = (HttpURLConnection) new URL(URLName).openConnection();
            con.setInstanceFollowRedirects(true);
            con.setRequestMethod("HEAD");

            int code = con.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                if (code == HttpURLConnection.HTTP_MOVED_TEMP
                        || code == HttpURLConnection.HTTP_MOVED_PERM
                        || code == HttpURLConnection.HTTP_SEE_OTHER) {
                    String newUrl = con.getHeaderField("Location");

                    // get the cookie if need, for login
                    String cookies = con.getHeaderField("Set-Cookie");
                    con = (HttpURLConnection) new URL(newUrl).openConnection();
                    con.setRequestProperty("Cookie", cookies);
                    code = con.getResponseCode();
                }
            }

            return (code == HttpURLConnection.HTTP_OK);
        } catch (MalformedURLException ex) {
            return false;
        } catch (IOException ex) {
            return false;
        }
    }

    public static CatalogueExporter getExporter(String catalogueURL) throws MalformedURLException, InterruptedException {
        if (isCKAN(catalogueURL)) {
            return new D4ScienceExporter(catalogueURL);
        }
        if (isCSW(catalogueURL + "/csw?REQUEST=GetCapabilities&SERVICE=CSW&VERSION=2.0.2&constraintLanguage=CQL_TEXT&constraint_language_version=1.1.0")) {
            return new OGCCSWExporter(catalogueURL);
        } else {
            return new RDFExporter(catalogueURL);
        }
    }

}
