/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.benchmarkcat;

//import io.micrometer.core.instrument.Clock;
//import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tag;
import java.io.File;
import java.io.FileOutputStream;
//import io.micrometer.influx.InfluxConfig;
//import io.micrometer.influx.InfluxConsistency;
//import io.micrometer.influx.InfluxMeterRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;

/**
 *
 * @author S. Koulouzis
 */
public class Main {

    private static final String HOST = "drip.vlan400.uvalight.net";//"drip.vlan400.uvalight.net"; //"localhost"
    private static final String CAT_BASE_URL = "http://" + HOST + ":8083/catalogue_mapper/";  //"http://" + HOST + ":8083/catalogue_mapper/"; //"http://" + HOST + ":8083/catalogue_mapper/";
    private static final String D4SCIENEC_CKAN = "https://ckan-d4s.d4science.org/";
    private static final String[] MAPPING_115 = new String[]{"https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/Mapping115.x3ml", "https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/CERIF-generator-policy-v5___21-08-2018124405___12069.xml"};
    private static final int LIMIT = 30;
    private static final String QUEUE_NAME = "metadata_records";
    private static final String RABBIT_API_URL = "http://" + HOST + ":15672/api/consumers/%2F";
    private static final String RABBIT_USER = "guest";
    private static final String RABBIT_PASS = "guest";
    private static final String INFLUXDB_URI = "http://" + HOST + ":8086";
//    private static InfluxConfig influxConfig;
//    private static InfluxMeterRegistry meterRegistry;

    private static void init() {
//        if (INFLUXDB_URI == null) {
//            influxConfig = InfluxConfig.DEFAULT;
//        } else {
//            influxConfig = new InfluxConfig() {
//                @Override
//                public String prefix() {
//                    return InfluxConfig.super.prefix(); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public String db() {
//                    return InfluxConfig.super.db(); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public InfluxConsistency consistency() {
//                    return InfluxConfig.super.consistency(); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public String userName() {
//                    return InfluxConfig.super.userName(); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public String password() {
//                    return InfluxConfig.super.password(); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public String retentionPolicy() {
//                    return InfluxConfig.super.retentionPolicy(); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public String retentionDuration() {
//                    return InfluxConfig.super.retentionDuration(); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public Integer retentionReplicationFactor() {
//                    return InfluxConfig.super.retentionReplicationFactor(); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public String retentionShardDuration() {
//                    return InfluxConfig.super.retentionShardDuration(); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public String uri() {
//                    return INFLUXDB_URI;
//                }
//
//                @Override
//                public boolean compressed() {
//                    return InfluxConfig.super.compressed(); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public boolean autoCreateDb() {
//                    return InfluxConfig.super.autoCreateDb(); //To change body of generated methods, choose Tools | Templates.
//                }
//
//                @Override
//                public String get(String key) {
//                    return null;
//                }
//
//                @Override
//                public Duration step() {
//                    return Duration.ofMillis(200);
//                }
//            };
//        }
//        meterRegistry = new InfluxMeterRegistry(influxConfig, Clock.SYSTEM);

    }

    public static void main(String[] args) throws InterruptedException {
        try {
            init();
            for (int i = 0; i < 2; i++) {

                benchmarkConversion(D4SCIENEC_CKAN, MAPPING_115, UUID.randomUUID().toString());

            }
//            meterRegistry.close();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void benchmarkConversion(String catalogueURL, String[] mapping, String exportID) throws IOException, InterruptedException {
        String mappingName = mapping[0].substring(mapping[0].lastIndexOf("/") + 1, mapping[0].lastIndexOf("."));
        Collection<Tag> tags = new ArrayList<>();
        tags.add(Tag.of("source", catalogueURL));
        tags.add(Tag.of("mapping.name", mappingName));
        tags.add(Tag.of("exportID", exportID));
        int numOfConsumers = Util.getNumberOfConsumers(QUEUE_NAME, RABBIT_API_URL, RABBIT_USER, RABBIT_PASS);
        tags.add(Tag.of("num.of.consumers", String.valueOf(numOfConsumers)));
        tags.add(Tag.of("records.size", String.valueOf(LIMIT)));

        long start = System.currentTimeMillis();

        ConvertControllerClient convertClient = new ConvertControllerClient(CAT_BASE_URL);
        String resp = convertClient.convert(catalogueURL, mapping[0], mapping[1], String.valueOf(LIMIT), exportID);
        System.err.println(resp);

        String folderName = mappingName + "/" + exportID;
        JSONArray res = convertClient.listResults(folderName);
        int count = (res.length() - 1) / 2;
//        Counter startCounter = meterRegistry.counter("start.benchmark.conversion.", tags);
//        startCounter.increment();
//        AtomicInteger n = meterRegistry.gauge("numberGauge", new AtomicInteger(0));
        while (count < LIMIT) {
            res = convertClient.listResults(folderName);
            count = (res.length() - 1) / 2;
            System.err.println("Records: " + count);
            Thread.sleep(200);
        }
        String csvFileName = Main.class.getName() + ".csv";
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + csvFileName;
        File benchmarkFile = new File(filePath);
        StringBuilder csvHeader = new StringBuilder();
        StringBuilder csvLine = new StringBuilder();
        csvLine.append(start).append(",").append(System.currentTimeMillis()).append(",");
        for (Tag tag : tags) {
            csvLine.append(tag.getValue()).append(",");
        }
        csvHeader.append("start").append(",").append("end").append(",");
        for (Tag tag : tags) {
            csvHeader.append(tag.getKey()).append(",");
        }
        csvLine.append("\n");
        csvHeader.append("\n");
        if (!benchmarkFile.exists()) {
            csvHeader.append(csvLine.toString());
            Files.write(Paths.get(filePath), csvHeader.toString().getBytes(), StandardOpenOption.CREATE);
        } else {
            Files.write(Paths.get(filePath), csvLine.toString().getBytes(), StandardOpenOption.APPEND);
        }
        Logger.getLogger(Main.class.getName()).log(Level.INFO, "Elapsed: {0}", System.currentTimeMillis() - start);
    }

}
