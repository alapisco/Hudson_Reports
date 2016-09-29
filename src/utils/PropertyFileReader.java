package utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyFileReader {

    String propertyFile;

    public final String HUDSON_HOME = "HUDSON_HOME";
    public final String GENERAL_CSV_REPORT_FILE = "GENERAL_CSV_REPORT_FILE";
    public final String JOBS_WITHOUT_CONFIG_FILE_CSV_REPORT_FILE = "JOBS_WITHOUT_CONFIG_FILE_CSV_REPORT_FILE";
    public final String DISABLED_JOBS_CSV_REPORT = "DISABLED_JOBS_CSV_REPORT";
    public final String JOBS_BIGGER_THAN_THRESHOLD_KB_CVS_REPORT = "JOBS_BIGGER_THAN_THRESHOLD_KB_CVS_REPORT";
    public final String TRESHOLD_KILOBYTES = "TRESHOLD_KILOBYTES";
    public final String JOBS_RUN_MORE_THAN_ONE_MONTH_AGO_CVS_REPORT = "JOBS_RUN_MORE_THAN_ONE_MONTH_AGO_CVS_REPORT";

    /**
     * Constructor.
     *
     * @param propertFile absolute path of a property file
     *
     */
    public PropertyFileReader(String propertFile) {

        this.propertyFile = propertFile;

    }

    /**
     * Returns the value of a property found in a property file
     *
     * @param propertyName the name of the property whose value is needed
     * @return the value of the property or <code>null</code> the property name
     * is not found
     */
    public String getPropertyValue(String propertyName) {

        String propertyValue = null;

        Properties prop = new Properties();
        InputStream input;

        try {
            input = new FileInputStream(propertyFile);
            prop.load(input);
            propertyValue = prop.getProperty(propertyName);
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage());
            System.out.println("Property file not found " + propertyFile);
            System.exit(1);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            System.out.println("Can not read file " + propertyFile);
        }

        return propertyValue;

    }
}
