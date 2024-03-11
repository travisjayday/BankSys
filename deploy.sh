#!/bin/bash

name="BankSys"
pluginName="BankSys"
server="/clients/frunex/bankmc/server"
plugins="/clients/frunex/bankmc"

echo "Deploying ..."
start="$(date +%s)"

"mvn" eclipse:clean -f "$plugins/${name}/pom.xml"
"mvn" eclipse:eclipse -f "$plugins/${name}/pom.xml"
"mvn" install -f "$plugins/${name}/pom.xml"
cp "$plugins/${name}/target/${pluginName}-1.0-SNAPSHOT.jar" "$server/plugins/"
screen -d -m stardust_screen "pm unload ${pluginName}\\n"
sleep 0.5
screen -d -m stardust_screen "pm load ${pluginName}\\n"

runtime=$[ $(date +%s) - $start ]
echo -e "\033[0;32mCreated successfully in ${runtime} seconds!\033[0m"