# Problems API server

## Description

This is API server for getting probelms from database based on [Our Saint-Petersburg](https://gorod.gov.spb.ru/) database.

## Launching

1. Install Postgres database and postgis extension
2. Fill the database by data with the script
3. Clone this repository
4. compile with `mvn compile assembly:single`
5. launch with `java -jar target/problemapi-2020-06-17-jar-with-dependencies.jar`

## CLI Parameters

* `-p,--port <int>` - port to run the api server \[default: _80_\]
* `-H,--db_addr <str>` - address of the postgres with problems \[_localhost_\]
* `-P,--db_port <str>` - port of the postgres with problems \[_5432_\]
* `-N,--db_name <str>` - name of the postgres database with problems \[_problems_\]
* `-U,--db_user <str>` - user name for database \[default: _postgres_\]
* `-W,--db_pass <str>` - user password for database \[default: _postgres_\]

## Environment parameters

The same parameters can be configured with environment variables:

* PROBLEMS_API_PORT - -p
* PROBLEMS_DB_ADDR - -H
* PROBLEMS_DB_PORT - -P
* PROBLEMS_DB_NAME - -N
* PROBLEMS_DB_USER - -U
* PROBLEMS_DB_PASS - -W

## Packing in Docker

1. open terminal in cloned repository
2. build image with `docker build --tag problems_api:2020-06-17 .`
3. run image with `docker run --net=host -e PROBLEMS_API_PORT=8080 --name problems_api problems_api:2020-06-17`

Last command runs the container within the host network-space, so you can easily configure database settings
if it runs in localhost by passing enviroment variables. If you manage to work with database with other Docker network,
you need to replace third command with something like `docker run --publish 8080:80 -e PROBLEMS_DB_ADDR=... -e PROBLEMS_DB_PORT=... --name problems_api problems_api:2020-06-17`. In both variants service will be avaliable at port 8080 on localhost.
At this point you can only know that database connection failed (and the reason why) when you do request, for example _/problems/search?limit=10_

## Usage

After the launch you can find api avaliable at localhost:port/ .

### Endpoints

At this moment there are 3 endpoints:

* **/problems/search** : takes parameters by query or inside the body as JSON. You can set _minDate_ and/or _maxDate_
  in format `YYYY-MM-DD`, _status_, _firstCoord_ and _secondCoord_ in format `longitude,latitude`, _category_, _subcategory_, _limit_.  
  Returns list of problems descriptions, coordinates and statuses corresponding to request, limited by 100000 by default.
  Also each of the entities has their link to get full information.
* **/problems/:problemID** : returns single problem information.
* **/groups/:labelName** : returns list of unique values in given label and number of problems having those values.
  It is used to get statuses, categories and subcategories.

Also with no parameters given URIs (/, /groups, /problems/search will return HTML with a bit of description)
