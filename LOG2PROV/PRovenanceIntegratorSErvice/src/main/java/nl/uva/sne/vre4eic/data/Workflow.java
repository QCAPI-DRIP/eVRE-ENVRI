package nl.uva.sne.vre4eic.data;

import java.util.Date;

public class Workflow {
    public Workflow(Date startTime, Date endTime){
        setStartTime(startTime);
        setEndTime(endTime);
    }

    private Date startTime, endTime;

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

    public String toString(){
        return "{" + "\"startTime\":" + getStartTime().getTime() + ", "
                + "\"endTime\":" + getEndTime().getTime()
                + "}";
    }
}
