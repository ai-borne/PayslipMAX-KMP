"""Tests for faq_watch.py, the weekly watcher of the PCDA(O) Pune public FAQ and notifications feeds.

Why they matter: the Claim Guide's rates and rules come from circulars the FAQ restates; when the portal edits an answer or
posts a notification the owner has to re-check a card. A watcher that cries wolf (HTML-only edits, a flipped "new" badge) gets
ignored, one that reads an unreachable feed as "everything was removed" opens a false alarm, and a manifest that kept feed text
would copy the portal's content into the repo. Run: python3 tools/test_faq_watch.py
"""
import json
import os
import tempfile
import unittest
import urllib.error
from unittest import mock

import faq_watch

FAQ_URL = faq_watch.FAQ_URL
HOME_URL = faq_watch.HOME_URL
# Longest allowed manifest value: a sha256 hex. An id (24) and a date (26) are shorter; prose would be far over this.
MAX_FIELD = 64


def faq(i, question='Q?', answer='<p>A.</p>', section='TA', status=True, updated='2026-01-01T00:00:00.1'):
    return {'id': i, 'question': question, 'answer': answer, 'faqStatus': status, 'section': {'id': 's-' + section, 'name': section},
            'updatedDate': updated}


def note(i, title='Notice', url=None, status='ACTIVE', is_new=False, order=1, updated='2026-02-02T00:00:00.1'):
    return {'id': i, 'titleEnglish': title, 'documentUrl': url, 'status': status, 'isNew': is_new, 'uiOrder': order,
            'updatedDate': updated}


def home(notes):
    return {'message': 'ok', 'responseBody': {'newsAndNotifications': notes, 'sliders': []}}


class Feeds:
    """Injected fetcher: serves canned JSON text per URL, or raises like the network would."""

    def __init__(self, faqs, notes, faq_raw=None, home_raw=None, fail=None):
        self.map = {FAQ_URL: faq_raw if faq_raw is not None else json.dumps(faqs),
                    HOME_URL: home_raw if home_raw is not None else json.dumps(home(notes))}
        self.fail = fail

    def __call__(self, url):
        if self.fail:
            raise faq_watch.FeedError(self.fail)
        return self.map[url]


BASE_FAQS = [faq('f1', 'How is CTG paid?', '<p>80% of basic.</p>'), faq('f2', 'What is RMA?', '<p>Rate per km.</p>', 'LTC')]
BASE_NOTES = [note('n1', 'SPR declaration', 'https://x/spr.pdf'), note('n2', 'Part II order for TD')]


def run(args, feeds, manifest):
    out = []
    code = faq_watch.main(args + ['--manifest', manifest], fetch=feeds, say=out.append)
    return code, '\n'.join(out)


class Case(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)
        self.manifest = os.path.join(self.dir.name, 'm.json')
        self.assertEqual(run(['--update'], Feeds(BASE_FAQS, BASE_NOTES), self.manifest)[0], 0)

    def check(self, faqs=None, notes=None, **kw):
        return run(['--check'], Feeds(BASE_FAQS if faqs is None else faqs, BASE_NOTES if notes is None else notes, **kw),
                   self.manifest)


