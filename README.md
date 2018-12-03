# Anomaly Detections using Apache Spark

Assumptions

1) GIT is installed

2) JDK (1.8+) is installed

3) Scala (2.1+) is installed

4) Maven is installed

5) SBT (Scala build tool) is installed

6) Spark cluster (with 1 master and at least 1 worker) is up and running and accessible

7) Following environment variables should be set:

          SPARK_HOME is set in ~/.bashrc




Steps to setup the project in your local system

1)  GIT CLONE avenir , beymani , chombo , hoidla.

2)  Navigate to the folder named hoilda and execute the below commands:

          mvn clean install
          
          sbt publishLocal 
          

3)  Navigate to the folder named chombo and follow the below sequence:

  Build chombo first in master branch with 
          
          mvn clean install 
          
          sbt publishLocal 
          
  Build chombo-spark in chombo/spark directory 
          
          sbt clean package 
          
          sbt publishLocal

4)   Navigate to the folder named avenir and execute the below command
          
          mvn clean install

5)  Navigate to the folder named beymani and execute the below command
          
          mvn clean install
          
          sbt publishLocal

6) Build beymani-spark in beymani/spark directory
          
          sbt clean package 
          
          sbt publishLocal


7)  Navigate to the folder named beymani /resource and execute
          
          ant -f beymani_spark.xml

8)  Navigate to the folder named chombo /resource and execute the below command
          
          ant -f  chombo_spark.xml

9)  Navigate to the folder named beymani /resource  and edit the and_spark.sh file to reflect the path in your local system:

          Set the project home path       (  PROJECT_HOME )
          set the spark home path	    ( SPARK_HOME )
          set the master as spark master       ( MASTER )





Now you are ready to run the file and_spark.sh and below are the various parameters you should use to run the file:



Step 1 : Create base normal data

./and_spark.sh crInput <num_of_days> <reading_intervaL> <num_servers> <output_file>

where
num_of_days = number of days e.g 10

reading_intervaL = reading interval in sec e.g. 300

num_servers = number of servers e.g. 4

output_file = output file, we will use c.txt from now on


            ./and_spark.sh crInput 10 300 40 c.txt

Step 2 : Copy modeling data

- insert outliers

./and_spark.sh insOutliers <normal_data_file> <with_outlier_data_file> 

where

normal_data_file = normal data file (c.txt)

with_outlier_data_file = data file with outliers (cusage.txt)

            ./and_spark.sh insOutliers c.txt cusage.txt

-copy

./and_spark.sh cpModData <with_outlier_data_file> 

where

with_outlier_data_file = data file with outliers (cusage.txt)

            ./and_spark.sh cpModData cusage.csv

Step 3 : Run Spark job for stats

            ./and_spark.sh numStat

Step 4 : Copy and consolidate stats file

            ./and_spark.sh crStatsFile

Step 5 : Run Spark job to detect outliers

Set output.outliers = true and rem.outliers = true

            ./and_spark.sh olPred


Step 6 : Copy and consolidate clean file

            ./and_spark.sh crCleanFile

Step 7 : Copy test data

- insert outliers

./and_spark.sh insOutliers <normal_data_file> <with_outlier_data_file> 

where

normal_data_file = normal data file (c.txt)

with_outlier_data_file = data file with outliers (cusage.txt)

            ./and_spark.sh insOutliers c.txt cusage.txt

-copy

./and_spark.sh cpTestData <with_outlier_data_file> 

where

with_outlier_data_file = data file with outliers (cusage.txt)

            ./and_spark.sh cpTestData cusage.txt

Step 8 : Run Spark job for stats again with clean data

            ./and_spark.sh numStat

Step 9 : Copy and consolidate stats file

            ./and_spark.sh crStatsFile

Step 10 : Run Spark job to detect outliers

Set output.outliers = true and rem.outliers = true

            ./and_spark.sh olPred

Configuration

Configuration is in and.conf & in and1.conf.




