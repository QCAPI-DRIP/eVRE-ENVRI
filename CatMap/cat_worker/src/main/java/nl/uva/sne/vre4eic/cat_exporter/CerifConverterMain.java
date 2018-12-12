/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.cat_exporter;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class CerifConverterMain {

    private static String rabbitMQHost;
    private static String webdavHost;
    private static String webdavUser;
    private static String webdavPass;
    private static String influxDB;

    public static void main(String[] argv) {
        try {
            Options options = new Options();
            Option rabbitHostOption = new Option("r", "rabbit_host", true, "rabbitMQ host");
            rabbitHostOption.setRequired(true);
            options.addOption(rabbitHostOption);

//            Option zookeeperHostOption = new Option("z", "zookeeper_host", true, "zookeeper host");
//            zookeeperHostOption.setRequired(true);
//            options.addOption(zookeeperHostOption);
            Option webdavHostOption = new Option("w", "webdav_host", true, "webdav host");
            webdavHostOption.setRequired(true);
            options.addOption(webdavHostOption);

            Option webdavUserOtion = new Option("u", "webdav_user", true, "webdav user");
            webdavUserOtion.setRequired(false);
            options.addOption(webdavUserOtion);

            Option webdavUPassOtion = new Option("p", "webdav_pass", true, "webdav pass");
            webdavUPassOtion.setRequired(false);
            options.addOption(webdavUPassOtion);

            Option influxOtion = new Option("i", "influxDB_URI", true, "influxDB URI");
            webdavUPassOtion.setRequired(false);
            options.addOption(influxOtion);

//
//            Option mappings = new Option("m", "mappings", true, "mappings path");
//            mappings.setRequired(true);
//            options.addOption(mappings);
//
//            Option generator = new Option("g", "generator", true, "generator path policy");
//            generator.setRequired(true);
//            options.addOption(generator);
            CommandLineParser parser = new BasicParser();
            HelpFormatter formatter = new HelpFormatter();
            CommandLine cmd;

            try {
                cmd = parser.parse(options, argv);
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                formatter.printHelp("utility-name", options);
                Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, "------------EXIT----------------");
                System.exit(1);
                return;
            }

            rabbitMQHost = cmd.getOptionValue("rabbit_host");
            Logger.getLogger(CerifConverterMain.class.getName()).log(Level.INFO, "rabbitMQ host: {0}", rabbitMQHost);

//            zookeeperHost = cmd.getOptionValue("zookeeper_host");
//            Logger.getLogger(CerifConverterMain.class.getName()).log(Level.INFO, "zookeeper host: {0}", zookeeperHost);
            webdavHost = cmd.getOptionValue("webdav_host");
            Logger.getLogger(CerifConverterMain.class.getName()).log(Level.INFO, "webdav host: {0}", webdavHost);

            webdavUser = cmd.getOptionValue("webdav_user");
            Logger.getLogger(CerifConverterMain.class.getName()).log(Level.INFO, "webdav host: {0}", webdavUser);

            webdavPass = cmd.getOptionValue("webdav_pass");
            Logger.getLogger(CerifConverterMain.class.getName()).log(Level.INFO, "webdav host: {0}", webdavPass);
            
            influxDB = cmd.getOptionValue("influxDB_URI");
            Logger.getLogger(CerifConverterMain.class.getName()).log(Level.INFO, "influxDB URI: {0}", influxDB);

            String taskQName = "metadata_records";
            File output = new File(System.getProperty("java.io.tmpdir") + File.separator + "cerif");
            output.mkdirs();
            new Worker(rabbitMQHost, webdavHost, webdavUser, webdavPass, taskQName, output.getAbsolutePath(),influxDB).consume();
//            new Executor(rabbitMQHost, zookeeperHost, webdavHost, webdavUser, webdavPass, "/catmap_conf").run();

//            mappingsPath = cmd.getOptionValue("mappings");
//            Logger.getLogger(CerifConverterWorker.class.getName()).log(Level.INFO, "mappings path: {0}", mappingsPath);
//            generatorPathPolicy = cmd.getOptionValue("generator");
//            Logger.getLogger(CerifConverterWorker.class.getName()).log(Level.INFO, "generator policy path: {0}", generatorPathPolicy);
//            consume();
        } catch (IOException | IllegalArgumentException | TimeoutException ex) {
            Logger.getLogger(CerifConverterMain.class.getName()).log(Level.SEVERE, null, ex);
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, "------------EXIT----------------");
            System.exit(-1);
        }

    }
}
