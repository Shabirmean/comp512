#!/bin/bash

testType=$1


if [ "$testType" = "1" ]; then
  	sh run.sh p 1 100 2 > results/1/READ_Load_2_1_RM
	sh run.sh p 1 100 4 > results/1/READ_Load_4_1_RM
	sh run.sh p 1 100 8 > results/1/READ_Load_8_1_RM
	sh run.sh p 1 100 16 > results/1/READ_Load_16_1_RM
	sh run.sh p 1 100 32 > results/1/READ_Load_32_1_RM
	sh run.sh p 1 100 64 > results/1/READ_Load_64_1_RM
	sh run.sh p 1 100 128 > results/1/READ_Load_128_1_RM
fi

if [ "$testType" = "2" ]; then
  	sh run.sh p 2 100 2 > results/2/READ_Load_2_M_RM
	sh run.sh p 2 100 4 > results/2/READ_Load_4_M_RM
	sh run.sh p 2 100 8 > results/2/READ_Load_8_M_RM
	sh run.sh p 2 100 16 > results/2/READ_Load_16_M_RM
	sh run.sh p 2 100 32 > results/2/READ_Load_32_M_RM
	sh run.sh p 2 100 64 > results/2/READ_Load_64_M_RM
	sh run.sh p 2 100 96 > results/2/READ_Load_96_M_RM
	sh run.sh p 2 100 104 > results/2/READ_Load_104_M_RM
	sh run.sh p 2 100 106 > results/2/READ_Load_106_M_RM
	sh run.sh p 2 100 107 > results/2/READ_Load_107_M_RM
	sh run.sh p 2 100 128 > results/2/READ_Load_128_M_RM
fi

if [ "$testType" = "3" ]; then
  	sh run.sh p 3 100 2 > results/3/WRITE_Load_2_1_RM
	sh run.sh p 3 100 4 > results/3/WRITE_Load_4_1_RM
	sh run.sh p 3 100 8 > results/3/WRITE_Load_8_1_RM
	sh run.sh p 3 100 16 > results/3/WRITE_Load_16_1_RM
	sh run.sh p 3 100 32 > results/3/WRITE_Load_32_1_RM
	sh run.sh p 3 100 64 > results/3/WRITE_Load_64_1_RM
	sh run.sh p 3 100 96 > results/3/WRITE_Load_96_1_RM
	sh run.sh p 3 100 104 > results/3/WRITE_Load_104_1_RM
	sh run.sh p 3 100 106 > results/3/WRITE_Load_106_1_RM
	sh run.sh p 3 100 107 > results/3/WRITE_Load_107_1_RM
	sh run.sh p 3 100 128 > results/3/WRITE_Load_108_1_RM
fi



