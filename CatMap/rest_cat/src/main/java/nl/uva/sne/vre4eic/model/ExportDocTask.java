/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.model;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import gr.forth.ics.isl.exporter.RDFExporter;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import gr.forth.ics.isl.exception.GenericException;
import gr.forth.ics.isl.exporter.CatalogueExporter;
import gr.forth.ics.isl.exporter.D4ScienceExporter;
import gr.forth.ics.isl.exporter.OGCCSWExporter;
import gr.forth.ics.isl.util.XML;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.io.File;
import java.io.FileOutputStream;
//import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import static nl.uva.sne.vre4eic.util.Util.isCKAN;
import static nl.uva.sne.vre4eic.util.Util.isCSW;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author S. Koulouzis
 */
public class ExportDocTask implements Callable<String> {

    private final String catalogueURL;
    private final ConnectionFactory factory;

//    @Autowired
//    MetricsEndpoint endpoint;
//    @Autowired
//    private MeterRegistry meterRegistry;
    private final String queue;
    private final String mappingURL;
    private final String generatorURL;
    private final Integer limit;
    private final String exportID;
//    private final Counter recordsCounter;

    private static StringBuilder csvHeader = new StringBuilder();
    private static StringBuilder csvLine = new StringBuilder();
    static Collection<Tag> tags = new ArrayList<>();

    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS'Z'");

    public ExportDocTask(String catalogueURL, ConnectionFactory factory, String queue, String mappingURL, String generatorURL, Integer limit, String exportID) {
        this.catalogueURL = catalogueURL;
        this.factory = factory;
        this.queue = queue;
        this.mappingURL = mappingURL;
        this.generatorURL = generatorURL;
        this.limit = limit;
        this.exportID = exportID;
        df.setTimeZone(tz);

//        this.recordsCounter = meterRegistry.counter("export.task.num", exportID, "records");
    }

    private void exportDocuments(String catalogueURL, String exportID) throws MalformedURLException, GenericException, InterruptedException, TransformerConfigurationException, TransformerException, ParserConfigurationException, SAXException, IOException {
        long start = System.currentTimeMillis();
//        Timer.Sample exportDocumentsTimer = Timer.start(this.meterRegistry);
        try {
            CatalogueExporter exporter = getExporter(catalogueURL);
            if (this.limit != null && this.limit > -1) {
                exporter.setLimit(limit);
            }
            String path = new URL(mappingURL).getPath();
            String mappingName = FilenameUtils.removeExtension(path.substring(path.lastIndexOf('/') + 1));
//            Timer.Sample getDataSetIdsTimer = Timer.start(this.meterRegistry);
            Collection<String> allResourceIDs = exporter.fetchAllDatasetUUIDs();

            tags.add(Tag.of("source", catalogueURL));
            tags.add(Tag.of("mapping.name", mappingName));
            tags.add(Tag.of("exportID", exportID));
            tags.add(Tag.of("records.size", String.valueOf(allResourceIDs.size())));
//            getDataSetIdsTimer.stop(meterRegistry.timer("fetchAllDatasetUUIDs." + exporter.getClass().getName(), tags));
            String xml = null;

            String now = df.format(new Date());
            int messageCount = 0;
            for (String resourceId : allResourceIDs) {

                Object resource = exporter.exportResource(resourceId);
                if (resource instanceof JSONObject) {
                    xml = exporter.transformToXml((JSONObject) resource);
                } else if (resource instanceof Document) {
                    DOMSource domSource = new DOMSource(((Document) resource));
                    StringWriter writer = new StringWriter();
                    StreamResult result = new StreamResult(writer);
                    TransformerFactory tf = TransformerFactory.newInstance();
                    Transformer transformer = tf.newTransformer();
                    transformer.transform(domSource, result);
                    xml = writer.toString();
                }

                try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
                    String qName = queue;
                    channel.queueDeclare(qName, true, false, false, null);

                    JSONObject json = new JSONObject();
                    json.put("metadata_record", xml);
                    String id = XML.getResourceID(xml);

                    json.put("record_id", id);
                    json.put("mappingURL", mappingURL);
                    json.put("generatorURL", generatorURL);
                    if (exportID != null) {
                        json.put("export_id", exportID);
                    }
                    json.put("records_size", allResourceIDs.size());
                    json.put("source_catalogue_url", catalogueURL);
                    json.put("creation_time", now);
                    json.put("message_count", messageCount++);

                    byte[] encoded = (Base64.encodeBase64(json.toString().getBytes()));
                    String message = new String(encoded, "UTF-8");

                    channel.basicPublish("", qName,
                            MessageProperties.PERSISTENT_TEXT_PLAIN,
                            message.getBytes("UTF-8"));

                    now = df.format(new Date());
                } catch (TimeoutException ex) {
                    Logger.getLogger(ExportDocTask.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
//            exportDocumentsTimer.stop(meterRegistry.timer("exportDocuments." + ExportDocTask.class.getName(), tags));

            String webdavHost = System.getenv("WEBDAV_HOST");

            Sardine sardine = SardineFactory.begin();
            String csvFileName = this.getClass().getName() + ".csv";
            String filePath = System.getProperty("user.home") + File.separator + csvFileName;
            File benchmarkFile = new File(filePath);
            if (sardine.exists("http://" + webdavHost + "/benchmark/" + csvFileName)) {
                try (FileOutputStream out = new FileOutputStream(benchmarkFile)) {
                    try (InputStream in = sardine.get(filePath)) {
                        IOUtils.copy(in, out);
                    }
                }
            }

            csvLine.append(start).append(",").append(System.currentTimeMillis()).append(",");
            for (Tag tag : tags) {
                csvLine.append(tag.getValue()).append(",");
            }
            csvHeader.append("start").append(",").append("end").append(",");
            for (Tag tag : tags) {
                csvHeader.append(tag.getKey()).append(",");
            }

            if (!benchmarkFile.exists()) {
                csvHeader.append("\n").append(csvLine.toString()).append("\n");
                Files.write(Paths.get(filePath), csvHeader.toString().getBytes(), StandardOpenOption.CREATE);
            } else {
                Files.write(Paths.get(filePath), csvLine.append("\n").toString().getBytes(), StandardOpenOption.APPEND);
            }
            sardine.put("http://" + webdavHost + "/benchmark/" + csvFileName, benchmarkFile, "text/csv");

        } catch (IOException ex) {
            Logger.getLogger(ExportDocTask.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public CatalogueExporter getExporter(String catalogueURL) throws MalformedURLException, InterruptedException {
        if (isCKAN(catalogueURL)) {
            return new D4ScienceExporter(catalogueURL);
        }
        if (isCSW(catalogueURL + "/csw?REQUEST=GetCapabilities&SERVICE=CSW&VERSION=2.0.2&constraintLanguage=CQL_TEXT&constraint_language_version=1.1.0")) {
            return new OGCCSWExporter(catalogueURL);
        } else {
            return new RDFExporter(catalogueURL);
        }
    }

    @Override
//    @Timed
    public String call() throws Exception {
        exportDocuments(this.catalogueURL, this.exportID);
        return null;
    }

    /**
     * @param meterRegistry the meterRegistry to set
     */
    public void setMeterRegistry(MeterRegistry meterRegistry) {
//        this.meterRegistry = meterRegistry;
    }

}
