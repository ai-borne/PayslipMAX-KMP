"""Tests for review_pack.py, the hand-out an outside PCDA(O) expert reads and answers.

Why they matter: the expert can only correct what they see, so a card missing or printed twice means a wrong rule
goes unreviewed or a reply cannot be matched to one card; unescaped card text could break the page or inject markup;
a pack that changes between runs cannot be diffed or trusted; a retired card in the pack sends the expert to
review a rule users no longer see. Run: python3 tools/test_review_pack.py
"""
import copy
import json
import os
import re
import unittest

import review_pack
from config import CARDS_DIR

RULEBOOK = json.load(open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8'))
FIGURES = json.load(open(os.path.join(CARDS_DIR, 'figures.json'), encoding='utf-8'))
DATE = '2026-10-09'
HTML = review_pack.build_html(RULEBOOK, FIGURES, DATE)
# A replaced card is no longer shown to users, so the pack leaves it out. The real rulebook will carry some once a rule changes
# (the 8th CPC), so these tests take the current cards from the data instead of assuming card 0 is one.
CURRENT = [c for c in RULEBOOK['cards'] if not c.get('replaced_by')]


def block_ids(html):
    return re.findall(r'<article class="card" id="(RB-[^"]+)"', html)


class Coverage(unittest.TestCase):
    def test_every_card_appears_exactly_once(self):
        ids = block_ids(HTML)
        self.assertEqual(sorted(ids), sorted(c['id'] for c in CURRENT))
        self.assertEqual(len(ids), len(set(ids)))

    def test_cards_are_grouped_area_then_case_in_nav_order(self):
        order = [i for a in RULEBOOK['nav'] for c in a['cases'] for i in c['cards']]
        self.assertEqual(block_ids(HTML), order)
        for a in RULEBOOK['nav']:
            self.assertIn(f'<h2>{review_pack.esc(a["title"])}</h2>', HTML)

    def test_a_card_listed_as_also_in_another_case_is_not_printed_twice(self):
        rb = copy.deepcopy(RULEBOOK)
        first = rb['nav'][0]['cases'][0]
        other = rb['nav'][-1]['cases'][-1]
        other['also'] = list(other['also']) + [first['cards'][0]]
        ids = block_ids(review_pack.build_html(rb, FIGURES, DATE))
        self.assertEqual(ids.count(first['cards'][0]), 1)


class CardContents(unittest.TestCase):
    def test_every_reviewer_field_is_printed(self):
        rb = copy.deepcopy(RULEBOOK)
        card = max((c for c in rb['cards'] if not c.get('replaced_by')), key=lambda c: sum(bool(c[k]) for k in ('open', 'from', 'attach', 'watch', 'details', 'chips', 'cite')))
        card['open'] = card['open'] or ['A made-up open point']
        html = review_pack.build_html(rb, FIGURES, DATE)
        block = html.split(f'id="{card["id"]}"')[1].split('</article>')[0]
        for text in [card['title'], card['answer'], card['cite'], card['details'], *card['key'], *card['attach'], *card['watch'],
                     *card['chips'], *card['from'], *card['open']]:
            self.assertIn(review_pack.esc(text), block, text)
        self.assertIn('Open point', block)

    def test_a_linked_figure_shows_its_evidence_level(self):
        printed = set(block_ids(HTML))
        fig = next(f for f in FIGURES['figures'].values() if f['card'] in printed)
        block = HTML.split(f'id="{fig["card"]}"')[1].split('</article>')[0]
        self.assertIn(fig['evidence'], block)

    def test_each_card_has_two_blank_verdict_lines(self):
        for block in HTML.split('<article')[1:]:
            self.assertEqual(block.count('class="blank"'), 2)


class CoverPage(unittest.TestCase):
    def test_cover_explains_how_to_respond(self):
        for text in ('Card ID', 'OK / Wrong / Outdated / Missing', 'Correction', 'Authority'):
            self.assertIn(text, HTML.split('<main>')[0])

    def test_appendix_lists_figures_and_unverified_cards(self):
        appendix = HTML.split('id="appendix"')[1]
        for key, fig in FIGURES['figures'].items():
            self.assertIn(review_pack.esc(fig['authority']), appendix, key)
            self.assertIn(fig.get('effective_from', 'see rate table'), appendix, key)
        for c in RULEBOOK['cards']:
            if c['open']:
                self.assertIn(c['id'], appendix)


class RuleChanges(unittest.TestCase):
    """M3: the expert must see when a card is a dated rule, and the 8th CPC sweep list of cards that quote a rate."""

    def dated(self):
        rb = copy.deepcopy(RULEBOOK)
        old, new = rb['cards'][0], rb['cards'][1]
        new.update(effective='2026-11-15', replaces=old['id'])
        old.update(replaced_by=new['id'], until='2026-11-15')
        return rb, old, new

    def test_a_dated_card_shows_its_effective_date_and_what_it_replaces(self):
        rb, old, new = self.dated()
        block = review_pack.build_html(rb, FIGURES, DATE).split(f'id="{new["id"]}"')[1].split('</article>')[0]
        self.assertIn('<p class="meta">Applies from 2026-11-15</p>', block)
        self.assertIn(f'<p class="meta">Replaces {old["id"]}</p>', block)

    def test_a_replaced_card_is_not_printed_because_users_no_longer_see_it(self):
        rb, old, _ = self.dated()
        self.assertNotIn(f'<article class="card" id="{old["id"]}"', review_pack.build_html(rb, FIGURES, DATE))

    def test_a_card_with_no_dated_rule_shows_no_date_line(self):
        dated = {c['id'] for c in RULEBOOK['cards'] if c.get('effective')}
        for block in HTML.split('<article class="card" id="')[1:]:
            if block.split('"')[0] not in dated:
                self.assertNotIn('<p class="meta">Applies from', block.split('</article>')[0])

    def test_the_appendix_lists_the_rates_sweep(self):
        import rates_report
        appendix = HTML.split('id="appendix"')[1]
        self.assertIn('8th CPC', appendix)
        for row in rates_report.report(RULEBOOK, FIGURES)['cards']:
            self.assertIn(row['id'], appendix)


class Safety(unittest.TestCase):
    def test_markup_characters_in_card_text_are_escaped(self):
        rb = copy.deepcopy(RULEBOOK)
        next(c for c in rb['cards'] if not c.get('replaced_by'))['answer'] = 'Pay <b>1</b> & "more" <script>x()</script>'
        html = review_pack.build_html(rb, FIGURES, DATE)
        self.assertNotIn('<script>', html)
        self.assertIn('Pay &lt;b&gt;1&lt;/b&gt; &amp; &quot;more&quot; &lt;script&gt;', html)

    def test_markup_in_figures_and_areas_is_escaped_too(self):
        rb, fg = copy.deepcopy(RULEBOOK), copy.deepcopy(FIGURES)
        rb['nav'][0]['title'] = '<i>Area</i>'
        next(iter(fg['figures'].values()))['authority'] = '<img src=x>'
        html = review_pack.build_html(rb, fg, DATE)
        self.assertNotIn('<i>Area</i>', html)
        self.assertNotIn('<img src=x>', html)


class Determinism(unittest.TestCase):
    def test_same_input_gives_identical_output(self):
        self.assertEqual(HTML, review_pack.build_html(RULEBOOK, FIGURES, DATE))

    def test_the_date_comes_from_the_caller_not_the_clock(self):
        self.assertIn(DATE, HTML)
        self.assertNotEqual(HTML, review_pack.build_html(RULEBOOK, FIGURES, '2027-01-01'))


class Retired(unittest.TestCase):
    def test_retired_cards_are_excluded_everywhere(self):
        with_figure = {f['card'] for f in FIGURES['figures'].values()}
        gone = next(c['id'] for c in CURRENT if c['id'] not in with_figure)
        html = review_pack.build_html(RULEBOOK, FIGURES, DATE, retired={gone})
        self.assertNotIn(gone, html)


class Output(unittest.TestCase):
    def test_file_name_carries_the_date_and_review_dir_is_ignored_by_git(self):
        self.assertTrue(review_pack.out_path(DATE).endswith(os.path.join('review', f'guide_review_{DATE}.html')))
        root = os.path.abspath(os.path.join(CARDS_DIR, '..', '..', '..'))
        self.assertIn('docs/Plan/rule_cards/review/', open(os.path.join(root, '.gitignore')).read())

    def test_pdf_fails_loudly_without_chrome(self):
        with self.assertRaises(SystemExit) as ctx:
            review_pack.to_pdf('in.html', 'out.pdf', chrome_candidates=['/no/such/chrome'])
        self.assertIn('Chrome', str(ctx.exception))


if __name__ == '__main__':
    unittest.main()
