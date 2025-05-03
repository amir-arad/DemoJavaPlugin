package net.mcreator.restapiplugin.config;

/**
 * Configuration settings for the REST API server.
 */
public class RESTAPIConfig {
    
    // Default port for the REST API server
    private static final int DEFAULT_PORT = 8080;
    
    // Port for the REST API server
    private int port = DEFAULT_PORT;
    
    /**
     * Get the port for the REST API server.
     * 
     * @return The port number
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Set the port for the REST API server.
     * 
     * @param port The port number
     */
    public void setPort(int port) {
        this.port = port;
    }
}