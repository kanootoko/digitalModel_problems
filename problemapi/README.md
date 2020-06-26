# Problems API server

## Description

This is API server for getting probelms from postgres database based on data from [Our Saint-Petersburg](https://gorod.gov.spb.ru/).

## Launching

1. install Postgres database and postgis extension
2. fill the database by data with the script
3. clone this repository
4. compile with `mvn compile assembly:single`
5. launch with `java -jar target/problemapi-2020-06-19-jar-with-dependencies.jar`

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
2. build image with `docker build --tag problems_api:2020-06-19 .`
3. run image with postgres server running on host machine on default port 5432
    1. For windows: `docker run --publish 8080:8080 -e PROBLEMS_API_PORT=8080 -e PROBLEMS_DB_ADDR=host.docker.internal --name problems_api problems_api:2020-06-19`
    2. For Linux: `docker run --publish 8080:8080 -e PROBLEMS_API_PORT=8080 -e PROBLEMS_DB_ADDR=$(ip -4 -o addr show docker0 | awk '{print $4}' | cut -d "/" -f 1) --name problems_api problems_api:2020-06-19`  
       Ensure that:
        1. _/etc/postgresql/12/main/postgresql.conf_ contains uncommented setting `listen_addresses = '*'` so app could access postgres from Docker network
        2. _/etc/postgresql/12/main/pg_hba.conf_ contains `host all all 0.0.0.0/0 md5` so login could be performed from anywhere (you can set docker container address instead of 0.0.0.0)
        3. command `ip -4 -o addr show docker0 | awk '{print $4}' | cut -d "/" -f 1` returns ip address
       If config files are not found, `sudo -u postgres psql -c 'SHOW config_file'` should say where they are

    Ensure that you configured user and password for database.

At this point you can only know that database connection failed (and the reason why) when connection is performed, for example after GET on _/problems/search?limit=10_

## Usage

After the launch you can find api avaliable at localhost:port/ . In example given it will be localhost with port 80 when launched without Docker and localhost:8080 with Docker

### Endpoints

At this moment there are 4 endpoints:

* **/api**: returns HAL description of API provided
* **/api/problems/search** : takes parameters by query or inside the body as JSON. You can set _minDate_ and/or _maxDate_
  in format `YYYY-MM-DD`, _status_, _firstCoord_ and _secondCoord_ in format `latitude,longitude`, _category_, _subcategory_, _limit_.  
  Returns list of problems descriptions, coordinates and statuses corresponding to request, limited by 100000 by default.
  Also each of the entities has their link to get full information.
* **/api/problems/:problemID** : returns single problem information. It should not be used by id as it is, link should come from
  /problems/search result list.
* **/api/groups/:labelName** : returns list of unique values in given label and number of problems having those values.
  It is used to get statuses, categories and subcategories.

Also with no parameters URIs {/, /api, /api/groups and /api/problems/search} will return HTML with a bit of description

### Output format

#### /api

```json
{
    "_links": {
        "problems-search": {
            "templated": true,
            "href": "/api/problems/search{?minDate,maxDate,firstCoord,secondCoord,category,subcategory,status,limit}"
        },
        "self": "/api",
        "statuses": {
            "href": "/api/groups/status"
        },
        "categories": {
            "href": "/api/groups/category"
        },
        "subcategories": {
            "href": "/api/groups/subcategory"
        }
    },
    "version": ":version"
}
```

Format:

* :version - string representing date in format "YYYY-MM-DD"

#### /api/problems/search

```json
{
    "size": ":problems_number",
    "_links": {
        "self": {
            "href": "/api/problems/search?<queryString>"
        }
    },
    "_embedded": {
        "problems": [
            {
                "_links": {
                    "self": {
                        "href": "/api/problems/:problemID"
                    }
                },
                "coordinates": [":latitude",":longitude"],
                "description": ":description",
                "status": ":status"
            }, <...>
        ]
    }
}
```

Formats:

* :problems_number, :problemID - integer
* :latitude, :longitude - double
* :description, :status - string

#### /api/problems/:problemID

```json
{
    "_links": {
        "self": {
            "href": "/api/problems/:problemID"
        }
    },
    "_embedded": {
        "reason": ":reason",
        "updateDate": ":updateDate",
        "address": ":address",
        "_links": {
            "self": {
                "href": "/api/problems/:problemID"
            }
        },
        "coordinates": [":latitude",":longitude"],
        "municipality": ":municipality",
        "description": ":description",
        "creationDate": ":creationDate",
        "userName": ":userName",
        "userID": ":userID",
        "name": ":name",
        "outerID": ":outerID",
        "id": ":id",
        "region": ":region",
        "category": ":category",
        "subcategory": ":subcategory",
        "status": ":status"
    }
}
```

Formats:

* :reason, :municipality, :description, :userName, :name, :region, :category, :subcategory, :status - string
* :address - string, can be empty
* :creationDate, :updateDate - string representing date in format "YYYY-MM-DD"
* :problemID, :outerID - integer
* :latitude, :longitude - double

#### /api/groups/:labelName

:labelName should be one of the ("status", "category", "subcategory"), but technicaly "region", "municipality" and
  "address" have meaning too. Getting groups of other database labels is possible, but not recommended

```json
{
    "size": 8,
    "_links": {
        "self": {
            "href": "/api/groups/status"
        }
    },
    "_embedded": {
        "groups": [
            {
                "size": ":groupSize",
                "name": ":name"
            }, <...>
        ]
    }
}
```

Formats:

* :size, :groupSize - integer
* :name - string
