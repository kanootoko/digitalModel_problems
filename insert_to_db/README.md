# insert_to_db

## Description

This script inserts creates database for problems from [Our Saint-Petersburg](https://gorod.gov.spb.ru/) and inserts data from csv file given.

## Launching

1. Install Postgres database and Postgis extension
2. Create database "problems"
3. Clone this repository
4. Make sure you have python3 modules: _psycopg2_, _pandas_
5. Launch script with `python insert_to_db.py` (python3 is used)

## Parameters

* `-H,--host <str>` - postgres host address [default: _localhost_]
* `-p,--port <int>` - postgres port number [default: _5432_]
* `-d,--database <str>` - postgres database name [default: _problems_]
* `-u,--user <str>` - postgres user name [default: _postgres_]
* `-P,--password <str>` - postgres user password [default: _postgres_]
* `-c,--csv_path <str>` - full path to csv file with problems [default: _problems_export_2020-05-27.csv_]

## Notes

This script can be rerun as it is when csv is updated. The latest outerID is read from database, so only new data will be inserted.
