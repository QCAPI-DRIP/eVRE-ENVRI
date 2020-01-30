package nl.uva.sne.vre4eic.prise.service;

import nl.uva.sne.vre4eic.data.RESTService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;

public class WorkflowParser {
    private final String
            PROCESSORS_TAG = "processors",
            ARTIFCAT_TAG = "artifact",
            REST_TAG = "rest-activity",
            NAME_TAG = "name",
            EP_TAG = "urlSignature",
            HTTP_TAG = "httpMethod",
            ACTIVITIES_TAG = "activities";


    private ArrayList<RESTService> servicelist;
    private Document doc;

    public WorkflowParser(File inputFile) throws Exception {
        servicelist = new ArrayList();

        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
        }catch (Exception e){
            throw e;
        }
    }

    public ArrayList<RESTService> extractServices(){
        NodeList nodes = getProcessorNodes(doc);

        Node currNode;
        Element currElement;
        RESTService restService;

        String type;
        for(int i = 0; i < nodes.getLength(); i++){
            currNode = nodes.item(i);

            if(typeElement(currNode)){
                currElement = (Element) currNode;
                type = getContext(getActivityElement(currElement), ARTIFCAT_TAG);

                if(type.equals(REST_TAG)){
                    restService = new RESTService();
                    restService.setName(getContext(currElement, NAME_TAG));
                    restService.setEndpoint(getContext(currElement, EP_TAG));
                    restService.setHttpMethod(getContext(currElement, HTTP_TAG));

                    servicelist.add(restService);
                }
            }
        }

        return servicelist;
    }

    public ArrayList<RESTService> getServicelist(){
        return servicelist;
    }

    private NodeList getProcessorNodes(Document inputdoc){
        return inputdoc.getElementsByTagName(PROCESSORS_TAG).item(0).getChildNodes();
    }

    private Element getActivityElement(Element inputElement) {
        Node n = inputElement.getElementsByTagName(ACTIVITIES_TAG).item(0).getChildNodes().item(0);

        if (typeElement(n)) {
            return (Element) n;
        }

        return null;
    }

    private boolean typeElement(Node n){
        return n.getNodeType() == Node.ELEMENT_NODE;
    }

    private String getContext(Element e, String tag){
        return e.getElementsByTagName(tag).item(0).getTextContent();
    }
}
