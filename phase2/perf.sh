#!/bin/bash

sh run.sh p 1 100 2 > results/1/READ_Load_2_1_RM
sh run.sh p 1 100 4 > results/1/READ_Load_4_1_RM
sh run.sh p 1 100 8 > results/1/READ_Load_8_1_RM
sh run.sh p 1 100 16 > results/1/READ_Load_16_1_RM
sh run.sh p 1 100 32 > results/1/READ_Load_32_1_RM
sh run.sh p 1 100 64 > results/1/READ_Load_64_1_RM
sh run.sh p 1 100 128 > results/1/READ_Load_128_1_RM