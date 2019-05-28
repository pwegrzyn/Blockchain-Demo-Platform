import * as http from "http"
import { catServer } from "./config"

// Listens for updates from the BDP client and renders the visuzalitons accordingly
export class VisualizationServer {

    private server: http.Server;

    constructor(private _port: number, connectionCallback: () => void) {
        this.server = http.createServer((function (req: any, res: any) {
            connectionCallback();
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