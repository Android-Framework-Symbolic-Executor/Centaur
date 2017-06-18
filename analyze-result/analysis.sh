#!/bin/bash
# $1: output from Eclipse
# $2: folder name where the analysis results should be put in; $2 should be ended with "/"

dir1=$2"result/"
if [ ! -d "$dir1" ]; then
    mkdir $dir1
    1-extractPathCondition/extractDiviePath -f $1  -o $dir1
else
    dirTemp=$dir1"*"
    rm $dirTemp
    1-extractPathCondition/extractDiviePath -f $1 -o $dir1    
fi



dir2=$2"formate/"
if [ ! -d "$dir2" ]; then
    mkdir $dir2
    2-transOutputToFormula/modifyFormateFolder -io $dir1 -oo $dir2
else
    dirTemp=$dir2"*"
    rm $dirTemp
    2-transOutputToFormula/modifyFormateFolder -io $dir1 -oo $dir2
fi



dir3=$2"formula/"
if [ ! -d "$dir3" ]; then
    mkdir $dir3
    2-transOutputToFormula/transFormulaFolder -io $dir2 -oo $dir3
else
    dirTemp=$dir3"*"
    rm $dirTemp
    2-transOutputToFormula/transFormulaFolder -io $dir2 -oo $dir3
fi


file1=$2"testCase"
2-transOutputToFormula/mathSatFolder -io $dir3 -pmath $3  > $file1



file2=$2"testCase_final"
3-generateTestCase/extractSatTestCase -f $file1 
3-generateTestCase/removeRepeatTestCase -f $file2

    
