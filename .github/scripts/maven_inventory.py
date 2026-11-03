#!/usr/bin/env python3
"""Dependency-governance helpers for the Dependency scan workflow.

check-repos EFFECTIVE_POM
    Fail unless every <repository>/<pluginRepository> URL in the effective POM
    is on ALLOWED_REPOSITORY_URLS (maven-enforcer's requireNoRepositories only
    checks repository ids, not where they point).

plugins-sbom RESOLVE_PLUGINS_OUTPUT OUT_CDX_JSON [EFFECTIVE_POM]
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
OSV_VULN_URL = "https://api.osv.dev/v1/vulns/"
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
    """Return {(group, artifact, version): set(plugin coordinates that pull it in)}."""
    components = {}
    plugin = None
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
        if parts[2] == "maven-plugin":
            plugin = "%s:%s:%s" % (group, artifact, version)
        components.setdefault((group, artifact, version), set()).add(plugin)
    return components


def plugin_dependency_overrides(effective_pom):
    """Return {"group:artifact" of plugin: {(group, artifact): version}} from <plugin><dependencies>."""
    overrides = {}
    for plugin in ET.parse(effective_pom).getroot().iter():
        if _local(plugin.tag) != "plugin":
            continue
        fields = {_local(c.tag): c for c in plugin}
        if "dependencies" not in fields:
            continue
        key = "%s:%s" % ((fields["groupId"].text or "").strip() if "groupId" in fields else "org.apache.maven.plugins",
                         (fields["artifactId"].text or "").strip())
        for dep in fields["dependencies"]:
            d = {_local(c.tag): (c.text or "").strip() for c in dep}
            overrides.setdefault(key, {})[(d["groupId"], d["artifactId"])] = d["version"]
    return overrides


def apply_overrides(coords, overrides):
    """resolve-plugins ignores plugin-level <dependencies>; apply them so the SBOM matches what runs."""
    result = {}
    for (g, a, v), plugins in coords.items():
        for plugin in plugins:
            pkey = ":".join(plugin.split(":")[:2]) if plugin else None
            version = overrides.get(pkey, {}).get((g, a), v)
            result.setdefault((g, a, version), set()).add(plugin)
    for plugin in {p for ps in coords.values() for p in ps if p}:
        for (g, a), version in overrides.get(":".join(plugin.split(":")[:2]), {}).items():
            result.setdefault((g, a, version), set()).add(plugin)
    return result


def plugins_sbom(src, out, effective_pom=None):
    with open(src) as f:
        coords = parse_coordinates(f)
    if effective_pom:
        coords = apply_overrides(coords, plugin_dependency_overrides(effective_pom))
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
                "properties": [{"name": "maven:plugin", "value": p} for p in sorted(coords[(g, a, v)])],
            }
            for g, a, v in sorted(coords)
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


def _osv_fixed_versions(vuln_id, package):
    with urllib.request.urlopen(OSV_VULN_URL + vuln_id, timeout=60) as resp:
        vuln = json.load(resp)
    fixed = []
    for affected in vuln.get("affected", []):
        if affected.get("package", {}).get("name") != package:
            continue
        for r in affected.get("ranges", []):
            fixed += [e["fixed"] for e in r.get("events", []) if "fixed" in e]
    return fixed


def _maven_components(paths, via=None):
    coords = set()
    for path in paths:
        with open(path) as f:
            for c in json.load(f).get("components", []):
                if c.get("group") and c.get("name") and c.get("version"):
                    coord = ("%s:%s" % (c["group"], c["name"]), c["version"])
                    coords.add(coord)
                    if via is not None:
                        via.setdefault(coord, set()).update(
                            p["value"] for p in c.get("properties", []) if p.get("name") == "maven:plugin")
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

    via = {}
    coords = _maven_components(args.sboms, via)
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
        try:
            fixed = ", ".join(_osv_fixed_versions(vuln_id, name)) or "no fix listed"
        except Exception as exc:  # detail lookup is best-effort enrichment
            fixed = "lookup failed: %s" % exc
        plugins = ", ".join(sorted(via.get((name, version), ()))) or "project dependencies"
        print("error: %s:%s %s (fixed in: %s; via %s)" % (name, version, vuln_id, fixed, plugins))
    return 1 if new else 0


def main(argv):
    if len(argv) == 3 and argv[1] == "check-repos":
        return check_repos(argv[2])
    if len(argv) in (4, 5) and argv[1] == "plugins-sbom":
        return plugins_sbom(*argv[2:])
    if len(argv) >= 3 and argv[1] == "osv-check":
        return osv_check(argv[2:])
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
