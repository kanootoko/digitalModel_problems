#!/bin/bash
echo 'aval_packages = available.packages(repos="http://cran.us.r-project.org")
getPackages <- function(install_packages) {
    packages = c()
    for (install_package in c(install_packages)) {
        packages = c(packages, install_package)
        dependencies <- unlist(tools::package_dependencies(install_package, aval_packages, which=c("Depends", "Imports", "LinkingTo")))
        for (package in dependencies) {
            packages = union(getPackages(package), packages)
        }
    }
    packages
}
packages <- getPackages(c("tidyverse", "readr"))
write.table(packages, file="/libs/packages.csv", row.names=F, col.names=F)
download.packages(packages, destdir="/libs", repos="http://cran.us.r-project.org")' | R --no-save

cd libs && mkdir -p build && cd build

for package in $(cat ../packages.csv | awk '{print substr($1, 2, length($1) - 2)}'); do
    if [[ "$(ls ../ | grep -cE  ^$package.\*)" -eq 0 ]]; then
        continue
    fi
    R CMD INSTALL --build ../$package*
    if [[ $? -ne 0 ]]; then
        echo '-------------------------------------  !!! Package compilation failed !!!  -------------------------------------'
    fi
done

if [[ $(ls .. | grep -c '.tar.gz') -ne $(ls . | grep -c '.tar.gz') ]]; then
    echo "Some packages compilation failed (built $(ls . | grep -c '.tar.gz') of $(ls .. | grep -c '.tar.gz'))"
else
    echo "Everything was compiled ($(ls .. | grep -c '.tar.gz') packages)"
fi

