const FEED = 'https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json';

// The 32 CWEs the OWASP Top 10:2025 maps to A04 Cryptographic Failures
const A04 = [
  261, 296, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332,
  334, 335, 336, 337, 338, 340, 342, 347, 523, 757, 759, 760, 780, 916, 1240, 1241,
];

const kev = await (await fetch(FEED)).json();
const cwes = (entry) => (entry.cwes ?? []).map((cwe) => Number(cwe.slice(4)));
const crypto = kev.vulnerabilities.filter((entry) => cwes(entry).some((id) => A04.includes(id)));

console.log(`Catalog ${kev.catalogVersion}: ${kev.vulnerabilities.length} entries, ${crypto.length} with a CWE of A04`);
for (const entry of crypto) {
  console.log(entry.dateAdded, entry.cveID, entry.cwes.join(','), entry.vendorProject);
}
