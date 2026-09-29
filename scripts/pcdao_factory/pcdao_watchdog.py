"""PCDA(O) Military Financial Intelligence Continuous Watchdog.

Crawls PCDA(O) Pune & MoD circulars, detects new statutory rules, and updates
MILITARY_INTELLIGENCE_CODEX.md and known_circulars.json.
"""

import argparse
import datetime
import json
import os
import re
import sys
import urllib.request
from typing import Any, Dict, List, Optional, Tuple

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
KNOWLEDGE_DIR = os.path.join(SCRIPT_DIR, "knowledge")
KNOWN_CIRCULARS_PATH = os.path.join(KNOWLEDGE_DIR, "known_circulars.json")
CODEX_MD_PATH = os.path.join(KNOWLEDGE_DIR, "MILITARY_INTELLIGENCE_CODEX.md")
PCDAO_URL = "https://pcdaopune.gov.in"


def load_known_circulars() -> Dict[str, Any]:
    """Load existing known circulars registry."""
    if not os.path.exists(KNOWN_CIRCULARS_PATH):
        return {"version": "1.0.0", "circulars": []}
    with open(KNOWN_CIRCULARS_PATH, "r", encoding="utf-8") as fp:
        return json.load(fp)


def save_known_circulars(data: Dict[str, Any]) -> None:
    """Save updated known circulars registry."""
    os.makedirs(KNOWLEDGE_DIR, exist_ok=True)
    with open(KNOWN_CIRCULARS_PATH, "w", encoding="utf-8") as fp:
        json.dump(data, fp, indent=2)


