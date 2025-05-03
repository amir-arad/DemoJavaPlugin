package net.mcreator.restapiplugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import net.mcreator.Workspace;
import net.mcreator.gradle.GradleConsole;
import net.mcreator.gradle.GradleTaskResult;
import net.mcreator.gradle.GradleTaskStartupListener;
import net.mcreator.io.FileIO;
import net.mcreator.plugin.JavaPlugin;
import net.mcreator.plugin.Plugin;
import net.mcreator.plugin.events.workspace.MCreatorLoadedEvent;
import net.mcreator.plugin.events.workspace.MCreatorUnloadedEvent;
import net.mcreator.ui.action.ActionRegistry;
import net.mcreator.ui.action.BasicAction;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.workspace.WorkspaceUtils;
import net.mcreator.workspace.WorkspaceSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MCreator REST API Plugin
 * 
 * This plugin exposes a REST API for headless operation of MCreator.
 */
public class MCreatorRESTAPIPlugin extends JavaPlugin {

    private static final Logger LOG = LogManager.getLogger("MCreator REST API Plugin");
    private WebServer server;
    private final RESTAPIConfig config;

    public MCreatorRESTAPIPlugin(Plugin plugin) {
        super(plugin);
        
        // Load configuration
        this.config = new RESTAPIConfig();
        
        // Add listener for MCreator loaded event
        addListener(MCreatorLoadedEvent.class, event -> {
            // Start the REST API server
            startRESTServer(event.getMCreator());
            
            // Add UI elements
            SwingUtilities.invokeLater(() -> {
                BasicAction statusAction = new BasicAction(event.getMCreator().getActionRegistry(),
                        L10N.t("plugin.restapi.menu.button"),
                        e -> showServerStatus());
                statusAction.setIcon(UIRES.get("16px.info"));

                JMenu menu = new JMenu(L10N.t("plugin.restapi.menu.main"));
                menu.add(statusAction);

                event.getMCreator().getMainMenuBar().add(menu);
                event.getMCreator().getToolBar().addToRightToolbar(statusAction);
            });
        });
        
        // Add listener for MCreator unloaded event
        addListener(MCreatorUnloadedEvent.class, event -> {
            // Stop the REST API server
            stopRESTServer();
        });

        LOG.info("MCreator REST API plugin was loaded");
    }
    
    private void startRESTServer(Workspace mcreator) {
        try {
            server = new WebServer(config.getPort(), mcreator);
            server.start();
            LOG.info(MessageFormat.format(L10N.t("plugin.restapi.server.started"), config.getPort()));
        } catch (Exception e) {
            LOG.error(MessageFormat.format(L10N.t("plugin.restapi.server.error"), e.getMessage()), e);
        }
    }
    
    private void stopRESTServer() {
        if (server != null) {
            server.stop();
            LOG.info(L10N.t("plugin.restapi.server.stopped"));
        }
    }
    
