#!/usr/bin/env python3
"""Dependency-governance helpers for the Dependency scan workflow.

check-repos EFFECTIVE_POM
    Fail unless every <repository>/<pluginRepository> URL in the effective POM
    is on ALLOWED_REPOSITORY_URLS (maven-enforcer's requireNoRepositories only
    checks repository ids, not where they point).

plugins-sbom RESOLVE_PLUGINS_OUTPUT OUT_CDX_JSON
    Turn `mvn dependency:resolve-plugins -DoutputFile=...` output (build plugins
    and their dependencies) into a CycloneDX SBOM so OSV-Scanner can scan it.
"""
import json
import re
import sys
import xml.etree.ElementTree as ET

ALLOWED_REPOSITORY_URLS = {
    "https://repo.maven.apache.org/maven2",
    "https://repo.spring.io/milestone",
}

SCOPES = {"compile", "runtime", "test", "provided", "system", "import"}
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


def main(argv):
    if len(argv) == 3 and argv[1] == "check-repos":
        return check_repos(argv[2])
    if len(argv) == 4 and argv[1] == "plugins-sbom":
        return plugins_sbom(argv[2], argv[3])
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
