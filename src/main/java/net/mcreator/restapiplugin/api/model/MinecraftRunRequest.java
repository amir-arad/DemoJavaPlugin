package net.mcreator.restapiplugin.api.model;

/**
 * Request model for running Minecraft.
 */
public class MinecraftRunRequest {
    
    private boolean waitForExit = false;
    
    /**
     * Check if the request should wait for Minecraft to exit.
     * 
     * @return True if the request should wait for Minecraft to exit, false otherwise
     */
    public boolean isWaitForExit() {
        return waitForExit;
    }
    
    /**
     * Set whether the request should wait for Minecraft to exit.
     * 
     * @param waitForExit True if the request should wait for Minecraft to exit, false otherwise
     */
    public void setWaitForExit(boolean waitForExit) {
        this.waitForExit = waitForExit;
    }
}