#!/bin/bash

templatesDir="docs/templates/content"
markdownDir="docs/markdown"
if [ -d "$templatesDir" ]
then
  echo "$templatesDir is a directory."
else
  echo "$templatesDir is not a directory. You need to run from the root of the Detect project (e.g. src/blackduck-detect)"
  exit 1
fi
if [ -d "$markdownDir" ]
then
  echo "$markdownDir already exists."
else
  mkdir $markdownDir
fi

convertOneScript=/tmp/convertOneDocFile.sh
concernsFile=/tmp/concerns.txt
rm $concernsFile
touch $concernsFile

echo "#!/bin/bash" > ${convertOneScript}
echo "# Arg 1: path of .ftl file relative to docs/templates/content" >> ${convertOneScript}
echo "# Arg 2: file to write concerns to" >> ${convertOneScript}
echo "f=\$1" >> ${convertOneScript}
echo "basename=\"\${f%.*}\"" >> ${convertOneScript}
echo "targetfile=../../markdown/\${basename}.md" >> ${convertOneScript}
echo "targetdir=\$(dirname \$targetfile)" >> ${convertOneScript}
echo "mkdir -p \${targetdir}" >> ${convertOneScript}
echo "echo \"Converting \$f -> \${targetfile}\"" >> ${convertOneScript}
echo "sed 's/\\\${\\([a-z_]*\\)}/[\\1]/g'  \${basename}.ftl > \${targetfile}" >> ${convertOneScript}

echo "echo \"------------------------\" >> \${2}" >> ${convertOneScript}
echo "echo \"\${targetfile} warnings, including links/references to check/fix:\" >> \${2}" >> ${convertOneScript}
echo "grep '\[[^]]*\[' \${targetfile} >> \${2}" >> ${convertOneScript}

echo "sectioncount=\$(grep '^##' \${targetfile} | wc -l)" >> ${convertOneScript}

echo "if (( \$sectioncount > 1 )); then"  >> ${convertOneScript}
echo "    echo \"Heading 2 count (\${sectioncount}) is greater than one\" >> \${2}" >> ${convertOneScript}
echo "fi" >> ${convertOneScript}
echo "echo \"       \" >> \${2}" >> ${convertOneScript}

#echo "echo SECTION COUNT: \${sectioncount}" >> ${convertOneScript}

chmod +x ${convertOneScript}

cd $templatesDir
pwd
find . -name "*.ftl" -exec $convertOneScript {} ${concernsFile} \;

echo ""
cat $concernsFile
echo ""
echo "These warnings are in $concernsFile"
