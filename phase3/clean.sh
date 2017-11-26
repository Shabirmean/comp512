#!/bin/bash

DIR=$(pwd)
find . -type f -name '*.class' -exec rm {} +
