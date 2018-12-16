/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.service;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import gr.forth.ics.isl.exporter.CatalogueExporter;
//import io.micrometer.core.instrument.MeterRegistry;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import nl.uva.sne.vre4eic.model.ExportDocTask;
import nl.uva.sne.vre4eic.model.ProcessingStatus;
import nl.uva.sne.vre4eic.util.Util;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;

@Service
public class ConvertService {

    @Autowired
    ThreadPoolTaskExecutor exec;

    @Autowired
    CachingConnectionFactory connectionFactory;

    @Autowired
    MetricsEndpoint endpoint;
//
//    @Autowired
//    MeterRegistry meterRegistry;

    Map<String, Future<String>> taskMap = new HashMap<>();

    public ProcessingStatus doProcess(String catalogueURL, String mappingURL, String generatorURL, int limit, String exportID) throws MalformedURLException, IOException, FileNotFoundException, InterruptedException, Exception {
        String taskID = catalogueURL + exportID;
        Future<String> convertTask = taskMap.get(taskID);

        if (convertTask == null) {
//            String path = new URL(mappingURL).getPath();
//            String queueName = path.substring(path.lastIndexOf('/') + 1);
            String queueName = "metadata_records";
            ExportDocTask task = new ExportDocTask(catalogueURL, connectionFactory.getRabbitConnectionFactory(), queueName, mappingURL, generatorURL, limit, exportID);
//            task.setMeterRegistry(meterRegistry);
            convertTask = exec.submit(task);
            taskMap.put(taskID, convertTask);

//            Timer timer = meterRegistry.timer("export.doc.task." + catalogueURL);
        }
        ProcessingStatus process = new ProcessingStatus();
        process.setCatalogueURL(new URL(catalogueURL));
        if (convertTask.isDone()) {
            taskMap.remove(taskID);
            process.setStatus("FINISHED");
            return process;
        } else {
            process.setStatus("WORKING");
            return process;
        }
    }

    public Collection<String> listRecords(String catalogueURL, Integer limit) throws MalformedURLException, IOException, InterruptedException {
        ExportDocTask task = new ExportDocTask(catalogueURL, connectionFactory.getRabbitConnectionFactory(), null, null, null, limit, null);
        CatalogueExporter exp;
        exp = task.getExporter(catalogueURL);
        if (limit != null && limit > -1) {
            exp.setLimit(limit);
        }
        return exp.fetchAllDatasetUUIDs();
    }

    public List<DavResource> listResults(String webdavURL) throws IOException {
        Sardine sardine = SardineFactory.begin();
        return sardine.list(webdavURL);
    }

    public String getCatalogueType(String catalogueURL) throws MalformedURLException, InterruptedException {
        if (Util.isCKAN(catalogueURL)) {
            return "CKAN";
        }
        if (Util.isCSW(catalogueURL)) {
            return "CSW";
        }

        return null;
    }

    public Integer countRDFRecords(String catalogueURL, String datasetName) {
        String query = "SELECT (COUNT(*) AS ?count) WHERE { ?subject ?predicate ?object}";
        if (!catalogueURL.endsWith("/")) {
            catalogueURL += "/";
        }
        ResultSet rs = QueryExecutionFactory.sparqlService(catalogueURL + datasetName + "/query", query).execSelect();

        QuerySolution r = rs.next();
        Literal countLiteral = ((Literal) r.get("count"));
        return countLiteral.getInt();

    }
}
