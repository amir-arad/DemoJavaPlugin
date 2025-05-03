package net.mcreator.restapiplugin.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import net.mcreator.Workspace;
import net.mcreator.io.FileIO;
import net.mcreator.restapiplugin.api.model.WorkspaceLoadRequest;
import net.mcreator.restapiplugin.api.util.ResponseUtils;
import net.mcreator.restapiplugin.api.util.ValidationUtils;
import net.mcreator.ui.workspace.WorkspaceUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for workspace operations.
 */
public class WorkspaceController {
    
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