class WhatCountsAsAChange(Case):
    def test_an_unchanged_feed_exits_zero(self):
        self.assertEqual(self.check()[0], 0)

    def test_an_added_faq_exits_one_and_names_it(self):
        code, out = self.check(faqs=BASE_FAQS + [faq('f3', 'New rule on lodging?')])
        self.assertEqual(code, 1)
        self.assertIn('added', out)
        self.assertIn('f3', out)
        self.assertIn('New rule on lodging?', out)

    def test_a_removed_faq_exits_one(self):
        code, out = self.check(faqs=BASE_FAQS[:1])
        self.assertEqual((code, 'removed' in out, 'f2' in out), (1, True, True))

    def test_an_edited_answer_exits_one(self):
        code, out = self.check(faqs=[faq('f1', 'How is CTG paid?', '<p>70% of basic.</p>'), BASE_FAQS[1]])
        self.assertEqual((code, 'changed' in out, 'f1' in out), (1, True, True))

    def test_an_edited_question_exits_one(self):
        self.assertEqual(self.check(faqs=[faq('f1', 'How is CTG now paid?', '<p>80% of basic.</p>'), BASE_FAQS[1]])[0], 1)

    def test_a_faq_switched_off_exits_one(self):
        self.assertEqual(self.check(faqs=[faq('f1', 'How is CTG paid?', '<p>80% of basic.</p>', status=False), BASE_FAQS[1]])[0], 1)

    def test_an_added_notification_exits_one(self):
        code, out = self.check(notes=BASE_NOTES + [note('n3', 'Outstanding DTS advances')])
        self.assertEqual((code, 'n3' in out, 'Outstanding DTS advances' in out), (1, True, True))

    def test_a_changed_document_link_exits_one(self):
        self.assertEqual(self.check(notes=[note('n1', 'SPR declaration', 'https://x/spr_v2.pdf'), BASE_NOTES[1]])[0], 1)

    def test_html_only_differences_are_not_a_change(self):
        # Markup, entities and whitespace move without the words moving; alerting on them would train the owner to ignore alerts.
        same_words = [faq('f1', 'How  is CTG paid?', '<div><strong>80%</strong>&nbsp;of  basic.</div>'), BASE_FAQS[1]]
        self.assertEqual(self.check(faqs=same_words)[0], 0)

    def test_a_flipped_new_badge_order_or_updated_date_alone_is_not_a_change(self):
        churn = [note('n1', 'SPR declaration', 'https://x/spr.pdf', is_new=True, order=9, updated='2026-10-10T00:00:00.1'),
                 BASE_NOTES[1]]
        self.assertEqual(self.check(notes=churn)[0], 0)

    def test_a_titled_report_line_cannot_break_out_of_the_issue_code_fence(self):
        code, out = self.check(faqs=BASE_FAQS + [faq('f3', 'bad ``` title\nsecond line')])
        self.assertEqual(code, 1)
        self.assertNotIn('```', out)
        self.assertIn('second line', next(line for line in out.splitlines() if 'f3' in line))  # one line per entry


class FailureIsNeverReadAsAChange(Case):
    def test_an_empty_faq_list_is_an_error_not_all_removed(self):
        code, out = self.check(faqs=[])
        self.assertEqual(code, 2)
        self.assertNotIn('removed', out)

    def test_an_empty_notification_list_is_an_error(self):
        self.assertEqual(self.check(notes=[])[0], 2)

    def test_malformed_json_exits_two(self):
        self.assertEqual(self.check(faq_raw='<html>blocked</html>')[0], 2)

    def test_a_fetch_error_exits_two(self):
        code, out = self.check(fail='HTTP 403')
        self.assertEqual((code, 'HTTP 403' in out), (2, True))

    def test_an_unexpected_shape_exits_two(self):
        self.assertEqual(self.check(home_raw=json.dumps({'responseBody': {}}))[0], 2)
        self.assertEqual(self.check(faq_raw=json.dumps({'oops': 1}))[0], 2)

    def test_an_entry_without_an_id_exits_two(self):
        self.assertEqual(self.check(faqs=[{'question': 'no id', 'answer': 'x'}])[0], 2)

    def test_duplicate_ids_exit_two(self):
        self.assertEqual(self.check(faqs=[BASE_FAQS[0], BASE_FAQS[0]])[0], 2)

    def test_a_missing_manifest_exits_two_and_says_how_to_seed_it(self):
        code, out = run(['--check'], Feeds(BASE_FAQS, BASE_NOTES), os.path.join(self.dir.name, 'none.json'))
        self.assertEqual((code, '--update' in out), (2, True))

    def test_a_failed_update_leaves_the_manifest_untouched(self):
        before = open(self.manifest).read()
        self.assertEqual(run(['--update'], Feeds([], BASE_NOTES), self.manifest)[0], 2)
        self.assertEqual(open(self.manifest).read(), before)


