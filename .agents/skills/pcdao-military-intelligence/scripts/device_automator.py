#!/usr/bin/env python3
"""
device_automator.py — Resilient ADB UIAutomator Helper for PayslipMax AI.
Locates UI elements dynamically by resource-id, text, or content-description,
eliminating brittle hardcoded screen coordinates across different devices.
"""

import argparse
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from typing import Optional, Tuple


def run_adb(device_id: Optional[str], *args: str) -> subprocess.CompletedProcess:
    cmd = ["adb"]
    if device_id:
        cmd.extend(["-s", device_id])
    cmd.extend(args)
    return subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)


def dump_hierarchy(device_id: Optional[str], retries: int = 3) -> ET.Element:
    """Dumps and parses the current UI hierarchy XML from device."""
    remote_path = "/data/local/tmp/uidump.xml"
    for attempt in range(retries):
        res = run_adb(device_id, "shell", "uiautomator", "dump", remote_path)
        if res.returncode == 0:
            cat_res = run_adb(device_id, "exec-out", "cat", remote_path)
            if cat_res.returncode == 0 and cat_res.stdout.strip():
                try:
                    return ET.fromstring(cat_res.stdout)
                except ET.ParseError:
                    pass
        time.sleep(0.5)
    raise RuntimeError(f"Failed to dump UI hierarchy after {retries} attempts.")


def parse_bounds(bounds_str: str) -> Tuple[int, int]:
    """Parses bounds '[x1,y1][x2,y2]' and returns center (cx, cy)."""
    match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds_str)
    if not match:
        raise ValueError(f"Invalid bounds string: {bounds_str}")
    x1, y1, x2, y2 = map(int, match.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2


def node_matches(node: ET.Element, text: Optional[str], contains: Optional[str],
                 desc: Optional[str], res_id: Optional[str]) -> bool:
    """Checks if a UIAutomator XML node satisfies the search criteria."""
    node_text = (node.get("text") or "").strip()
    node_desc = (node.get("content-desc") or "").strip()
    node_res = (node.get("resource-id") or "").strip()

    if res_id:
        if node_res != res_id and not node_res.endswith(f":id/{res_id}"):
            return False
    if text and node_text != text:
        return False
    if contains and (contains.lower() not in node_text.lower() and contains.lower() not in node_desc.lower()):
        return False
    if desc and node_desc != desc:
        return False

    return bool(res_id or text or contains or desc)


def find_element(root: ET.Element, text: Optional[str] = None, contains: Optional[str] = None,
                 desc: Optional[str] = None, res_id: Optional[str] = None) -> Optional[Tuple[int, int]]:
    """Searches hierarchy for matching node and returns its center coordinates."""
    for node in root.findall(".//node"):
        if node_matches(node, text, contains, desc, res_id):
            bounds = node.get("bounds")
            if bounds:
                return parse_bounds(bounds)
    return None


def swipe_screen(device_id: Optional[str], direction: str = "down") -> None:
    """Performs a vertical swipe to scroll content."""
    if direction == "down":
        # Swipe up to reveal content below
        run_adb(device_id, "shell", "input", "swipe", "540", "1600", "540", "800", "300")
    else:
        # Swipe down to reveal content above
        run_adb(device_id, "shell", "input", "swipe", "540", "800", "540", "1600", "300")
    time.sleep(0.8)


def find_and_tap(device_id: Optional[str], text: Optional[str] = None, contains: Optional[str] = None,
                 desc: Optional[str] = None, res_id: Optional[str] = None, max_scrolls: int = 4) -> bool:
    """Finds an element, scrolling if necessary, and taps its center."""
    scrolls_done = 0
    for scroll in range(max_scrolls + 1):
        root = dump_hierarchy(device_id)
        coords = find_element(root, text=text, contains=contains, desc=desc, res_id=res_id)
        if coords:
            cx, cy = coords
            print(f"  [Automator] Found target at ({cx}, {cy}). Tapping...")
            run_adb(device_id, "shell", "input", "tap", str(cx), str(cy))
            time.sleep(0.8)
            return True
        if scroll < max_scrolls:
            print(f"  [Automator] Target not in view. Scrolling down (attempt {scroll + 1}/{max_scrolls})...")
            swipe_screen(device_id, direction="down")
            scrolls_done += 1

    if scrolls_done > 0:
        print(f"  [Automator] Resetting view (scrolling up {scrolls_done} times)...")
        for _ in range(scrolls_done):
            swipe_screen(device_id, direction="up")

    return False


def wait_for_element(device_id: Optional[str], text: Optional[str] = None, contains: Optional[str] = None,
                     desc: Optional[str] = None, res_id: Optional[str] = None, timeout: float = 10.0) -> bool:
    """Polls until element appears or timeout expires."""
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            root = dump_hierarchy(device_id)
            if find_element(root, text=text, contains=contains, desc=desc, res_id=res_id):
                return True
        except Exception:
            pass
        time.sleep(1.0)
    return False


def main():
    parser = argparse.ArgumentParser(description="ADB UIAutomator Dynamic Element Runner")
    parser.add_argument("--device", help="ADB Device Serial")
    subparsers = parser.add_subparsers(dest="command", required=True)

    tap_p = subparsers.add_parser("tap", help="Locate and tap element")
    tap_p.add_argument("--text", help="Exact node text")
    tap_p.add_argument("--contains", help="Sub-string in node text")
    tap_p.add_argument("--desc", help="Exact content-desc")
    tap_p.add_argument("--res-id", help="Resource ID or Compose testTag")
    tap_p.add_argument("--scrolls", type=int, default=3, help="Max scrolls if not visible")
    tap_p.add_argument("--optional", action="store_true", help="Don't fail if element missing")

    wait_p = subparsers.add_parser("wait-for", help="Wait for element to appear")
    wait_p.add_argument("--text", help="Exact node text")
    wait_p.add_argument("--contains", help="Sub-string in node text")
    wait_p.add_argument("--desc", help="Exact content-desc")
    wait_p.add_argument("--res-id", help="Resource ID or Compose testTag")
    wait_p.add_argument("--timeout", type=float, default=10.0, help="Timeout in seconds")

    swipe_p = subparsers.add_parser("swipe", help="Scroll screen")
    swipe_p.add_argument("--direction", choices=["up", "down"], default="down")

    args = parser.parse_args()

    if args.command == "tap":
        found = find_and_tap(args.device, text=args.text, contains=args.contains,
                             desc=args.desc, res_id=args.res_id, max_scrolls=args.scrolls)
        if not found and not args.optional:
            target = args.res_id or args.text or args.contains or args.desc
            print(f"ERROR: Element not found matching target: {target}", file=sys.stderr)
            sys.exit(1)

    elif args.command == "wait-for":
        found = wait_for_element(args.device, text=args.text, contains=args.contains,
                                 desc=args.desc, res_id=args.res_id, timeout=args.timeout)
        if not found:
            target = args.res_id or args.text or args.contains or args.desc
            print(f"ERROR: Timed out waiting for element: {target}", file=sys.stderr)
            sys.exit(1)

    elif args.command == "swipe":
        swipe_screen(args.device, direction=args.direction)


if __name__ == "__main__":
    main()
