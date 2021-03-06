package eu.vre4eic.eVRETaverna;

import java.util.ArrayList;
import java.util.List;

import net.sf.taverna.t2.visit.VisitReport;
import net.sf.taverna.t2.visit.VisitReport.Status;
import net.sf.taverna.t2.workflowmodel.health.HealthCheck;
import net.sf.taverna.t2.workflowmodel.health.HealthChecker;

/**
 * Example health checker
 * 
 */
public class VRE4EICActivityHealthChecker implements
		HealthChecker<VRE4EICActivity> {

	public boolean canVisit(Object o) {
		
		return o instanceof VRE4EICActivity;
	}

	public boolean isTimeConsuming() {
		
		return false;
	}

	public VisitReport visit(VRE4EICActivity activity, List<Object> ancestry) {
		VRE4EICActivityConfigurationBean config = activity.getConfiguration();

		
		List<VisitReport> subReports = new ArrayList<VisitReport>();

		if (!config.getResourceUri().isAbsolute()) {
			// Report Severe problems we know won't work
			VisitReport report = new VisitReport(HealthCheck.getInstance(),
					activity, "Example URI must be absolute", HealthCheck.INVALID_URL,
					Status.SEVERE);
			subReports.add(report);
		}

		if (config.getResourceName().equals("")) {
			// Warning on possible problems
			subReports.add(new VisitReport(HealthCheck.getInstance(), activity,
					"Example string empty", HealthCheck.NO_CONFIGURATION,
					Status.WARNING));
		}

		
		return new VisitReport(HealthCheck.getInstance(), activity,
				"Example service OK", HealthCheck.NO_PROBLEM, subReports);
	}

}
