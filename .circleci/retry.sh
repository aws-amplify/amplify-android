#!/bin/bash
# Usage: retry.sh <max_tries> <timeout_seconds> <command to run>
sudo apt-get update
sudo apt-get install expect

readonly max_tries=$1
readonly timeout=$2
readonly command="${@: 3}"
attempts=0
return_code=1
while [[ $attempts -lt $max_tries ]]; do
  ((attempts++))
  if [[ attempts -gt 1 ]]; then sleep 10; fi
  echo "RETRY: $command : Attempt $attempts of $max_tries."
  expect -c "set timeout $timeout; spawn $command; expect timeout { exit 1 } eof { exit 0 }" && break
done

return_code=$?

if [[ $return_code == 0 ]]; then
  echo "RETRY: $command : Attempt $attempts succeeded."
else
  echo "RETRY: $command : All $attempts attempts failed."
fi

exit $return_code
