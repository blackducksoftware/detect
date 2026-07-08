#!/bin/bash

markdownDir="docs/markdown"
if [ -d "$markdownDir" ]
then
  echo "$markdownDir is a directory."
else
  echo "$markdownDir is not a directory. You need to run from the root of the Black Duck Detect project (e.g. src/blackduck-detect)"
  exit 1
fi

convertOneScript=/tmp/convertOneDocFile.sh
concernsFile=/tmp/concerns.txt
rm $concernsFile
touch $concernsFile

echo "#!/bin/bash" > ${convertOneScript}
echo "# Arg 1: path of .md file relative to docs/markdown" >> ${convertOneScript}
echo "# Arg 2: file to write concerns to" >> ${convertOneScript}
echo "f=\$1" >> ${convertOneScript}
echo "targetfile=\$1" >> ${convertOneScript}
echo "echo \"Checking \${targetfile}\"" >> ${convertOneScript}
echo "sectioncount=\$(grep '^##' \${targetfile} | wc -l)" >> ${convertOneScript}
echo "if (( \$sectioncount > 1 )); then"  >> ${convertOneScript}
echo "    echo \"\${sectioncount} Heading2's in \${1}\" >> \${2}" >> ${convertOneScript}
echo "fi" >> ${convertOneScript}

chmod +x ${convertOneScript}

cd $markdownDir
pwd
find . -name "*.md" -exec $convertOneScript {} ${concernsFile} \;

echo ""
sort -u $concernsFile
echo ""
echo "These warnings are in $concernsFile"
