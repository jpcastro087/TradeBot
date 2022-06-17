#!/bin/bash

sleep 5s
currentDirectory=`pwd`
dbname=binance
username=postgres
export PGPASSWORD='postgres';
proccessId=`psql -t -d $dbname -U $username -c "select proccessid proccessid from restartparams where directory='"$currentDirectory"'"`
timeUnit=`psql -t -d $dbname -U $username -c "select timeunit timeunit from restartparams where directory = '"$currentDirectory"'"`
sleep $timeUnit
`kill $proccessId`

