#!/usr/bin/env python3
"""Replay a fixed request script against a running Online-Banking-System and
save status, content type, Location and a normalized body per request."""
import json
import os
import re
import subprocess
import sys

import requests

BASE = os.environ.get("BASE_URL", "http://localhost:8080")
MYSQL = os.environ.get("MYSQL_CMD", "docker exec -i obs-mysql mysql -uroot OnlineBankingSystem")

VOLATILE_JSON_KEYS = {"id", "userId", "date", "password", "accountNumber", "userRoleId"}

DATE_PATTERNS = [
    re.compile(r"\b\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:?\d{2})?\b"),
    re.compile(r"\b(Mon|Tue|Wed|Thu|Fri|Sat|Sun) (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \d{1,2} \d{2}:\d{2}:\d{2} \w+ \d{4}\b"),
    re.compile(r"\b\d{4}-\d{2}-\d{2}\b"),
]


def sql(stmt):
    subprocess.run(MYSQL, shell=True, input=stmt.encode(), check=True)


def mask_text(text):
    for p in DATE_PATTERNS:
        text = p.sub("<DATE>", text)
    text = re.sub(r";jsessionid=[0-9A-Fa-f]+", "", text)
    return text


def mask_json(value):
    if isinstance(value, dict):
        out = {k: ("<MASKED>" if k in VOLATILE_JSON_KEYS and v is not None else mask_json(v))
               for k, v in sorted(value.items())}
        if isinstance(out.get("authorities"), list):
            # authorities come from a HashSet, so their order is not stable
            out["authorities"] = sorted(out["authorities"], key=lambda a: json.dumps(a, sort_keys=True))
        return out
    if isinstance(value, list):
        return [mask_json(v) for v in value]
    if isinstance(value, str):
        return mask_text(value)
    return value


def normalize(resp):
    ctype = resp.headers.get("Content-Type", "")
    if "json" in ctype:
        try:
            return json.dumps(mask_json(resp.json()), indent=1, sort_keys=True)
        except ValueError:
            pass
    text = resp.text
    if "html" in ctype:
        text = re.sub(r">\s+<", "><", text)
        text = re.sub(r"\s+", " ", text).strip()
        # account numbers are random 11-digit values generated at signup
        text = re.sub(r"\b\d{8,}\b", "<NUM>", text)
    return mask_text(text)


def location(resp):
    loc = resp.headers.get("Location")
    if loc is None:
        return None
    return mask_text(loc.replace(BASE, ""))


class Recorder:
    def __init__(self, out):
        self.out = out
        self.n = 0
        self.index = []
        os.makedirs(out, exist_ok=True)

    def call(self, session, name, method, path, **kw):
        self.n += 1
        resp = session.request(method, BASE + path, allow_redirects=False, **kw)
        rec = {
            "step": self.n,
            "name": name,
            "method": method,
            "path": re.sub(r"/\d+/", "/{id}/", path.split("?")[0]),
            "status": resp.status_code,
            "content_type": resp.headers.get("Content-Type"),
            "location": location(resp),
            "body": normalize(resp),
        }
        fname = "%02d_%s.json" % (self.n, name)
        with open(os.path.join(self.out, fname), "w") as f:
            json.dump(rec, f, indent=1, sort_keys=True)
            f.write("\n")
        self.index.append({k: rec[k] for k in ("step", "name", "method", "path", "status", "location")})
        print("%02d %-6s %-45s %s %s" % (self.n, method, path, resp.status_code, rec["location"] or ""))
        return resp


def signup(r, s, name, username, email):
    return r.call(s, name, "POST", "/signup", data={
        "firstName": username.title(), "lastName": "Parity", "phone": "555-0100",
        "email": email, "username": username, "password": username + "-pass"})


def login(r, s, name, username, password):
    return r.call(s, name, "POST", "/index", data={"username": username, "password": password})


