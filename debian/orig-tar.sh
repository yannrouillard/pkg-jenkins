#/bin/sh -e

VERSION=$2
TAR=../jenkins_$VERSION.orig.tar.gz
DIR=jenkins-$VERSION
mkdir -p $DIR

# Unpack ready fo re-packing
tar -xzf $TAR -C $DIR --strip-components=1

# Repack excluding stuff we don't need
GZIP=--best tar -czf $TAR --exclude '*.jar' --exclude '*.class' \
     --exclude 'CVS' --exclude '.svn' --exclude "jenkins-$VERSION/debian" \
     --exclude "jenkins-$VERSION/test" --exclude "jenkins-$VERSION/osx" \
     --exclude "jenkins-$VERSION/ebuild" --exclude "jenkins-$VERSION/opensuse" \
     --exclude "jenkins-$VERSION/rpm" --exclude 'hudson-logo.vsd' \
     --exclude 'TangoProject-License.url' --exclude 'dummy.keystore' $DIR

rm -rf $DIR

