#!/usr/bin/env python
from __future__ import with_statement
from stanford_to_conll import stanford_to_conll
import unittest

INPUT_FILENAME = "fixtures/input.txt"
EXPECTED_OUTPUT_FILENAME = "fixtures/expected_output.conll"


class TestStanfordToConll(unittest.TestCase):
    def test_fixtures(self):
        """
        Verifies that stanford_to_conll gives the expected output for the
        input
        """
        with open(INPUT_FILENAME) as in_file:
            in_text = in_file.read()
        with open(EXPECTED_OUTPUT_FILENAME) as out_file:
            expected_out_text = out_file.read()
        # run stanford_to_conll on each line
        out_text = '\n'.join(stanford_to_conll(line) for line in in_text.split('\n'))
        # verify that it matches the expected output
        self.assertEqual(out_text, expected_out_text)

if __name__ == "__main__":
    unittest.main()
