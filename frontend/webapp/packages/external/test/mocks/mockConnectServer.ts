import { readFileSync } from "fs";
import { createServer } from "http";
import path from "path";

type Delta = {
  messageType: "Update";
  symbol: string;
  fields: Record<string, unknown>;
  timestamp: string;
};

export function startMockConnectServer(port = 9999) {
  const baseDir = __dirname;
  const payloadPath = path.join(baseDir, "/..", "dummyData", "dummyNatGas.payload.json");
  const deltasPath = path.join(baseDir,"/..", "dummyData", "dummyNatGas.deltas.json");

  // These files are the ones you already saved on disk
  const payload = JSON.parse(readFileSync(payloadPath, "utf8"));
  const deltas: Delta[] = JSON.parse(readFileSync(deltasPath, "utf8"));

  const server = createServer((req, res) => {
    if (req.url !== "/connect") {
      res.writeHead(404).end();
      return;
    }

    // --- SSE handshake
    res.writeHead(200, {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache",
      Connection: "keep-alive",
      // Optional reconnection advice for the client:
      // "Access-Control-Allow-Origin": "*"  // if you need CORS in browser tests
    });

    // Helper: write SSE frame
    let seq = 1;
    const send = (evt: string, data: unknown, id?: number) => {
      if (id) res.write(`id: ${id}\n`);
      res.write(`event: ${evt}\n`);
      res.write(`data: ${JSON.stringify(data)}\n\n`);
    };

    // 1) Immediately send the snapshot (full payload) as the first event
    //    Keep it compact or pre-map to your FE format if preferred.
    send("snapshot", payload, seq++);

    // 2) Start delta pump (about 20 per second)
    let i = 0;
    const pump = setInterval(() => {
      // Heartbeat every ~2s to keep proxies alive
      if (seq % 40 === 0) {
        res.write(`: keepalive ${Date.now()}\n\n`);
      }

      const update = deltas[i++];
      if (!update) {
        clearInterval(pump);
        res.write(`event: end\ndata: {}\n\n`);
        res.end(); // close stream after the last delta
        return;
      }

      send("update", update, seq++);
    }, 50);

    // 3) Handle client disconnect
    req.on("close", () => clearInterval(pump));
  });

  server.listen(port, () => {
    console.log(`Mock /connect SSE server on http://localhost:${port}`);
  });

  return () => server.close();
}