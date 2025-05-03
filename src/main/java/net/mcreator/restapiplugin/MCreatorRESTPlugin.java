package net.mcreator.restapiplugin;

import net.mcreator.plugin.JavaPlugin;
import net.mcreator.plugin.Plugin;
import net.mcreator.plugin.events.workspace.MCreatorLoadedEvent;
import net.mcreator.plugin.events.workspace.MCreatorUnloadedEvent;
import net.mcreator.restapiplugin.config.RESTAPIConfig;
import net.mcreator.restapiplugin.server.WebServer;
import net.mcreator.ui.action.BasicAction;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;

public class MCreatorRESTPlugin extends JavaPlugin {

    private static final Logger LOG = LogManager.getLogger("MCreator REST API Plugin");
    private WebServer server;
    private final RESTAPIConfig config;

    public MCreatorRESTPlugin(Plugin plugin) {
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
    
    private void startRESTServer(net.mcreator.Workspace mcreator) {
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
}