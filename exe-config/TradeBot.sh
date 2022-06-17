#!/bin/bash



while true
do
	sh DelayRestarter.sh&
	java -jar TradeBot.jar
done
