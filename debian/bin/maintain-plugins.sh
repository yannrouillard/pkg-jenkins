#!/bin/sh
# Maintains plugins installed through Ubuntu/Debian

PLUGIN_HOME="/var/lib/jenkins/plugins"
PLUGIN_ROOT="/usr/share/jenkins/plugins"

if [ -d ${PLUGIN_ROOT} ]; then
    echo "Checking for new plugins"
    for plugin in $(ls -1 ${PLUGIN_ROOT}/*.hpi); do
        name=$(basename ${plugin})
        if [ ! -w ${PLUGIN_HOME}/${name} ]; then
            echo "Installing plugin: ${name}"
            ln -s  ${PLUGIN_ROOT}/${name} ${PLUGIN_HOME}/${name}
        fi
    done
fi

# Removes and installed plugins where owned by root
# leaving any installed through jenkins intact
echo "Removing un-installed plugins"
for plugin in $(find ${PLUGIN_HOME} -name "*.hpi" -user root \
                -type l); do
    name=$(basename ${plugin})
    if [ ! -r ${PLUGIN_ROOT}/${name} ]; then
        echo "Removing plugin: ${name}"
        rm -rf ${PLUGIN_HOME}/${name%.*}*
    fi
done

