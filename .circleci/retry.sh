#!/bin/bash

readonly max_tries=$1
readonly command="${@: 2}"
attempts=0
return_code=1
while [[ $attempts -lt $max_tries ]]; do
  ((attempts++))
  if [[ attempts -gt 1 ]]; then sleep 10; fi
  echo "RETRY: Attempt $attempts of $max_tries."
  $command && break
done

return_code=$?

if [[ $return_code == 0 ]]; then
  echo "RETRY: Attempt $attempts succeeded."
else
  echo "RETRY: All $attempts attempts failed."
fi

exit $return_code
