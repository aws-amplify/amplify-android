#!/bin/bash

# Retry on Device Farm API throttling (~60s total backoff)
export AWS_MAX_ATTEMPTS=8

project_arn=$DEVICEFARM_PROJECT_ARN
max_devices=$NUMBER_OF_DEVICES_TO_TEST
test_spec_arn=$DEVICEFARM_TEST_SPEC_ARN
commit_sha=$COMMIT_SHA
module_name=$1
file_name="$module_name-debug-androidTest.apk"

if [[ -z "${project_arn}" ]]; then
  echo "DEVICEFARM_PROJECT_ARN environment variable not set."
  exit 1
fi

if [[ -z "${max_devices}" ]]; then
  echo "NUMBER_OF_DEVICES_TO_TEST not set. Defaulting to 1."
  max_devices=1
fi

# Function to setup the app uploads in device farm.
# Retries with random jitter on throttling failures.
function createUploadWithRetry {
  test_type=$1
  max_upload_attempts=3
  for upload_attempt in $(seq 1 $max_upload_attempts); do
    upload_response=`aws devicefarm create-upload --type $test_type \
                               --content-type="application/octet-stream" \
                               --project-arn="$project_arn" \
                               --name="$file_name" \
                               --query="upload.[url, arn]" \
                               --region="us-west-2" \
                               --output=text 2>&1`
    # Check if we got a valid response (URL + ARN)
    read -a parts <<< "$upload_response"
    if [[ -n "${parts[1]}" && "${parts[1]}" == arn:* ]]; then
      echo "$upload_response"
      return 0
    fi
    if [ $upload_attempt -lt $max_upload_attempts ]; then
      jitter=$((30 + RANDOM % 60))
      echo "[RUN_IN_DEVICEFARM] CreateUpload throttled (attempt $upload_attempt/$max_upload_attempts). Retrying in ${jitter}s..." >&2
      sleep $jitter
    fi
  done
  echo "[RUN_IN_DEVICEFARM] CreateUpload failed after $max_upload_attempts attempts" >&2
  echo ""
  return 1
}

echo 'Uploading test package'
read -a result <<< $(createUploadWithRetry "INSTRUMENTATION_TEST_PACKAGE")
test_package_url=${result[0]}
test_package_upload_arn=${result[1]}
if [[ -z "$test_package_upload_arn" ]]; then
  echo "Failed to create test package upload (see logs above). Exiting."
  exit 1
fi
curl -H "Content-Type:application/octet-stream" -T $file_name $test_package_url

echo 'Uploading app package'
read -a result <<< $(createUploadWithRetry "ANDROID_APP")
app_package_url=${result[0]}
app_package_upload_arn=${result[1]}
if [[ -z "$app_package_upload_arn" ]]; then
  echo "Failed to create app package upload (see logs above). Exiting."
  exit 1
fi
curl -H "Content-Type:application/octet-stream" -T $file_name $app_package_url

# Wait for uploads to complete
for arn in "$test_package_upload_arn" "$app_package_upload_arn"; do
  while true; do
    upload_status=$(aws devicefarm get-upload --arn "$arn" --region="us-west-2" --query="upload.status" --output text)
    if [ "$upload_status" = "SUCCEEDED" ]; then
      break
    elif [ "$upload_status" = "FAILED" ]; then
      echo "Upload failed for $arn"
      exit 1
    fi
    sleep 5
  done
done

# Get oldest device we can test against.
minDevice=$(aws devicefarm list-devices \
                --region="us-west-2" \
                --filters '[
                    {"attribute":"AVAILABILITY","operator":"EQUALS","values":["HIGHLY_AVAILABLE"]},
                    {"attribute":"PLATFORM","operator":"EQUALS","values":["ANDROID"]},
                    {"attribute":"OS_VERSION","operator":"GREATER_THAN_OR_EQUALS","values":["8"]},
                    {"attribute":"OS_VERSION","operator":"LESS_THAN","values":["8.1"]},
                    {"attribute":"MANUFACTURER","operator":"IN","values":["Google", "Pixel", "Samsung"]}
                ]' \
                | jq -r '.devices[0].arn')

# Get middle device we can test against.
middleDevice=$(aws devicefarm list-devices \
                --region="us-west-2" \
                --filters '[
                    {"attribute":"AVAILABILITY","operator":"EQUALS","values":["HIGHLY_AVAILABLE"]},
                    {"attribute":"PLATFORM","operator":"EQUALS","values":["ANDROID"]},
                    {"attribute":"OS_VERSION","operator":"GREATER_THAN_OR_EQUALS","values":["10"]},
                    {"attribute":"OS_VERSION","operator":"LESS_THAN","values":["11"]},
                    {"attribute":"MANUFACTURER","operator":"IN","values":["Samsung"]}
                ]' \
                | jq -r '.devices[0].arn')