def main(out):
    sql("INSERT INTO role (role_id, name) VALUES (1, 'ROLE_USER'), (0, 'ROLE_ADMIN');")
    r = Recorder(out)

    anon = requests.Session()
    r.call(anon, "root", "GET", "/")
    r.call(anon, "index", "GET", "/index")
    r.call(anon, "signup_form", "GET", "/signup")
    r.call(anon, "userfront_anonymous", "GET", "/userFront")
    r.call(anon, "api_user_all_anonymous", "GET", "/api/user/all")
    r.call(anon, "api_preflight", "OPTIONS", "/api/user/all",
           headers={"Origin": "http://localhost:4200", "Access-Control-Request-Method": "GET"})
    r.call(anon, "static_css", "GET", "/css/main.css")
    r.call(anon, "public_unmapped_contact", "GET", "/contact")
    signup(r, anon, "signup_alice", "alice", "alice@example.com")
    signup(r, anon, "signup_alice_duplicate", "alice", "alice@example.com")
    signup(r, anon, "signup_bob", "bob", "bob@example.com")
    sql("INSERT INTO user_role (user_role_id, user_id, role_id) "
        "SELECT 1000, user_id, 0 FROM user WHERE username = 'bob';")

    alice = requests.Session()
    login(r, alice, "login_alice_bad_password", "alice", "wrong")
    login(r, alice, "login_alice", "alice", "alice-pass")
    r.call(alice, "userfront", "GET", "/userFront")
    r.call(alice, "deposit_form", "GET", "/account/deposit")
    r.call(alice, "deposit_primary", "POST", "/account/deposit", data={"accountType": "Primary", "amount": "500"})
    r.call(alice, "deposit_savings", "POST", "/account/deposit", data={"accountType": "Savings", "amount": "50"})
    r.call(alice, "withdraw_form", "GET", "/account/withdraw")
    r.call(alice, "withdraw_primary", "POST", "/account/withdraw", data={"accountType": "Primary", "amount": "25"})
    r.call(alice, "between_form", "GET", "/transfer/betweenAccounts")
    r.call(alice, "between_post", "POST", "/transfer/betweenAccounts",
           data={"transferFrom": "Primary", "transferTo": "Savings", "amount": "200"})
    r.call(alice, "primary_account", "GET", "/account/primaryAccount")
    r.call(alice, "savings_account", "GET", "/account/savingsAccount")
    r.call(alice, "recipient_list", "GET", "/transfer/recipient")
    r.call(alice, "recipient_save", "POST", "/transfer/recipient/save", data={
        "name": "carol", "email": "carol@example.com", "phone": "555-0199",
        "accountNumber": "12345678", "description": "rent"})
    r.call(alice, "recipient_edit", "GET", "/transfer/recipient/edit?recipientName=carol")
    r.call(alice, "to_someone_form", "GET", "/transfer/toSomeoneElse")
    r.call(alice, "to_someone_post", "POST", "/transfer/toSomeoneElse",
           data={"recipientName": "carol", "accountType": "Primary", "amount": "10"})
    r.call(alice, "recipient_delete", "GET", "/transfer/recipient/delete?recipientName=carol")
    r.call(alice, "appointment_form", "GET", "/appointment/create")
    r.call(alice, "appointment_post", "POST", "/appointment/create",
           data={"dateString": "2030-01-15 10:30", "location": "Main branch", "description": "loan"})
    r.call(alice, "profile", "GET", "/user/profile")
    r.call(alice, "profile_post", "POST", "/user/profile", data={
        "username": "alice", "firstName": "Alice", "lastName": "Parity",
        "email": "alice@example.com", "phone": "555-0111"})
    r.call(alice, "api_user_all_as_user", "GET", "/api/user/all")
    r.call(alice, "userfront_after", "GET", "/userFront")
    r.call(alice, "logout", "GET", "/logout")
    r.call(alice, "userfront_after_logout", "GET", "/userFront")

    bob = requests.Session()
    login(r, bob, "login_admin", "bob", "bob-pass")
    r.call(bob, "api_user_all", "GET", "/api/user/all")
    r.call(bob, "api_primary_tx", "GET", "/api/user/primary/transaction?username=alice")
    r.call(bob, "api_savings_tx", "GET", "/api/user/savings/transaction?username=alice")
    appts = r.call(bob, "api_appointment_all", "GET", "/api/appointment/all")
    appt_id = appts.json()[0]["id"]
    r.call(bob, "api_appointment_confirm", "GET", "/api/appointment/%d/confirm" % appt_id)
    r.call(bob, "api_appointment_all_confirmed", "GET", "/api/appointment/all")
    r.call(bob, "api_user_disable", "GET", "/api/user/alice/disable")
    login(r, requests.Session(), "login_alice_disabled", "alice", "alice-pass")
    r.call(bob, "api_user_enable", "GET", "/api/user/alice/enable")
    login(r, requests.Session(), "login_alice_reenabled", "alice", "alice-pass")

    with open(os.path.join(out, "index.json"), "w") as f:
        json.dump(r.index, f, indent=1)
        f.write("\n")


if __name__ == "__main__":
    main(sys.argv[1])
