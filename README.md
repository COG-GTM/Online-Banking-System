# Online-Banking-System
Spring Boot Online Banking System 
About
This is a project for practicing Spring + Thymeleaf. The idea was to build online banking system.

It was made using Spring Boot, Spring Security, Thymeleaf, Spring Data JPA, Spring Data REST, JavaScript, JQuery. Database is in memory sql Workbench.
Online Banking Requirements

About

The Banking system consists of two parts: User-Front and Admin-Portal. User-Front is a user-facing system and it includes such modules as User Signup/Login, Account, Transfer, Appointment, Transaction and User Profile. Admin-Portal is mainly used by Admin and it involves User Account and Appointment modules.


Er diagram

![er diagram](https://user-images.githubusercontent.com/34470526/37703339-8e85fcae-2d1f-11e8-900f-94cb2046d97f.png)



Online banking system detail diagram

![online banking system detail diagram](https://user-images.githubusercontent.com/34470526/37703353-999023fe-2d1f-11e8-96f6-db40724c5d14.png)


## Dependency governance

Third-party versions are declared in `pom.xml` (the Spring Boot parent manages most transitive versions) and are reviewed through:

* **Dependabot** (`.github/dependabot.yml`): weekly update PRs for Maven dependencies, the Spring Boot parent, Maven plugins and GitHub Actions.
* **Dependency scan workflow** (`.github/workflows/dependency-scan.yml`), on pull requests, `master` pushes, weekly and on demand:
  * `maven-enforcer-plugin` checks on every build (Maven 3.6.3+, Java 8+, no duplicate dependency declarations, no repositories other than Maven Central and Spring milestones).
  * Every repository URL in the effective POM must be Maven Central or `https://repo.spring.io/milestone` (`.github/scripts/maven_inventory.py check-repos`).
  * A CycloneDX SBOM of the Maven-resolved dependencies (`target/bom.json`) and of the build plugins plus their dependencies (`target/plugins.cdx.json`), uploaded with the dependency tree and `osv-findings.json` as the `dependency-inventory` artifact.
  * Both SBOMs are checked against the OSV database (`maven_inventory.py osv-check`). On pull requests only vulnerabilities not already present on the base branch fail the job; on `master`, weekly and on demand any known vulnerability fails it.

Review cadence:

* Weekly: triage Dependabot PRs and the scheduled OSV-Scanner report.
* Fix critical/high findings within 14 days, medium within 30 days, low at the next quarterly review (or record why a finding does not apply).
* Quarterly: review the Spring Boot parent line. The current parent (`2.0.0.M7`) is a pre-GA milestone, which is why the Spring milestone repository is still allowed; moving to a supported GA release removes that exception.
