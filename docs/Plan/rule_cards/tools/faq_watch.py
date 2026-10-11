#!/usr/bin/env python3
"""Watch the PCDA(O) Pune public FAQ and notifications feeds for changes (the weekly job in .github/workflows/faq-watch.yml).

The Claim Guide restates circulars the portal FAQ also restates; when an answer is edited or a notification is posted, a card
may need a re-check. This compares the live feeds with tools/faq_manifest.json, which holds only ids, section names, updated
dates and sha256 hashes (section = the portal's section id), never the portal's text (it is their content, and the hash is enough to see a change).

  --check   exit 0 nothing changed, 1 something changed (report on stdout), 2 the feeds could not be read or trusted.
  --update  rewrite the manifest to the live state (after the owner has looked at the change); exit 0, or 2 and no write.

An empty list, malformed JSON, an HTTP error or an unexpected shape is exit 2, never "everything was removed". What counts as a
change is the words (and routine notices matching IGNORED_NOTIFICATIONS are skipped): markup, entities, whitespace, the "new" badge, list order and the updated date alone do not.
The report prints live titles (cut to 120 characters, one line each, no backticks) so it can sit in a fenced issue body.

Usage: faq_watch.py (--check | --update) [--manifest PATH]
"""
import hashlib
import json
import os
import re
import sys
import urllib.error
import urllib.request
from html.parser import HTMLParser

