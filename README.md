# MCreator REST API Plugin

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://github.com/MCreatpr/net.mcreator.demojavaplugin.DemoJavaPlugin/blob/master/LICENSE)

This plugin extends MCreator with a REST API for headless operation. It allows you to control MCreator programmatically through HTTP requests, enabling automation of common tasks like loading workspaces, building projects, and running Minecraft.

Java plugins only work with MCreator 2022.2 or newer.

## Setup

In order for the plugin to work, make a new file called `gradle.properties` with the following contents:

```
mcreator_path=<path to MCreator core Gradle project directory>
```

For example, on Windows:
```
mcreator_path=C\:\\Program Files\\Pylo\\MCreator
```

On Linux/macOS:
```
mcreator_path=/path/to/MCreator
```

## Building and Deploying

Here are the complete steps to build and deploy the plugin using bash commands:

```bash
# Clone the repository (if you haven't already)
git clone https://github.com/yourusername/mcreator-restapi-plugin.git
cd mcreator-restapi-plugin

# Create gradle.properties file
echo 'mcreator_path=/path/to/MCreator' > gradle.properties

# Build the plugin
./gradlew jar

# The plugin will be in build/libs/mcreator-restapi-plugin.zip
# Copy it to your MCreator plugins directory
cp build/libs/mcreator-restapi-plugin.zip /path/to/MCreator/plugins/

# Start MCreator (example for Linux/macOS)
/path/to/MCreator/mcreator

# For Windows, you would use:
# "C:\Program Files\Pylo\MCreator\mcreator.exe"
```

## REST API Endpoints

The plugin exposes the following REST API endpoints:

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

## Running MCreator with the plugin

This plugin comes with some Gradle tasks to help you with the development of your plugin. 

```bash
# Run MCreator with the plugin loaded
./gradlew runMCreatorWithPlugin

# Run tests with the plugin loaded
./gradlew test
```

There is also IntelliJ IDEA run configuration for this task provided in the repository.

**Make sure to enable Java plugins in MCreator preferences, or the plugin will not be loaded.**

## Using the REST API

Once MCreator is running with the plugin loaded, the REST API server will start automatically on port 8080. You can then use tools like `curl`, Postman, or any HTTP client to interact with the API.

### Complete Workflow Example (Bash)

Here's a complete workflow example using bash and curl:

```bash
# Start MCreator with the plugin (in a separate terminal)
./gradlew runMCreatorWithPlugin

# Check if MCreator is running and get workspace status
curl http://localhost:8080/api/workspace/status

# Load a workspace
curl -X POST http://localhost:8080/api/workspace/load \
  -H "Content-Type: application/json" \
  -d '{"path": "/absolute/path/to/your/mcreator/workspace"}'

# Build the workspace
curl -X POST http://localhost:8080/api/workspace/build

# Run Minecraft
curl -X POST http://localhost:8080/api/minecraft/run \
  -H "Content-Type: application/json" \
  -d '{"waitForExit": false}'

# Create a script to automate the entire process
cat > build_and_run.sh << 'EOF'
#!/bin/bash

# Configuration
MCREATOR_WORKSPACE="/path/to/your/mcreator/workspace"
API_URL="http://localhost:8080/api"

# Check if MCreator is running
echo "Checking MCreator status..."
STATUS=$(curl -s $API_URL/workspace/status)
if [[ $STATUS == *"workspaceLoaded"* ]]; then
  echo "MCreator is running"
else
  echo "Error: MCreator is not running or the plugin is not loaded"
  exit 1
fi

# Load workspace
echo "Loading workspace..."
LOAD_RESULT=$(curl -s -X POST $API_URL/workspace/load \
  -H "Content-Type: application/json" \
  -d "{\"path\": \"$MCREATOR_WORKSPACE\"}")
echo $LOAD_RESULT

# Build workspace
echo "Building workspace..."
BUILD_RESULT=$(curl -s -X POST $API_URL/workspace/build)
echo $BUILD_RESULT

# Run Minecraft
echo "Running Minecraft..."
RUN_RESULT=$(curl -s -X POST $API_URL/minecraft/run \
  -H "Content-Type: application/json" \
  -d '{"waitForExit": false}')
echo $RUN_RESULT

echo "Done!"
EOF

# Make the script executable
chmod +x build_and_run.sh

# Run the script
./build_and_run.sh
```

## Testing

We highly recommend to test your plugin by running MCreator's tests with your plugin loaded.

```bash
# Run tests with the plugin loaded
./gradlew test
```

There is also IntelliJ IDEA run configuration for this task provided in the repository.

## Exporting

To export the plugin:

```bash
# Build the plugin
./gradlew jar

# The plugin will be in build/libs/mcreator-restapi-plugin.zip
ls -la build/libs/
```

## Installation

To install the plugin in MCreator:

```bash
# Build the plugin (if you haven't already)
./gradlew jar

# Copy the plugin to MCreator's plugins directory
# For Linux/macOS:
cp build/libs/mcreator-restapi-plugin.zip /path/to/MCreator/plugins/

# For Windows (using PowerShell):
# Copy-Item .\build\libs\mcreator-restapi-plugin.zip "C:\Program Files\Pylo\MCreator\plugins\"

# Start MCreator and enable Java plugins in preferences
```

## Headless Operation

One of the main benefits of this plugin is enabling headless operation of MCreator. Here's an example of how to use it in a CI/CD pipeline:

```bash
#!/bin/bash
# Example CI/CD script for building a Minecraft mod with MCreator headlessly

# Start MCreator with the plugin (headless mode)
nohup /path/to/MCreator/mcreator --no-ui &
MCREATOR_PID=$!

# Wait for the REST API to become available
sleep 10

# Load the workspace
curl -X POST http://localhost:8080/api/workspace/load \
  -H "Content-Type: application/json" \
  -d "{\"path\": \"$WORKSPACE_PATH\"}"

# Build the mod
BUILD_RESULT=$(curl -s -X POST http://localhost:8080/api/workspace/build)

# Extract the JAR file path from the build result
JAR_PATH=$(echo $BUILD_RESULT | grep -o '"jarFilePath":"[^"]*"' | cut -d'"' -f4)

# Copy the built JAR to the artifacts directory
cp "$JAR_PATH" ./artifacts/

# Terminate MCreator
kill $MCREATOR_PID

echo "Build completed successfully!"
