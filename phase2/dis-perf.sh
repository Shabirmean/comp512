#!/bin/bash

testType=$1

sh run.sh p 6 100 2 true 0 > results/6/RW_Load_2_M_RM
sh run.sh p 6 100 4 true 0 > results/6/RW_Load_4_M_RM
sh run.sh p 6 100 8 true 0 > results/6/RW_Load_8_M_RM
sh run.sh p 6 100 16 true 0 > results/6/RW_Load_16_M_RM
sh run.sh p 6 100 32 true 0 > results/6/RW_Load_32_M_RM
sh run.sh p 6 100 64 true 0 > results/6/RW_Load_64_M_RM
sh run.sh p 6 100 96 true 0 > results/6/RW_Load_96_M_RM
sh run.sh p 6 100 104 true 0 > results/6/RW_Load_104_M_RM
sh run.sh p 6 100 106 true 0 > results/6/RW_Load_106_M_RM
sh run.sh p 6 100 107 true 0 > results/6/RW_Load_107_M_RM
sh run.sh p 6 100 128 true 0 > results/6/RW_Load_128_M_RM
sh run.sh p 6 100 256 true 0 > results/6/RW_Load_256_M_RM
sh run.sh p 6 100 512 true 0 > results/6/RW_Load_512_M_RM



