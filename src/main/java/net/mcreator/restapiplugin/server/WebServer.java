package net.mcreator.restapiplugin.server;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import net.mcreator.Workspace;
import net.mcreator.restapiplugin.api.controllers.BuildController;
import net.mcreator.restapiplugin.api.controllers.MinecraftController;
import net.mcreator.restapiplugin.api.controllers.StatusController;
import net.mcreator.restapiplugin.api.controllers.WorkspaceController;
import net.mcreator.restapiplugin.api.util.ResponseUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Web server for the REST API.
 */
public class WebServer {
    
    private static final Logger LOG = LogManager.getLogger("REST API Server");
    
    private final int port;
    private final Workspace mcreator;
    private Javalin server;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    // Controllers
    private final WorkspaceController workspaceController;
    private final BuildController buildController;
    private final MinecraftController minecraftController;
    private final StatusController statusController;
    
    /**
     * Create a new web server.
     * 
     * @param port The port to listen on
     * @param mcreator The MCreator workspace
     */
    public WebServer(int port, Workspace mcreator) {
        this.port = port;
        this.mcreator = mcreator;
        
        // Initialize controllers
        this.workspaceController = new WorkspaceController(mcreator);
        this.buildController = new BuildController(mcreator);
        this.minecraftController = new MinecraftController(mcreator);
        this.statusController = new StatusController(mcreator);
    }
    
    /**
     * Start the web server.
     */
    public void start() {
        if (running.get()) {
            return;
        }
        
        server = Javalin.create(config -> {
            config.plugins.enableCors(cors -> {
                cors.add(corsConfig -> {
                    corsConfig.allowHost("*");
                });
            });
        });
        
        // Set up error handling
        server.exception(Exception.class, (e, ctx) -> {
            LOG.error("Error handling request: " + ctx.path(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(ResponseUtils.createErrorResponse("Internal server error: " + e.getMessage()));
        });
        
        // Set up routes
        setupRoutes();
        
        // Start the server
        server.start(port);
        running.set(true);
        
        LOG.info("REST API server started on port " + port);
    }
    
    /**
     * Stop the web server.
     */
    public void stop() {
        if (!running.get() || server == null) {
            return;
        }
        
        server.stop();
        running.set(false);
        
        LOG.info("REST API server stopped");
    }
    
    /**
     * Check if the server is running.
     * 
     * @return True if the server is running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Set up the routes for the web server.
     */
    private void setupRoutes() {
        // Workspace routes
        server.post("/api/workspace/load", this::handleLoadWorkspace);
        
        // Build routes
        server.post("/api/workspace/build", this::handleBuildWorkspace);
        
        // Minecraft routes
        server.post("/api/minecraft/run", this::handleRunMinecraft);
        
        // Status routes
        server.get("/api/workspace/status", this::handleWorkspaceStatus);
    }
    
    /**
     * Handle a request to load a workspace.
     * 
     * @param ctx The context
     */
    private void handleLoadWorkspace(Context ctx) {
        workspaceController.handleLoadWorkspace(ctx);
    }
    
    /**
     * Handle a request to build a workspace.
     * 
     * @param ctx The context
     */
    private void handleBuildWorkspace(Context ctx) {
        buildController.handleBuildWorkspace(ctx);
    }
    
    /**
     * Handle a request to run Minecraft.
     * 
     * @param ctx The context
     */
    private void handleRunMinecraft(Context ctx) {
        minecraftController.handleRunMinecraft(ctx);
    }
    
    /**
     * Handle a request to get workspace status.
     * 
     * @param ctx The context
     */
    private void handleWorkspaceStatus(Context ctx) {
        statusController.handleWorkspaceStatus(ctx);
    }
}