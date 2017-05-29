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

Publications
-------------
1. Leung, Alden King-Yung, et al. "OMBlast: alignment tool for optical mapping using a seed-and-extend approach." Bioinformatics (2017).

2. Leung, Alden King-Yung, et al. "OMTools: a software package for visualizing and processing optical mapping data." Bioinformatics (2017).
