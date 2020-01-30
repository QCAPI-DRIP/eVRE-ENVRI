#!/bin/bash

cd PRovenanceIntegratorSErvice
mvn clean package && cp target/ProvenanceIntegrationServcie-0.0.1-SNAPSHOT.jar ../docker/PRovenanceIntegratorSErvice

cd ../docker/PRovenanceIntegratorSErvice/

sudo docker build -t prise .


cd ../../

cp -r PyLog2Prov/ docker/PyLog2Prov/

cd docker/PyLog2Prov/

sudo docker build -t pylog2prov .
