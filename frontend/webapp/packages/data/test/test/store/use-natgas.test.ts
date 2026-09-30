// packages/data/test/test/store/useNatGas.test.ts
import { describe, it, expect, beforeEach } from "vitest";
import { NatGasCurveMap, useNatGas } from "@atlas/data";
// Reset Zustand state before each test
beforeEach(() => {
  const { clear } = useNatGas.getState();
  clear();
});

describe("useNatGas store", () => {

  const snapshot: NatGasCurveMap = {
    NBP: {
      "Apr-26": { instrument: "NBP", tenor: "Apr-26", last: 10 },
      "May-26": { instrument: "NBP", tenor: "May-26", bid: 1.1 },
    },
    TTF: {
      "Apr-26": { instrument: "TTF", tenor: "Apr-26", ask: 22 },
    },
  };

  it("push loads full snapshot", () => {
    useNatGas.getState().push(snapshot);

    const curves = useNatGas.getState().curves;
    expect(curves.NBP!["Apr-26"]!.last).toBe(10);
    expect(curves.NBP!["May-26"]!.bid).toBe(1.1);
    expect(curves.TTF!["Apr-26"]!.ask).toBe(22);
  });

  it("patch merges into existing quote", () => {
    useNatGas.getState().push(snapshot);

    useNatGas.getState().patch({
      instrument: "NBP",
      tenor: "Apr-26",
      last: 99,
    });

    const q = useNatGas.getState().curves.NBP!["Apr-26"]!;
    expect(q.last).toBe(99); // updated
    expect(q.instrument).toBe("NBP"); // preserved
  });

  it("patch creates instrument & tenor if missing", () => {
    useNatGas.getState().patch({
      instrument: "NBP",
      tenor: "Dec-26",
      bid: 7,
    });

    const q = useNatGas.getState().curves.NBP!["Dec-26"]!;
    expect(q.bid).toBe(7);
  });

  it("patch marks the tick up when last rises and down when it falls", () => {
    const { patch } = useNatGas.getState();
    const tick = () => useNatGas.getState().curves.TTF!["Oct-26"]!.tick;

    patch({ instrument: "TTF", tenor: "Oct-26", last: 77.85 });
    expect(tick()).toBeUndefined(); // first price: nothing to compare with

    patch({ instrument: "TTF", tenor: "Oct-26", last: 77.895 });
    expect(tick()).toBe("up");

    patch({ instrument: "TTF", tenor: "Oct-26", last: 77.89 });
    expect(tick()).toBe("down");
  });

  it("patch keeps the tick when last is unchanged or absent", () => {
    const { patch } = useNatGas.getState();
    const tick = () => useNatGas.getState().curves.TTF!["Nov-26"]!.tick;

    patch({ instrument: "TTF", tenor: "Nov-26", last: 77.5 });
    patch({ instrument: "TTF", tenor: "Nov-26", last: 77.54 });

    patch({ instrument: "TTF", tenor: "Nov-26", last: 77.54, bid: 77.475 }); // same last again
    expect(tick()).toBe("up");

    patch({ instrument: "TTF", tenor: "Nov-26", bid: 77.48 }); // no last at all
    expect(tick()).toBe("up");
  });

  it("patch does nothing when instrument OR tenor missing", () => {
    useNatGas.getState().patch({ instrument: "NBP" });
    useNatGas.getState().patch({ tenor: "X" });

    expect(useNatGas.getState().curves).toEqual({});
  });

  it("clear wipes curves", () => {
    useNatGas.getState().push(snapshot);

    useNatGas.getState().clear();
    expect(useNatGas.getState().curves).toEqual({});
  });

  it("getQuote returns expected quote", () => {
    useNatGas.getState().push(snapshot);
    const q = useNatGas.getState().getQuote("NBP", "Apr-26");

    expect(q?.last).toBe(10);
  });

  it("getQuote returns undefined when missing", () => {
    expect(useNatGas.getState().getQuote("NBP", "ZZZ")).toBeUndefined();
  });

  it("getMany returns array of quotes in order", () => {
    useNatGas.getState().push(snapshot);

    const out = useNatGas.getState().getMany([
      { instrument: "NBP", tenor: "Apr-26" },
      { instrument: "TTF", tenor: "Apr-26" },
    ]);

    expect(out[0]!.last).toBe(10);
    expect(out[1]!.ask).toBe(22);
  });

  it("getByTenor returns map for all instruments", () => {
    useNatGas.getState().push(snapshot);

    const map = useNatGas.getState().getByTenor("Apr-26");

    expect(map.NBP!.last).toBe(10);
    expect(map.TTF!.ask).toBe(22);
  });

  it("getByTenor with explicit instruments filters correctly", () => {
    useNatGas.getState().push(snapshot);

    const map = useNatGas.getState().getByTenor("Apr-26", ["NBP"]);

    expect(map.NBP!.last).toBe(10);
    expect(map.TTF).toBeUndefined(); // not requested
  });

});
describe("useNatGas patchMany", () => {
  it("applies a batch of deltas as one state change, ticks included", () => {
    let changes = 0;
    const stop = useNatGas.subscribe(() => { changes++; });

    useNatGas.getState().patchMany([
      { instrument: "TTF", tenor: "Oct-26", last: 77.85 },
      { instrument: "NBP", tenor: "Oct-26", bid: 100.5 },
      { instrument: "TTF", tenor: "Oct-26", last: 77.9 }, // the same strip again, later in the batch
    ]);
    stop();

    expect(changes).toBe(1);
    const ttf = useNatGas.getState().curves.TTF!["Oct-26"]!;
    expect(ttf.last).toBe(77.9);
    expect(ttf.tick).toBe("up");
    expect(useNatGas.getState().curves.NBP!["Oct-26"]!.bid).toBe(100.5);
  });

  it("changes nothing for an empty batch or one with nothing to apply", () => {
    let changes = 0;
    const stop = useNatGas.subscribe(() => { changes++; });

    useNatGas.getState().patchMany([]);
    useNatGas.getState().patchMany([{ instrument: "NBP" }, { tenor: "X" }]);
    stop();

    expect(changes).toBe(0);
    expect(useNatGas.getState().curves).toEqual({});
  });
});
