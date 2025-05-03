package net.mcreator.restapiplugin.api.model;

import java.util.List;

/**
 * Response model for Minecraft run operations.
 */
public class MinecraftRunResponse {
    
    private boolean success;
    private int exitCode;
    private List<String> logs;
    private String errorMessage;
    
    /**
     * Check if the Minecraft run was successful.
     * 
     * @return True if the run was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Set whether the Minecraft run was successful.
     * 
     * @param success True if the run was successful, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    /**
     * Get the exit code of the Minecraft process.
     * 
     * @return The exit code
     */
    public int getExitCode() {
        return exitCode;
    }
    
    /**
     * Set the exit code of the Minecraft process.
     * 
     * @param exitCode The exit code
     */
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
    
    /**
     * Get the Minecraft logs.
     * 
     * @return The logs
     */
    public List<String> getLogs() {
        return logs;
    }
    
    /**
     * Set the Minecraft logs.
     * 
     * @param logs The logs
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