HOST = 'https://pcdaopune.gov.in'
FAQ_URL = f'{HOST}/api/api/faqdetails/getFaqTableData'
HOME_URL = f'{HOST}/api/api/homepage/content?locale=en'
MANIFEST_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'faq_manifest.json')
MANIFEST_VERSION = 1
TIMEOUT_SECONDS = 30
MAX_BYTES = 10 * 1024 * 1024
LABEL_LIMIT = 120
USER_AGENT = 'payslipmax-faq-watch (weekly public-feed check)'
# Routine notices that carry no rule and would open an issue every month. Matched against the visible words of a title, whole
# title, case-insensitive. Owner, 2026-10-11: the monthly Statement of Account notice. Add a pattern only with a test.
IGNORED_NOTIFICATIONS = (re.compile(r'Statement of Account for the month of .+ has been uploaded\.?', re.IGNORECASE),)
BLOCK_TAGS = frozenset({'br', 'p', 'div', 'li', 'ul', 'ol', 'tr', 'td', 'th', 'table', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6'})


class FeedError(Exception):
    """The feed could not be read or does not look like the feed we know: exit 2, never a change."""


def fetch_url(url):
    request = urllib.request.Request(url, headers={'User-Agent': USER_AGENT, 'Accept': 'application/json'})
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            if not response.geturl().startswith('https://'):
                raise FeedError(f'{url}: redirected away from https')
            data = response.read(MAX_BYTES + 1)
    except urllib.error.HTTPError as e:
        raise FeedError(f'{url}: HTTP {e.code}') from e
    except (urllib.error.URLError, OSError) as e:
        raise FeedError(f'{url}: {type(e).__name__}') from e
    if len(data) > MAX_BYTES:
        raise FeedError(f'{url}: response larger than {MAX_BYTES} bytes')
    return data.decode('utf-8', errors='replace')


class _Text(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.parts = []

    def handle_starttag(self, tag, attrs):
        if tag in BLOCK_TAGS:
            self.parts.append(' ')

    def handle_endtag(self, tag):
        self.handle_starttag(tag, ())

    def handle_data(self, data):
        self.parts.append(data)


def words(html):
    """The visible words of an HTML fragment, single-spaced: what a reader sees, not how it is marked up."""
    parser = _Text()
    parser.feed(html or '')
    parser.close()
    return ' '.join(''.join(parser.parts).split())


def digest(*parts):
    return hashlib.sha256(json.dumps(parts, ensure_ascii=False).encode('utf-8')).hexdigest()


def label(text):
    """One safe report line: no newlines, no backticks (they could close the issue's code fence), cut short."""
    return ' '.join((text or '').replace('`', "'").split())[:LABEL_LIMIT]


def parse_json(raw, url):
    try:
        return json.loads(raw)
    except ValueError as e:
        raise FeedError(f'{url}: not JSON ({type(e).__name__})') from e


def entries(feed, rows):
    """Rows must be a non-empty list of objects with unique string ids; an empty list means the feed is unreachable."""
    if not isinstance(rows, list) or not all(isinstance(r, dict) for r in rows):
        raise FeedError(f'{feed}: unexpected shape')
    if not rows:
        raise FeedError(f'{feed}: empty list (treated as unreachable)')
    ids = [r.get('id') for r in rows]
    if not all(isinstance(i, str) and i for i in ids):
        raise FeedError(f'{feed}: an entry has no id')
    if len(set(ids)) != len(ids):
        raise FeedError(f'{feed}: duplicate ids')
    return rows


def read_faq(fetch):
    rows = entries('faq', parse_json(fetch(FAQ_URL), FAQ_URL))
    snapshot, labels = {}, {}
    for r in rows:
        section = r.get('section') if isinstance(r.get('section'), dict) else {}
        name = words(section.get('name'))
        snapshot[r['id']] = {'section': str(section.get('id') or ''), 'updated': str(r.get('updatedDate') or ''),
                             'sha256': digest(words(r.get('question')), words(r.get('answer')), bool(r.get('faqStatus')), name)}
        labels[r['id']] = f"[{label(name)}] {label(r.get('question'))}"
    return snapshot, labels


def read_notifications(fetch):
    body = parse_json(fetch(HOME_URL), HOME_URL)
    inner = body.get('responseBody') if isinstance(body, dict) else None
    rows = entries('notifications', inner.get('newsAndNotifications') if isinstance(inner, dict) else None)
    snapshot, labels = {}, {}
    for r in rows:
        title = words(r.get('titleEnglish'))
        if any(p.fullmatch(title) for p in IGNORED_NOTIFICATIONS):
            continue
        snapshot[r['id']] = {'updated': str(r.get('updatedDate') or ''),
                             'sha256': digest(title, r.get('status'), r.get('documentUrl'))}
        labels[r['id']] = label(title)
    return snapshot, labels


def load_manifest(path):
    try:
        with open(path, encoding='utf-8') as f:
            m = json.load(f)
        if m['version'] != MANIFEST_VERSION or not isinstance(m['faq'], dict) or not isinstance(m['notifications'], dict):
            raise ValueError
    except FileNotFoundError as e:
        raise FeedError(f'{path} not found; seed it with: faq_watch.py --update') from e
    except (ValueError, KeyError, TypeError) as e:
        raise FeedError(f'{path} is not a version {MANIFEST_VERSION} manifest') from e
    return m


def save_manifest(path, faq, notifications):
    with open(path, 'w', encoding='utf-8') as f:
        json.dump({'version': MANIFEST_VERSION, 'faq': faq, 'notifications': notifications}, f, indent=1, sort_keys=True)
        f.write('\n')


def compare(old, new):
    return {'added': sorted(set(new) - set(old)), 'removed': sorted(set(old) - set(new)),
            'changed': sorted(i for i in set(old) & set(new) if old[i]['sha256'] != new[i]['sha256'])}


def report(title, diff, old, labels):
    lines = []
    for kind in ('added', 'removed', 'changed'):
        for i in diff[kind]:
            section = old.get(i, {}).get('section')
            what = labels.get(i) or ('(no longer listed' + (f', section {section})' if section else ')'))
            lines.append(f'{title} {kind}: {i} {what}')
    return lines


def main(argv, fetch=None, say=print):
    fetch = fetch or fetch_url
    modes = [a for a in argv if a in ('--check', '--update')]
    if len(modes) != 1:
        say(__doc__.strip().splitlines()[-1])
        return 2
    path = argv[argv.index('--manifest') + 1] if '--manifest' in argv else MANIFEST_PATH
    try:
        faq, faq_labels = read_faq(fetch)
        notes, note_labels = read_notifications(fetch)
        seeding = modes[0] == '--update' and not os.path.exists(path)
        old = {'faq': {}, 'notifications': {}} if seeding else load_manifest(path)
    except FeedError as e:
        say(f'faq_watch: {e}')
        return 2
    lines = [] if seeding else (report('FAQ', compare(old['faq'], faq), old['faq'], faq_labels)
                                + report('Notification', compare(old['notifications'], notes), old['notifications'], note_labels))
    for line in lines:
        say(line)
    if modes[0] == '--update':
        save_manifest(path, faq, notes)
        say(f'manifest updated: {len(faq)} FAQ entries, {len(notes)} notifications')
        return 0
    say(f'{len(lines)} change(s)' if lines else f'no change ({len(faq)} FAQ entries, {len(notes)} notifications)')
    return 1 if lines else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
