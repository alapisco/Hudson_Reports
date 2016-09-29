package models;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class Hudson {

    private final String homeDirectory;
    private ArrayList<Job> hudsonJobs;

    /**
     * Constructor.
     *
     * @param homeDirectory absolute path for the Hudson home directory
     * 
     */
    public Hudson(String homeDirectory) {

        this.homeDirectory = homeDirectory;

    }

    /**
     * Returns a list of Job objects representing all the jobs that exists in a
     * Hudson instance.
     *
     * @return A list of all jobs in a Hudson instance
     */
    public ArrayList<Job> getJobs() {

        if (hudsonJobs != null) {

            return hudsonJobs;

        }

        ArrayList<Job> allJobs = new ArrayList();

        File[] teamDirectories = getTeamDirectories(homeDirectory);

        for (File teamDirectory : teamDirectories) {

            File[] jobDirectories = getJobDirectories(teamDirectory.getAbsolutePath());

            for (File jobDirectory : jobDirectories) {

                Job hudsonJob = new Job(jobDirectory.getAbsolutePath());
                allJobs.add(hudsonJob);

            }

        }

        this.hudsonJobs = allJobs;
        return allJobs;
    }

    /**
     * Returns an array of File objects that represent the root directories for
     * all the existing teams in a Hudson instance.
     *
     * @param hudsonHome an absolute path to the Hudson home directory
     * @return An array with the File objects of all the teams in a Hudson
     * instance
     */
    private File[] getTeamDirectories(String hudsonHome) {

        return getSubDirectories(hudsonHome + File.separator + "teams");

    }

    /**
     * Returns an array of File objects that represent the root directories for
     * all the jobs that belong to a team.
     *
     * @param teamDirectory an absolute path to the Team root directory
     * @return An array with the File objects of all the jobs of a Team in
     * Hudson.
     */
    private File[] getJobDirectories(String teamDirectory) {

        return getSubDirectories(teamDirectory + File.separator + "jobs");
    }

    /**
     * Returns an array of Files of all the directories inside a parent
     * directory
     *
     * @param parent an absolute path to a directory
     * @return An array with the File reference to all the directories inside a
     * parent directory
     */
    private File[] getSubDirectories(String parent) {

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

}