    private void showServerStatus() {
        JOptionPane.showMessageDialog(null, 
                "REST API Server Status:\n" +
                "Running: " + (server != null && server.isRunning()) + "\n" +
                "Port: " + config.getPort(),
                "REST API Server Status", 
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Configuration settings for the REST API server.
     */
    public static class RESTAPIConfig {
        
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
    
    /**
     * Web server for the REST API.
     */
    public static class WebServer {
        
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
    
    /**
     * Controller for workspace operations.
     */
    public static class WorkspaceController {
        
        private static final Logger LOG = LogManager.getLogger("Workspace Controller");
        private final Workspace mcreator;
        private final ObjectMapper objectMapper = new ObjectMapper();
        
        /**
         * Create a new workspace controller.
         * 
         * @param mcreator The MCreator workspace
         */
        public WorkspaceController(Workspace mcreator) {
            this.mcreator = mcreator;
        }
        
        /**
         * Handle a request to load a workspace.
         * 
         * @param ctx The context
         */
        public void handleLoadWorkspace(Context ctx) {
            try {
                // Parse the request
                Map<String, Object> request = objectMapper.readValue(ctx.body(), Map.class);
                String workspacePath = (String) request.get("path");
                
                // Validate the request
                if (ValidationUtils.isNullOrEmpty(workspacePath)) {
                    ctx.status(HttpStatus.BAD_REQUEST);
                    ctx.json(ResponseUtils.createErrorResponse("Workspace path is required"));
                    return;
                }
                
                // Validate the workspace path
                if (!ValidationUtils.isValidWorkspacePath(workspacePath)) {
                    ctx.status(HttpStatus.BAD_REQUEST);
                    ctx.json(ResponseUtils.createErrorResponse("Invalid workspace path: " + workspacePath));
                    return;
                }
                
                // Get the .mcreator file
                File mcreatorFile = ValidationUtils.getMCreatorFile(workspacePath);
                if (mcreatorFile == null) {
                    ctx.status(HttpStatus.BAD_REQUEST);
                    ctx.json(ResponseUtils.createErrorResponse("Could not find .mcreator file in workspace: " + workspacePath));
                    return;
                }
                
                // Load the workspace
                try {
                    // Close the current workspace if one is open
                    if (mcreator.getWorkspace() != null) {
                        mcreator.closeWorkspace();
                    }
                    
                    // Load the new workspace
                    WorkspaceUtils.openWorkspaceInMCreator(mcreator, mcreatorFile);
                    
                    // Create the response
                    Map<String, Object> data = new HashMap<>();
                    data.put("workspacePath", workspacePath);
                    data.put("workspaceName", mcreator.getWorkspace().getWorkspaceSettings().getModName());
                    
                    ctx.status(HttpStatus.OK);
                    ctx.json(ResponseUtils.createSuccessResponse(data));
                    
                    LOG.info("Workspace loaded: " + workspacePath);
                } catch (Exception e) {
                    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                    ctx.json(ResponseUtils.createErrorResponse("Error loading workspace: " + e.getMessage()));
                    LOG.error("Error loading workspace: " + workspacePath, e);
                }
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ResponseUtils.createErrorResponse("Invalid request: " + e.getMessage()));
                LOG.error("Error parsing request", e);
            }
        }
    }
    
    /**
     * Controller for build operations.
     */
    public static class BuildController {
        
        private static final Logger LOG = LogManager.getLogger("Build Controller");
        private final Workspace mcreator;
        private final ObjectMapper objectMapper = new ObjectMapper();
        
        /**
         * Create a new build controller.
         * 
         * @param mcreator The MCreator workspace
         */
        public BuildController(Workspace mcreator) {
            this.mcreator = mcreator;
        }
        
        /**
         * Handle a request to build a workspace.
         * 
         * @param ctx The context
         */
        public void handleBuildWorkspace(Context ctx) {
            try {
                // Check if a workspace is loaded
                if (mcreator.getWorkspace() == null) {
                    ctx.status(HttpStatus.BAD_REQUEST);
                    ctx.json(ResponseUtils.createErrorResponse("No workspace is loaded"));
                    return;
                }
                
                // Get the action registry
                ActionRegistry actionRegistry = mcreator.getActionRegistry();
                
                // Create a list to store the build logs
                List<String> buildLogs = new CopyOnWriteArrayList<>();
                
                // Create a future to wait for the build to complete
                CompletableFuture<Boolean> buildFuture = new CompletableFuture<>();
                
                // Add a listener to the gradle console to capture the build logs
                GradleConsole gradleConsole = mcreator.getGradleConsole();
                gradleConsole.addGradleTaskStartupListener(new GradleTaskStartupListener() {
                    @Override
                    public void onGradleTaskStartup(GradleTaskResult result) {
                        // Capture the build logs
                        buildLogs.addAll(result.getOutput());
                        
                        // Complete the future with the build result
                        buildFuture.complete(result.isSuccess());
                        
                        // Remove the listener
                        gradleConsole.removeGradleTaskStartupListener(this);
                    }
                });
                
                // Trigger the build
                actionRegistry.buildWorkspace.doAction();
                
                // Wait for the build to complete
                boolean success = buildFuture.get();
                
                // Create the response
                Map<String, Object> data = new HashMap<>();
                data.put("success", success);
                data.put("logs", buildLogs);
                
                // Add the JAR file path if the build was successful
                if (success) {
                    String jarFilePath = mcreator.getWorkspace().getWorkspaceFolder().getAbsolutePath() + "/build/libs/" + 
                            mcreator.getWorkspace().getWorkspaceSettings().getModID() + "-1.0.jar";
                    data.put("jarFilePath", jarFilePath);
                }
                
                ctx.status(HttpStatus.OK);
                ctx.json(ResponseUtils.createSuccessResponse(data));
                
                LOG.info("Build " + (success ? "succeeded" : "failed"));
            } catch (Exception e) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.json(ResponseUtils.createErrorResponse("Error building workspace: " + e.getMessage()));
                LOG.error("Error building workspace", e);
            }
        }
    }
    
    /**
     * Controller for Minecraft operations.
     */
    public static class MinecraftController {
        
        private static final Logger LOG = LogManager.getLogger("Minecraft Controller");
        private final Workspace mcreator;
        private final ObjectMapper objectMapper = new ObjectMapper();
        
        /**
         * Create a new Minecraft controller.
         * 
         * @param mcreator The MCreator workspace
         */
        public MinecraftController(Workspace mcreator) {
            this.mcreator = mcreator;
        }
        
        /**
         * Handle a request to run Minecraft.
         * 
         * @param ctx The context
         */
        public void handleRunMinecraft(Context ctx) {
            try {
                // Parse the request
                Map<String, Object> request = objectMapper.readValue(ctx.body(), Map.class);
                boolean waitForExit = request.containsKey("waitForExit") ? (boolean) request.get("waitForExit") : false;
                
                // Check if a workspace is loaded
                if (mcreator.getWorkspace() == null) {
                    ctx.status(HttpStatus.BAD_REQUEST);
                    ctx.json(ResponseUtils.createErrorResponse("No workspace is loaded"));
                    return;
                }
                
                // Get the workspace folder path
                String workspaceFolderPath = mcreator.getWorkspace().getWorkspaceFolder().getAbsolutePath();
                
                // Create a list to store the Minecraft logs
                List<String> minecraftLogs = new CopyOnWriteArrayList<>();
                
                // Create a future to wait for Minecraft to exit
                CompletableFuture<Integer> minecraftFuture = new CompletableFuture<>();
                
                // Add a listener to the gradle console to capture the Minecraft logs
                GradleConsole gradleConsole = mcreator.getGradleConsole();
                gradleConsole.addGradleTaskStartupListener(new GradleTaskStartupListener() {
                    @Override
                    public void onGradleTaskStartup(GradleTaskResult result) {
                        // Capture the Minecraft logs
                        minecraftLogs.addAll(result.getOutput());
                        
                        // Complete the future with the exit code
                        minecraftFuture.complete(result.isSuccess() ? 0 : 1);
                        
                        // Remove the listener
                        gradleConsole.removeGradleTaskStartupListener(this);
                    }
                });
                
                // Run Minecraft
                gradleConsole.exec(workspaceFolderPath, GradleConsole.TASK_TYPE_RUN, true);
                
                // If we're not waiting for Minecraft to exit, return immediately
                if (!waitForExit) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("success", true);
                    data.put("message", "Minecraft is running");
                    
                    ctx.status(HttpStatus.OK);
                    ctx.json(ResponseUtils.createSuccessResponse(data));
                    
                    LOG.info("Minecraft is running");
                    return;
                }
                
                // Wait for Minecraft to exit
                int exitCode = minecraftFuture.get();
                
                // Create the response
                Map<String, Object> data = new HashMap<>();
                data.put("success", exitCode == 0);
                data.put("exitCode", exitCode);
                data.put("logs", minecraftLogs);
                
                ctx.status(HttpStatus.OK);
                ctx.json(ResponseUtils.createSuccessResponse(data));
                
                LOG.info("Minecraft exited with code " + exitCode);
            } catch (Exception e) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.json(ResponseUtils.createErrorResponse("Error running Minecraft: " + e.getMessage()));
                LOG.error("Error running Minecraft", e);
            }
        }
    }
    
