# Notes 001 to 099

The labs of the AppSecLab notes numbered 001 to 099, one folder each. How to read a folder, and what to know before you run anything, is in the [README of the repository](../README.md).

| Note | What the lab tests | Folder |
| --- | --- | --- |
| [001 What is SAST? We ran it on four planted flaws](https://www.appseclab.io/blog/001-what-is-sast/) | What Semgrep reports, and misses, in a 31-line Express app with four planted flaws | [001-what-is-sast](001-what-is-sast/) |
| [002 What is SCA? Two dependencies, 51 packages, 8 vulnerable](https://www.appseclab.io/blog/002-what-is-sca/) | What `npm audit` finds behind two outdated dependencies | [002-what-is-sca](002-what-is-sca/) |
| [003 What is DAST? We let a fuzzer loose on a three-route API](https://www.appseclab.io/blog/003-what-is-dast/) | What a fuzzer finds in a three-route API, from its OpenAPI contract alone | [003-what-is-dast](003-what-is-dast/) |
| [004 Is your HTTPS post-quantum? We checked 19 sites](https://www.appseclab.io/blog/004-is-your-https-post-quantum/) | Which TLS key exchange a browser negotiates with 19 sites | [004-is-your-https-post-quantum](004-is-your-https-post-quantum/) |
| [005 The test no scanner wrote: two users, one invoice](https://www.appseclab.io/blog/005-two-users-one-invoice/) | A flaw two scanners do not report and a test with two users does | [005-two-users-one-invoice](005-two-users-one-invoice/) |
| [006 Our authenticated DAST scan never logged in](https://www.appseclab.io/blog/006-authenticated-scan-never-logged-in/) | The check that fails a scan with no session. Code only: there is no lab to run | [006-authenticated-scan-never-logged-in](006-authenticated-scan-never-logged-in/) |
| [007 Step one of NIST's PQC migration, run on our own site](https://www.appseclab.io/blog/007-nist-pqc-migration-step-one/) | Which key exchange a web endpoint and an FTP server accept | [007-nist-pqc-migration-step-one](007-nist-pqc-migration-step-one/) |
| [008 Node.js 22 went post-quantum and cannot tell you](https://www.appseclab.io/blog/008-node-22-post-quantum-cannot-tell-you/) | What Node.js 22 negotiates in TLS and what it tells the application | [008-node-22-post-quantum-cannot-tell-you](008-node-22-post-quantum-cannot-tell-you/) |
| [009 MAgiCS 2026: ten open papers on cryptographic migration](https://www.appseclab.io/blog/009-magics-2026-cryptographic-migration/) | Signing a 1 GiB file with ML-DSA, given the message and given the external μ | [009-magics-2026-cryptographic-migration](009-magics-2026-cryptographic-migration/) |
| [011 What is a CBOM? Eight uses of cryptography, four listed](https://www.appseclab.io/blog/011-what-is-a-cbom/) | What a CBOM generator lists, and misses, in a Node.js app with eight planted uses of cryptography | [011-what-is-a-cbom](011-what-is-a-cbom/) |
| [012 Threat intel for a quantum-safe plan: 14 of 1,792](https://www.appseclab.io/blog/012-threat-intel-quantum-safe-plan/) | How many of the vulnerabilities attackers exploit are failures of cryptography, counted on CISA's KEV catalog | [012-threat-intel-quantum-safe-plan](012-threat-intel-quantum-safe-plan/) |
| [013 What does a CBOM read in Java? 55 cases, 43 listed](https://www.appseclab.io/blog/013-what-a-cbom-reads-in-java/) | What a SonarQube plugin and cdxgen list, and get wrong, in 55 Java cases that run and in the eight uses of note 011 | [013-what-a-cbom-reads-in-java](013-what-a-cbom-reads-in-java/) |
| [014 What does an SBOM miss? Ten components, five listed](https://www.appseclab.io/blog/014-what-an-sbom-misses/) | Which of ten components planted in a Node.js app and its container image four SBOM generators list | [014-what-an-sbom-misses](014-what-an-sbom-misses/) |
