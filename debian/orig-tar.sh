#/bin/sh -e

VERSION=$2
TAR=../jenkins_$VERSION.orig.tar.xz
DIR=jenkins-$VERSION
mkdir -p $DIR

# Unpack ready fo re-packing
tar -xzf $3 -C $DIR --strip-components=1
rm $3

# Repack excluding stuff we don't need
XZ_OPT=--best tar -cJf $TAR \
     --exclude '*.jar' \
     --exclude '*.class' \
     --exclude '*.swf' \
     --exclude 'CVS' \
     --exclude '.svn' \
     --exclude "jenkins-$VERSION/debian" \
     --exclude "jenkins-$VERSION/test" \
     --exclude "jenkins-$VERSION/osx" \
     --exclude "jenkins-$VERSION/ebuild" \
     --exclude "jenkins-$VERSION/opensuse" \
     --exclude "jenkins-$VERSION/rpm" \
     --exclude "jenkins-$VERSION/ips" \
     --exclude "jenkins-$VERSION/msi" \
     --exclude 'hudson-logo.vsd' \
     --exclude 'TangoProject-License.url' \
     --exclude 'dummy.keystore' \
     $DIR

rm -rf $DIR
