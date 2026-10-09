#!/usr/bin/env python3
"""One command to rebuild every generated Claim Guide file from authoring/*.txt and figures.json.

Write mode : compile --check, compile, ids --update, render_md, bundle, review_queue, tool tests, copycheck.
--check    : the CI variant. Writes nothing; fails if the ID lock or the bundle is stale.
copycheck needs the extracted source text, which lives outside git. When it is missing the step is reported as
SKIPPED and the run fails, unless --allow-no-sources is passed (CI, which has no sources).
Stops at the first failing step.

Usage: refresh.py [--check] [--allow-no-sources]
"""
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(__file__))
from config import CARDS_DIR, SOURCES_DIR

TOOLS = os.path.dirname(os.path.abspath(__file__))
SKIP_MESSAGE = 'SKIPPED copycheck (sources missing): the own-words guard did not run'


def tool(name, *args):
    return [sys.executable, os.path.join(TOOLS, name), *args]


def plan(check, have_sources):
    tests = ('tool tests', [sys.executable, '-m', 'unittest', 'discover', '-s', TOOLS, '-p', 'test_*.py'])
    if check:
        steps = [('compile --check --fresh', tool('compile.py', '--check', '--fresh')), ('ids --check', tool('ids.py', '--check')),
                 ('bundle --check', tool('bundle.py', '--check')), tests]
    else:
        steps = [('compile --check', tool('compile.py', '--check')), ('compile', tool('compile.py')),
                 ('ids --update', tool('ids.py', '--update')), ('render_md', tool('render_md.py')), ('bundle', tool('bundle.py')),
                 ('review_queue', tool('review_queue.py')), tests]
    if have_sources:
        steps.append(('copycheck', tool('copycheck.py')))
    return steps


def run(steps, runner, say=print):
    for name, argv in steps:
        say(f'== {name}')
        code = runner(argv)
        if code:
            say(f'FAILED at step "{name}" (exit {code})')
            return code
    return 0


def main(argv, have_sources=None, runner=None, say=print):
    have_sources = os.path.isdir(SOURCES_DIR) if have_sources is None else have_sources
    runner = runner or (lambda a: subprocess.call(a, cwd=CARDS_DIR))
    code = run(plan('--check' in argv, have_sources), runner, say)
    if code:
        return code
    if not have_sources:
        say(SKIP_MESSAGE)
        if '--allow-no-sources' not in argv:
            say('Failing because the guard was skipped; pass --allow-no-sources to accept that (CI does).')
            return 2
    else:
        say('copycheck output above must say CLEAN')
    say('refresh OK')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
