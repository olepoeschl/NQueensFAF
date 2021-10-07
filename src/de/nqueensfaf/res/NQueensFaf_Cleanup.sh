#!/bin/sh
sleep 5;
tmpdir=$(dirname $(mktemp -u));
find $tmpdir -type d -name "NQueensFaf*" -print0 | xargs -0 rm -r -- 2>&1 | grep -v "find: ";
rm -- "$0";