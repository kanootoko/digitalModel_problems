# Problems API server

## Description

This is API server for getting probelms from postgres database based on data from [Our Saint-Petersburg](https://gorod.gov.spb.ru/).

## Launching on host machine

1. install Postgres database and postgis extension
2. clone this repository
3. fill the database by data with the [script](../insert_to_db/insert_to_db.py)
4. open terminal in cloned repository
5. compile with `mvn compile assembly:single`
6. install R and install `tidyverse` library (look at **R installation** section in case of problems)
7. launch with `java -jar target/problemapi-2020-08-06-jar-with-dependencies.jar`

## R installation (Linux machine instruction)

You need to have R (r-base package) and "tidyverse" package installed to use /api/ranking endpoint. In case
  of using without docker you can just have it with `R -e 'install.package("tidyverse")'` command, ensuring that
  you have all of the dependencies (see _dockerfile_ and _download-build-lbs.sh_ in __ranking-model__ directory).  
In case of using Docker you will need to run container which will build all of the needed libraries and then pass
  them to the main container in build time.

1. open terminal in cloned repository
2. launch `cd ranking-model && mkdir libs` to change the directory and create libs folder
3. launch `docker build --tag build_libs . && docker run --name build_libs -v "$PWD/libs":/libs build_libs`
4. launch `docker rm build_libs && docker rmi build_libs` to remove unnedeed container and image

After that ./ranking-model/libs/build directory will contain suitable compiled libraries. You can delete folder after the
  building of the main container

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
2. build image with `docker build --tag kanootoko/digitalModel_problems:2020-08-06 .`
3. run image with postgres server running on host machine on default port 5432
    1. For windows: `docker run --publish 8080:8080 -e PROBLEMS_API_PORT=8080 -e PROBLEMS_DB_ADDR=host.docker.internal --name problems_api kanootoko/digitalModel_problems:2020-08-06`
    2. For Linux: `docker run --publish 8080:8080 -e PROBLEMS_API_PORT=8080 -e PROBLEMS_DB_ADDR=$(ip -4 -o addr show docker0 | awk '{print $4}' | cut -d "/" -f 1) --name problems_api kanootoko/digitalModel_problems:2020-08-06`  
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
    in format `YYYY-MM-DD`, _status_, _firstCoord_ and _secondCoord_ in format `latitude,longitude`, _category_, _subcategory_,
    _municipality_, _region_, _limit_. At least one parameter must be set.  
  Returns a list of problems descriptions, coordinates and statuses corresponding to request, limited by 100000 by default.
    Also each of the entities has their link to get full information.
* **/api/ranking/** : takes parameters by query or inside the body as JSON. You should set _firstCoord_ and _secondCoord_ in format
    `latitude,longitude`.  
  Returns 4 ranking results: safety, physical comfort, feelings comfort and total rank of given territory based on problems
* **/api/problems/:problemID** : returns single problem information. It should not be used by id as it is, link should come from
  /problems/search result list.
* **/api/groups/:labelName** : returns list of unique values in given label and number of problems having those values.
  It is used to get statuses, categories and subcategories.

Also with no parameters URIs {/, /api/groups, /api/ranking and /api/problems/search} will return HTML with a bit of description

### Output format

#### /api

```json
{
    "_links": {
        "regions": {
            "href": "/api/groups/region"
        },
        "problems-search": {
            "templated": true,
            "href": "/api/problems/search?<queryString>"
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
        },
        "munitipalities": {
            "href": "/api/groups/municipality"
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
                "creationDate": ":creationDate",
                "status": ":status"
            }, <...>
        ]
    }
}
```

Formats:

* :problems_number, :problemID - integer
* :latitude, :longitude - floating point number with precision of 4 digits after the point
* :description, :status - string
* :creationDate - string representing date in format "YYYY-MM-DD"

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
* :latitude, :longitude - floating point number with precision of 4 digits after the point

#### /api/groups/:labelName

:labelName should be one of the ("status", "category", "subcategory", "municipality", "region").
  Getting groups of other database labels is possible, but not recommended

```json
{
    "size": ":groups_number",
    "_links": {
        "self": {
            "href": "/api/groups/:name"
        }
    },
    "_embedded": {
        "groups": [
            {
                "size": ":groupSize",
                "name": ":groupName"
            }, <...>
        ]
    }
}
```

Formats:

* :groups_number, :groupSize - integer
* :name, groupName - string

#### /api/ranking

```json
{
    "_links": {
        "self": {
            "href": "/api/ranking?<queryString>"
        }
    },
    "_embedded": {
        "rank": {
            "total": ":total",
            "S": ":S",
            "C": ":C",
            "I": ":I"
        },
        "problems_number": ":problems_number"
    }
}
```

Formats:

* :problems_number - integer
* :total, :S, :C, :I - floating point number with precision of 4 digits after the point

S - safety, C - physical comfort, C - feelings comfort, problems_number - number of problems
  got by coordinates and used in calculations
