#!/bin/bash

# List all available gradle tasks, grep for the integration test tasks, and then use cut to strip the task description
# and just return the name of the task, one for each module (e.g. aws-api:connectedDebugAndroidTest
tasks=($(./gradlew tasks --all | grep connectedDebugAndroidTest | cut -d " " -f 1))

# Run the integration tests for each module.  If it fails, retry.sh
return_code=0
summary=""
tag="RUN_INTEGRATION_TESTS"
for task in "${tasks[@]}"; do
  echo "$tag: $task"
  .circleci/retry.sh 3 900 ./gradlew $task --no-daemon
  if [[ $? == 1 ]]; then
    return_code=1
    summary+="$task failed.\n"
  else
    summary+="$task succeeded.\n"
  fi
done
echo "$tag SUMMARY:\n"
printf $summary
exit $return_code

