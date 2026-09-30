// ICE's contract codes, as the options desk reads them: a month letter and a two-digit year - "X26" for Nov26.
// The other Atlas screens say "Nov26", as the gas desk reads its wall screens.

const MonthCodes: Record<string, string> = {
  Jan: "F", Feb: "G", Mar: "H", Apr: "J", May: "K", Jun: "M", Jul: "N", Aug: "Q", Sep: "U", Oct: "V", Nov: "X", Dec: "Z",
};

/**
 * "Nov26" → "X26". A label that is not a month - "Q1 27", "Winter26", "Cal 27", "Jan27-Jun27" - comes back
 * unchanged: ICE's codes for strips would clash with the months ("F27" is both January and the Q1 27 strip).
 */
export function contractCode(label: string): string {
  const m = /^([A-Z][a-z]{2})(\d{2})$/.exec(label);
  const code = m ? MonthCodes[m[1]] : undefined;
  return code ? `${code}${m![2]}` : label;
}
