OMTools
================
A software package for optical mapping data processing, analysis and visualization

Please refer to "OMToolsManual.pdf" for more details. 

Quick steps
------------
1.	Compile the OMTools package in the OMTools folder:
javac -d bin -sourcepath src -cp "lib/*" @classes
2.	Build a runnable jar file for OMTools:
jar cvfm OMTools.jar manifest -C bin .
3.	Run OMTools:
java -jar OMTools.jar

Basic steps 
------------
You may refer to the [wiki](https://github.com/TF-Chan-Lab/OMTools/wiki) page to check how to use OMTools, particularly for the latest DLE-1 data.

Issues 
------------
In case other users have the same problems in using OMTools, please report the potential issues on the [Issues page](https://github.com/TF-Chan-Lab/OMTools/issues). We will answer you ASAP.

Publications
-------------
1. Leung, Alden King-Yung, et al. "OMBlast: alignment tool for optical mapping using a seed-and-extend approach." Bioinformatics (2017).

2. Leung, Alden King-Yung, et al. "OMTools: a software package for visualizing and processing optical mapping data." Bioinformatics (2017).
