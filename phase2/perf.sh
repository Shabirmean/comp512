#!/bin/bash

testType=$1


if [ "$testType" = "1" ]; then
  	sh run.sh p 1 100 2 false 0 > results/1/READ_Load_2_1_RM
	sh run.sh p 1 100 4 false 0 > results/1/READ_Load_4_1_RM
	sh run.sh p 1 100 8 false 0 > results/1/READ_Load_8_1_RM
	sh run.sh p 1 100 16 false 0 > results/1/READ_Load_16_1_RM
	sh run.sh p 1 100 32 false 0 > results/1/READ_Load_32_1_RM
	sh run.sh p 1 100 64 false 0 > results/1/READ_Load_64_1_RM
	sh run.sh p 1 100 96 false 0 > results/1/READ_Load_96_1_RM
	sh run.sh p 1 100 104 false 0 > results/1/READ_Load_104_1_RM
	sh run.sh p 1 100 106 false 0 > results/1/READ_Load_106_1_RM
	sh run.sh p 1 100 128 false 0 > results/1/READ_Load_128_1_RM
	sh run.sh p 1 100 256 false 0 > results/1/READ_Load_256_1_RM
	sh run.sh p 1 100 512 false 0 > results/1/READ_Load_512_1_RM
fi

if [ "$testType" = "2" ]; then
  	sh run.sh p 2 100 2 false 0 > results/2/READ_Load_2_M_RM
	sh run.sh p 2 100 4 false 0 > results/2/READ_Load_4_M_RM
	sh run.sh p 2 100 8 false 0 > results/2/READ_Load_8_M_RM
	sh run.sh p 2 100 16 false 0 > results/2/READ_Load_16_M_RM
	sh run.sh p 2 100 32 false 0 > results/2/READ_Load_32_M_RM
	sh run.sh p 2 100 64 false 0 > results/2/READ_Load_64_M_RM
	sh run.sh p 2 100 96 false 0 > results/2/READ_Load_96_M_RM
	sh run.sh p 2 100 104 false 0 > results/2/READ_Load_104_M_RM
	sh run.sh p 2 100 106 false 0 > results/2/READ_Load_106_M_RM
	sh run.sh p 2 100 107 false 0 > results/2/READ_Load_107_M_RM
	sh run.sh p 2 100 128 false 0 > results/2/READ_Load_128_M_RM
	sh run.sh p 2 100 256 false 0 > results/2/READ_Load_256_M_RM
	sh run.sh p 2 100 512 false 0 > results/2/READ_Load_512_M_RM
fi

if [ "$testType" = "3" ]; then
  	sh run.sh p 3 100 2 false 0 > results/3/WRITE_Load_2_1_RM
	sh run.sh p 3 100 4 false 0 > results/3/WRITE_Load_4_1_RM
	sh run.sh p 3 100 8 false 0 > results/3/WRITE_Load_8_1_RM
	sh run.sh p 3 100 16 false 0 > results/3/WRITE_Load_16_1_RM
	sh run.sh p 3 100 32 false 0 > results/3/WRITE_Load_32_1_RM
	sh run.sh p 3 100 64 false 0 > results/3/WRITE_Load_64_1_RM
	sh run.sh p 3 100 96 false 0 > results/3/WRITE_Load_96_1_RM
	sh run.sh p 3 100 104 false 0 > results/3/WRITE_Load_104_1_RM
	sh run.sh p 3 100 106 false 0 > results/3/WRITE_Load_106_1_RM
	sh run.sh p 3 100 107 false 0 > results/3/WRITE_Load_107_1_RM
	sh run.sh p 3 100 128 false 0 > results/3/WRITE_Load_128_1_RM
	sh run.sh p 3 100 256 false 0 > results/3/WRITE_Load_256_1_RM
	sh run.sh p 3 100 512 false 0 > results/3/WRITE_Load_512_1_RM
fi


