package net.mcreator.restapiplugin.api.model;

/**
 * Request model for loading a workspace.
 */
public class WorkspaceLoadRequest {
    
    private String path;
    
    /**
     * Get the path to the workspace.
     * 
     * @return The path
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Set the path to the workspace.
     * 
     * @param path The path
     */
    public void setPath(String path) {
        this.path = path;
    }
}