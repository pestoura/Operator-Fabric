#!/usr/bin/env bash

# Default values
revisionDate="$(LC_ALL=en_GB.utf8 date +'%d %B %Y')"

display_usage() {
	echo "This script makes the necessary changes to version controlled files to prepare for a RELEASE version."
	echo -e "Usage:\n"
	echo -e "\tprepare_release_version.sh [OPTIONS] \n"
	echo -e "options:\n"
	echo -e "\t-d, --date  : string. Revision date (please use %d %B %Y format). Defaults to today ($revisionDate)"
}

# Read parameters
while [[ $# -gt 0 ]]
do
key="$1"
# echo $key
case $key in
    -d|--date)
    revisionDate="$2"
    shift # past argument
    shift # past value
    ;;
    -h|--help)
    shift # past argument
display_usage
    exit 0
    ;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done

echo "Revision date: $revisionDate"

# Get current (SNAPSHOT) version from VERSION file
oldVersion=$(cat VERSION)
echo "Current version is $oldVersion (based on VERSION file)"

# Check that current version is a SNAPSHOT version as expected
if [[ $oldVersion != *.SNAPSHOT ]]; then
  echo "Current version is not a SNAPSHOT version, this script shouldn't be used."
  exit 1;
fi

# Determine RELEASE version
newVersion=$(cat VERSION | sed 's/SNAPSHOT/RELEASE/' )
echo "Preparing $newVersion"

# Replace SNAPSHOT with RELEASE
echo "Updating version for pipeline in VERSION file"
sed -i "s/$oldVersion/$newVersion/g" VERSION;

echo "Replacing $oldVersion with $newVersion in swagger.yaml files"
find . -name swagger.yaml | xargs sed -i "s/\(version: *\)$oldVersion/\1$newVersion/g";
# With the current commmand, if the "version" key appears somewhere else in the file it will be affected as well.
# That's why oldVersion is part of the pattern, as it is less likely that another version key would appear with the exact same value.
# The issue is that if the value has been mistakenly modified and is not $oldVersion, it won't be updated
# TODO Find a better solution or add a check

echo "Replacing $oldVersion with $newVersion in :revnumber in adoc files"
find . -name "*.adoc" | xargs sed -i "s/\(:revnumber: *\)$oldVersion/\1$newVersion/g";

echo "Replacing $oldVersion with $newVersion in links in adoc files"
find . -name "*.adoc" | xargs sed -i "s/\/$oldVersion\//\/$newVersion\//g";

echo "Updating revision date in adoc files"
find . -name "*.adoc" | xargs sed -i "s/\(:revdate:\)\(.*\)$/\1 $revisionDate/g";

echo "Using $newVersion for lfeoperatorfabric images in deploy docker-compose file"
# String example for regexp: image: "lfeoperatorfabric/of-web-ui:0.13.1.RELEASE"
sed -i "s/\( *image *: *\"lfeoperatorfabric\/.*:\)\(.*\)\"/\1$newVersion\"/g" ./src/main/docker/deploy/docker-compose.yml;

echo "The following files have been updated: "
echo | git status --porcelain

