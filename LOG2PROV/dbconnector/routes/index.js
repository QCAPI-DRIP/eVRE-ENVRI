var express = require('express');
var axios = require('axios');
var URL = require("url").URL;
var router = express.Router();
var fs = require("fs");

/* GET home page. */
router.get('/metrics', function (req, res, next) {
  var resultOBJ = new Object();
  axios.all([
    getCPUdata(req.query.endpoint, req.query.startTime, req.query.endTime),
    getMEMdata(req.query.endpoint, req.query.startTime, req.query.endTime),
    getNETINdata(req.query.endpoint, req.query.startTime, req.query.endTime),
    getNETOUTdata(req.query.endpoint, req.query.startTime, req.query.endTime)
  ])
    .then(axios.spread(function (cpu, mem, netin, netout) {
      resultOBJ.cpu = extractData(cpu.data.data.result,req.query.startTime, req.query.endTime);
      resultOBJ.mem = extractData(mem.data.data.result,req.query.startTime, req.query.endTime);
      resultOBJ.net_in = extractData(netin.data.data.result,req.query.startTime, req.query.endTime);
      resultOBJ.net_out = extractData(netout.data.data.result,req.query.startTime, req.query.endTime);

      res.send(resultOBJ);
    }))
    .catch(function (error) {
      console.log(error.stack);
      res.send(error.stack);
    });
});

function getCPUdata(endpoint, startTime, endTime) {
  var url = new URL(endpoint);

  url.pathname = '/api/v1/query_range';
  url.searchParams.set('query', 'sum(rate(container_cpu_usage_seconds_total{name=~".+"}[22s])) by (name) * 100');
  url.searchParams.append('start', startTime);
  url.searchParams.append('end', endTime);
  url.searchParams.append('step', 1);
  url.searchParams.append('timeout', '5s');

  return axios.get(url.href);
}

function getMEMdata(endpoint, startTime, endTime) {
  var url = new URL(endpoint);
  url.pathname = '/api/v1/query_range';
  url.searchParams.set('query', 'container_memory_usage_bytes{name=~".+"}');
  url.searchParams.append('start', startTime);
  url.searchParams.append('end', endTime);
  url.searchParams.append('step', 1);
  url.searchParams.append('timeout', '5s');

  return axios.get(url.href);
}

function getNETOUTdata(endpoint, startTime, endTime) {
  var url = new URL(endpoint);

  url.pathname = '/api/v1/query_range';
  url.searchParams.set('query', 'sum(rate(container_network_transmit_bytes_total{name=~".+"}[22s])) by (name)');
  url.searchParams.append('start', startTime);
  url.searchParams.append('end', endTime);
  url.searchParams.append('step', 1);
  url.searchParams.append('timeout', '5s');

  return axios.get(url.href);
}

function getNETINdata(endpoint, startTime, endTime) {
  var url = new URL(endpoint);

  url.pathname = '/api/v1/query_range';
  url.searchParams.set('query', 'sum(rate(container_network_receive_bytes_total{name=~".+"}[22s])) by (name)');
  url.searchParams.append('start', startTime);
  url.searchParams.append('end', endTime);
  url.searchParams.append('step', 1);
  url.searchParams.append('timeout', '5s');

  return axios.get(url.href);
}

function createEmptyData(startsec, endsec){
  var emptyArray = new Array();
  for(var i = startsec; i < endsec; i++){
    emptyArray.push([i, null]);
  }
  return emptyArray;
}

function populateData(data, startsec, endsec){
  var result = createEmptyData(startsec, endsec);

  data.forEach(([sec, value]) => {
    if(sec < endsec && sec > startsec){
      result[sec - startsec - 1][1] = value;
    }
  });
  
  return result;
}

function filterData(data) {
  var result;

  data.forEach(function (element) {
    var expr = /grafana|cAdvisor|cadvisor|nodeexporter|alertmanager|caddy|prometheus|influxdb/;
    if (!expr.test(element.metric.name)) {
      result = element.values;
    }
  })

  return result;
}

function extractData(responseData, startsec, endsec){
  var resultdata;

  resultdata = filterData(responseData);
  resultdata = populateData(resultdata,startsec,endsec);

  return resultdata;
}

module.exports = router;
