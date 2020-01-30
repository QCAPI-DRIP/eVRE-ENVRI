var resources, resourceColors;
var colors = ['#33cc33', '#ff0000', '#6699ff', '#ffff1a', '#cc00cc']
var graphs, plotData;

function visualizeData(data) {
    console.log(wfObject);
    plotData = data;
    resourceColors = createColorDictionary(plotData.resources);
    
    plotDyGraphs();

    drawTimeline(wfObject);
    
}

function createColorDictionary(resources) {
    var result = {};
    result.colorArray = [];

    for (var i = 0; i < resources.length; i++) {
        result[resources[i]] = colors[i % colors.length];
        result.colorArray.push(colors[i % colors.length]);
    }

    return result;
}

function drawTimeline(resultObject) {
    google.charts.load('current', { 'packages': ['timeline'] });
    google.charts.setOnLoadCallback(drawChart);

    function drawChart() {
        var dataTable = new google.visualization.DataTable();
        dataTable.addColumn('string', 'Task ID');
        dataTable.addColumn('string', 'Resource');
        dataTable.addColumn({ type: 'string', id: 'style', role: 'style' });
        dataTable.addColumn('date', 'Start');
        dataTable.addColumn('date', 'End');

        resultObject.services.forEach(element => {
            dataTable.addRow([element.resource,
            element.name,
            resourceColors[element.resource],
            new Date(element.startTime),
            new Date(element.endTime)
            ]);
        });

        var container = document.getElementById('timeline');
        var chart = new google.visualization.Timeline(container);
        chart.draw(dataTable);
    }
}

function plotDyGraphs() {
    var range = {
        minValue: new Date(wfObject.workflow.startTime),
        maxValue: new Date(wfObject.workflow.endTime)
    }

    if (!graphs) {
        graphs = {};
    }

    document.getElementById('plot_div').style.display = "block";
    graphs.cpu = new Dygraph(document.getElementById("plot_cpu"), plotData.cpu, {
        labels: ["time"].concat(plotData.resources), 
        colors: resourceColors.colorArray,
        labelsSeparateLines: true,
        ylabel: "Usage percentage",
        fillGraph: true
    });
    graphs.mem = new Dygraph(document.getElementById("plot_mem"), plotData.mem, {
        labels: ["time"].concat(plotData.resources), 
        colors: resourceColors.colorArray,
        ylabel: "Usage in MBs",
        fillGraph: true
    });
    graphs.net_in = new Dygraph(document.getElementById("plot_net_in"), plotData.net_in, {
        labels: ["time"].concat(plotData.resources), 
        colors: resourceColors.colorArray,
        ylabel: "KB/s",
        fillGraph: true
    });
    graphs.net_out = new Dygraph(document.getElementById("plot_net_out"), plotData.net_out, {
        labels: ["time"].concat(plotData.resources), 
        colors: resourceColors.colorArray,
        ylabel: "KB/s",
        fillGraph: true
    });

    return graphs;
}

function hex2rgba(hex,opacity){
    hex = hex.replace('#','');
    r = parseInt(hex.substring(0,2), 16);
    g = parseInt(hex.substring(2,4), 16);
    b = parseInt(hex.substring(4,6), 16);

    result = 'rgba('+r+','+g+','+b+','+opacity/100+')';
    return result;
}

function highlight(data_type) {
    var checked, graph;
    checked = document.getElementById('chk_highlight_' + data_type).checked;
    graph = graphs[data_type];

    graph.updateOptions({
        underlayCallback: function (canvas, area, g) {
            wfObject.services.forEach(service => {
                var bottom_left = g.toDomCoords(new Date(service.startTime), g.yAxisRange()[0]);
                var top_right = g.toDomCoords(new Date(service.endTime), g.yAxisRange()[0]);
                var left = bottom_left[0];
                var right = top_right[0];

                if (checked) {
                    canvas.fillStyle = hex2rgba(resourceColors[service.resource], 20);
                } else {
                    canvas.fillStyle = '#ffffff';
                }

                canvas.fillRect(left, area.y, right - left, area.h);
            });
        }
    });
}