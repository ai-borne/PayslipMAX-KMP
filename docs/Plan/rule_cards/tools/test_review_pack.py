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


def block_ids(html):
    return re.findall(r'<article class="card" id="(RB-[^"]+)"', html)


class Coverage(unittest.TestCase):
    def test_every_card_appears_exactly_once(self):
        ids = block_ids(HTML)
        self.assertEqual(sorted(ids), sorted(c['id'] for c in RULEBOOK['cards']))
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
        card = max(rb['cards'], key=lambda c: sum(bool(c[k]) for k in ('open', 'from', 'attach', 'watch', 'details', 'chips', 'cite')))
        card['open'] = card['open'] or ['A made-up open point']
        html = review_pack.build_html(rb, FIGURES, DATE)
        block = html.split(f'id="{card["id"]}"')[1].split('</article>')[0]
        for text in [card['title'], card['answer'], card['cite'], card['details'], *card['key'], *card['attach'], *card['watch'],
                     *card['chips'], *card['from'], *card['open']]:
            self.assertIn(review_pack.esc(text), block, text)
        self.assertIn('Open point', block)

    def test_a_linked_figure_shows_its_evidence_level(self):
        fig = next(iter(FIGURES['figures'].values()))
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


class Safety(unittest.TestCase):
    def test_markup_characters_in_card_text_are_escaped(self):
        rb = copy.deepcopy(RULEBOOK)
        rb['cards'][0]['answer'] = 'Pay <b>1</b> & "more" <script>x()</script>'
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
        gone = next(c['id'] for c in RULEBOOK['cards'] if c['id'] not in with_figure)
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
