import * as http from "http"
import { catServer } from "./config"


// Listens for updates from the BDP client and renders the visuzalitons accordingly
export class VisualizationServer {

    private server: http.Server;
    private timeouts: Array<Number>;
    private keepAliveTimer: Number;
    private mainWindow: Electron.BrowserWindow;

    constructor(private _port: number, window: Electron.BrowserWindow) {
        this.timeouts = new Array();
        this.keepAliveTimer = 5000;
        this.mainWindow = window;
        this.server = http.createServer((function (req: any, res: any) {
            // Cancel all previous keep-alive timers and set a new one
            for (let i = 0; i < this.timeouts.length; i++) {
                clearTimeout(this.timeouts[i]);
            }
            this.timeouts.push(setTimeout((() => window.webContents.send('hasServerConnection', false)),
                 this.keepAliveTimer));
            // Notify the rendering process of the incoming connection
            this.mainWindow.webContents.send('hasServerConnection', true);
            catServer.info(() => "Accepted new connection.");
            res.end();
        }).bind(this));
    }

    get port() {
        return this._port;
    }

    listen(): void {
        catServer.info(() => "Listening for BDP client on localhost:" + this._port);
        this.server.listen(this._port);
    }

}