# Get latest device we can test against.
latestDevice=$(aws devicefarm list-devices \
                --region="us-west-2" \
                --filters '[
                    {"attribute":"AVAILABILITY","operator":"EQUALS","values":["HIGHLY_AVAILABLE"]},
                    {"attribute":"PLATFORM","operator":"EQUALS","values":["ANDROID"]},
                    {"attribute":"OS_VERSION","operator":"GREATER_THAN_OR_EQUALS","values":["14"]},
                    {"attribute":"MANUFACTURER","operator":"IN","values":["Google", "Pixel"]}
                ]' \
                | jq -r '.devices[0].arn')

# IF we fail to find our required test devices, fail.
if [[ -z "${minDevice}" || -z "${middleDevice}" || -z "${latestDevice}" ]]; then
    echo "Failed to grab 3 required devices for integration tests."
    exit 1
fi

# Function to cancel duplicate runs for same code source in device farm.
function stopDuplicates {
  echo "Stopping duplicate runs"
  name="$file_name-$commit_sha"
  read -a running_arns <<< $(aws devicefarm list-runs \
                          --arn="$project_arn" \
                          --query="runs[?(status == 'RUNNING' || status == 'PENDING')  && name == '${name}'].arn" \
                          --region="us-west-2" \
                          --max-items=5 \
                          | jq -r '.[]')

  for arn in "${running_arns[@]}"
  do
    ## Just consume the result and do nothing with it.
    result=`aws devicefarm stop-run --arn $arn --region="us-west-2" --query="run.name"`
  done
}
stopDuplicates

# Most modules complete within 30 minutes
job_timeout=30

# Retry the Device Farm test run on failure. Transient issues like DNS
# flakes, auth credential races, or process crashes on shared Device Farm
# devices usually pass on a second attempt.
max_run_attempts=2
final_result='NONE'

for run_attempt in $(seq 1 $max_run_attempts); do
  echo "============================================================"
  echo "[RUN_IN_DEVICEFARM] Attempt $run_attempt/$max_run_attempts for $module_name"
  echo "============================================================"

  echo "[RUN_IN_DEVICEFARM] Scheduling test run..."
  run_arn=`aws devicefarm schedule-run --project-arn=$project_arn \
    --app-arn="$app_package_upload_arn" \
    --device-selection-configuration='{
        "filters": [
          {"attribute": "ARN", "operator":"IN", "values":["'$minDevice'", "'$middleDevice'", "'$latestDevice'"]}
        ],
        "maxDevices": '$max_devices'
    }' \
    --name="$file_name-$commit_sha" \
    --test="testSpecArn=$test_spec_arn,type=INSTRUMENTATION,testPackageArn=$test_package_upload_arn" \
    --execution-configuration="jobTimeoutMinutes=$job_timeout,videoCapture=false" \
    --query="run.arn" \
    --output=text \
    --region="us-west-2"`

  echo "[RUN_IN_DEVICEFARM] Run ARN: $run_arn"
  echo "[RUN_IN_DEVICEFARM] Waiting for test run to complete..."

  status='NONE'
  result='NONE'
  while true; do
    run_status_response=`aws devicefarm get-run --arn="$run_arn" --region="us-west-2" --query="run.[status, result]" --output text`
    read -a result_arr <<< $run_status_response
    status=${result_arr[0]}
    result=${result_arr[1]}
    if [ "$status" = "COMPLETED" ]; then
      break
    fi
    sleep 30
  done

  final_result=$result
  echo "[RUN_IN_DEVICEFARM] Attempt $run_attempt/$max_run_attempts: Status=$status Result=$result"

  if [ "$result" = "PASSED" ]; then
    if [ $run_attempt -gt 1 ]; then
      echo "[RUN_IN_DEVICEFARM] Tests passed on retry (attempt $run_attempt)"
    fi
    break
  fi

  if [ $run_attempt -lt $max_run_attempts ]; then
    echo "[RUN_IN_DEVICEFARM] Tests did not pass (result=$result). Will retry..."
  else
    echo "[RUN_IN_DEVICEFARM] Tests did not pass after $max_run_attempts attempts."
  fi
done

echo "============================================================"
echo "[RUN_IN_DEVICEFARM] Final result for $module_name: $final_result"
echo "============================================================"

./scripts/python/generate_df_testrun_report.py \
  -r "$run_arn" \
  -m "$module_name" \
  -o "build/allTests/$module_name/"

if [ "$final_result" = "PASSED" ]; then
  exit 0
fi
exit 1
