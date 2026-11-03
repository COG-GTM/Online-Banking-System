# Front-end libraries

Third-party JavaScript/CSS served by the User-Front templates (`src/main/resources/templates/common/header.html`).
They are vendored under `src/main/resources/static/` and are **not** managed by Maven, npm or any update bot, so this file
is the inventory. `FrontEndAssetsTest` fails the build if a file under `static/js` or `static/css` is missing from the
table, if its SHA-256 differs from the recorded one, or if a template references a local asset that does not exist.

## Vendored files

| File | Library | Version | Upstream | SHA-256 |
| --- | --- | --- | --- | --- |
| `static/js/jquery.js` | jQuery (minified build) | 1.11.1 | https://github.com/jquery/jquery | `24262baafef17092927c3dafe764aaa52a2a371b83ed2249cca7e414df99fac1` |
| `static/js/bootstrap.min.js` | Bootstrap | 3.3.7 | https://github.com/twbs/bootstrap | `53964478a7c634e8dad34ecc303dd8048d00dce4993906de1bacf67f663486ef` |
| `static/css/bootstrap.min.css` | Bootstrap | 3.3.7 | https://github.com/twbs/bootstrap | `f75e846cc83bd11432f4b1e21a45f31bc85283d11d372f7b19accd1bf6a2635c` |
| `static/js/jquery.dataTables.min.js` | DataTables | 1.10.12 | https://github.com/DataTables/DataTables | `4d7e8f389436bb9fda2661d327f5d42f9bd609bb8ec34010760504ce4e2f60c7` |
| `static/js/dataTables.bootstrap.min.js` | DataTables Bootstrap 3 integration | 1.10.x (no version in file) | https://github.com/DataTables/Dist-DataTables-Bootstrap | `f7462a9c7a26e23f0e85c110832508d888661984c13b9e0075c7f7603654f713` |
| `static/css/dataTables.bootstrap.min.css` | DataTables Bootstrap 3 integration | 1.10.x (no version in file) | https://github.com/DataTables/Dist-DataTables-Bootstrap | `ecc5c7ae5698fab611d69e2378b236ded82251a9905729b61569a252392c1437` |
| `static/js/bootbox.min.js` | bootbox.js | 4.4.0 | https://github.com/makeusabrew/bootbox | `0b6bab63e3aa24b4ab6aab63765a217db5004961bab1ab7d74ffebfa33efb5c4` |
| `static/js/bootstrap-datetimepicker.min.js` | smalot bootstrap-datetimepicker | unknown (no version in file) | https://github.com/smalot/bootstrap-datetimepicker | `d7f1229cfca03e86251a32c3e20c96ff78322208b180bb398870ae20a1c4644b` |
| `static/css/bootstrap-datetimepicker.min.css` | smalot bootstrap-datetimepicker | unknown (no version in file) | https://github.com/smalot/bootstrap-datetimepicker | `c702ec621db7bc81e5b34fa5ec4a8925de7833939867faef7f4cff76c0b96d6d` |
| `static/js/jquery.easing.min.js` | jQuery Easing | 1.3 | https://github.com/gdsmith/jquery.easing | `ecfc183e33d25d24aa7c06218e0a413488fff8774e4b4b87543c766db9b0b8ba` |
| `static/js/main.js` | first-party | - | this repository | - |
| `static/css/main.css` | first-party | - | this repository | - |

Loaded from a CDN, not vendored: Font Awesome 4.6.1 (`maxcdn.bootstrapcdn.com`) and Google Fonts (Passion One, Oxygen).

## Known currency gaps

These versions are old and have published advisories. Upgrading them changes behaviour, so it is not part of this inventory:

- jQuery 1.11.1: CVE-2015-9251, CVE-2019-11358, CVE-2020-11022, CVE-2020-11023 (fixed in 3.5.0+).
- Bootstrap 3.3.7: CVE-2018-14040, CVE-2018-14041, CVE-2018-14042, CVE-2019-8331 (fixed in 3.4.1).
- DataTables 1.10.12: CVE-2020-28458, CVE-2021-23445 (fixed in 1.10.23 / 1.11.3).

## Update process

1. Download the release from the upstream project (or its official CDN) and check it against the published checksum or SRI hash.
2. Replace the file under `src/main/resources/static/` and update the version and SHA-256 in the table above
   (`sha256sum src/main/resources/static/js/<file>`).
3. Run `FrontEndAssetsTest` and check the pages that use the library (DataTables: primary/savings account pages;
   bootbox: recipient delete; datetimepicker: appointment).
4. Do not add unreferenced builds (unminified copies, other DataTables themes). The test rejects files that are not
   in the table.

Moving these files to Maven-managed WebJars (`org.webjars:jquery`, `bootstrap`, `datatables`, `bootbox`) would let
dependency scanners and update bots track them. That needs Maven Central access to resolve the artifacts.
