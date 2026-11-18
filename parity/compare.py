#!/usr/bin/env python3
"""Diff two capture directories. Usage: parity/compare.py parity/baseline parity/migrated"""
import difflib
import json
import os
import sys

FIELDS = ("method", "path", "status", "content_type", "location", "cors_headers", "body")


def load(d):
    return {f: json.load(open(os.path.join(d, f)))
            for f in sorted(os.listdir(d)) if f[0].isdigit()}


def main(a, b):
    old, new = load(a), load(b)
    mismatches = 0
    for name in sorted(set(old) | set(new)):
        if name not in old or name not in new:
            print("MISSING", name)
            mismatches += 1
            continue
        diffs = [f for f in FIELDS if old[name].get(f) != new[name].get(f)]
        if diffs:
            mismatches += 1
            print("MISMATCH %s: %s" % (name, ", ".join(diffs)))
            for f in diffs:
                if f == "body":
                    for line in difflib.unified_diff(
                            old[name][f].replace("><", ">\n<").splitlines(),
                            new[name][f].replace("><", ">\n<").splitlines(), lineterm="", n=1):
                        print("   ", line[:300])
                else:
                    print("    %s: %r -> %r" % (f, old[name].get(f), new[name].get(f)))
    endpoints = {(r["method"], r["path"]) for r in old.values()}
    print("requests compared: %d, distinct endpoints: %d, mismatches: %d"
          % (len(old), len(endpoints), mismatches))
    return 1 if mismatches else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1], sys.argv[2]))
