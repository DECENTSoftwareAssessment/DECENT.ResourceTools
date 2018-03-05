# DECENT Resource Tools Quickstart Guide

## Setup Instructions

### Check out the project and its dependencies:
1. ```DECENT.ResourceTools```
2. ```emf.resource.tools```
3. ```famix.m3x```
4. ```famix.m3x.tests```
5. ```famix.m3x.ui```
6. ```dag.m1x```
7. ```dag.m1x.tests```
8. ```dag.m1x.ui```

### Prepare the meta-models
Check out the meta-models (```DECENT.Meta```) project and generate model code for all meta-models (except ```DECENTv2```)

### Generate Xtext related resources
Run the MWE2 workflows for ```famix.m3x``` and ```dag.m1x``` (xtext needs to be installed)

## Usage Instructions

Run ```app.App.java``` with a configuration file as a parameter. See ```properties/sample.properties``` for an example. Most important configuration settings are the ```dataLocation``` (absolute path for the parent location where the project related resources are to be stored, shall be created in advance) and ```project``` (project name - a sub location within the data location, shall be created in advance as well) entries. The ```mgConfiguration```, ```bzConfiguration```, and ```dudeConfiguration``` settings indicate the location of the corresponding ```Configuration``` model instances containing above all database configurations for the corresponding model instances (```MG```, ```BZ```, ```DUDE```). Such instances can be created either by using the ```Configuration``` model API, or simply by adapting the following template:

```
<?xml version="1.0" encoding="ASCII"?>
<Configuration:Setting xmlns:Configuration="http://Configuration/1.0" name="dbConf">
  <settings name="dbName" value=“DBNAME”/>
  <settings name="dbUser" value=“DBUSER”/>
  <settings name="dbPass" value=“DBPASSWORD”/>
  <settings name="dbPort" value=“DBPORT”/>
  <settings name="dbServer" value=“DBSEVER”/>
</Configuration:Setting>
```
Optionally, provide appropriate memory settings, e.g. ```-Xmx4096m -Xss256m -XX:MaxPermSize=256m```. 

Optionally, the steps can be provided directly thus overriding the steps defined in the configuration file. Some of the facilities require the presence of a configuration in the ```dataLocation/project/configuration``` folder (or a different folder indicated in the configuration file).

The translation steps create XMI-based model instance serialisations by default and take the default underlying representation as input (text files, MySQL databases, etc.).  For ```FAMIX```, there is a preprocessing step which reduces the FAMIX facts assets (MSE files) to the interesting parts (as indicated in the ```filteredElements``` configuration setting) and discards all other information. The filtering can be disabled by means of the ```filterFamix``` configuration setting.

## Extension Instructions

Implement support for additional resource types as necessary for the different resource tools. Generic support facilities shall be implemented upstream in the ```emf.resource.tools``` project. See ```app.App.java``` for some examples how to convert resources. 
