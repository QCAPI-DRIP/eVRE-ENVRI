/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.cat_exporter;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.codec.binary.Base64;
import org.xml.sax.SAXException;
import eu.delving.x3ml.X3MLEngine;
import eu.delving.x3ml.X3MLGeneratorPolicy;
import eu.delving.x3ml.engine.Generator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;
import org.w3c.dom.Element;

public class Worker {

    private final String rabbitMQHost;
    private final String taskQeueName;
    private final String outputRfdFolder;
    private final String webdavHost;
    private String webdavUser;
    private String webdavPass;

//        private enum outputFormat {
//        RDF_XML,
//        NTRIPLES,
//        TURTLE
//    }
//
//    private enum outputStream {
//        SYSTEM_OUT,
//        FILE,
//        DISABLED
//    }
    public Worker(String rabbitMQHost, String webdavHost, String webdavUser, String webdavPass, String taskQeueName, String output) throws IOException {
        this.taskQeueName = taskQeueName;
        this.rabbitMQHost = rabbitMQHost;
        this.outputRfdFolder = output;
        if (webdavHost == null) {
            this.webdavHost = System.getenv("WEBDAV_HOST");
        } else {
            this.webdavHost = webdavHost;
            this.webdavUser = webdavUser;
            this.webdavPass = webdavPass;
        }

        Logger.getLogger(Worker.class.getName()).log(Level.INFO, "Consuming from qeue: {0}", taskQeueName);
    }

    public void consume() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQHost);
//        MeterRegistry registry = new InfluxMeterRegistry(InfluxConfig.DEFAULT, Clock.SYSTEM);
//
//        MicrometerMetricsCollector metricsCollector = new MicrometerMetricsCollector(registry);
//        factory.setMetricsCollector(metricsCollector);

        final Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();

        channel.queueDeclare(taskQeueName, true, false, false, null);

        channel.basicQos(1);

        final Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException, FileNotFoundException {

                byte[] decodedBytes = Base64.decodeBase64(body);
                String message = new String(decodedBytes, "UTF-8");
                JSONObject jObject = new JSONObject(message);
                File mapping = null;
                File generator = null;
                String ckanRecordID = null;
//                Logger.getLogger(Worker.class.getName()).log(Level.INFO, "message: {0}", message);
                try {
                    byte[] mappingData = getBytes(new URL(jObject.getString("mappingURL")));
                    byte[] generatorData = getBytes(new URL(jObject.getString("generatorURL")));

                    mapping = File.createTempFile("mapping", ".tmp");
                    FileUtils.writeByteArrayToFile(mapping, mappingData);

                    generator = File.createTempFile("generator", ".tmp");
                    FileUtils.writeByteArrayToFile(generator, generatorData);
                    String xmlCkan = jObject.getString("metadata_record");
                    ckanRecordID = jObject.getString("record_id");
                    X3MLEngine.Output rdf = convert(xmlCkan, mapping, generator);
//                    Logger.getLogger(Worker.class.getName()).log(Level.INFO, "rdf: {0}", rdf);

                    String path = new URL(jObject.getString("mappingURL")).getPath();
                    String mappingName = FilenameUtils.removeExtension(path.substring(path.lastIndexOf('/') + 1));

                    String fileName = mappingName + "_" + ckanRecordID;
                    String exportID = jObject.getString("export_id");
                    String webdavFolder = mappingName;
                    if (exportID != null) {
                        webdavFolder = mappingName + "/" + exportID;
                    }
                    File rdfFile = new File(outputRfdFolder + File.separator + fileName + ".ttl");
                    Logger.getLogger(Worker.class.getName()).log(Level.INFO, "fileName: {0}", fileName);

//                    rdf.write(new PrintStream(rdfFile), "application/rdf+xml");
                    rdf.write(new PrintStream(rdfFile), "text/turtle");

                    Logger.getLogger(Worker.class.getName()).log(Level.INFO, "Saved file :{0}", rdfFile.getAbsolutePath());
                    if (webdavHost != null) {

                        Logger.getLogger(Worker.class.getName()).log(Level.INFO, "Connected to :{0}", webdavHost);
                        String webdavUserEnv = System.getenv("WEBDAV_USERNAME");
                        if (webdavUserEnv != null) {
                            webdavUser = webdavUserEnv;
                        }
                        String webdavPasswordEnv = System.getenv("WEBDAV_PASSWORD");
                        if (webdavPasswordEnv != null) {
                            webdavPasswordEnv = webdavPass;
                        }

                        Sardine sardine = SardineFactory.begin(webdavUser, webdavPass);

                        if (sardine.exists("http://" + webdavHost + "/" + webdavFolder + "/" + rdfFile.getName())) {
                            sardine.delete("http://" + webdavHost + "/" + webdavFolder + "/" + rdfFile.getName());
                        }

                        byte[] rdfData = FileUtils.readFileToByteArray(rdfFile);

                        sardine.put("http://" + webdavHost + "/" + webdavFolder + "/" + rdfFile.getName(), rdfData);

                        sardine.put("http://" + webdavHost + "/" + webdavFolder + "/" + fileName + ".xml", xmlCkan.getBytes());
//                        sardine.put("http://" + webdavHost + "/" + webdavFolder + "/" + fileName + ".json", jsonCkan.getBytes());

                    }

                } catch (IOException | ParserConfigurationException | SAXException ex) {
                    if (ex instanceof org.xml.sax.SAXParseException) {
                        Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, "Failed to convert record with id: " + ckanRecordID, ex);
                    } else {
                        Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } finally {
                    if (channel.isOpen()) {
                        channel.basicAck(envelope.getDeliveryTag(), false);
                    }
                    if (mapping != null) {
                        mapping.delete();
                    }
                    if (generator != null) {
                        generator.delete();
                    }
                }
            }
        };
        channel.basicConsume(taskQeueName, false, consumer);
    }

    private X3MLEngine.Output convert(String task, File mappingsFile, File generatorPathFile) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        final int UUID_SIZE = 2;

        X3MLEngine.REPORT_PROGRESS = true;
        X3MLEngine engine;
        engine = createEngine(mappingsFile);

        Generator policy;
        if (generatorPathFile == null || !generatorPathFile.exists() || generatorPathFile.length() < 1) {
            policy = X3MLGeneratorPolicy.load(null, X3MLGeneratorPolicy.createUUIDSource(UUID_SIZE));
        } else {
            policy = X3MLGeneratorPolicy.load(new FileInputStream(generatorPathFile), X3MLGeneratorPolicy.createUUIDSource(UUID_SIZE));
        }
        Element doc = createDocument(new ByteArrayInputStream(task.getBytes()));
        X3MLEngine.Output output = engine.execute(doc, policy);
        return output;

    }

    private Element createDocument(InputStream in) throws ParserConfigurationException, SAXException, IOException {
        return documentBuilderFactory().newDocumentBuilder().parse(in).getDocumentElement();
    }

    private DocumentBuilderFactory documentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory;
    }

    private X3MLEngine createEngine(File mappingsFile) throws FileNotFoundException {
        return X3MLEngine.load(new FileInputStream(mappingsFile));
    }

    private byte[] getBytes(URL url) throws IOException {

        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try (InputStream inputStream = url.openStream()) {
            int n = 0;
            byte[] buffer = new byte[1024];
            while (-1 != (n = inputStream.read(buffer))) {
                data.write(buffer, 0, n);
            }
        }
        return data.toByteArray();
    }

}
