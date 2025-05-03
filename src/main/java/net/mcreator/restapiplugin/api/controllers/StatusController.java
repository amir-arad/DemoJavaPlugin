package net.mcreator.restapiplugin.api.controllers;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import net.mcreator.Workspace;
import net.mcreator.restapiplugin.api.util.ResponseUtils;
import net.mcreator.workspace.WorkspaceSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for status operations.
 */
public class StatusController {

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
