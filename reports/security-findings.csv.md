# security-findings.csv

Findings inventory for this repository, intended as the ingestion source for an
ingestion-mode code scan.

## Provenance

- **SCA rows (235)** — the Maven dependency tree resolved from `pom.xml`
  (`mvn dependency:tree`, 86 resolved artifacts) queried against the
  [OSV.dev](https://osv.dev) API. Every row is a published advisory affecting
  the exact version currently resolved by the build; 234 of the 235 carry a CVE
  ID. Severity and CVSS vector come from the GitHub Advisory data in OSV.
  `fixed_version` is the lowest published fix newer than the resolved version.
- **SAST rows (13)** — first-party issues in `src/main/java` and
  `src/main/resources`, each verified by reading the code; identified as
  `OBS-SAST-NNN` with a CWE instead of a CVE.

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
| `package`, `installed_version`, `fixed_version` | Maven coordinates and remediation target (SCA only) |
| `file_path`, `line` | `pom.xml` for SCA rows; source location for SAST rows |
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

The SAST rows are independent of each other and each is fixable in isolation.
