/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.model;

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
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
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
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
    private MeterRegistry meterRegistry;

    private final String queue;
    private final String mappingURL;
    private final String generatorURL;
    private final Integer limit;
    private final String exportID;
//    private final Counter recordsCounter;

    public ExportDocTask(String catalogueURL, ConnectionFactory factory, String queue, String mappingURL, String generatorURL, Integer limit, String exportID) {
        this.catalogueURL = catalogueURL;
        this.factory = factory;
        this.queue = queue;
        this.mappingURL = mappingURL;
        this.generatorURL = generatorURL;
        this.limit = limit;
        this.exportID = exportID;

//        this.recordsCounter = meterRegistry.counter("export.task.num", exportID, "records");
    }

    private void exportDocuments(String catalogueURL, String exportID) throws MalformedURLException, GenericException, InterruptedException, TransformerConfigurationException, TransformerException, ParserConfigurationException, SAXException {

        try {
            long start = System.currentTimeMillis();
            CatalogueExporter exporter = getExporter(catalogueURL);
            if (this.limit != null && this.limit > -1) {
                exporter.setLimit(limit);
            }
            Collection<String> allResourceIDs = exporter.fetchAllDatasetUUIDs();
            String xml = null;
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

                    byte[] encoded = (Base64.encodeBase64(json.toString().getBytes()));
                    String message = new String(encoded, "UTF-8");

                    channel.basicPublish("", qName,
                            MessageProperties.PERSISTENT_TEXT_PLAIN,
                            message.getBytes("UTF-8"));

                } catch (TimeoutException ex) {
                    Logger.getLogger(ExportDocTask.class.getName()).log(Level.SEVERE, null, ex);
                }
//                this.recordsCounter.increment();

            }
            long elapsed = System.currentTimeMillis() - start;
            System.err.println("elapsed: " + elapsed);
//            Set<String> names = endpoint.listNames().getNames();
//        endpoint.metric(catalogueURL, list);
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
        Timer.Sample timer = Timer.start(this.meterRegistry);
        exportDocuments(this.catalogueURL, this.exportID);
        timer.stop(meterRegistry.timer("export.doc.task." + catalogueURL, "response", "FINISHED"));
        return null;
    }

    /**
     * @param meterRegistry the meterRegistry to set
     */
    public void setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

}
