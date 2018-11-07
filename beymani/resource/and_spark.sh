#!/bin/bash

PROJECT_HOME=/home/centeritylatest/Documents/CenterityLatest
SPARK_HOME=/home/centeritylatest/Documents/CenterityLatest/local-spark-1-2/client
CHOMBO_JAR_NAME=$PROJECT_HOME/chombo/resource/uber-chombo-spark-1.0.jar
BEYMANI_JAR_NAME=$PROJECT_HOME/beymani/resource/uber-beymani-spark-1.0.jar
MASTER=spark://localhost:7077


case "$1" in

"crInput")
    echo "args: num_of_days time_interval(sec) num_of_servers output_file"
    python3 $PROJECT_HOME/beymani/python/app/cpu_usage.py usage $2 $3 $4 > $5
    ls -l $PROJECT_HOME/beymani/resource
;;

"insOutliers")
    echo "args: normal_data_file  output_file"
    python3 $PROJECT_HOME/beymani/python/app/cpu_usage.py anomaly $2 > $3
    ls -l $PROJECT_HOME/beymani/resource
;;

"cpModData")
    echo "args: modeling_data_file  "
    cp $2 $PROJECT_HOME/beymani/input/nas/
    cp $2 $PROJECT_HOME/beymani/input/olp/
    ls -l $PROJECT_HOME/beymani/input/nas
    ls -l $PROJECT_HOME/beymani/input/olp
;;

"cpTestData")
    echo "args: test_data_file  "
    cp $2 $PROJECT_HOME/beymani/input/olp/
    ls -l $PROJECT_HOME/beymani/input/olp
;;

"numStat")
    echo "running NumericalAttrStats Spark job"
    rm -rf $PROJECT_HOME/beymani/output/nas
    CLASS_NAME=org.chombo.spark.explore.NumericalAttrStats
    INPUT=$PROJECT_HOME/beymani/resource/cusage.csv
    OUTPUT=$PROJECT_HOME/beymani/output/nas
    $SPARK_HOME/bin/spark-submit --class $CLASS_NAME   \
    --conf spark.ui.killEnabled=true --master $MASTER $CHOMBO_JAR_NAME  $INPUT $OUTPUT $PROJECT_HOME/beymani/resource/and.conf
;;

"crStatsFile")
    echo "copying and consolidating stats file"
    cat $PROJECT_HOME/beymani/output/nas/part-00000 > $PROJECT_HOME/beymani/other/olp/stats.txt
    cat $PROJECT_HOME/beymani/output/nas/part-00001 >> $PROJECT_HOME/beymani/other/olp/stats.txt
    ls -l $PROJECT_HOME/beymani/other/olp
;;

"olPred")
    echo "running StatsBasedOutlierPredictor Spark job"
    rm -rf $PROJECT_HOME/beymani/output/olp
    rm -rf $PROJECT_HOME/beymani/other/olp/clean   
    CLASS_NAME=org.beymani.spark.dist.StatsBasedOutlierPredictor
    INPUT=$PROJECT_HOME/beymani/input/olp/cusage.csv
    OUTPUT=$PROJECT_HOME/beymani/output/olp
    $SPARK_HOME/bin/spark-submit --class $CLASS_NAME   \
    --conf spark.ui.killEnabled=true --master $MASTER $BEYMANI_JAR_NAME  $INPUT $OUTPUT $PROJECT_HOME/beymani/resource/and1.conf
    echo "number of outliers"
    wc -l $PROJECT_HOME/beymani/output/olp/part-00000
    wc -l $PROJECT_HOME/beymani/output/olp/part-00001
;;


"crCleanFile")
    echo "copying, consolidating and moving clean training data file"
    cat $PROJECT_HOME/beymani/output/olp/part-00000 > $PROJECT_HOME/beymani/other/olp/cusage.csv
    cat $PROJECT_HOME/beymani/output/olp/part-00001 >> $PROJECT_HOME/beymani/other/olp/cusage.csv
    cp $PROJECT_HOME/beymani/input/nas/cusage.csv $PROJECT_HOME/beymani/other/nas/cusage_1.txt
    cp $PROJECT_HOME/beymani/other/olp/cusage.csv $PROJECT_HOME/beymani/input/nas/cusage.csv
    cp $PROJECT_HOME/beymani/other/olp/stats.txt $PROJECT_HOME/beymani/other/olp/stats_1.txt
;;


"mvOutlFile")
    echo "moving outlier output file"
    cat $PROJECT_HOME/beymani/output/olp/part-00000 > $PROJECT_HOME/beymani/other/olp/outl.txt
    cat $PROJECT_HOME/beymani/output/olp/part-00001 >> $PROJECT_HOME/beymani/other/olp/outl.txt
;;


*)
    echo "unknown operation $1"
    ;;

esac
