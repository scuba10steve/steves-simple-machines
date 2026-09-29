#!/usr/bin/env bash

version=${1:-"0.1.0"}
username=${2:-"Steven Tompkins"}
modpack=${3:-"steves-simple-machines-env"}

cp -r "/mnt/c/Users/${username}/AppData/Roaming/gdlauncher_carbon/data/instances/${modpack}/logs" "logs"
