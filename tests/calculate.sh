#!/bin/bash
cd $1

find .  -name "*\.zip" -exec unzip -q {} \; >/dev/null # unzip the results files

# - .txt files have no stochastic content, may be md5sum-checked
# - .html files are generically named, no stochastic content, may be md5sum-checked
# - .fo files are generically named, no stochastic content, may be md5sum-checked
# - Icons/*.png files are also generically named, no stochastic content, may be md5sum-checked
# - Images/*.png - the same

# Therefore:
# - Check md5sums for all types of files, sort

echo ".txt files:"
find . -name "*.txt" | xargs md5sum | sort -V

echo ".html files:"
for f in *.html;do sed 's/<div id="header_filename">.*<br\/>.*.fastq.gz<\/div>//' $f | md5sum;done | sort -V

echo ".fo files:"
find . -name "*.fo" | xargs md5sum | sort -V 

echo ".png files:"
find . -name "*.png" | xargs md5sum | sort -V
