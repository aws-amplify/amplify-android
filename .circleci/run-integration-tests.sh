#!/bin/bash

# List all available gradle tasks, grep for the integration test tasks, and then use cut to strip the task description
# and just return the name of the task, one for each module (e.g. aws-api:connectedDebugAndroidTest
tasks=($(./gradlew tasks --all | grep connectedDebugAndroidTest | cut -d " " -f 1))

# Run the integration tests for each module.  If it fails, retry.sh
return_code=0
tag="RUN_INTEGRATION_TESTS"
for task in "${tasks[@]}"; do
  echo "$tag: $task"
  .circleci/retry.sh 3 ./gradlew $task --no-daemon
  if [[ $? == 1 ]]; then
    return_code=1
    echo "$task failed."
  else
    echo "$task succeeded."
  fi
done
exit $return_code

