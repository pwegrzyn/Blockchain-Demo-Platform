import * as http from "http"
import { catServer } from "./config"

// Listens for updates from the BDP client and renders the visuzalitons accordingly
export class VisualizationServer {

    private server: http.Server;
    private _hasConnection: boolean = false;

    constructor(private _port: number) {
        this.server = http.createServer((function (req: any, res: any) {
            this._hasConnection = true;
            catServer.info(() => "Accepted new connection.");
            res.write('Hello World!');
            res.end();
        }).bind(this));
    }

    get port() {
        return this._port;
    }

    get hasConnection() {
        return this._hasConnection;
    }

    listen(): void {
        catServer.info(() => "Listening for BDP client on localhost:" + this._port);
        this.server.listen(this._port);
    }

}