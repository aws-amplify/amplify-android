#!/bin/bash
project_arn=$1
device_pool_name=$2
module_name=$3
file_name="$module_name-debug-androidTest.apk"
full_path="$module_name/build/outputs/apk/androidTest/debug/$file_name"

# Get the device pool arn for the name passed in
device_pool_arn=`aws devicefarm list-device-pools --arn=$project_arn --region="us-west-2" --query="devicePools[?name=='$device_pool_name'].arn" --output=text`

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

# Schedule the test run in device farm
echo "Scheduling test run"
run_arn=`aws devicefarm schedule-run --project-arn=$project_arn \
                            --app-arn="$app_package_upload_arn" \
                            --device-pool-arn=$device_pool_arn \
                            --name="$file_name" \
                            --test="type=INSTRUMENTATION,testPackageArn=$test_package_upload_arn" \
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
  echo "Status = $status Result = $result"
  if [ "$status" = "COMPLETED" ]
  then
    break
  fi
  sleep 5
done

# If the result is PASSED, then exit with a return code 0
if [ "$result" = "PASSED" ]
then
  exit 0
fi
# Otherwise, exit with a non-zero
exit 1
