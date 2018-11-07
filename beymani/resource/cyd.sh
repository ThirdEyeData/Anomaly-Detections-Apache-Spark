#!/bin/bash

PROJECT_HOME=/home/centeritylatest/Documents/CenterityLatest
SPARK_HOME=/home/centeritylatest/Documents/CenterityLatest/local-spark-1-2/client
CHOMBO_JAR_NAME=$PROJECT_HOME/chombo/resource/uber-chombo-spark-1.0.jar
BEYMANI_JAR_NAME=$PROJECT_HOME/beymani/resource/uber-beymani-spark-1.0.jar
MASTER=spark://localhost:7077

case "$1" in

"numStat")
	echo "running NumericalAttrStats Spark job"
	CLASS_NAME=org.chombo.spark.explore.NumericalAttrStats
	INPUT=$PROJECT_HOME/beymani/input/teg/cusage.csv
	OUTPUT=$PROJECT_HOME/beymani/output/mea
	rm -rf ./output/mea
	$SPARK_HOME/bin/spark-submit --class $CLASS_NAME   \
	--conf spark.ui.killEnabled=true --master $MASTER $CHOMBO_JAR_NAME  $INPUT $OUTPUT $PROJECT_HOME/beymani/resource/cyd.conf
;;

"crStatsFile")
	echo "copying and consolidating stats file"
	cat $PROJECT_HOME/beymani/output/mea/part-00000 > $PROJECT_HOME/beymani/other/auc/stats.txt
	cat $PROJECT_HOME/beymani/output/mea/part-00001 >> $PROJECT_HOME/beymani/other/auc/stats.txt
	ls -l $PROJECT_HOME/beymani/other/auc
;;

"tempAggr")
	echo "running TemporalAggregator Spark job"
	CLASS_NAME=org.chombo.spark.explore.TemporalAggregator
	INPUT=$PROJECT_HOME/beymani/input/teg/cusage.csv
	OUTPUT=$PROJECT_HOME/beymani/output/teg
	rm -rf ./output/teg
	$SPARK_HOME/bin/spark-submit --class $CLASS_NAME   \
	--conf spark.ui.killEnabled=true --master $MASTER $CHOMBO_JAR_NAME  $INPUT $OUTPUT $PROJECT_HOME/beymani/resource/cyd.conf
;;

"crAucInput")
	echo "copying and consolidating tem aggregation output file"
	cat $PROJECT_HOME/beymani/output/teg/part-00000 > $PROJECT_HOME/beymani/input/auc/cusage.csv
	cat $PROJECT_HOME/beymani/output/teg/part-00001 >> $PROJECT_HOME/beymani/input/auc/cusage.csv
	ls -l $PROJECT_HOME/beymani/input/auc
;;

"autoCor")
	echo "running AutoCorrelation Spark job"
	CLASS_NAME=org.chombo.spark.explore.AutoCorrelation
	INPUT=$PROJECT_HOME/beymani/input/auc/cusage.csv
	OUTPUT=$PROJECT_HOME/beymani/output/auc
	rm -rf ./output/auc
	$SPARK_HOME/bin/spark-submit --class $CLASS_NAME   \
	--conf spark.ui.killEnabled=true --master $MASTER $CHOMBO_JAR_NAME  $INPUT $OUTPUT $PROJECT_HOME/beymani/resource/cyd.conf
;;

*) 
	echo "unknown operation $1"
	;;

esac
