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
