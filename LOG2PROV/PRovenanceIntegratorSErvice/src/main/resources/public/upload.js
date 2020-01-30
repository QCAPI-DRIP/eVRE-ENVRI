var wfObject;
// Example of object:
// {
//     "services":[
//        {
//           "name":"GETIMAGE",
//           "endpoint":"http://jang.lab130.uvalight.net:8080",
//           "method":"GET",
//           "startTime":1551373890583,
//           "endTime":1551373890611,
//           "resource":"jang.lab130.uvalight.net"
//        },
//        {
//           "name":"heavy_cpu",
//           "endpoint":"http://jang.lab130.uvalight.net:8080/exhaustCPU",
//           "method":"POST",
//           "startTime":1551373890618,
//           "endTime":1551373897627,
//           "resource":"jang.lab130.uvalight.net"
//        },
//        {
//           "name":"heavy_mem",
//           "endpoint":"http://jang.lab130.uvalight.net:8080/exhaustMEM",
//           "method":"POST",
//           "startTime":1551373897643,
//           "endTime":1551373897754,
//           "resource":"jang.lab130.uvalight.net"
//        }
//     ],
//     "workflow":{
//        "startTime":1551373890516,
//        "endTime":1551373897762
//     }
//  }

function getFormData(fileID, formData) {
    var x = document.getElementById(fileID);

    if ('files' in x) {
        for (var i = 0; i < x.files.length; i++) {
            var file = x.files[i];
            formData.append("files", file);
        }
    }

    return formData;
}


function uploadAll() {
    var formData = new FormData();
    formData = getFormData("provUpload", formData);
    formData = getFormData("serviceLogUpload", formData);
    document.getElementById('uploadBtn').disabled = true;
    var innerHTML = window.location.href.split('/');
    innerHTML.pop();
    innerHTML = innerHTML.join('/');
    var xhr = new XMLHttpRequest();
    xhr.open("POST", innerHTML + '/uploadFile', false);
    xhr.send(formData);

    wfObject = JSON.parse(xhr.responseText);
    
    
    drawTable(wfObject);
};

function checkAll(checked){
    var rows = document.getElementById('output_table').rows;
    for(var i = 0; i < rows.length; i++){
        rows.item(i).cells[0].childNodes[0].checked = checked;
    }
}

function drawTable(resultObject) {
    var serviceArray = resultObject.services;
    var table = document.getElementById('output_table');
    table.setAttribute('workflow', JSON.stringify(resultObject.workflow));
    var startTime, endTime;

    var element, row;
    for (var i = 0; i < serviceArray.length; i++) {
        element = serviceArray[i];
        element.resource = element.endpoint.replace(/:[0-9]+(?:\/.*)?/, '').replace(/http:\/\//, '');

        row = table.insertRow(i + 1);
        row.setAttribute('data-rest', element.name);
        row.insertCell(0).innerHTML = "<input type=\"checkbox\">";
        row.insertCell(1).innerHTML = element.name;
        row.insertCell(2).innerHTML = element.endpoint;
        row.insertCell(3).innerHTML = element.method;
        row.insertCell(4).innerHTML = printTime(new Date(element.startTime));
        row.insertCell(5).innerHTML = printTime(new Date(element.endTime));
    }

    document.getElementById('demo_output').style.display = "block";
}

function printTime(timestamp) {
    return timestamp.getFullYear() + "-" +
        timestamp.getMonth() + "-" +
        timestamp.getDate() + " " +
        timestamp.getHours() + ":" +
        timestamp.getMinutes() + ":" +
        timestamp.getSeconds() + "." +
        timestamp.getMilliseconds();
}

function move(docID, to, ctxName, json) {
    var elem = document.getElementById('bar' + docID);
    var width = 0;
    var id = setInterval(frame, 4);
    function frame() {
        if (width >= 100) {
            clearInterval(id);
            document.getElementById("myP" + docID).className = "w3-text-green w3-animate-opacity";
            document.getElementById("myP" + docID).innerHTML = 'Successfully created ' + to + ' ' + ctxName + ' context';

            if (docID === '3') {
                document.getElementById('uploadBtn').disabled = false;
                document.getElementById('source').value = JSON.stringify(json, undefined, 2);
            }


        } else {
            width++;
            elem.style.width = width + '%';
            var num = width * 1 / to;
            num = num.toFixed(0);
            document.getElementById('demo' + docID).innerHTML = num;
        }
    }
}