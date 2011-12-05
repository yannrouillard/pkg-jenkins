#!/bin/sh
SLAVE_JAR=/var/run/jenkins/slave.jar
# Retrieve Slave JAR from Master Server
echo "Downloading slave.jar from ${JENKINS_URL}..."
wget -q -O ${SLAVE_JAR} ${JENKINS_URL}/jnlpJars/slave.jar

# Check to make sure slave.jar was downloaded.
if [ -f ${SLAVE_JAR} ] 
then
	exit 0
else
	exit 1
fi
