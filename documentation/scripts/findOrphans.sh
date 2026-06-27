#!/bin/bash

markdownDir="docs/markdown"
if [ -d "$markdownDir" ]
then
  echo "$markdownDir is a directory."
else
  echo "$markdownDir is not a directory. You need to run from the root of the Detect project (e.g. src/blackduck-detect)"
  exit 1
fi

convertOneScript=/tmp/convertOneDocFile.sh
concernsFile=/tmp/concerns.txt
rm $concernsFile
touch $concernsFile

echo "#!/bin/bash" > ${convertOneScript}
echo "# Arg 1: path of .md file relative to docs/templates/content" >> ${convertOneScript}
echo "f=\$1" >> ${convertOneScript}

##echo "filename=\$(echo \${f} | sed 's/.*\///')" >> ${convertOneScript}
##echo "filename=\$(echo \${f} | sed 's/.*\/\(.*\/\)/\1/') | sed 's/markdown\///'" >> ${convertOneScript}
echo "filename=\$(echo \${f} | sed 's/.*\/\(.*\/\)/\1/' | sed 's/markdown\///')" >> ${convertOneScript}

##echo "echo \${filename}:" >> ${convertOneScript}
##echo "grep \${filename} detect.ditamap | wc -l" >> ${convertOneScript}
echo "refcount=\$(grep \${filename} detect.ditamap | wc -l)" >> ${convertOneScript}
echo "if (( \$refcount < 1 )); then"  >> ${convertOneScript}
echo "    echo \"\${filename} is not referenced in detect.ditamap\"" >> ${convertOneScript}
echo "fi" >> ${convertOneScript}

chmod +x ${convertOneScript}

cd $markdownDir
cd ..
rm -rf generated
pwd
find . -name "*.md" -exec $convertOneScript {} ${concernsFile} \;


