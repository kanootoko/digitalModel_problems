# Problems microservice

## Description

This is the repository of microservice to feed frontend of the SPb digital model with data of problems writen by citizens on [Our Saint-Petersburg](https://gorod.gov.spb.ru/) platform.

## Usage (without docker)

1. Download data from the portal
2. Prepare database as `insert_to_db` README says
3. Launch backend as `problemapi` README says
4. Backend will be avaliable on port 80 by default, you can change it by CLI arguments. Configure and launch frontend

## Usage (with docker)

1. Download data from portal
2. Clone this repository
3. Change _docker-compose.yml_ in the place of path to csv file with data (problems-db-create launch command)
4. launch `docker-compose up --build problems-db-create` to build container with database and fill the data in
5. launch `docker-compose up --build problems-api` to build API server container
6. Backend will be avaliable on port 8081 by default, you can change it in docker-compose.yml. Configure and launch frontend
