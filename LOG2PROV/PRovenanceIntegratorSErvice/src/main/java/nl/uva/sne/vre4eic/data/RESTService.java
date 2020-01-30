package nl.uva.sne.vre4eic.data;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RESTService {
    private String name, endpoint, httpMethod;
    private Date startTime, endTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String toString(){
        return "{" + "\"name\":" + "\"" + getName() + "\"" + ", "
                + "\"endpoint\":" + "\"" + getEndpoint() + "\"" + ", "
                + "\"method\":" + "\"" + getHttpMethod() + "\"" + ", "
                + "\"startTime\":" + getStartTime().getTime() + ", "
                + "\"endTime\":" + getEndTime().getTime()
                + "}";
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

     public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
