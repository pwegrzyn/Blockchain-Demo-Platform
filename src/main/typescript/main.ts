import { app, BrowserWindow } from "electron";
import * as path from "path";
import { catMain } from "./config"
import * as propertiesReader from "properties-reader";
import { VisualizationServer } from "./server";

// GUI
let mainWindow: Electron.BrowserWindow;

function createWindow() {

    mainWindow = new BrowserWindow({
        height: 880,
        width: 1350,
    });

    mainWindow.loadFile(path.join(__dirname, "../src/main/resources/templates/index.html"));

    mainWindow.on("closed", () => {
        catMain.info(() => "Shutting down...");
        mainWindow = null;
        app.quit();
    });

}

app.on("ready", () =>{
    createWindow();
    // Client-server communication with the jvm process
    catMain.info(() => "Reading config files...");
    const props = propertiesReader('src/main/resources/config.properties');
    const serverPort = props.get('vis.visualization_port');
    const visualizationServer: VisualizationServer = new VisualizationServer(Number(serverPort), mainWindow);
    
    visualizationServer.listen();
});

// OS-X specific 
app.on("activate", () => {
    if (mainWindow === null) {
        createWindow();
    }
});
