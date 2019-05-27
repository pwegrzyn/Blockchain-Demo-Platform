import * as http from "http"
import { catServer } from "./config"

// Listens for updates from the BDP client and renders the visuzalitons accordingly
export class VisualizationServer {

    private server: http.Server;
    
    constructor(private _port: number) {
        this.server = http.createServer(function (req, res) {
            res.write('Hello World!');
            res.end();
        });
    }

    get Port() {
        return this._port;
    }

    listen(): void {
        catServer.info(() => "Listening for BDP client on localhost:" + this._port);
        this.server.listen(this._port);
    }

}