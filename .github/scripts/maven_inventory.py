#!/usr/bin/env python3
"""Dependency-governance helpers for the Dependency scan workflow.

check-repos EFFECTIVE_POM
    Fail unless every <repository>/<pluginRepository> URL in the effective POM
    is on ALLOWED_REPOSITORY_URLS (maven-enforcer's requireNoRepositories only
    checks repository ids, not where they point).

plugins-sbom RESOLVE_PLUGINS_OUTPUT OUT_CDX_JSON
    Turn `mvn dependency:resolve-plugins -DoutputFile=...` output (build plugins
    and their dependencies) into a CycloneDX SBOM.

osv-check [--baseline BASE_CDX_JSON ...] [--output FINDINGS_JSON] CDX_JSON ...
    Query the OSV API (https://api.osv.dev/v1/querybatch) for every Maven
    component in the SBOMs. Fail if any component has a known vulnerability
    that is not already present in the baseline SBOMs (e.g. the PR base).
"""
import argparse
import json
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET

ALLOWED_REPOSITORY_URLS = {
    "https://repo.maven.apache.org/maven2",
    "https://repo.spring.io/milestone",
}

SCOPES = {"compile", "runtime", "test", "provided", "system", "import"}
OSV_QUERYBATCH_URL = "https://api.osv.dev/v1/querybatch"
OSV_BATCH_SIZE = 1000
COORDINATE = re.compile(r"^\s*([\w.\-]+):([\w.\-]+):([\w.\-]+(?::[\w.\-]+)*)\s*$")


def _local(tag):
    return tag.rsplit("}", 1)[-1]


def check_repos(path):
    root = ET.parse(path).getroot()
    seen, bad = [], []
    for el in root.iter():
        if _local(el.tag) not in ("repository", "pluginRepository"):
            continue
        fields = {_local(c.tag): (c.text or "").strip() for c in el}
        repo_id, url = fields.get("id", "?"), fields.get("url", "").rstrip("/")
        seen.append((repo_id, url))
        if url not in ALLOWED_REPOSITORY_URLS:
            bad.append((repo_id, url))
    for repo_id, url in seen:
        print("repository %s -> %s" % (repo_id, url))
    if not seen:
        print("error: no repositories found in %s" % path)
        return 1
    if bad:
        for repo_id, url in bad:
            print("error: repository '%s' points at non-allowlisted URL '%s'" % (repo_id, url))
        return 1
    return 0


def parse_coordinates(lines):
    components = {}
    for line in lines:
        m = COORDINATE.match(line)
        if not m:
            continue
        parts = line.strip().split(":")
        if parts[-1] in SCOPES:
            parts = parts[:-1]
        if len(parts) < 4:
            continue
        group, artifact, version = parts[0], parts[1], parts[-1]
        components[(group, artifact, version)] = None
    return sorted(components)


def plugins_sbom(src, out):
    with open(src) as f:
        coords = parse_coordinates(f)
    if not coords:
        print("error: no plugin coordinates parsed from %s" % src)
        return 1
    bom = {
        "bomFormat": "CycloneDX",
        "specVersion": "1.5",
        "version": 1,
        "components": [
            {
                "type": "library",
                "group": g,
                "name": a,
                "version": v,
                "purl": "pkg:maven/%s/%s@%s" % (g, a, v),
            }
            for g, a, v in coords
        ],
    }
    with open(out, "w") as f:
        json.dump(bom, f, indent=2)
    print("wrote %d build-plugin components to %s" % (len(coords), out))
    return 0


def _osv_querybatch(queries):
    req = urllib.request.Request(
        OSV_QUERYBATCH_URL,
        data=json.dumps({"queries": queries}).encode(),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp)["results"]


def _maven_components(paths):
    coords = set()
    for path in paths:
        with open(path) as f:
            for c in json.load(f).get("components", []):
                if c.get("group") and c.get("name") and c.get("version"):
                    coords.add(("%s:%s" % (c["group"], c["name"]), c["version"]))
    return sorted(coords)


def osv_findings(coords):
    findings = set()
    for start in range(0, len(coords), OSV_BATCH_SIZE):
        batch = coords[start:start + OSV_BATCH_SIZE]
        queries = [{"package": {"ecosystem": "Maven", "name": n}, "version": v} for n, v in batch]
        results = _osv_querybatch(queries)
        if len(results) != len(batch):
            raise RuntimeError("OSV returned %d results for %d queries" % (len(results), len(batch)))
        for (name, version), result in zip(batch, results):
            for vuln in result.get("vulns", []):
                findings.add((name, version, vuln["id"]))
    return findings


def osv_check(argv):
    parser = argparse.ArgumentParser(prog="maven_inventory.py osv-check")
    parser.add_argument("--baseline", action="append", default=[])
    parser.add_argument("--output")
    parser.add_argument("sboms", nargs="+")
    args = parser.parse_args(argv)

    coords = _maven_components(args.sboms)
    if not coords:
        print("error: no Maven components in %s" % ", ".join(args.sboms))
        return 1
    findings = osv_findings(coords)
    known = set()
    if args.baseline:
        known = {(n, i) for n, _, i in osv_findings(_maven_components(args.baseline))}
    new = sorted(f for f in findings if (f[0], f[2]) not in known)

    print("checked %d components against OSV: %d known vulnerabilities, %d not in baseline"
          % (len(coords), len(findings), len(new)))
    if args.output:
        with open(args.output, "w") as f:
            json.dump([{"package": n, "version": v, "id": i, "new": (n, v, i) in new}
                       for n, v, i in sorted(findings)], f, indent=2)
    for name, version, vuln_id in sorted(findings):
        if (name, version, vuln_id) not in new:
            print("baseline: %s:%s %s" % (name, version, vuln_id))
    for name, version, vuln_id in new:
        print("error: %s:%s %s" % (name, version, vuln_id))
    return 1 if new else 0


def main(argv):
    if len(argv) == 3 and argv[1] == "check-repos":
        return check_repos(argv[2])
    if len(argv) == 4 and argv[1] == "plugins-sbom":
        return plugins_sbom(argv[2], argv[3])
    if len(argv) >= 3 and argv[1] == "osv-check":
        return osv_check(argv[2:])
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
