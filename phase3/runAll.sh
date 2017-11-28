#!/bin/bash

git stash && git pull && sh clean.sh
sh setup.sh && sh setup.sh