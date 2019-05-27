import { app, BrowserWindow } from "electron";
import * as path from "path";

let mainWindow: Electron.BrowserWindow;

function createWindow() {

    mainWindow = new BrowserWindow({
        height: 600,
        width: 800,
    });

    mainWindow.loadFile(path.join(__dirname, "../src/main/resources/templates/index.html"));

    mainWindow.on("closed", () => {
        mainWindow = null;
        app.quit();
    });

}

app.on("ready", createWindow);

// OS-X specific 
app.on("activate", () => {
    if (mainWindow === null) {
        createWindow();
    }
});