# security-findings.csv

Findings inventory for this repository, intended as the ingestion source for an
ingestion-mode code scan.

## Provenance

276 findings: 249 SCA, 27 SAST.

- **SCA rows, Maven (235)** — the Maven dependency tree resolved from `pom.xml`
  (`mvn dependency:tree`, 86 resolved artifacts) queried against the
  [OSV.dev](https://osv.dev) API. Every row is a published advisory affecting
  the exact version currently resolved by the build; 234 of the 235 carry a CVE
  ID. Severity and CVSS vector come from the GitHub Advisory data in OSV.
  `fixed_version` is the lowest published fix newer than the resolved version.
- **SCA rows, npm (14)** — four JavaScript libraries are vendored into
  `src/main/resources/static/js` and ship in the war. Their versions were read
  from the file headers (jQuery 1.11.1, Bootstrap 3.3.7, DataTables 1.10.12,
  Bootbox 4.4.0) and queried against OSV under npm coordinates. `file_path`
  points at the vendored file, so each of these is fixed by swapping one file
  rather than by a dependency bump.
- **SAST rows (27)** — first-party issues in `src/main/java` and
  `src/main/resources`, each verified by reading the code; identified as
  `OBS-SAST-NNN` with a CWE instead of a CVE.

Every SCA row was checked against the advisory's `affected` ranges and version
lists for the exact installed version, and every `fixed_version` was checked to
be newer than the installed version and absent from the affected set.

Regenerating the SCA rows is a re-run of `mvn dependency:tree` plus an OSV
`querybatch` call; nothing in the CSV is hand-authored except the SAST rows.

## Columns

| column | meaning |
| --- | --- |
| `finding_id` | CVE ID for SCA rows, `OBS-SAST-NNN` for code findings |
| `finding_type` | `SCA` or `SAST` |
| `cve_id` / `advisory_id` | CVE and GHSA identifiers (advisory ID falls back to the OSV ID) |
| `severity` | Critical / High / Medium / Low |
| `cvss_vector` | CVSS v3.1 or v4.0 vector where OSV publishes one |
| `cwe` | semicolon-separated CWE IDs |
| `ecosystem` | `Maven` or `npm` (SCA only) |
| `package`, `installed_version`, `fixed_version` | package coordinates and remediation target (SCA only) |
| `file_path`, `line` | `pom.xml` or the vendored JS file for SCA rows; source location for SAST rows |
| `title`, `description` | advisory summary/details, or the finding write-up |
| `references` | advisory and upstream issue links |
| `remediation` | concrete fix for this row |

## Context for triage

The dependency findings all trace back to one root cause: the project inherits
`spring-boot-starter-parent:2.0.0.M7`, a 2017 milestone release, so the whole
transitive set (Tomcat 8.5.23, Jackson 2.9.2, Spring Framework 5.0.2, Spring
Security 5.0.0, Hibernate 5.2.12, snakeyaml 1.19, MySQL Connector/J 5.1.44,
logback 1.2.3) predates hundreds of advisories. `jackson-databind` (69) and
`tomcat-embed-core` (53) account for half the rows on their own. Any triage pass
should expect heavy duplication by package and treat "upgrade the parent BOM" as
the shared fix, with per-CVE reachability the interesting question.

The npm rows are the exception among SCA findings: each vendored library is a
separate file swap, so they triage and remediate independently.

The SAST rows are independent of each other and each is fixable in isolation.
They span authorization (IDOR on the payee and transfer paths, profile update
keyed on a request parameter), authentication and session configuration (CSRF
disabled, hardcoded BCrypt seed, no brute-force protection, no transport
security, no password policy), money handling (unvalidated transfer amounts, no
balance check, non-transactional read-modify-write balance updates, `double`
arithmetic, sequential account numbers), data exposure (password in `toString`,
full `User` entities on the admin API, password echoed into the signup form,
committed database credentials) and reliability (null dereference in appointment
confirmation, unbounded `findAll` on the payee list).

Note that a few of the lower-severity SAST rows are correctness or reliability
defects rather than exploitable vulnerabilities (`OBS-SAST-023`, `-024`, `-025`,
`-026`); they are included because they are real, independently fixable, and
useful triage material, but a scan profile that only wants exploitable findings
should drop them.
