package net.mcreator.restapiplugin.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import net.mcreator.Workspace;
import net.mcreator.gradle.GradleConsole;
import net.mcreator.gradle.GradleTaskResult;
import net.mcreator.gradle.GradleTaskStartupListener;
import net.mcreator.restapiplugin.api.util.ResponseUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controller for build operations.
 */
public class BuildController {
    
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
            net.mcreator.ui.action.ActionRegistry actionRegistry = mcreator.getActionRegistry();
            
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