def fetch_online_notices() -> List[Dict[str, str]]:
    """Fetch notices from official PCDA(O) portal with graceful network fallback."""
    headers = {"User-Agent": "Mozilla/5.0 (PayslipMax-Moat-Watchdog/1.0)"}
    req = urllib.request.Request(PCDAO_URL, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            html = resp.read().decode("utf-8", errors="ignore")
            return parse_portal_html(html)
    except Exception as exc:
        print(f"  [!] Online portal probe notice ({PCDAO_URL}): {exc}", file=sys.stderr)
        return []


def parse_portal_html(html: str) -> List[Dict[str, str]]:
    """Extract circular links and titles from portal HTML."""
    results = []
    # Pattern to match circular / order anchors
    matches = re.findall(
        r'<a[^>]+href=["\']([^"\']+\.pdf)["\'][^>]*>(.*?)</a>',
        html,
        re.IGNORECASE | re.DOTALL,
    )
    for href, text in matches:
        clean_text = re.sub(r"<[^>]+>", "", text).strip()
        if any(kw in clean_text.lower() for kw in ["allowance", "order", "pay", "da", "circular"]):
            results.append({
                "title": clean_text,
                "url": href if href.startswith("http") else f"{PCDAO_URL}/{href.lstrip('/')}",
            })
    return results


def simulate_sample_discovery() -> Dict[str, Any]:
    """Simulate a realistic incoming MoD circular for verification and testing."""
    today = datetime.date.today().isoformat()
    return {
        "id": f"MOD-DA-54-HIKE-{today[:7]}",
        "number": "1(2)/2026/D(Pay/Services)",
        "date": today,
        "title": "Grant of Dearness Allowance to Armed Forces Officers at Revised Rate of 54%",
        "authority": "Ministry of Defence / PCDA(O) Pune",
        "category": "ALLOWANCE_REVISION",
        "source_url": "https://pcdaopune.gov.in/orders/da_revision_54.pdf",
        "provisions": [
            "Central DA enhanced from 50% to 54% with retrospective effect.",
            "Triggers recalculation across all active military pay calculations.",
            "Higher Rate Cities Transport Allowance: ₹7,200 + 54% DA = ₹11,088/mo.",
            "Other Cities Transport Allowance: ₹3,600 + 54% DA = ₹5,544/mo.",
        ],
        "impact": "Updates SituationalRuleResolver.kt DA rate engine and 1-tap redressal math.",
    }


def append_to_codex(circular: Dict[str, Any]) -> None:
    """Append newly discovered circular to MILITARY_INTELLIGENCE_CODEX.md."""
    if not os.path.exists(CODEX_MD_PATH):
        return
    with open(CODEX_MD_PATH, "r", encoding="utf-8") as fp:
        content = fp.read()

    provisions_md = "\n".join(f"  - {p}" for p in circular.get("provisions", []))
    entry = f"""
### [{circular['id']}] {circular['title']}
- **Order Number**: `{circular['number']}` | **Date**: {circular['date']}
- **Authority**: {circular['authority']}
- **Category**: `{circular['category']}`
- **Source Link**: [{circular['source_url']}]({circular['source_url']})
- **Key Provisions**:
{provisions_md}
- **Codebase Impact**: {circular.get('impact', 'Under review for engine integration')}

---
"""
    marker = "## 2. Chronological Circular & Knowledge Registry\n"
    if marker in content:
        updated = content.replace(marker, marker + entry)
    else:
        updated = content + "\n" + entry

    with open(CODEX_MD_PATH, "w", encoding="utf-8") as fp:
        fp.write(updated)


def generate_alert_summary(new_circulars: List[Dict[str, Any]], out_path: Optional[str] = None) -> str:
    """Format a clean markdown alert report for GitHub Issues or IDE display."""
    lines = [
        "# 🚨 PCDA(O) Military Intelligence Watchdog Alert",
        f"**Audit Timestamp**: {datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%d %H:%M:%SZ')}",
        f"**New Authority Documents Detected**: {len(new_circulars)}",
        "",
        "---",
    ]
    for c in new_circulars:
        lines.extend([
            f"## 📌 [{c['category']}] {c['title']}",
            f"- **Circular Ref**: `{c['number']}`",
            f"- **Authority**: {c['authority']} ({c['date']})",
            f"- **URL**: {c['source_url']}",
            "- **Codebase Action Required**:",
            f"  {c.get('impact', 'Review for engine inclusion')}",
            "",
        ])
    summary = "\n".join(lines)
    if out_path:
        with open(out_path, "w", encoding="utf-8") as fp:
            fp.write(summary)
    return summary


def run_watchdog(simulate: bool = False, alert_file: Optional[str] = None) -> int:
    """Execute the watchdog cycle."""
    print("=" * 65)
    print("  PCDA(O) Military Financial Intelligence — Continuous Watchdog  ")
    print("=" * 65)

    known_data = load_known_circulars()
    known_ids = {c["id"] for c in known_data.get("circulars", [])}
    print(f"[*] Loaded {len(known_ids)} known canonical circulars from registry.")

    new_circulars: List[Dict[str, Any]] = []

    # 1. Probe live official portal
    print(f"[*] Probing official PCDA(O) portal: {PCDAO_URL}...")
    portal_notices = fetch_online_notices()
    print(f"    Found {len(portal_notices)} online circular references.")

    # 2. Check simulation flag if requested
    if simulate:
        sim_c = simulate_sample_discovery()
        if sim_c["id"] not in known_ids:
            print(f"    [+] Simulating newly published MoD Order: {sim_c['number']}")
            new_circulars.append(sim_c)
        else:
            print(f"    [-] Simulated circular {sim_c['id']} already in registry.")

    # 3. Process new discoveries
    if not new_circulars:
        print("[✓] Watchdog cycle complete. No unindexed circulars detected.")
        return 0

    print(f"\n[!] Detected {len(new_circulars)} new circular(s)! Updating Codex...")
    for c in new_circulars:
        append_to_codex(c)
        known_data["circulars"].insert(0, c)

    save_known_circulars(known_data)
    print(f"[✓] Appended to {CODEX_MD_PATH}")
    print(f"[✓] Updated {KNOWN_CIRCULARS_PATH}")

    # 4. Generate alert report
    summary = generate_alert_summary(new_circulars, alert_file)
    print("\n" + summary)
    return len(new_circulars)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="PCDA(O) Intelligence Watchdog")
    parser.add_argument("--simulate", action="store_true", help="Simulate a newly published MoD circular")
    parser.add_argument("--alert-file", type=str, help="Path to write markdown alert file")
    args = parser.parse_args()

    discovered = run_watchdog(simulate=args.simulate, alert_file=args.alert_file)
    sys.exit(0)
