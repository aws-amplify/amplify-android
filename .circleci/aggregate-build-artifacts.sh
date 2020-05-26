#!/bin/bash

set -e 

modules_with_build_trees() {
   for item in *; do
       if [ -d "$item/build" ]; then
           echo "$item"
       fi
   done
}
readonly modules=($(modules_with_build_trees))
readonly current_dir="$(pwd)"
readonly aggregation_dir="build-artifacts"

if [ ! -d "$aggregation_dir" ]; then
    mkdir "$aggregation_dir"
fi
cd "$aggregation_dir"

for item in ${modules[@]}; do
    [ ! -d "$item" ] && mkdir "$item"
    echo "Aggregating build artifacts for CircleCI [$item]..."
    cp -a "$current_dir/$item/build" "$item"
done

cd "$current_dir"

tar czf build-artifacts.tar.gz "$aggregation_dir"
echo "Aggregating build artifacts completed: see build-artifacts.tar.gz."

