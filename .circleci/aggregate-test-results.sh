#!/bin/bash

set -e

readonly circle_ci_storage_dir='test-results'
readonly cur_dir=$(pwd)
readonly junit_xmls_path='build/test-results/testDebugUnitTest'

[ -z "$1" ] && echo "Usage: $0 <workdir_path>" >&2 && exit 1

module_test_results() {
    for item in *; do
        if [ -d "$item/$junit_xmls_path" ]; then
            echo $item
        fi
    done
}
readonly test_results_dirs=($(module_test_results))

mkdir -p $circle_ci_storage_dir && cd $circle_ci_storage_dir

for item in ${test_results_dirs[@]}; do
    echo "Aggregating test results for CircleCI [$item]..."
    find "$cur_dir/$item/$junit_xmls_path" -name "*.xml" \
        -exec cp {} . \;
done

cd $cur_dir

