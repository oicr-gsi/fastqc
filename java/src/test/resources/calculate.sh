#/bin/bash
cd $1
 
find . -name "*.zip" -exec unzip {} \; >/dev/null
find . -type f -name fastqc_report.html -exec sed -i '/<div id="header_filename">/,/<\/div>/{/<div id="header_filename">/!{/<\/div>/!d}}' {} \;
find . -type f -not -path "./*.zip" -exec md5sum {} + 
 

