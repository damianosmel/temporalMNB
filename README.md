# TemporalMNB

This is the code for the development of the research work published as "Learning under Feature Drifts in Textual Streams". The method enchances MNB classifiers for feature-evolving streams. It consists of two components. The sketch to adaptively select important features. And the ensemble to predict feature value aggregating predictions of experts each modeling a distinct temporal trend. This work will be presented in CIKM 2018 Torino Italy.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

1) Download a data set and preprocess
2) Pass the data set to a MySQL database

### Installing

1) You will need MOA API (https://www.cs.waikato.ac.nz/~abifet/MOA/API/index.html)
2) Use your favourite IDE and follow the existing pom
3) Understand the options of the method checking the code/ensemble/commandLineOptions.txt
4) Build your ensembleWA.jar
5) Then run 

```
java -classpath /foo/bar/ensembleWA.jar de.l3s.oscar.Main --verbose true --run_mode EvaluateOffline --collection_location /path2/commandLineOptions.txt --saved_db_title tweets140 --short_text true --learning_algorithm mnb --evaluation_scheme prequential --root_output_directory /path2/output
```
## Built With

* [IntelliJ](https://www.jetbrains.com/idea/) - Java IDEA
* [Maven](https://maven.apache.org/) - Dependency Management
* [MOA] (https://moa.cms.waikato.ac.nz/) - Massive Online Analysis
* [java-timeseries] (https://github.com/signaflo/java-timeseries) - Time Series Analysis in Java

## Authors

* **Damianos P. Melidis** - *Idea and Implementation* - [damianosmel](https://github.com/damianosmel)

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE.txt](LICENSE.txt) file for details

## Acknowledgments

* Jan-Hendrik Zab, Emmanouil Gkatzourias and Maximilian Idahl for active discussion on bugs and features
* Inspiration and help by the work of Dr. Luis Moreira Matias
* Great help by Jacob Rachiele for this TimeSeries Java library
* Funding by DFG OSCAR and ERC ALEXANDRIA projects
