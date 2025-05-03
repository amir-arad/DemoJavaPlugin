package net.mcreator.restapiplugin.api.model;

import java.util.List;

/**
 * Response model for build operations.
 */
public class BuildResponse {
    
    private boolean success;
    private String jarFilePath;
    private List<String> logs;
    private String errorMessage;
    
    /**
     * Check if the build was successful.
     * 
     * @return True if the build was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Set whether the build was successful.
     * 
     * @param success True if the build was successful, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    /**
     * Get the path to the built JAR file.
     * 
     * @return The path to the JAR file
     */
    public String getJarFilePath() {
        return jarFilePath;
    }
    
    /**
     * Set the path to the built JAR file.
     * 
     * @param jarFilePath The path to the JAR file
     */
    public void setJarFilePath(String jarFilePath) {
        this.jarFilePath = jarFilePath;
    }
    
    /**
     * Get the build logs.
     * 
     * @return The build logs
     */
    public List<String> getLogs() {
        return logs;
    }
    
    /**
     * Set the build logs.
     * 
     * @param logs The build logs
     */
    public void setLogs(List<String> logs) {
        this.logs = logs;
    }
    
    /**
     * Get the error message.
     * 
     * @return The error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Set the error message.
     * 
     * @param errorMessage The error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}