package nl.uva.sne.vre4eic.prise.service;

import nl.uva.sne.vre4eic.data.RESTService;
import nl.uva.sne.vre4eic.data.Workflow;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.query.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class ProvParser {
    Model model;
    Workflow workflow;

    public ProvParser(File inputProv, ArrayList<RESTService> RESTServices) throws FileNotFoundException {
        try {
            parseProv(new FileInputStream(inputProv), RESTServices);
        } catch (FileNotFoundException e) {
            throw e;
        }
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void parseProv(FileInputStream inputFile, ArrayList<RESTService> RESTServices){
        model = ModelFactory.createDefaultModel();
        model.read(inputFile, null,"TTL");

        for(RESTService s : RESTServices){
            Query query = QueryFactory.create(queryExecutionTimes(s.getName()));
            QueryExecution qe = QueryExecutionFactory.create(query, model);
            ResultSet rs = qe.execSelect();

            while(rs.hasNext()){
                QuerySolution sol = rs.nextSolution();

                XSDDateTime startTime = (XSDDateTime) sol.get("startTime").asLiteral().getValue();
                XSDDateTime endTime = (XSDDateTime) sol.get("endTime").asLiteral().getValue();

                s.setStartTime(startTime.asCalendar().getTime());
                s.setEndTime(endTime.asCalendar().getTime());
            }
        }

        Query query = QueryFactory.create(queryWorkflowExecution());
        QueryExecution qe = QueryExecutionFactory.create(query, model);
        QuerySolution sol = qe.execSelect().nextSolution();
        XSDDateTime wfStart = (XSDDateTime) sol.get("startTime").asLiteral().getValue();
        XSDDateTime wfEnd = (XSDDateTime) sol.get("endTime").asLiteral().getValue();
        this.workflow = new Workflow(wfStart.asCalendar().getTime(), wfEnd.asCalendar().getTime());
    }

    String queryExecutionTimes(String serviceName){
        return  "PREFIX prov: <http://www.w3.org/ns/prov#> " +
                "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#> " +
                "SELECT ?startTime ?endTime " +
                "WHERE { " +
                    "?y  rdfs:label  \"Processor execution " + serviceName + "\"@en . "+
                    "?y  prov:startedAtTime  ?startTime . "+
                    "?y  prov:endedAtTime  ?endTime . " +
                "}";

    }

    String queryWorkflowExecution(){
        return  "PREFIX prov: <http://www.w3.org/ns/prov#> " +
                "PREFIX wfprov: <http://purl.org/wf4ever/wfprov#> " +
                "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "SELECT ?startTime ?endTime " +
                "WHERE { " +
                    "?y  rdf:type wfprov:WorkflowRun . "+
                    "?y  prov:startedAtTime  ?startTime . "+
                    "?y  prov:endedAtTime  ?endTime . " +
                "}";

    }
}
