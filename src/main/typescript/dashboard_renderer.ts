import { ipcRenderer } from "electron";

// Check the connection
function update_dashboard(isConnected: boolean): void {
    if (!isConnected) {
        document.getElementById("mainContent").style.display = "none";
        document.getElementById("loadingIcon").style.display = "block";
    } else {
        document.getElementById("mainContent").style.display = "block";
        document.getElementById("loadingIcon").style.display = "none";
    } 
}

ipcRenderer.on('hasServerConnection', function(event: any, arg: any) {
    update_dashboard(arg);
})

update_dashboard(false)