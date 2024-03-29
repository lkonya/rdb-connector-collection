#!/usr/bin/env bash

function test_mysql {
  mysqladmin ping -h mysql-db-57 --silent
}

count=0
until ( test_mysql )
do
  ((count++))
  if [ ${count} -gt 1200 ]
  then
    echo "Services didn't become ready in time"
    exit 1
  fi
  sleep 0.1
done

sbt mysql/it:test
