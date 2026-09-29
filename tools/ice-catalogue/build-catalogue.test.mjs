// Run: node --test tools/ice-catalogue/build-catalogue.test.mjs   (Node's own test runner, no packages)
import { test } from "node:test";
import assert from "node:assert/strict";
import { classify, parseArgs, parseCsvLine } from "./build-catalogue.mjs";

// The three columns of ICE's FTPCSD file the builder reads.
const row = (symbol, name, expiry) => ({ "SYMBOL.ESIGNAL.TICKER": symbol, INSTR_NAME2: name, "EXPIRATION.DATE": expiry });

test("parseArgs keeps every file when --as-of is absent", () => {
  const { files } = parseArgs(["a.csv", "b.csv"]);
  assert.deepEqual(files, ["a.csv", "b.csv"]);
});

test("parseArgs takes the as-of date out and leaves the files", () => {
  const { files, asOf } = parseArgs(["a.csv", "--as-of", "2026-09-24", "b.csv"]);
  assert.deepEqual(files, ["a.csv", "b.csv"]);
  assert.equal(asOf, "2026-09-24");
});

test("parseArgs dates the catalogue today when no --as-of is given", () => {
  const { asOf } = parseArgs(["a.csv"], new Date("2026-09-24T12:00:00Z"));
  assert.equal(asOf, "2026-09-24");
});

test("classify lists a coal month with its hub, strip name, first delivery month and expiry", () => {
  const { outrights } = classify([row("ATW 27F-ICE", "Rotterdam Coal Futures - ARA - Jan27", "2027-01-29")]);
  assert.deepEqual(outrights, [
    { hub: "ARA", kind: "Month", name: "Jan27", symbol: "ATW 27F-ICE", start: "2027-01", expiry: "2027-01-29" },
  ]);
});

test("classify reads the strip kind from the letter after the root, for both coal hubs", () => {
  const { outrights } = classify([
    row("ATWQ 27F-ICE", "Rotterdam Coal Futures - ARA - Q1 27", "2027-01-29"),
    row("ATWY 27F-ICE", "Rotterdam Coal Futures - ARA - Cal 27", "2027-01-29"),
    row("NCF 27F-ICE", "Newcastle Coal Futures - Newcastle - Jan27", "2027-01-29"),
  ]);
  assert.deepEqual(outrights.map(o => [o.hub, o.kind, o.name]), [
    ["ARA", "Cal", "Cal 27"],
    ["ARA", "Quarter", "Q1 27"],
    ["Newcastle", "Month", "Jan27"],
  ]);
});

test("classify splits a calendar spread into its near and far legs", () => {
  const { spreads } = classify([row("ATW 27F:ATW27G-ICE", "Rotterdam Coal Spr - ARA - Jan27/Feb27", "2027-01-29")]);
  assert.deepEqual(spreads, [
    { hub: "ARA", name: "Jan27/Feb27", near: "Jan27", far: "Feb27", symbol: "ATW 27F:ATW27G-ICE", expiry: "2027-01-29" },
  ]);
});

test("classify leaves out a spread between two hubs and any family Atlas does not list", () => {
  const { outrights, spreads } = classify([
    row("ATWQ 27F:NCFQ27F-ICE", "Rotterdam vs Newcastle Coal Spr - Q1 27", "2027-01-29"),
    row("BRN 27F-ICE", "Brent Crude Futures - Jan27", "2026-11-30"),
  ]);
  assert.deepEqual(outrights, []);
  assert.deepEqual(spreads, []);
});

test("classify still lists the gas families, each with its own venue ending", () => {
  const { outrights } = classify([
    row("TFM 26V-ICN", "Dutch TTF Natural Gas Futures - TTF - Oct26", "2026-09-29"),
    row("GWM 26V-ICE", "UK Natural Gas Futures - NBP - Oct26", "2026-09-29"),
  ]);
  assert.deepEqual(outrights.map(o => [o.hub, o.symbol]), [["NBP", "GWM 26V-ICE"], ["TTF", "TFM 26V-ICN"]]);
});

test("classify orders outrights by hub, kind and first delivery month, and spreads by hub and symbol", () => {
  const { outrights, spreads } = classify([
    row("TFM 26X-ICN", "Dutch TTF Natural Gas Futures - TTF - Nov26", "2026-10-29"),
    row("ATW 27G-ICE", "Rotterdam Coal Futures - ARA - Feb27", "2027-02-26"),
    row("ATW 27F-ICE", "Rotterdam Coal Futures - ARA - Jan27", "2027-01-29"),
    row("ATW 27G:ATW27H-ICE", "Rotterdam Coal Spr - ARA - Feb27/Mar27", "2027-02-26"),
    row("ATW 27F:ATW27G-ICE", "Rotterdam Coal Spr - ARA - Jan27/Feb27", "2027-01-29"),
  ]);
  assert.deepEqual(outrights.map(o => o.name), ["Jan27", "Feb27", "Nov26"]);
  assert.deepEqual(spreads.map(s => s.name), ["Jan27/Feb27", "Feb27/Mar27"]);
});

test("parseCsvLine keeps a comma that sits inside quotes", () => {
  assert.deepEqual(parseCsvLine('a,"b, c",d'), ["a", "b, c", "d"]);
});

test("parseArgs refuses an option it does not know rather than ignoring it", () => {
  assert.throws(() => parseArgs(["--bogus", "a.csv"]), /Unknown option: --bogus/);
});
