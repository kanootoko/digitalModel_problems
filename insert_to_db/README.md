# insert_to_db

## Description

This script creates table for problems data exported from [Our Saint-Petersburg](https://gorod.gov.spb.ru/)
  service and inserts data from csv file given.  
It works with Postgres database and supports configuration. It is also avaliable to generate sql script to execute
  it without needing python on database machine/container

## Launching

1. Install Postgres database and Postgis extension
2. Create database "problems"
3. Clone this repository
4. Make sure you have python3 modules: _psycopg2_, _pandas_
5. Launch script with `python insert_to_db.py` (python3 is used)

## Parameters

### Input data configuration

* `-c,--csv_path <str>` - full path to csv file with problems [default: _problems_export_2020-05-27.csv_]

### Database configuration

* `-H,--host <str>` - postgres host address [default: _localhost_]
* `-p,--port <int>` - postgres port number [default: _5432_]
* `-d,--database <str>` - postgres database name [default: _problems_]
* `-u,--user <str>` - postgres user name [default: _postgres_]
* `-P,--password <str>` - postgres user password [default: _postgres_]

### Generation of sql-script

* `-g,--generate <str>` - name of sql script to generate without inserting anything in the base
* `-m,--min_outer_id <int>` - minimal OuterID to insert, only used with -g. If this parameter is given, no
  connection to database is performed to get last outerID inserted
* `-s,--insert_size <int>` - number of values per one insert, only used with -g

### Logging

* `-v,--verbose` - output progress (some intermediate steps and insert process - print after each 50000 entities)

## Notes

This script can be rerun as it is when csv is updated. The latest outerID is read from database (or set explicitly), so only new data will be inserted.
