"""Shared paths for the rule-card tools.

CARDS_DIR   : this project folder (docs/Plan/rule_cards), holds the cards and datasets.
SOURCES_DIR : extracted source text (handbooks, FAQ json, OCR'd TR 2014). Derived from copyrighted
              PDFs, so it lives OUTSIDE the repo. Override with the RULECARDS_SOURCES env var.
"""
import os

CARDS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
AUTH_DIR = os.path.join(CARDS_DIR, 'authoring')
SOURCES_DIR = os.path.expanduser(os.environ.get('RULECARDS_SOURCES', '~/Downloads/rulecards_workdir'))


def src(name):
    """Absolute path of a source file; stops with a clear message if the folder is missing."""
    if not os.path.isdir(SOURCES_DIR):
        raise SystemExit(f'Source text folder not found: {SOURCES_DIR}\nSet RULECARDS_SOURCES or ask the owner to rebuild it from the PCDAO PDFs.')
    return os.path.join(SOURCES_DIR, name)
