#!/bin/bash
#
#This script can only run through Jenkins
set -e
source config.env

INTERNAL_DOCKER_REGISTRY="artifactory.tools.duckutil.net:5002"
COMMIT_MESSAGE=$(git log -1 --pretty=%B)
GIT_USER_EMAIL=$(git config user.email)
GIT_USER_NAME=$(git config user.name)

docker tag ${TARGET_REPO}/${TARGET_IMAGE}:${TARGET_IMAGE_TAG} ${INTERNAL_DOCKER_REGISTRY}/${TARGET_REPO}/${TARGET_IMAGE}:${TARGET_IMAGE_TAG}
docker push ${INTERNAL_DOCKER_REGISTRY}/${TARGET_REPO}/${TARGET_IMAGE}:${TARGET_IMAGE_TAG}
docker tag ${INTERNAL_DOCKER_REGISTRY}/${TARGET_REPO}/${TARGET_IMAGE}:${TARGET_IMAGE_TAG} ${TARGET_REPO}/${TARGET_IMAGE}:${TARGET_IMAGE_TAG}

# clean up docker images after pushing
docker rmi ${INTERNAL_DOCKER_REGISTRY}/${TARGET_REPO}/${TARGET_IMAGE}:${TARGET_IMAGE_TAG}
docker rmi ${TARGET_REPO}/${TARGET_IMAGE}:${TARGET_IMAGE_TAG}

# upload to artifactory
zip_sha=$(echo $(sha256sum detect-${RELEASE_VERSION}-air-gap.zip) | cut -d' ' -f 1)

sed -e "s/TARGET_IMAGE_TAG/${TARGET_IMAGE_TAG}/g" -e "s/RELEASE_VERSION/${RELEASE_VERSION}/g" -e "s/UBI_VERSION/${UBI_VERSION}/g" -e "s/ZIP_SHA256_VALUE/${zip_sha}/g" hardening_manifest.yaml.template > hardening_manifest.yaml

curl -X PUT -u ${ARTIFACTORY_USERNAME}:${ARTIFACTORY_PASSWORD} ${ARTIFACTORY_HOST}/blackduck-repo1.dso.mil-generic/${TARGET_IMAGE}/detect-${RELEASE_VERSION}-air-gap.zip -T detect-${RELEASE_VERSION}-air-gap.zip

# push to repo1

PROJECT=${TARGET_IMAGE}

# create  tmp-repo1
rm -rf tmp-repo1
mkdir tmp-repo1
cd tmp-repo1

echo "cloning $PROJECT from repo1"
git clone ${REPO1_URL}/blackduck/${PROJECT}.git
cd ${PROJECT}
git config --global user.email ${GIT_USER_EMAIL}
git config --global user.name "${GIT_USER_NAME}"

# create and checkout branch
set +e
git show-branch remotes/origin/${BRANCH}
checkBranch=$?
set -e
if [ $checkBranch -eq 0 ]
then
    echo checkout existing ${BRANCH}
    git checkout ${BRANCH}
    git pull
else
    echo create and checkout ${BRANCH}
    git checkout -b ${BRANCH}
fi

# copy all files
echo copying files
cp ../../Dockerfile .
cp ../../LICENSE .
cp ../../README.md .
cp ../../hardening_manifest.yaml .

# git add
git add Dockerfile hardening_manifest.yaml README.md LICENSE
git status

# git commit and push
git commit -m "${COMMIT_MESSAGE}"
git push origin ${BRANCH}
