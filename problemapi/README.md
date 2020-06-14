# Problems API server

## Description

This is API server for getting probelms from database based on [Our Saint-Petersburg](https://gorod.gov.spb.ru/) database.

## Launching

1. Install Postgres database and postgis extension
2. Fill the database by data with the script
3. Clone this repository
4. compile with `mvn compile assembly:single`
5. launch with `java -jar target/problemapi-1.0-SNAPSHOT-jar-with-dependencies.jar`

## Parameters

* `-d,--db_string <str>` - string for jdbc to connect to database \[default:
_jdbc:postgresql://127.0.0.1:5432/problems_\]
* `-p,--port <int>` - port to run the server \[default: _80_\]
* `-P,--db_pass <str>` - user password for database \[default: _postgres_\]
* `-u,--db_user <str>` - user name for database \[default: _postgres_\]

## Usage

After the launch you can find api avaliable at localhost:port/ .

### Endpoints

At this moment there are 3 endpoints:

* **/problems/search** : takes parameters by query or inside the body as JSON. You can set _minDate_ and/or _maxDate_
  in format `YYYY-MM-DD`, _status_, _firstCoord_ and _secondCoord_ in format `longitude,latitude`, _category_, _subcategory_, _limit_.  
  Returns list of problems descriptions, coordinates and statuses corresponding to request, limited by 100000 by default.
  Also each of the entities has their link to get full information.
* **/problem/:problemID** : returns single problem information.
* **/groups/:labelName** : returns list of unique values in given label and number of problems having those values.
  It is used to get statuses, categories and subcategories.