if [ "$testType" = "4" ]; then
  	sh run.sh p 4 100 2 false 0 > results/4/WRITE_Load_2_M_RM
	sh run.sh p 4 100 4 false 0 > results/4/WRITE_Load_4_M_RM
	sh run.sh p 4 100 8 false 0 > results/4/WRITE_Load_8_M_RM
	sh run.sh p 4 100 16 false 0 > results/4/WRITE_Load_16_M_RM
	sh run.sh p 4 100 32 false 0 > results/4/WRITE_Load_32_M_RM
	sh run.sh p 4 100 64 false 0 > results/4/WRITE_Load_64_M_RM
	sh run.sh p 4 100 96 false 0 > results/4/WRITE_Load_96_M_RM
	sh run.sh p 4 100 104 false 0 > results/4/WRITE_Load_104_M_RM
	sh run.sh p 4 100 106 false 0 > results/4/WRITE_Load_106_M_RM
	sh run.sh p 4 100 107 false 0 > results/4/WRITE_Load_107_M_RM
	sh run.sh p 4 100 128 false 0 > results/4/WRITE_Load_128_M_RM
	sh run.sh p 4 100 256 false 0 > results/4/WRITE_Load_256_M_RM
	sh run.sh p 4 100 512 false 0 > results/4/WRITE_Load_512_M_RM
fi


if [ "$testType" = "5" ]; then
  	sh run.sh p 5 100 2 false 0 > results/5/RW_Load_2_1_RM
	sh run.sh p 5 100 4 false 0 > results/5/RW_Load_4_1_RM
	sh run.sh p 5 100 8 false 0 > results/5/RW_Load_8_1_RM
	sh run.sh p 5 100 16 false 0 > results/5/RW_Load_16_1_RM
	sh run.sh p 5 100 32 false 0 > results/5/RW_Load_32_1_RM
	sh run.sh p 5 100 64 false 0 > results/5/RW_Load_64_1_RM
	sh run.sh p 5 100 96 false 0 > results/5/RW_Load_96_1_RM
	sh run.sh p 5 100 104 false 0 > results/5/RW_Load_104_1_RM
	sh run.sh p 5 100 106 false 0 > results/5/RW_Load_106_1_RM
	sh run.sh p 5 100 107 false 0 > results/5/RW_Load_107_1_RM
	sh run.sh p 5 100 128 false 0 > results/5/RW_Load_128_1_RM
	sh run.sh p 5 100 256 false 0 > results/5/RW_Load_256_1_RM
	sh run.sh p 5 100 512 false 0 > results/5/RW_Load_512_1_RM
fi

if [ "$testType" = "6" ]; then
  	sh run.sh p 6 100 2 false 0 > results/6/RW_Load_2_M_RM
	sh run.sh p 6 100 4 false 0 > results/6/RW_Load_4_M_RM
	sh run.sh p 6 100 8 false 0 > results/6/RW_Load_8_M_RM
	sh run.sh p 6 100 16 false 0 > results/6/RW_Load_16_M_RM
	sh run.sh p 6 100 32 false 0 > results/6/RW_Load_32_M_RM
	sh run.sh p 6 100 64 false 0 > results/6/RW_Load_64_M_RM
	sh run.sh p 6 100 96 false 0 > results/6/RW_Load_96_M_RM
	sh run.sh p 6 100 104 false 0 > results/6/RW_Load_104_M_RM
	sh run.sh p 6 100 106 false 0 > results/6/RW_Load_106_M_RM
	sh run.sh p 6 100 107 false 0 > results/6/RW_Load_107_M_RM
	sh run.sh p 6 100 128 false 0 > results/6/RW_Load_128_M_RM
	sh run.sh p 6 100 256 false 0 > results/6/RW_Load_256_M_RM
	sh run.sh p 6 100 512 false 0 > results/6/RW_Load_512_M_RM
fi


if [ "$testType" = "7" ]; then
  	sh run.sh p 7 20 1 true 10 > results/distrib/Ten_1
	sh run.sh p 7 20 2 true 10 > results/distrib/Ten_2
	sh run.sh p 7 20 5 true 10 > results/distrib/Ten_5
	sh run.sh p 7 20 10 true 10 > results/distrib/Ten_10
	sh run.sh p 7 20 1 true 20 > results/distrib/Twenty_1
	sh run.sh p 7 20 2 true 20 > results/distrib/Twenty_2
	sh run.sh p 7 20 5 true 20 > results/distrib/Twenty_5
	sh run.sh p 7 20 10 true 20 > results/distrib/Twenty_10
	sh run.sh p 7 20 1 true 50 > results/distrib/Fifty_1
	sh run.sh p 7 20 2 true 50 > results/distrib/Fifty_2
	sh run.sh p 7 20 5 true 50 > results/distrib/Fifty_5
	sh run.sh p 7 20 10 true 50 > results/distrib/Fifty_10
fi
