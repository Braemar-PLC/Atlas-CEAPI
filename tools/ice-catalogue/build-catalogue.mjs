// Builds webapp/src/Atlas.Web.Ice/Catalogue/ice-instruments.json from ICE's static reference files.
//
// Why: Atlas decides which strips to show by rule ("the next 20 months", "Oct26 against Nov26", ...). To turn a
// rule into something ICE understands it needs, for every gas contract: ICE's code (the E-Signal ticker), the
// strip name ICE uses ("Oct26", "Q4 26", "Winter26/Summer27") and the date the contract stops trading.
// ICE publishes all of that in FTPCSD_<source>.csv. Those files are large and are not kept in this repo, so this
// script copies out just the rows Atlas needs.
//
// Run it again whenever ICE sends fresh files (they list new contracts as time goes on):
//   1. Unzip:  bzip2 -dkc FTPCSD_270.csv.bz2 > FTPCSD_270.csv      (same for 756)
//   2. node tools/ice-catalogue/build-catalogue.mjs <FTPCSD_270.csv> <FTPCSD_756.csv> --as-of 2026-09-17
//
// No npm packages are needed - plain Node.
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

// Which contract families Atlas cares about. `root` is the start of ICE's ticker, `venue` its ending.
const HUBS = [
  { hub: "TTF", root: "TFM", venue: "ICN" }, // ICE Endex Dutch TTF gas futures (source 270)
  { hub: "NBP", root: "GWM", venue: "ICE" }, // ICE Futures Europe UK NBP gas futures (source 756)
];

// ICE's month letters, January to December.
const MONTH_CODES = "FGHJKMNQUVXZ";
// The letter after the root says what kind of strip it is; no letter means a single month.
const KINDS = { "": "Month", Q: "Quarter", S: "Season", Y: "Cal" };

function parseCsvLine(line) {
  const out = [];
  let cur = "", quoted = false;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (quoted) {
      if (c === '"') { if (line[i + 1] === '"') { cur += '"'; i++; } else quoted = false; }
      else cur += c;
    } else if (c === '"') quoted = true;
    else if (c === ",") { out.push(cur); cur = ""; }
    else cur += c;
  }
  out.push(cur);
  return out;
}

function readRows(file) {
  const lines = readFileSync(file, "latin1").split(/\r?\n/).filter(Boolean);
  const head = parseCsvLine(lines[0]).map(h => h.replace(/[<>]/g, ""));
  return lines.slice(1).map(line => {
    const values = parseCsvLine(line);
    return Object.fromEntries(head.map((h, i) => [h, values[i]]));
  });
}

// "2026-10" for the first delivery month, from the two-digit year and the month letter in the ticker.
const startOf = (yy, code) => `${2000 + Number(yy)}-${String(MONTH_CODES.indexOf(code) + 1).padStart(2, "0")}`;

const args = process.argv.slice(2);
const asOfAt = args.indexOf("--as-of");
const asOf = asOfAt >= 0 ? args[asOfAt + 1] : new Date().toISOString().slice(0, 10);
const files = args.filter((a, i) => !a.startsWith("--") && i !== asOfAt + 1);
if (files.length === 0) {
  console.error("Usage: node build-catalogue.mjs <FTPCSD_270.csv> <FTPCSD_756.csv> [--as-of YYYY-MM-DD]");
  process.exit(1);
}

const rows = files.flatMap(readRows);
const outrights = [], spreads = [];

for (const { hub, root, venue } of HUBS) {
  const outright = new RegExp(`^${root}([QSY]?) ([0-9]{2})([${MONTH_CODES}])-${venue}$`);
  const spread = new RegExp(`^${root}([QSY]?) ([0-9]{2})([${MONTH_CODES}]):${root}([QSY]?)([0-9]{2})([${MONTH_CODES}])-${venue}$`);

  for (const r of rows) {
    const symbol = r["SYMBOL.ESIGNAL.TICKER"] ?? "";
    // ICE's description ends with the strip name: "Dutch TTF Natural Gas Futures - TTF - Oct26".
    const name = (r["INSTR_NAME2"] ?? "").split(" - ").pop();
    const expiry = r["EXPIRATION.DATE"];

    let m = outright.exec(symbol);
    if (m) {
      outrights.push({ hub, kind: KINDS[m[1]], name, symbol, start: startOf(m[2], m[3]), expiry });
      continue;
    }
    m = spread.exec(symbol);
    if (m) {
      const [near, far] = name.split("/");
      spreads.push({ hub, name, near, far, symbol, expiry });
    }
  }
}

const byHubKindStart = (a, b) =>
  a.hub.localeCompare(b.hub) || (a.kind ?? "").localeCompare(b.kind ?? "") || (a.start ?? a.name).localeCompare(b.start ?? b.name);
outrights.sort(byHubKindStart);
spreads.sort((a, b) => a.hub.localeCompare(b.hub) || a.symbol.localeCompare(b.symbol));

// One entry per line keeps the file readable and keeps git diffs small when ICE lists new contracts.
const lines = list => list.map(x => "    " + JSON.stringify(x)).join(",\n");
const json =
  `{\n  "source": "ICE static reference files FTPCSD_270 (ICE Endex) and FTPCSD_756 (ICE Futures Europe)",\n` +
  `  "asOf": ${JSON.stringify(asOf)},\n  "outrights": [\n${lines(outrights)}\n  ],\n  "spreads": [\n${lines(spreads)}\n  ]\n}\n`;

const here = dirname(fileURLToPath(import.meta.url));
const target = resolve(here, "../../webapp/src/Atlas.Web.Ice/Catalogue/ice-instruments.json");
writeFileSync(target, json);

for (const { hub } of HUBS) {
  const count = kind => outrights.filter(o => o.hub === hub && o.kind === kind).length;
  console.log(`${hub}: ${count("Month")} months, ${count("Quarter")} quarters, ${count("Season")} seasons, ${count("Cal")} cals, ` +
    `${spreads.filter(s => s.hub === hub).length} spreads`);
}
console.log(`Wrote ${target}`);
