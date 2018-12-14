/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.rest;

import com.github.sardine.DavResource;
import io.micrometer.core.annotation.Timed;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import nl.uva.sne.vre4eic.model.ProcessingStatus;
import nl.uva.sne.vre4eic.service.ConvertService;
import nl.uva.sne.vre4eic.service.VREPortalIngestService;
import nl.uva.sne.vre4eic.util.Util;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author S. Koulouzis
 */
@RestController
@Timed
public class ConvertController {

    @Autowired
    private ConvertService service;

    @Autowired
    private VREPortalIngestService ingestService;

//http://localhost:8080/rest/convert?catalogue_url=https://ckan-d4s.d4science.org&mapping_url=https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/Mapping115.x3ml&generator_url=https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/CERIF-generator-policy-v5___21-08-2018124405___12069.xml
//http://localhost:8083/catalogue_mapper/convert?catalogue_url=https://ckan-d4s.d4science.org&mapping_url=https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/Mapping115.x3ml&generator_url=https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/CERIF-generator-policy-v5___21-08-2018124405___12069.xml
//    http://localhost:8083/catalogue_mapper/convert?catalogue_url=http://172.17.0.2:8080/Mapping120/&mapping_url=http://172.17.0.2:8080/Mapping120/Mapping120.x3ml&generator_url=http://172.17.0.2:8080/Mapping120/ENVRIplus-generator-policy___13-07-2018131200___11511.xml
//http://localhost:8080/rest/convert?catalogue_url=http://localhost:8081/Mapping120/&mapping_url=http://localhost:8081/Mapping120/Mapping120.x3ml&generator_url=http://localhost:8081/Mapping120/ENVRIplus-generator-policy___13-07-2018131200___11511.xml
//    http://localhost:8080/rest/convert?catalogue_url=%20https://ckan-d4s.d4science.org&mapping_url=https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/Mapping62.x3ml&generator_url=https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/generator.xml
//    http://localhost:8080/rest/convert?catalogue_url=%20https://catalog.data.gov&mapping_url=https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/Mapping62.x3ml&generator_url=https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/generator.xml
//    http://localhost:8080/catalogue_mapper/convert?catalogue_url=%20https://ckan-d4s.d4science.org&mapping_url=https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/Mapping62.x3ml&generator_url=https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/generator.xml
    @RequestMapping(value = "/convert", method = RequestMethod.GET,
            params = {"catalogue_url", "mapping_url", "generator_url", "export_id"},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @GetMapping("/")
    public @ResponseBody
    ProcessingStatus convert(@RequestParam(value = "catalogue_url") String catalogueURL,
            @RequestParam(value = "mapping_url") String mappingURL,
            @RequestParam(value = "generator_url") String generatorURL,
            @RequestParam(value = "limit", required = false) int limit,
            @RequestParam(value = "export_id") String exportID) {
        try {
            long start = System.currentTimeMillis();
            ProcessingStatus status = service.doProcess(catalogueURL, mappingURL, generatorURL, limit, exportID);
            System.err.println("Start: " + start + " End: " + System.currentTimeMillis());
            return status;
        } catch (MalformedURLException ex) {
            Logger.getLogger(ConvertController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(ConvertController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ConvertController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @RequestMapping(value = "/list_records", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    Collection<String> listRecords(
            @RequestParam(value = "catalogue_url") String catalogueURL,
            @RequestParam(value = "limit", required = false) Integer limit) {
        Collection<String> records = null;
        try {
            records = service.listRecords(catalogueURL, limit);
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(ConvertController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return records;
    }

    @RequestMapping(value = "/download/{mappingName}/{mappingID}", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public void downloadFile(HttpServletResponse response,
            @PathVariable("mappingName") String mappingName,
            @PathVariable("mappingID") String mappingID) throws IOException {
        String webdavHost = System.getenv("WEBDAV_HOST");
        String webDAVURL;
        String folderName = mappingName + "/" + mappingID;
        if (webdavHost == null) {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            webdavHost = addr.getHostName();

            webDAVURL = "http://" + webdavHost + "/" + folderName;
        } else {
            webDAVURL = "http://" + webdavHost + "/" + folderName;
        }
        File file = new File(Util.zipRecords(webDAVURL));

        if (!file.exists()) {
            String errorMessage = "Sorry. The file you are looking for does not exist";
            Logger.getLogger(ConvertController.class.getName()).log(Level.WARNING, "File not found: {0}", file.getAbsolutePath());
            try (OutputStream outputStream = response.getOutputStream()) {
                outputStream.write(errorMessage.getBytes(Charset.forName("UTF-8")));
            }
            return;
        }
        String mimeType = URLConnection.guessContentTypeFromName(file.getName());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        response.setContentType(mimeType);

        response.setHeader("Content-Disposition", String.format("inline; filename=\"" + file.getName() + "\""));

        /* "Content-Disposition : attachment" will be directly download, may provide save as popup, based on your browser setting*/
        //response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", file.getName()));
        response.setContentLength((int) file.length());

        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

        //Copy bytes from source to destination(outputstream in this example), closes both streams.
        FileCopyUtils.copy(inputStream, response.getOutputStream());
    }

    @RequestMapping(value = "/list_results", method = RequestMethod.GET,
            params = {"folder_name"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    Collection<DavResource> listResults(@RequestParam(value = "folder_name") String folderName) {
        Collection<DavResource> records = null;
        try {
            String webdavHost = System.getenv("WEBDAV_HOST");
            String webDAVURL;
            if (webdavHost == null) {
                InetAddress addr;
                addr = InetAddress.getLocalHost();
                webdavHost = addr.getHostName();
                webDAVURL = "http://" + webdavHost + "/" + folderName;
                if (!Util.urlExists(webDAVURL)) {
                    records = new ArrayList<>();
                    return records;
                }
            } else {
                webDAVURL = "http://" + webdavHost + "/" + folderName;
            }
            Logger.getLogger(ConvertController.class.getName()).log(Level.INFO, "Webdav: {0}", webDAVURL);
            records = service.listResults(webDAVURL);
        } catch (IOException ex) {
            Logger.getLogger(ConvertController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return records;
    }

    @RequestMapping(value = "/catalogue_type", method = RequestMethod.GET, params = {"catalogue_url"})
    public @ResponseBody
    String getCatalogueType(@RequestParam(value = "catalogue_url") String catalogueURL) {
        if (catalogueURL.length() <= 1) {
            return null;
        }
        try {
            return service.getCatalogueType(catalogueURL);
        } catch (MalformedURLException | InterruptedException ex) {
            Logger.getLogger(ConvertController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

////    http://localhost:8080/rest/ingest_records/?source_rec_url=http://localhost/Mapping115/3955e877-1d73-45fd-94ea-54b495465573/&ingest_cat_url=http://localhost:3030
//    @RequestMapping(value = "/ingest_records", method = RequestMethod.GET, params = {"source_rec_url", "ingest_cat_url"})
//    public void ingestRecords(@RequestParam(value = "source_rec_url") String sourceRecURL,
//            @RequestParam(value = "ingest_cat_url") String ingestCatURL,
//            @RequestParam(value = "dataset_name") String datasetName) {
//        try {
//            service.ingest(sourceRecURL, ingestCatURL, datasetName);
//        } catch (IOException ex) {
//            Logger.getLogger(ConvertController.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    @RequestMapping(value = "/ingest_records", method = RequestMethod.POST, produces = {"application/json"})
    public @ResponseBody
    JSONObject ingestRecords(@RequestBody JSONObject requestParams) {
        try {
            return ingestService.ingest(requestParams);
        } catch (IOException | ParseException ex) {
            Logger.getLogger(ConvertController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

//    http://localhost:8080/rest/count_rdf_records/?catalogue_url=http://localhost:3030
    @RequestMapping(value = "/count_rdf_records", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    Integer countRDFRecords(
            @RequestParam(value = "catalogue_url") String catalogueURL,
            @RequestParam(value = "dataset_name") String datasetName) {

        Integer recordsNum = service.countRDFRecords(catalogueURL, datasetName);

        return recordsNum;
    }

////    http://localhost:8080/rest/get_stats?rdf_url=ftp://user:12345@localhost/ckan_Mapping62.x3ml/
//    @RequestMapping(value = "/get_stats", method = RequestMethod.GET, params = {"rdf_url"})
//    @GetMapping("/")
//    public @ResponseBody
//    Map<Object, Object> getRDFStats(@RequestParam(value = "rdf_url") String rdfURL) {
//        try {
//            return stats.getStats(rdfURL);
//        } catch (IOException | TimeoutException | InterruptedException ex) {
//            Logger.getLogger(ConvertController.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return null;
//    }    
}
