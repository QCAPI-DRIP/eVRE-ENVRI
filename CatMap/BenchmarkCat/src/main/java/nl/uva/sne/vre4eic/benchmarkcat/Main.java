/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.benchmarkcat;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxConsistency;
import io.micrometer.influx.InfluxMeterRegistry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author S. Koulouzis
 */
public class Main {

    private static final String CAT_BASE_URL = "http://localhost:8083/catalogue_mapper/";
    private static final String D4SCIENEC_CKAN = "https://ckan-d4s.d4science.org/";
    private static final String[] MAPPING_115 = new String[]{"https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/Mapping115.x3ml", "https://raw.githubusercontent.com/skoulouzis/eVRECatalogueIntegration/master/etc/CERIF-generator-policy-v5___21-08-2018124405___12069.xml"};
    private static final int LIMIT = 10;
    private static final String QUEUE_NAME = "metadata_records";
    private static final String RABBIT_API_URL = "http://localhost:15672/api/consumers/%2F";
    private static final String RABBIT_USER = "guest";
    private static final String RABBIT_PASS = "guest";
    private static final String INFLUXDB_URI = "http://localhost:8086";
    private static InfluxConfig influxConfig;
    private static InfluxMeterRegistry meterRegistry;

    private static void init() {
        if (INFLUXDB_URI == null) {
            influxConfig = InfluxConfig.DEFAULT;
        } else {
            influxConfig = new InfluxConfig() {
                @Override
                public String prefix() {
                    return InfluxConfig.super.prefix(); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public String db() {
                    return InfluxConfig.super.db(); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public InfluxConsistency consistency() {
                    return InfluxConfig.super.consistency(); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public String userName() {
                    return InfluxConfig.super.userName(); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public String password() {
                    return InfluxConfig.super.password(); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public String retentionPolicy() {
                    return InfluxConfig.super.retentionPolicy(); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public String retentionDuration() {
                    return InfluxConfig.super.retentionDuration(); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public Integer retentionReplicationFactor() {
                    return InfluxConfig.super.retentionReplicationFactor(); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public String retentionShardDuration() {
                    return InfluxConfig.super.retentionShardDuration(); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public String uri() {
                    return INFLUXDB_URI;
                }

                @Override
                public boolean compressed() {
                    return InfluxConfig.super.compressed(); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public boolean autoCreateDb() {
                    return InfluxConfig.super.autoCreateDb(); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public String get(String key) {
                    return null;
                }
            };
        }
        meterRegistry = new InfluxMeterRegistry(influxConfig, Clock.SYSTEM);

    }

    public static void main(String[] args) throws InterruptedException {
        try {
            init();
            benchmarkConversion(D4SCIENEC_CKAN, MAPPING_115, UUID.randomUUID().toString());

            meterRegistry.close();
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

        Timer.Sample hbenchmarkConversionTimer = Timer.start(meterRegistry);
        ConvertControllerClient convertClient = new ConvertControllerClient(CAT_BASE_URL);
        String resp = convertClient.convert(catalogueURL, mapping[0], mapping[1], String.valueOf(LIMIT), exportID);
        System.err.println(resp);

        String folderName = mappingName + "/" + exportID;
        JSONArray res = convertClient.listResults(folderName);
        int count = (res.length() - 1) / 2;
        Counter counter = meterRegistry.counter("benchmarkConversion." + Main.class.getName() + ".records.converted", tags);
        counter.increment(count);
        while (count < LIMIT) {
            res = convertClient.listResults(folderName);
            count = (res.length() - 1) / 2;
            counter.increment(count);
            Thread.sleep(500);
        }
        hbenchmarkConversionTimer.stop(meterRegistry.timer("benchmarkConversion." + Main.class.getName(), tags));
        counter.close();

//        System.err.println(((res.length()-1) / 2));
    }

}
