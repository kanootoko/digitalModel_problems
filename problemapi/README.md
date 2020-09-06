# Problems API server

## Description

This is API server for getting probelms from postgres database based on data from [Our Saint-Petersburg](https://gorod.gov.spb.ru/).

## Launching on host machine

1. install Postgres database and postgis extension
2. clone this repository
3. fill the database by data with the [script](../insert_to_db/insert_to_db.py)
4. open terminal in cloned repository
5. compile with `mvn compile assembly:single`
6. install R and libraries (`tidyverse`, `readr`) (look at **R installation** section in case of problems)
7. copy or symlink R scripts to the directory of launching (`cp evaluation-model/*.R .`)
8. launch with `java -jar target/problemapi-2020-09-06-jar-with-dependencies.jar`

## R installation

You need to have R (r-base package) and "tidyverse" package installed to use /api/evaluation endpoint. In case
  of using without docker you can just have it with `R -e 'install.package(c("tidyverse", "readr"))'` command, ensuring that
  you have all of the dependencies (see _dockerfile_ and _download-build-lbs.sh_ in __evaluation-model__ directory).  
In case of using Docker you will need to run container which will build all of the needed libraries and then pass
  them to the main container at its build time.

1. open terminal in cloned repository
2. launch `cd evaluation-model && mkdir libs` to change the directory and create libs folder
3. launch `docker build --tag build_libs . && docker run --name build_libs -v "$PWD/libs":/libs build_libs`
4. launch `docker rm build_libs && docker rmi build_libs` to remove unnedeed container and image

After that ./evaluation-model/libs/build directory will contain suitable compiled libraries. You can delete folder after the
  building of the main container

## Configuration by launch.properties file

You can configure application parameters by creating and editing file named __launch.properties__:

* `api_port=<int>` - port to run the api server \[default: _80_\]
* `db_addr=<str>` - address of the postgres with problems \[_localhost_\]
* `db_port=<int>` - port of the postgres with problems \[_5432_\]
* `db_name=<str>` - name of the postgres database with problems \[_problems_\]
* `db_user=<str>` - user name for database \[default: _postgres_\]
* `db_pass=<str>` - user password for database \[default: _postgres_\]
* `skip_evaluation=true` - skip calculation of regions and municipalities evaluation

## Configuration by environment variables

The same parameters can be configured with environment variables (overrides launch.properties configuration):

* PROBLEMS_API_PORT - api_port
* PROBLEMS_DB_ADDR - db_addr
* PROBLEMS_DB_PORT - db_port
* PROBLEMS_DB_NAME - db_name
* PROBLEMS_DB_USER - db_user
* PROBLEMS_DB_PASS - db_pass
* PROBLEMS_SKIP_EVALUATION - skip_evaluation

## Configuration by CLI Parameters

Command line arguments configuration is also avaliable (overrides environment variables configuration)

* `-p,--port <int>` - api_port
* `-H,--db_addr <str>` - db_addr
* `-P,--db_port <int>` - db_port
* `-N,--db_name <str>` - db_name
* `-U,--db_user <str>` - db_user
* `-W,--db_pass <str>` - db_pass
* `-S,--skip_evaluation` - skip_evaluation

## Building Docker image (the other way is to use Docker repository: kanootoko/digitalmodel_problems:2020-09-06)

1. open terminal in cloned repository
2. build image with `docker build --tag kanootoko/digitalmodel_problems:2020-09-06 .`
3. run image with postgres server running on host machine on default port 5432
    1. For windows: `docker run --publish 8080:8080 -e PROBLEMS_API_PORT=8080 -e PROBLEMS_DB_ADDR=host.docker.internal --name problems_api kanootoko/digitalmodel_problems:2020-09-06`
    2. For Linux: `docker run --publish 8080:8080 -e PROBLEMS_API_PORT=8080 -e PROBLEMS_DB_ADDR=$(ip -4 -o addr show docker0 | awk '{print $4}' | cut -d "/" -f 1) --name problems_api kanootoko/digitalmodel_problems:2020-09-06`  
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

At this moment there are some main endpoints:

* **/api**: returns HAL description of API provided
* **/api/problems/search** : takes parameters by query or inside the body as JSON. You can set _minDate_ and/or _maxDate_
    in format `YYYY-MM-DD`, _status_, _firstCoord_ and _secondCoord_ in format `latitude,longitude`, _category_, _subcategory_,
    _municipality_, _region_, _limit_. At least one parameter must be set.  
  Returns a list of problems descriptions, coordinates and statuses corresponding to request, limited by 100000 by default (limit=0 to turn limiting off).
    Also each of the entities has their link to get full information.
* **/api/problems/:problemID** : returns single problem information. It should not be used by id as it is, link should come from
  /problems/search result list.
* **/api/evaluation/polygon** : takes parameters by query or inside the body as JSON. You should set _firstCoord_ and _secondCoord_ in format
    `latitude,longitude`.  
  Returns 4 evaluation results: safety, physical comfort, esthetic comfort and total evaluation value of given territory based on problems
  * **/api/evaluation/municipalities** : returns polygon evaluation of all of the municipalities
  * **/api/evaluation/regions** : returns polygon evaluation of all of the regions
* **/api/evaluation/objects** : takes type of object to evaluate (building. yard, maf, water, greenzone, uds) and coordinates bounds, returns list
  of evaluated objects with 4 evaluation results
* **/api/groups/:labelName** : returns list of unique values in given label and number of problems having those values.
  It is used to get statuses, categories and subcategories.

Also with no parameters URIs {/, /api/groups, /api/evaluation/polygon, /api/evaluation/objects and /api/problems/search}
  will return HTML with a bit of description

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

#### /api/evaluation/polygon

```json
{
    "_links": {
        "self": {
            "href": "/api/evaluation/polygon?<queryString>"
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

S - safety, C - physical comfort, I - estetic comfort, problems_number - number of problems
  got by coordinates and used in calculations

#### /api/evaluation/municipalities

```json
{
    "_links": {
        "self": {
            "href": "/api/evaluation/municipalities"
        }
    },
    "_embedded": {
        "municipalities": [
            {
                "total": ":total",
                "S": ":S",
                "C": ":C",
                "name": ":municipality_name",
                "I": ":I",
                "problems_number": ":problems_number"
            }, <...>
        ]
    }
}
```

Formats:

* :municipality_name - string
* :problems_number - integer
* :total, :S, :C, :I - floating point number with precision of 4 digits after the point

S - safety, C - physical comfort, I - estetic comfort, problems_number - number of problems
  got by coordinates and used in calculations

#### /api/evaluation/regions

```json
{
    "_links": {
        "self": {
            "href": "/api/evaluation/regions"
        }
    },
    "_embedded": {
        "municipalities": [
            {
                "total": ":total",
                "S": ":S",
                "C": ":C",
                "name": ":region_name",
                "I": ":I",
                "problems_number": ":problems_number"
            }, <...>
        ]
    }
}
```

Formats:

* :region_name - string
* :problems_number - integer
* :total, :S, :C, :I - floating point number with precision of 4 digits after the point

S - safety, C - physical comfort, I - estetic comfort, problems_number - number of problems
  got by coordinates and used in calculations

#### /api/evaluation/objects

```json
{
    "_links": {
        "self": {
            "href": "/api/evaluation/objects?<queryString>"
        }
    },
    "_embedded": {
        "evaluations": [
            {
                "total": ":total",
                "S": ":S",
                "C": ":C",
                "coordinates": [":latitude",":longitude"],
                "I": ":I"
            }, <...>
        ]
    }
}
```

Formats:

* :region_name - string
* :problems_number - integer
* :total, :S, :C, :I, :latitude, :longitude - floating point number with precision of 4 digits after the point

S - safety, C - physical comfort, I - estetic comfort, problems_number - number of problems
  got by coordinates and used in calculations
