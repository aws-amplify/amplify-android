#!/bin/bash
project_arn=$DEVICEFARM_PROJECT_ARN
max_devices=$NUMBER_OF_DEVICES_TO_TEST
test_spec_arn=$DEVICEFARM_TEST_SPEC_ARN
module_path=$1
# Extract everything after the last "/" if it exists, otherwise use the full value
module_name=${1##*/}

file_name="$module_name-debug-androidTest.apk"
full_path="$module_path/build/outputs/apk/androidTest/debug/$file_name"

if [[ -z "${project_arn}" ]]; then
  echo "DEVICEFARM_PROJECT_ARN environment variable not set."
  exit 1
fi

# Check if GitHub environment variables are set
for var in GITHUB_TOKEN GITHUB_REPO GITHUB_SHA; do
  if [[ -z "${!var}" ]]; then
    echo "Warning: $var environment variable not set. GitHub status checks will be skipped."
    break
  fi
done
if [[ -z "${max_devices}" ]]; then
  echo "NUMBER_OF_DEVICES_TO_TEST not set. Defaulting to 1."
  max_devices=1
fi

# Function to setup the app uploads in device farm
function createUpload {
  test_type=$1
  upload_response=`aws devicefarm create-upload --type $test_type \
                             --content-type="application/octet-stream" \
                             --project-arn="$project_arn" \
                             --name="$file_name" \
                             --query="upload.[url, arn]" \
                             --region="us-west-2" \
                             --output=text`
  echo $upload_response
}

echo 'Uploading test package'
# Create an upload for the instrumentation test package
read -a result <<< $(createUpload "INSTRUMENTATION_TEST_PACKAGE")
test_package_url=${result[0]}
test_package_upload_arn=${result[1]}
# Upload the apk
curl -H "Content-Type:application/octet-stream" -T $full_path $test_package_url

# Create an upload for the app package (They're the same, but they have to be setup in device farm)
echo 'Uploading app package'
read -a result <<< $(createUpload "ANDROID_APP")
app_package_url=${result[0]}
app_package_upload_arn=${result[1]}
# Upload the apk
curl -H "Content-Type:application/octet-stream" -T $full_path $app_package_url

# Wait to make sure the upload completes. This should actually make a get-upload call and check the status.
echo "Waiting for uploads to complete"
sleep 10

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
  name="$file_name-$CODEBUILD_SOURCE_VERSION"
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

# Schedule the test run in device farm
echo "Scheduling test run"
run_arn=`aws devicefarm schedule-run --project-arn=$project_arn \
  --app-arn="$app_package_upload_arn" \
  --device-selection-configuration='{
      "filters": [
        {"attribute": "ARN", "operator":"IN", "values":["'$minDevice'", "'$middleDevice'", "'$latestDevice'"]}
      ],
      "maxDevices": '$max_devices'
  }' \
  --name="$file_name-$CODEBUILD_SOURCE_VERSION" \
  --test="testSpecArn=$test_spec_arn,type=INSTRUMENTATION,testPackageArn=$test_package_upload_arn" \
  --execution-configuration="jobTimeoutMinutes=30,videoCapture=false" \
  --query="run.arn" \
  --output=text \
  --region="us-west-2"`

status='NONE'
result='NONE'
# Wait for the test to complete
while true; do
  run_status_response=`aws devicefarm get-run --arn="$run_arn" --region="us-west-2" --query="run.[status, result]" --output text`
  read -a result_arr <<< $run_status_response
  status=${result_arr[0]}
  result=${result_arr[1]}
  if [ "$status" = "COMPLETED" ]
  then
    break
  fi
  sleep 30
done
echo "Status = $status Result = $result"

./scripts/python/generate_df_testrun_report.py \
  -r "$run_arn" \
  -m "$module_name" \
  -o "build/allTests/$module_name/"

# Update GitHub status check for this specific module
if [ -n "$GITHUB_TOKEN" ] && [ -n "$GITHUB_REPO" ] && [ -n "$GITHUB_SHA" ]; then
  # Set the status context to include the module name
  STATUS_CONTEXT="DeviceFarm/$module_name"
  
  # Set description based on result
  if [ "$result" = "PASSED" ]; then
    STATUS_STATE="success"
    STATUS_DESC="All tests passed for $module_name"
  else
    STATUS_STATE="failure"
    STATUS_DESC="Tests failed for $module_name"
  fi
  
  # Send status update to GitHub
  curl -s -X POST \
    -H "Authorization: token $GITHUB_TOKEN" \
    -H "Accept: application/vnd.github.v3+json" \
    -d "{\"state\":\"$STATUS_STATE\",\"context\":\"$STATUS_CONTEXT\",\"description\":\"$STATUS_DESC\"}" \
    "https://api.github.com/repos/$GITHUB_REPO/statuses/$GITHUB_SHA"
  
  echo "GitHub status check updated for $module_name: $STATUS_STATE"
fi

# If the result is PASSED, then exit with a return code 0
if [ "$result" = "PASSED" ]
then
  exit 0
fi
# Otherwise, exit with a non-zero.
exit 1
