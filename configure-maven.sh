#!/bin/bash

sed -i~ "/<servers>/ a\
<server>\
  <id>${NEXUS_SERVER_ID}</id>\
  <username>${MAVEN_USERNAME}</username>\
  <password>${MAVEN_PASSWORD}</password>\
</server>" /usr/share/maven/conf/settings.xml
