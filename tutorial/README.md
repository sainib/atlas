

## Objective

The Objective of the tutorial is to show a data ingestion lineage with Apache Sqoop between Mysql and Apache Hive, in Apache Atlas. The example will show custom lineage reporting from outside hadoop as well as native lineage reporting in hadoop via HiveServer2


For this demo we will use Mysql database containing the trucking tables
* DRIVERS
* TIMESHEET

Both these tables are very simple and are used as part of the trucking demo.

As part of the demo a sqoop ingestion job will be executed along with 3 java helper classes that will:
* Create Entities of type Mysql table in the Atlas
* Create entities of type hive_table
* Create an instance for Process type
* Model the lineage between the Mysql tables and Hive tables

Post that you can run a CTAS operation on the new hive table, using hive CLI and you will be able to see a lineage of the table in the Atlas UI Search.


# Technical Details

Apache Hadoop consists of below projects for Data Inestion 

* Apache Sqoop - 
http://sqoop.apache.org/

Apache Sqoop is hadoop project for ingesting data from Relation Databases into Hadoop. It has been in the Hadoop Ecosystem for a while and well proven in the industry.

* Apache Atlas

http://incubator.apache.org/projects/atlas.html

Apache Atlas is a scalable and extensible set of core foundational governance services that enables enterprises to effectively and efficiently meet their compliance requirements within Hadoop and allows integration with the complete enterprise data ecosystem.

## The Demo project

To demonstrate a simple scenario we are going to setup system with the following.

* Source System.

This is a mysql database. 

* Destination System
 
We need HDP 2.3 Cluster. For our tutorial we are going to use the HDP 2.3 Sandbox

Design

In the Source System we are going to have an online system that is storing Truck Drivers Information System. The Truck Driver Information System, tracks Drivers and information systems.

Following are the tables in the Source System:-

#### DRIVERS

<table>
 <tr>
  <td>Cloumn Name</td>
  <td>Cloumn Type</td>
  <td>Not Null </td>
  <td> Primary Key</td>
 </tr>
 <tr>
  <td>DRIVER_ID</td>
  <td>Varchar(200)</td>
  <td>Y</td>
  <td>Y</td>
 </tr>
  <tr>
  <td>DRIVER_NAME</td>
  <td>Varchar(1000)</td>
  <td>Y</td>
  <td>N</td>
 </tr>
  <tr>
  <td>CERTIFIED</td>
  <td>Varchar(100)</td>
  <td>Y</td>
  <td>N</td>
 </tr>
 <tr>
  <td>WAGE_PLAN</td>
  <td>Varchar(200)</td>
  <td>Y</td>
  <td>N</td>
 </tr>
</table>

##### TimeSheet

<table>
 <tr>
  <td>Cloumn Name</td>
  <td>Cloumn Type</td>
  <td>Not Null </td>
  <td> Primary Key</td>
 </tr>
 <tr>
  <td>DRIVER_ID</td>
  <td>Varchar(200)</td>
  <td>Y</td>
  <td>Y</td>
 </tr>
  <tr>
  <td>DRIVER_WEEK</td>
  <td>int(11)</td>
  <td>Y</td>
  <td>N</td>
 </tr>
  <tr>
  <td>HOURS_LOGGED</td>
  <td>bigint(100)</td>
  <td>Y</td>
  <td>N</td>
 </tr>
 <tr>
  <td>TIME_LOGGED</td>
  <td>bigint(200)</td>
  <td>Y</td>
  <td>N</td>
 </tr>
</table>


## Create Source System

#### Install Mysql

* Create a CentOS 6.5 Virtula Machine
* Install mysql server
* wget http://dev.mysql.com/get/mysql-community-release-el6-5.noarch.rpm/from/http://repo.mysql.com/
* sudo yum localinstall mysql-community-release-el6-*.noarch.rpm
* sudo yum install mysql-community-server
* sudo service mysqld start
* sudo chkconfig mysqld on
* chkconfig --list mysqld

[Refer to the MySQL Install docs](https://dev.mysql.com/doc/refman/5.6/en/linux-installation-yum-repo.html)

Next login as 

    mysql -u root -h <hostname>

#### Create Table and Load the Data
Execute the script in the github repo 
    MySQLSourceSystem.ddl

This will create all the needed tables.

Load the Drivers Data. Copy the drivers.csv file over from the repo

    LOAD DATA LOCAL INFILE '<dir>/drivers.csv' into table DRIVERS FIELDS TERMINATED BY "," LINES TERMINATEd BY '\n' (DRIVER_ID, DRIVER_NAME,CERTIFIED, WAGE_PLAN);
    

Load the Timesheet Data. Copy the timesheet.csv file over from the repo


     LOAD DATA LOCAL INFILE '<dir>/timesheet.csv' into table DRIVERS FIELDS TERMINATED BY "," LINES TERMINATEd BY '\n' (DRIVER_ID, DRIVER_NAME,HOURS_LOGGED, MILES_LOGGED);

###### Now you should have the Source System Ready! 

######## [Next Follow the Steps](Step1.md)

















