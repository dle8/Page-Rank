# [Page Rank](https://github.com/dle8/Page-Rank)

Google PageRank algorithm implemented with Apache Hadoop framework and MapReduce programming model.

## 📚 Table of contents

- [Structure](#structure)
- [Installation](#installation)
- [Run](#run)
- [Author](#author)

## 🚀 Structure

- PageRank.java
    - Driver class which calls other classes for computing the PageRank.
- GraphLink.java
    - GraphLink generates the title and its list of outgoing links
- WikiCount.java
    - Counts the number of lines/pages
- PageRankInitializer.java
    - Initialize page rank score with 1/N
- PageRankAlgo.java
    - PageRank computation using weightage/contribution.
    - Map output 	<Title>, <!> 	for titles whose pagerank needs to be computed
- Sorter.java
    - Sorts the titles by PageRank.

## ⬇ Installation

- Download and setup[Java 8 Package](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html) 
- Download and setup [Apache Hadoop](https://hadoop.apache.org/releases.html)
- Add Hadoop and Java paths in the bash file or zsh file: (to know what shell you are using, run `echo $0`):
    - In order to know your Java home, run this command: 
        - `dirname $(dirname $(readlink -f $(which javac)))` (Linux)
        - `$(dirname $(readlink $(which javac)))/java_home` (MacOS)
    - Example for setting HDFS and Java path:
        - <p align="right"><img width=20% src="https://github.com/dle8/Page-Rank/blob/master/images/sample_path.png"></p>
    - Apply changes to the current terminal with:
        - `source .bash_rc` if using bash
        - `source .zhrc` if using zsh
    - Verify Java and Hadoop were properly installed with `java -version` and `hadoop version`

## ⬇ Run

This project is deployed on a t2.micro AWS EC2 instance running Ubuntu 18.04
- Create directories in hadoop
    - `hadoop fs -mkdir /user/page-rank /user/page-rank/input` 

- Copy input files to hadoop
    - `hadoop fs -copyFromLocal /home/page-rank/input/* /user/page-rank/input`

- Create directory build
    - `mkdir -p build`

- Compile all java files using *.java
    - `javac -cp /usr/lib/hadoop/*:/usr/lib/hadoop-mapreduce/* *.java -d build -Xlint`

- Create jar pagerank.jar 
    - `jar -cvf pagerank.jar -C build/ .`

- Run the PageRank class and pass the input and output paths
    - `hadoop jar pagerank.jar org.myorg.PageRank /user/page-rank/input /user/page-rank/o1`

- Copy the output to local directory by using the following command 
    - `hadoop fs -copyToLocal /user/page-rank/o1/* /home/page-rank/output`

Running SimpleWiki on cluster

- Used the following scp command to copy files to cluster
    - `scp PageRank.java dle8@cycle1.csug.rochester.edu:~/Desktop/page-rank`

- Connecting to the cluster
    - `ssh -X dle8@u.rochester.edu`

- Go to the directory of /projects/cloud/pagerank to copy the file to hadoop
    - `hadoop fs ­put simplewiki* ~/i1`

- Go to the directory where you have kept the java files. In my case it was 'page-rank'.
    - `cd ..`

- Create directory build
    - `mkdir build`

- Compile all java files using *.java
    - `javac -cp /usr/lib/hadoop/*:/usr/lib/hadoop-mapreduce/* *.java -d build -Xlint`

- Create jar pagerank.jar 
    - `jar -cvf pagerank.jar -C build/ .`

- Run the PageRank class and pass the input and output paths
    - `hadoop jar pagerank.jar org.myorg.PageRank /user/dle8/i1 /user/dle8/o1`

- Merging the output files generated
    - `hadoop fs -getmerge /user/dle8/o1/part­r* outputfile`

- Copy the output from hadoop to your cluster
    - `hadoop fs -copyToLocal /user/dle8/o1/* /users/dle8/page-rank/output`

- Copy the outputfile to your local system
    - `scp dle8@u.rochester.edu:~/page-rank/outputfile .`

## 👨‍💻 Author

- [Dung Tuan Le](https://dungtuanle.me) <br/>
University of Rochester '21.
