import { describe, it, expect } from "vitest";
import { productOf } from "@/application/registries/products";

describe("productOf", () => {
  it("names the coal products as WebICE's Product column does: outrights and spreads are two products", () => {
    expect(productOf("ARA", "Months")).toBe("Rotterdam Coal Futures");
    expect(productOf("ARA", "Cals")).toBe("Rotterdam Coal Futures");
    expect(productOf("ARA", "Spreads")).toBe("Rotterdam Coal Spr");
    expect(productOf("Newcastle", "Quarters")).toBe("Newcastle Coal Futures");
    expect(productOf("Newcastle", "Spreads")).toBe("Newcastle Coal Futures Spr");
  });

  it("has no product for the gas hubs, whose screens have no Product column", () => {
    expect(productOf("TTF", "Months")).toBeUndefined();
    expect(productOf("NBP", "Spreads")).toBeUndefined();
  });
});
