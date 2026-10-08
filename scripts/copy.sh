#!/usr/bin/env bash

jar=$(ls neoforge/build/libs/ssm-*.jar 2>/dev/null | sort -V | tail -1)
username=${1:-"Steven Tompkins"}
modpack=${2:-"steves-simple-machines-env"}

if [ -z "$jar" ]; then
  echo "No jar found in neoforge/build/libs/. Run ./gradlew :neoforge:build first."
  exit 1
fi

modsdir="/mnt/c/Users/${username}/AppData/Roaming/gdlauncher_carbon/data/instances/${modpack}/instance/mods"

# Remove old ssm jars to avoid duplicate mod conflicts
rm -f "$modsdir"/ssm-*.jar

echo "Copying $jar"
cp "$jar" "$modsdir"
