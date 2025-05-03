# MCreator REST API Plugin Implementation Guide

This document provides a comprehensive implementation guide for creating a MCreator plugin that exposes a REST API for headless operation of MCreator.

## Project Structure

```
src/main/java/net/mcreator/restapiplugin/
├── MCreatorRESTPlugin.java           # Main plugin class
├── config/
│   └── RESTAPIConfig.java            # Configuration settings
├── server/
│   └── WebServer.java                # Web server implementation
├── api/
│   ├── controllers/
│   │   ├── WorkspaceController.java  # Workspace operations
│   │   ├── BuildController.java      # Build operations
│   │   ├── MinecraftController.java  # Minecraft operations
│   │   └── StatusController.java     # Status operations
│   ├── model/
│   │   ├── WorkspaceLoadRequest.java # Request model for loading a workspace
│   │   ├── BuildResponse.java        # Response model for build operations
│   │   └── MinecraftRunRequest.java  # Request model for running Minecraft
│   └── util/
│       ├── ResponseUtils.java        # Response formatting utilities
│       └── ValidationUtils.java      # Request validation utilities
```

## Dependencies

Add the following dependencies to your `build.gradle` file:

```gradle
// REST API dependencies
export group: 'io.javalin', name: 'javalin', version: '5.6.3'
export group: 'com.fasterxml.jackson.core', name: 'jackson-databind', version: '2.15.2'
export group: 'org.slf4j', name: 'slf4j-simple', version: '2.0.7'
```

## API Endpoints

### 1. POST /api/workspace/load

Loads an existing MCreator workspace.

**Request:**
```json
{
  "path": "/absolute/path/to/workspace"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "workspacePath": "/absolute/path/to/workspace",
    "workspaceName": "My Mod"
  }
}
```

### 2. POST /api/workspace/build

Triggers the MCreator build process.

**Request:**
```json
{}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "success": true,
    "logs": ["Build log line 1", "Build log line 2", ...],
    "jarFilePath": "/absolute/path/to/workspace/build/libs/mymod-1.0.jar"
  }
}
```

### 3. POST /api/minecraft/run

Launches Minecraft using MCreator's integrated client launcher.

**Request:**
```json
{
  "waitForExit": false
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "success": true,
    "message": "Minecraft is running"
  }
}
```

### 4. GET /api/workspace/status

Provides real-time details on the current workspace state.

**Response:**
```json
{
  "success": true,
  "data": {
    "workspaceLoaded": true,
    "workspacePath": "/absolute/path/to/workspace",
    "workspaceName": "My Mod",
    "modID": "mymod",
    "modVersion": "1.0.0",
    "modDescription": "My awesome mod",
    "modAuthor": "Me",
    "modLicense": "MIT",
    "modWebsite": "https://example.com"
  }
}
```

## Implementation Notes

1. The plugin uses Javalin as a lightweight web server to handle HTTP requests.
2. All API endpoints return standardized JSON responses with a `success` flag and either a `data` object or an `error` message.
3. The plugin integrates directly with MCreator's internal API to perform operations, rather than simulating UI actions.
4. Error handling is implemented at multiple levels to ensure robust operation.
5. The plugin adds a menu item and toolbar button to show the REST API server status.

## Key MCreator API Integration Points

1. **Workspace Loading**: Uses `WorkspaceUtils.openWorkspaceInMCreator(mcreator, mcreatorFile)` to load workspaces.
2. **Build Process**: Uses `mcreator.getActionRegistry().buildWorkspace.doAction()` to trigger builds.
3. **Minecraft Launch**: Uses `mcreator.getGradleConsole().exec(workspaceFolderPath, GradleConsole.TASK_TYPE_RUN, true)` to launch Minecraft.
4. **Workspace Status**: Uses `mcreator.getWorkspace().getWorkspaceSettings()` to get workspace information.

This implementation provides a complete REST API for headless operation of MCreator, allowing for automation of common tasks through HTTP requests.
