FROM openjdk:8-jdk-alpine
COPY ./target/ProvenanceIntegrationServcie-0.0.1-SNAPSHOT.war /usr/app/
WORKDIR /usr/app
EXPOSE 8081
RUN sh -c 'touch ProvenanceIntegrationServcie-0.0.1-SNAPSHOT.war'
ENTRYPOINT ["java","-jar","ProvenanceIntegrationServcie-0.0.1-SNAPSHOT.war"]
