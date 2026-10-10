#!/bin/bash
mongoimport \
  --db neAvito \
  --collection categories \
  --file /docker-entrypoint-initdb.d/01-categories.json \
  --jsonArray