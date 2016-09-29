package reporter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import models.Hudson;
import models.Job;
import utils.PropertyFileReader;

public class Reporter {

	public static void main(String[] args) {

		new Reporter().generateReports();

	}

	/**
	 * This method generates all the reports
	 *
	 */
	public void generateReports() {

		// getting properties from poperty file
		PropertyFileReader pr = new PropertyFileReader("reporter.properties");

		// getting hudson home
		String hudsonHomeDir = pr.getPropertyValue(pr.HUDSON_HOME);

		// create Hudson instance
		Hudson hudsonInstance = new Hudson(hudsonHomeDir);

		// get all hudson jobs
		List<Job> allJobs = hudsonInstance.getJobs();

		// general report
		String reportFileName = pr.getPropertyValue(pr.GENERAL_CSV_REPORT_FILE);
		writeJobsToCSVFile(allJobs, reportFileName);

		// size report
		reportFileName = pr.getPropertyValue(pr.JOBS_BIGGER_THAN_THRESHOLD_KB_CVS_REPORT);
		String sizeThresholdStr = pr.getPropertyValue(pr.TRESHOLD_KILOBYTES);
		List<Job> bigJobs = getJobsBiggerThanThreshold(allJobs, sizeThresholdStr);
		writeJobsToCSVFile(bigJobs, reportFileName);

		// disabled jobs
		reportFileName = pr.getPropertyValue(pr.DISABLED_JOBS_CSV_REPORT);
		List<Job> disabledJobs = getDisabledJobs(allJobs);
		writeJobsToCSVFile(disabledJobs, reportFileName);

		// jobs without config file
		reportFileName = pr.getPropertyValue(pr.JOBS_WITHOUT_CONFIG_FILE_CSV_REPORT_FILE);
		List<Job> noConfigFileJobs = getJobsWithoutConfigFile(allJobs);
		writeJobsToCSVFile(noConfigFileJobs, reportFileName);

		// jobs without config file
		reportFileName = pr.getPropertyValue(pr.JOBS_RUN_MORE_THAN_ONE_MONTH_AGO_CVS_REPORT);
		List<Job> oldJobs = getJobsOlderThanAMonth(allJobs);
		writeJobsToCSVFile(oldJobs, reportFileName);

	}

	private List<Job> getJobsBiggerThanThreshold(List<Job> jobs, String threshold) {

		if (jobs == null || threshold == null) {
			return null;
		}

		long thresholdL = Long.parseLong(threshold);

		return jobs.stream().filter(job -> job.getDiskSpaceSize() > thresholdL).collect(Collectors.toList());

	}

	private List<Job> getJobsOlderThanAMonth(List<Job> jobs) {

		// get Date from a month ago
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		Date oneMonthAgo = cal.getTime();
		return jobs.stream()
				.filter(job -> job.getLastRunDate() != null && job.getLastRunDate().compareTo(oneMonthAgo) == -1)
				.collect(Collectors.toList());

	}

	private List<Job> getDisabledJobs(List<Job> jobs) {

		return jobs.stream().filter(job -> job.isDisabled() != null && job.isDisabled()).collect(Collectors.toList());

	}

	private List<Job> getJobsWithoutConfigFile(List<Job> jobs) {

		return jobs.stream().filter(job -> job.hasConfigFile()).collect(Collectors.toList());

	}

	/**
	 * Converts a List of Job objects into a CSV file containing the data of all
	 * jobs each job per line
	 *
	 * @exception FileNotFoundException
	 *                if the csv file cant be created
	 */
	private void writeJobsToCSVFile(List<Job> hudsonJobs, String csvFilePath) {
		

		if (hudsonJobs == null || csvFilePath == null) {
			return;
		}
		
		System.out.println("Generating report file : " + csvFilePath);

		String csvRows = "'Team','Name','Owner','Description','Disk Size KB','Creation Date','Last Run Date','Disabled','Running'";

		PrintWriter reportPW;
		try {
			reportPW = new PrintWriter(new FileOutputStream(csvFilePath, false));
			reportPW.write(csvRows + "\n");
			hudsonJobs.stream().map((hudsonJob) -> jobToCVSLine(hudsonJob)).forEach((csvLine) -> {
				reportPW.write(csvLine);
			});

			reportPW.close();
		} catch (FileNotFoundException ex) {
			System.out.println(ex.getMessage());
		}

	}

	/**
	 * Converts a job object into a CSV line
	 *
	 * @param job
	 *            , a Job object
	 * @return a csv string representation of the job or <code>null</code> if
	 *         the provided Job object is null
	 */
	private String jobToCVSLine(Job job) {

		if (job == null) {

			return null;
		}

		String team = job.getTeamName();
		String name = job.getJobName();

		String owner = job.getCreatedBy();
		owner = owner == null ? "" : owner;

		String description = job.getDescription();
		description = description == null ? "" : description.replace("'", "");

		String size = Long.toString(job.getDiskSpaceSize());

		Date creationDate = job.getCreationDate();
		String creationDateStr = creationDate == null ? "" : creationDate.toString();

		Date lastRunDate = job.getLastRunDate();
		String lastRunDateStr = lastRunDate == null ? "" : lastRunDate.toString();

		Boolean disabled = job.isDisabled();
		String disabledStr = disabled == null ? "" : disabled.toString();

		Boolean inExecution = job.isJobInExecution();
		String runningStr = inExecution == null ? "" : inExecution.toString();

		return "'" + team + "','" + name + "','" + owner + "','" + description + "','" + size + "','" + creationDateStr
				+ "','" + lastRunDateStr + "','" + disabledStr + "','" + runningStr + "'\n";

	}

}
