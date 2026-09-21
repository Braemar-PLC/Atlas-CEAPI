// Stand-in for CEAPI. Serves the recorded ICE feed over a WebSocket server so the
// .NET API can be developed and tested without ICE credentials or the SDK DLLs.
//
//   node replay.mjs [--port=9002] [--file=../../webapp/test-data/ceapidata.json] [--interval=300] [--loop=false]
//
// Speaks the CEAPI wire protocol: one JSON array per text frame, a ["status", ...]
// message on connect, and "resync" from the client restarts the replay from the top.
import { WebSocketServer } from 'ws';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const args = Object.fromEntries(
  process.argv.slice(2).map(a => { const [k, v] = a.replace(/^--/, '').split('='); return [k, v ?? 'true']; })
);
const port = Number(args.port ?? 9002);
const file = resolve(here, args.file ?? '../../webapp/test-data/ceapidata.json');
const intervalMs = Number(args.interval ?? 300);
const loop = args.loop !== 'false';

const LF = String.fromCharCode(10);
const lines = readFileSync(file, 'utf8').split(LF).map(l => l.trim()).filter(Boolean);
console.log(`ceapi-replay: loaded ${lines.length} messages from ${file}`);

const wss = new WebSocketServer({ port });
wss.on('listening', () => console.log(`ceapi-replay: listening on ws://localhost:${port} (interval ${intervalMs}ms, loop=${loop})`));
wss.on('error', e => { console.error(`ceapi-replay: ${e.message}`); process.exit(1); });

wss.on('connection', (ws, req) => {
  console.log(`client connected from ${req.socket.remoteAddress}`);
  let i = 0;
  let timer = null;

  const start = () => {
    clearInterval(timer);
    i = 0;
    timer = setInterval(() => {
      if (i >= lines.length) {
        if (!loop) { clearInterval(timer); console.log('replay finished (loop=false)'); return; }
        i = 0;
      }
      ws.send(lines[i++]);
    }, intervalMs);
  };

  ws.send(JSON.stringify(['status', 'replay connected']));
  start();

  ws.on('message', data => {
    const cmd = data.toString().trim().toLowerCase();
    if (cmd === 'resync') { console.log('resync received: restarting from the top'); start(); }
    else console.log(`unknown command ignored: ${cmd}`);
  });
  ws.on('close', () => { clearInterval(timer); console.log('client disconnected'); });
  ws.on('error', e => console.error(`client error: ${e.message}`));
});