    /**
     * Controller for status operations.
     */
    public static class StatusController {
        
        private static final Logger LOG = LogManager.getLogger("Status Controller");
        private final Workspace mcreator;
        
        /**
         * Create a new status controller.
         * 
         * @param mcreator The MCreator workspace
         */
        public StatusController(Workspace mcreator) {
            this.mcreator = mcreator;
        }
        
        /**
         * Handle a request to get workspace status.
         * 
         * @param ctx The context
         */
        public void handleWorkspaceStatus(Context ctx) {
            try {
                // Check if a workspace is loaded
                if (mcreator.getWorkspace() == null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("workspaceLoaded", false);
                    
                    ctx.status(HttpStatus.OK);
                    ctx.json(ResponseUtils.createSuccessResponse(data));
                    return;
                }
                
                // Get the workspace settings
                WorkspaceSettings settings = mcreator.getWorkspace().getWorkspaceSettings();
                
                // Create the response
                Map<String, Object> data = new HashMap<>();
                data.put("workspaceLoaded", true);
                data.put("workspacePath", mcreator.getWorkspace().getWorkspaceFolder().getAbsolutePath());
                data.put("workspaceName", settings.getModName());
                data.put("modID", settings.getModID());
                data.put("modVersion", settings.getVersion());
                data.put("modDescription", settings.getDescription());
                data.put("modAuthor", settings.getAuthor());
                data.put("modLicense", settings.getLicense());
                data.put("modWebsite", settings.getWebsiteURL());
                
                ctx.status(HttpStatus.OK);
                ctx.json(ResponseUtils.createSuccessResponse(data));
                
                LOG.info("Workspace status requested");
            } catch (Exception e) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.json(ResponseUtils.createErrorResponse("Error getting workspace status: " + e.getMessage()));
                LOG.error("Error getting workspace status", e);
            }
        }
    }
    
    /**
     * Utility class for creating standardized API responses.
     */
    public static class ResponseUtils {
        
        /**
         * Create a success response with the given data.
         * 
         * @param data The data to include in the response
         * @return A map representing the response
         */
        public static Map<String, Object> createSuccessResponse(Object data) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            return response;
        }
        
        /**
         * Create an error response with the given message.
         * 
         * @param message The error message
         * @return A map representing the response
         */
        public static Map<String, Object> createErrorResponse(String message) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", message);
            return response;
        }
        
        /**
         * Create an error response with the given message and additional data.
         * 
         * @param message The error message
         * @param data Additional data to include in the response
         * @return A map representing the response
         */
        public static Map<String, Object> createErrorResponse(String message, Object data) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", message);
            response.put("data", data);
            return response;
        }
    }
    
    /**
     * Utility class for validating request data.
     */
    public static class ValidationUtils {
        
        /**
         * Validate that a workspace path exists and is a valid MCreator workspace.
         * 
         * @param workspacePath The path to validate
         * @return True if the path is valid, false otherwise
         */
        public static boolean isValidWorkspacePath(String workspacePath) {
            if (workspacePath == null || workspacePath.isEmpty()) {
                return false;
            }
            
            Path path = Paths.get(workspacePath);
            
            // Check if the path exists and is a directory
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return false;
            }
            
            // Check if the directory contains the .mcreator file
            Path mcreatorFile = path.resolve(".mcreator");
            return Files.exists(mcreatorFile) && Files.isRegularFile(mcreatorFile);
        }
        
        /**
         * Get the .mcreator file from a workspace path.
         * 
         * @param workspacePath The workspace path
         * @return The .mcreator file, or null if it doesn't exist
         */
        public static File getMCreatorFile(String workspacePath) {
            if (!isValidWorkspacePath(workspacePath)) {
                return null;
            }
            
            return Paths.get(workspacePath, ".mcreator").toFile();
        }
        
        /**
         * Check if a string is null or empty.
         * 
         * @param str The string to check
         * @return True if the string is null or empty, false otherwise
         */
        public static boolean isNullOrEmpty(String str) {
            return str == null || str.isEmpty();
        }
    }
}