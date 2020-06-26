#!/bin/bash
# Usage: retry.sh <max_tries> <command to run>

tag="RETRY"
readonly max_tries=$1
readonly command="${@: 2}"
attempts=0
return_code=1

while [[ $attempts -lt $max_tries ]]; do
  ((attempts++))
  if [[ attempts -gt 1 ]]; then sleep 10; fi
  echo "$tag: $command : Attempt $attempts of $max_tries."
  $command && break
done

return_code=$?

if [[ $return_code == 0 ]]; then
  echo "$tag: $command : Attempt $attempts succeeded."
else
  echo "$tag: $command : All $attempts attempts failed."
fi

exit $return_code
