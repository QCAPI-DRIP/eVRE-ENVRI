FROM openjdk:8

ADD cat_worker-1.0-SNAPSHOT-jar-with-dependencies.jar /

CMD java -Dlog4j.configurationFile=log4j.properties -jar /cat_worker-1.0-SNAPSHOT-jar-with-dependencies.jar -r ${RABBIT_HOST} -w ${WEBDAV_HOST} -p ${WEBDAV_PASSWORD} -u ${WEBDAV_USERNAME} -i ${INFLUX_URI}
