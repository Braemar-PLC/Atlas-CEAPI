import { describe, it, expect } from "vitest";
import { contractCode } from "@/application/modules/calculator/contract-code";

describe("contractCode", () => {
  it("turns a month into ICE's letter and two-digit year", () => {
    expect(["Jan27", "Feb27", "Mar27", "Apr27", "May27", "Jun27", "Jul27", "Aug27", "Sep27", "Oct26", "Nov26", "Dec26"].map(contractCode))
      .toEqual(["F27", "G27", "H27", "J27", "K27", "M27", "N27", "Q27", "U27", "V26", "X26", "Z26"]);
  });

  it("leaves a strip's name alone, because ICE's strip codes would clash with the months", () => {
    for (const label of ["Q1 27", "Winter26", "Summer27", "Cal 27", "Jan27-Jun27"]) {
      expect(contractCode(label)).toBe(label);
    }
  });

  it("leaves anything it does not recognise alone", () => {
    expect(contractCode("Nov2026")).toBe("Nov2026");
    expect(contractCode("Xyz26")).toBe("Xyz26");
    expect(contractCode("")).toBe("");
  });
});
