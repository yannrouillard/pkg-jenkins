#/bin/sh -e

VERSION=$2
TAR=../jenkins_$VERSION.orig.tar.gz
DIR=jenkins-$VERSION
mkdir -p $DIR

# Unpack ready fo re-packing
tar -xzf $TAR -C $DIR --strip-components=1

# Repack excluding stuff we don't need
GZIP=--best tar -czf $TAR --exclude '*.jar' --exclude '*.class' \
     --exclude 'CVS' --exclude '.svn' --exclude 'debian' $DIR
rm -rf $DIR

