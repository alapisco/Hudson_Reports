package models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Job {

    private String teamName;
    private File directory;
    private Long diskSpaceSize;
    private String createdBy;
    private String creationDate;
    private Boolean disabled;
    private String description;
    private String lastRunDate;
    private boolean hasConfigFile = false;

    /**
     * Constructor.
     *
     * @param jobDirectory absolute path for the root directory of a job
     *
     */
    public Job(String jobDirectory) {

        if (jobDirectory == null) {
            throw new IllegalArgumentException("jobDirectory can not be null");
        }

        directory = new File(jobDirectory);

        if (!directory.exists()) {
            throw new IllegalArgumentException(directory + " doesnt exist");
        }

        parseConfigFile();
        parseRunmapFile();

    }

    /**
     * @return availability of a job. Disabled means that a job can not
     * currently be executed.
     */
    public Boolean isDisabled() {

        return this.disabled;
    }

    /**
     * @return true if the job has a configuration file or false if it does not
     */
    public boolean hasConfigFile() {

        return hasConfigFile;
    }

    /**
     * @return File object that represents the configuration file of a job or
     * <code>null</code> if it doesn't have a configuration file
     *
     */
    private File getConfigurationFile() {

        File configFile = new File(directory, "config.xml");
        if (configFile.exists()) {
            return configFile;
        }

        return null;
    }

    /**
     * @return the name of the job
     *
     */
    public String getJobName() {
        return directory.getName();
    }

    /**
     * @return the name of the team the job belongs to or <code>"public"</code>
     * if the job doesnt belong to a team.
     *
     */
    public String getTeamName() {

        if (this.teamName != null) {

            return teamName;

        }

        teamName = getTeamNameFromDirectory();
        return teamName;

    }

    /**
     * @return the description of the job.
     *
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the Hudson username who created the job.
     *
     */
    public String getCreatedBy() {

        return this.createdBy;
    }

    /**
     * @return the disk space in bytes that the job folder takes.
     *
     */
    public Long getDiskSpaceSize() {

        if (this.diskSpaceSize == null) {

            diskSpaceSize = this.getSize();
        }

        return diskSpaceSize;
    }

    /**
     * @return the creation date of the job in timestamp format
     *
     */
    public String getCreationDateTimestamp() {

        return this.creationDate;
    }

    /**
     * @return the creation date of the job in a Date object
     *
     */
    public Date getCreationDate() {

        String timestamp = getCreationDateTimestamp();

        if (timestamp == null) {
            return null;
        }
        Date creationDate;
        
        try{
        	
        	creationDate =  new Date(Long.parseLong(getCreationDateTimestamp()));
        	return creationDate;
        	
        }catch(NumberFormatException nfe){

        	return null;
        }

    }

    /**
     * @return the last run date of the job in a Date object
     *
     */
    public String getLastRunDateTimestamp() {

        return this.lastRunDate;

    }

    /**
     * @return the last run date of the job in timestamp format
     *
     */
    public Date getLastRunDate() {

        String timestamp = getLastRunDateTimestamp();

        if (timestamp == null) {
            return null;
        }

        return new Date(Long.parseLong(timestamp));
    }

    /**
     * @return <code>true</code> if the job is currently in execution or
     * <code>false</code> if it isn't
     *
     */
    public Boolean isJobInExecution() {

        File latestBuildDir = getLatestBuildDirectory(directory);

        if (latestBuildDir == null) {
            return null;
        }

        File buildFile = new File(latestBuildDir, "build.xml");

        List<String> lines;
        try {
            lines = Files.readAllLines(buildFile.toPath());
            for (String line : lines) {
                if (line.contains("<duration>0</duration>")) {
                    return true;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return false;

    }

    /**
     * @return the name of the team the job belong to using the job directory to
     * figure it out.
     *
     */
    private String getTeamNameFromDirectory() {

        File parentDirectory = directory.getParentFile();
        File grandParentDirectory = parentDirectory.getParentFile();

        if (grandParentDirectory.getParentFile().getName().equals("teams")) {

            return grandParentDirectory.getName();

        } else {

            return "public";

        }

    }

    public File getLatestBuildDirectory(File jobDirectory) {

        File parent = new File(jobDirectory, "builds");

        File[] buildDirectories = getSubDirectories(parent.getAbsolutePath());

        if (buildDirectories == null || buildDirectories.length == 0) {
            return null;
        }

        int latest = 0;
        for (File buildDirectory : buildDirectories) {

            if (buildDirectory.getName().contains("-")) {
                continue;
            }

            int buildDirectoryNumber = Integer.parseInt(buildDirectory.getName());

            if (buildDirectoryNumber > latest) {
                latest = buildDirectoryNumber;

            }

        }

        return new File(parent, Integer.toString(latest));

    }

    /**
     * Returns an array of Files of all the directories inside a parent
     * directory
     *
     * @param parent an absolute path to a directory
     * @return An array with the File reference to all the directories inside a
     * parent directory
     */
    public File[] getSubDirectories(String parent) {

        File parentDirectory = new File(parent);

        // Iterate through all team folders
        File[] directories = parentDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File parent, String child) {
                return new File(parent, child).isDirectory();
            }
        });

        return directories;
    }

    /**
     * Parses the job configuration file and sets the attributes: description ,
     * createdBy , creationDate and disabled
     *
     * @exception Exception if there is a problem parsing the configuration file
     */
    private void parseConfigFile() {

        File configurationFile = getConfigurationFile();

        if (configurationFile == null) {
            this.hasConfigFile=false;
            return;
        }

        String configFile = configurationFile.getAbsolutePath();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(configFile));
            Element root = document.getDocumentElement();

            NodeList rootNodes = root.getChildNodes();

            for (int i = 0; i < rootNodes.getLength(); i++) {

                if (rootNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {

                    String name = rootNodes.item(i).getNodeName();
                    String value = rootNodes.item(i).getTextContent();

                    if (name.equals("description")) {

                        this.description = value;

                    }

                    if (name.equals("createdBy")) {

                        this.createdBy = value;
                    }

                    if (name.equals("creationTime")) {

                        this.creationDate = value;
                    }

                    if (name.equals("disabled")) {

                        if (value.equals("true")) {
                            this.disabled = true;
                        } else if (value.equals("false")) {
                            this.disabled = false;
                        }

                    }

                }
            }

        } catch (ParserConfigurationException | SAXException | IOException | DOMException ex) {

            System.out.println(ex.getMessage());

        }

    }

    /**
     * Returns a File that represents the the runmap xml of the job
     *
     * @return a File representing the runmap
     */
    private File getRunmapFile() {

        File buildsFolder = new File(directory, "builds");
        File runmapFile = new File(buildsFolder, "_runmap.xml");

        if (runmapFile.exists()) {
            return runmapFile;
        }

        return null;
    }

    /**
     * Parses the job _runmap file and sets the attributes lastRunDate
     *
     * @exception Exception if there is a problem parsing the runmap file
     */
    private void parseRunmapFile() {

        File runmapFile = getRunmapFile();

        if (runmapFile == null) {
            return;
        }

        String runmapFileStr = runmapFile.getAbsolutePath();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(runmapFileStr));
            Element root = document.getDocumentElement();

            NodeList rootNodes = root.getChildNodes();

            int buildNodesIndex = -1;
            for (int i = 0; i < rootNodes.getLength(); i++) {

                if (rootNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    buildNodesIndex = i;
                    break;

                }

            }

            NodeList buildNodes = rootNodes.item(buildNodesIndex).getChildNodes();

            int lastBuildIndex = -1;
            for (int i = 0; i < buildNodes.getLength(); i++) {

                if (buildNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    lastBuildIndex = i;

                }

            }

            NodeList lastBuildNodes = buildNodes.item(lastBuildIndex).getChildNodes();

            for (int i = 0; i < lastBuildNodes.getLength(); i++) {

                if (lastBuildNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    String name = lastBuildNodes.item(i).getNodeName();
                    String value = lastBuildNodes.item(i).getTextContent();
                    if (name.equals("timestamp")) {

                        this.lastRunDate = value;
                        break;
                    }

                }

            }

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex);
        }

    }

    /**
     * Returns the size in bytes of the disk space that the job directory takes.
     * It will work only in Unix.
     *
     * @return the size in bytes of the job disk space
     */
    private long getSize() {

        String command = "du -h -k";
        ProcessBuilder pb = new ProcessBuilder(command.split(" "));
        pb.directory(directory);
        try {
            Process p = pb.start();
            InputStream is = p.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader bf = new BufferedReader(isr);
            String lastLine = "";
            String line;
            while (((line = bf.readLine()) != null)) {
                lastLine = line;
            }
            bf.close();
            String size = lastLine.split("\t")[0];
            return Long.parseLong(size);

        } catch (IOException ex) {

            System.out.println(ex.getMessage());
            return -1;

        }

    }

    @Override
    public String toString() {

        String team = getTeamName();
        String name = getJobName();

        String owner = getCreatedBy();
        owner = owner == null ? "" : owner;

        String description = getDescription();
        description = description == null ? "" : description;

        String size = Long.toString(getDiskSpaceSize());

        Date creationDate = getCreationDate();
        String creationDateStr = creationDate == null ? "" : creationDate.toString();

        Date lastRunDate = getLastRunDate();
        String lastRunDateStr = lastRunDate == null ? "" : lastRunDate.toString();

        Boolean disabled = isDisabled();
        Boolean enabled = disabled == null ? null : !disabled;

        Boolean inExecution = this.isJobInExecution();

        String enabledStr = enabled == null ? "" : enabled.toString();
        String runningStr = inExecution == null ? "" : inExecution.toString();

        return "Name:" + name + ","
                + "Team:" + team + ","
                //+ "Description: " + description +"\n"
                + "Owner: " + owner + ","
                + "Created on: " + getCreationDate() + ","
                + "Last time run: " + getLastRunDate() + ","
                + "Active:" + enabledStr + ","
                + "Running:" + runningStr + "\n";

    }

}
