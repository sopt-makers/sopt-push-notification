#!/bin/bash

SERVERLESS_DIR=".serverless"

if [ -d "$SERVERLESS_DIR" ] && [ "$(ls -A $SERVERLESS_DIR)" ]; then
  echo "Serverless directory is created successfuly and contains files."
else
  echo "Serverless directory is not created or not contains any files."
  exit 1
fi