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
 * Controller for Minecraft operations.
 */
public class MinecraftController {

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
