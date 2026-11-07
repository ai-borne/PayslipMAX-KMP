"""The casualty slice of the bundle's `part2` section: Annexure III columns, the 30 plain-English pages and the related links (Part II
Orders Bank, phase 1). bundle.py calls for_bundle(); compile.py calls check(). The three authored files keep their audit fields
(`hand`, and the codes and page already shipped with the serial list); only what the app reads ships, in authored order.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import part2_about_check
import part2_codes_check
import part2_columns_check
import part2_related_check

ROW_KEYS = ('serial', 'from', 'to', 'data')


def limits():
    """The caps the app rechecks on load, carried in the section's own limits so Kotlin holds no second copy."""
    return {'caption': part2_columns_check.CAPTION_MAX, 'about_part': part2_about_check.PART_MAX}


def _problems(columns, about, related, codes):
    return part2_columns_check.problems(columns, codes) + part2_about_check.problems(about, codes) + part2_related_check.problems(related, codes)


def check():
    """Every problem in the real files; compile.py gates the build on this."""
    return _problems(part2_columns_check.load(), part2_about_check.load(), part2_related_check.load(), part2_codes_check.load())


def for_bundle(columns, about, related, codes):
    found = _problems(columns, about, related, codes)
    if found:
        sys.exit('part2 casualty files are not fit to bundle:\n  ' + '\n  '.join(found))
    return {'columns': [{k: row[k] for k in ROW_KEYS} for row in columns['rows']],
            'about': about['pages'],
            'related': related['related']}
