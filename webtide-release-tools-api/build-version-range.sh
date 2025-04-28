#!/usr/bin/env bash

git_repo_home=$1
version_prefix=$2
config_file=$3

function printUsage()
{
    echo "Usage: $0 <git_repo_home> <version_prefix> <version_config_file>"
}

if [ -z $git_repo_home ] ; then
    echo "ERROR: Must specify git_repo_home directory"
    printUsage
    exit -1
fi

if [ ! -d $git_repo_home ] ; then
    echo "ERROR: Invalid git_repo_home directory (directory doesn't exist): $git_repo_home"
    printUsage
    exit -1
fi

if [ -z $version_prefix ] ; then
    echo "ERROR: Must specify version_prefix"
    printUsage
    exit -1
fi

if [ -z $config_file ] ; then
    echo "ERROR: Must specify config_file"
    printUsage
    exit -1
fi

if [ ! -f $config_file ] ; then
    echo "ERROR: Invalid config_file (file does not exist): $config_file"
    printUsage
    exit -1
fi

# TODO: use git tag to get the list of tags, sort them numerically, and then run the set of changes for each rev.

echo java -jar target/webtide-release-tools-api-1.0-SNAPSHOT-hybrid.jar \
     --config_file=$config_file \
     --repo_path=$git_repo_home \
     --includeDependencyChanges=false \
     --tag_version_prior=jetty-12.0.18 \
     --ref_version_current=jetty-12.0.19 \
     --output_path=logs/12.0.19

# TODO: aggressively use temp directory filesystem cache for github in above command line.