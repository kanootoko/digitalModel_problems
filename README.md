# Problems microservice

## Description

This is SPb problems microservice of Digital City Model project. It is used to feed frontend of the project with data of
  problems posted by citizens on [Our Saint-Petersburg](https://gorod.gov.spb.ru/) platform.

## Usage (without docker)

1. Download data from the portal
2. Prepare database as [`insert_to_db` README](insert_to_db/README.md) says
3. Launch backend as [`problemapi` README](problemapi/README.md) says
4. Backend will be avaliable on port 80 at localhost by default, you can change it by CLI arguments or environment variables.
5. Configure and launch frontend.

## Usage (with docker)

API server can be easily packed in Docker container, the process is described in its README in section "Packing in Docker".  
If you need to, you can also use database with docker, the only thing you need to is to run postgres database and set its
  address and other parameters to arguments for insertion script (or feed it with sql script generated with it, see the README)
  and API service.
