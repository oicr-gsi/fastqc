#/bin/bash
set -o nounset
set -o errexit
set -o pipefail
set -o noclobber

cd "$1"

find . -type f -name "*fastqc.html" -printf '%p\t' -exec bash -c 'cat "$0" | sed "s/\(<div id=\"header_filename\">\)[^<]*\(<br\/>\)/\1\2/g" | md5sum | cut -d " " -f 1' {} \;