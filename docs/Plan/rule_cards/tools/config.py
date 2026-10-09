"""Shared paths for the rule-card tools.

CARDS_DIR   : this project folder (docs/Plan/rule_cards), holds the cards and datasets.
SOURCES_DIR : extracted source text (handbooks, FAQ json, OCR'd TR 2014). Derived from copyrighted
              PDFs, so it lives OUTSIDE the repo. Override with the RULECARDS_SOURCES env var.
"""
import os

CARDS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
AUTH_DIR = os.path.join(CARDS_DIR, 'authoring')
# Every card ID that has ever shipped (ids.py); an ID may only leave it via an authoring `--- retire` line.
LOCK_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'ids_lock.json')
SOURCES_DIR = os.path.expanduser(os.environ.get('RULECARDS_SOURCES', '~/Downloads/rulecards_workdir'))

# The Claim Guide bundle the app ships: generated from rulebook.json by bundle.py, never hand-edited.
REPO_ROOT = os.path.abspath(os.path.join(CARDS_DIR, '..', '..', '..'))
BUNDLE_PATH = os.path.join(REPO_ROOT, 'composeApp', 'src', 'commonMain', 'composeResources', 'files', 'guide', 'guide_bundle.json')
# Schema major the app accepts (GuideBundleParser.SUPPORTED_MAJOR). Bump only for a breaking change: the app
# rejects a newer major with an error state, and ignores unknown fields, so additive changes keep this value.
BUNDLE_VERSION = 1
# Month the rate figures are current to, shown on the "Rates as of" chip. Owner, 2026-10-07: the DA 60% step.
RATES_AS_OF = '2026-01'


def src(name):
    """Absolute path of a source file; stops with a clear message if the folder is missing."""
    if not os.path.isdir(SOURCES_DIR):
        raise SystemExit(f'Source text folder not found: {SOURCES_DIR}\nSet RULECARDS_SOURCES or ask the owner to rebuild it from the PCDAO PDFs.')
    return os.path.join(SOURCES_DIR, name)