class ManifestKeepsNoFeedText(Case):
    def test_only_ids_sections_dates_and_hashes_are_stored(self):
        m = json.load(open(self.manifest))
        self.assertEqual(set(m), {'version', 'faq', 'notifications'})
        for entry in m['faq'].values():
            self.assertEqual(set(entry), {'section', 'updated', 'sha256'})
        for entry in m['notifications'].values():
            self.assertEqual(set(entry), {'updated', 'sha256'})
        self.assertLessEqual(max(len(s) for s in self.strings(m)), MAX_FIELD)

    def test_no_question_answer_or_title_text_reaches_the_file(self):
        raw = open(self.manifest).read()
        for text in ('How is CTG paid?', '80% of basic', 'SPR declaration', 'spr.pdf'):
            self.assertNotIn(text, raw)

    def test_update_is_deterministic_so_diffs_show_only_real_changes(self):
        first = open(self.manifest).read()
        run(['--update'], Feeds(list(reversed(BASE_FAQS)), list(reversed(BASE_NOTES))), self.manifest)
        self.assertEqual(open(self.manifest).read(), first)

    @staticmethod
    def strings(node):
        if isinstance(node, dict):
            for k, v in node.items():
                yield str(k)
                yield from ManifestKeepsNoFeedText.strings(v)
        elif isinstance(node, str):
            yield node


class UpdateAfterAChange(Case):
    def test_update_accepts_the_new_state_and_the_next_check_is_clean(self):
        changed = BASE_FAQS + [faq('f3')]
        code, out = run(['--update'], Feeds(changed, BASE_NOTES), self.manifest)
        self.assertEqual(code, 0)
        self.assertIn('f3', out)
        self.assertEqual(run(['--check'], Feeds(changed, BASE_NOTES), self.manifest)[0], 0)


class CommittedManifest(unittest.TestCase):
    def test_the_seeded_manifest_is_valid_and_text_free(self):
        m = faq_watch.load_manifest(faq_watch.MANIFEST_PATH)
        self.assertGreater(len(m['faq']), 100)
        self.assertGreater(len(m['notifications']), 0)
        for entry in list(m['faq'].values()) + list(m['notifications'].values()):
            self.assertEqual(len(entry['sha256']), 64)
            self.assertLessEqual(max(len(str(v)) for v in entry.values()), MAX_FIELD)


class FakeResponse:
    def __init__(self, body=b'[]', url='https://pcdaopune.gov.in/x'):
        self.body, self.url = body, url

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        return False

    def geturl(self):
        return self.url

    def read(self, limit=-1):
        return self.body[:limit] if limit >= 0 else self.body


class FetchUrl(unittest.TestCase):
    """The real fetcher, with urlopen replaced: every network failure must surface as FeedError (exit 2), never a crash or a change."""

    def fetch(self, **kw):
        with mock.patch('urllib.request.urlopen', **kw):
            return faq_watch.fetch_url(FAQ_URL)

    def test_a_good_response_is_returned_as_text(self):
        self.assertEqual(self.fetch(return_value=FakeResponse('[1]'.encode())), '[1]')

    def test_an_http_error_names_the_status_and_nothing_else(self):
        err = urllib.error.HTTPError(FAQ_URL, 403, 'Forbidden', {}, None)
        with self.assertRaisesRegex(faq_watch.FeedError, 'HTTP 403'):
            self.fetch(side_effect=err)

    def test_a_timeout_or_dns_failure_is_a_feed_error(self):
        for exc in (urllib.error.URLError('dns'), TimeoutError(), ConnectionResetError()):
            with self.assertRaises(faq_watch.FeedError):
                self.fetch(side_effect=exc)

    def test_a_redirect_away_from_https_is_refused(self):
        with self.assertRaisesRegex(faq_watch.FeedError, 'https'):
            self.fetch(return_value=FakeResponse(url='http://pcdaopune.gov.in/x'))

    def test_a_response_over_the_size_cap_is_refused(self):
        with self.assertRaisesRegex(faq_watch.FeedError, 'larger'):
            self.fetch(return_value=FakeResponse(b'x' * (faq_watch.MAX_BYTES + 1)))


if __name__ == '__main__':
    unittest.main()
