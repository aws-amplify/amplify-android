#!/usr/bin/env python3
"""
Python script that parses the Android Test Orchestrator's instrumentation logs for a given
Device Farm test run and generates a user-readable Junit report.
"""
import os
import argparse
import dload
import boto3
import sys
import logging
from instrumentation_parser import Parser
from metrics import *
from junit_xml import TestSuite, TestCase

LOG_FORMATTER = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
CONSOLE_HANDLER = logging.StreamHandler()
CONSOLE_HANDLER.setFormatter(LOG_FORMATTER)
LOGGER = logging.getLogger("DeviceFarmTestRunReportGenerator")
LOGGER.setLevel(os.getenv("LOG_LEVEL") if os.getenv("LOG_LEVEL") is not None else "INFO")
LOGGER.addHandler(CONSOLE_HANDLER)

# Parse the required script arguments
def parse_arguments():
    parser = argparse.ArgumentParser(description="Utility that generates a report for a DeviceFarm test run.")
    parser.add_argument("-r", "--run_arn", help="The ARN of the DeviceFarm test run.", required=True)
    parser.add_argument("-m", "--module_name", help="The module name for the test suite.", required=True)
    parser.add_argument("-o", "--output_path", help="Destination path for the build reports.", required=True)
    return parser.parse_args()

def main(arguments):
    LOGGER.info(f"Starting to generate report...")
    args = parse_arguments()

    # The path that the unzipped Device Farm artifacts will be unzipped into
    logs_dir = "build/allTests/{}".format(args.module_name)

    # The path that the Device Farm instrumentation logs are in
    log_file = logs_dir + "/Host_Machine_Files/$DEVICEFARM_LOG_DIR/instrument.log"

    df_client = boto3.client(
        'devicefarm',
        region_name='us-west-2'
    )

    # For a particular Device Farm run, grab the list of all of the artifacts
    response = df_client.list_artifacts(
        arn=args.run_arn,
        type="FILE"
    )

    # The instrumentation logs are stored in the "CUSTOMER_ARTIFACT" file for a test job
    customer_artifacts = (artifact for artifact in response["artifacts"] if artifact["type"] == "CUSTOMER_ARTIFACT")

    # A single test run may have multiple jobs where each job tests on a different device
    # A regular PR typically tests on one device while a release PR typically tests on 3 devices
    # The instrumentation logs for each job is uploaded as a separate CUSTOMER_ARTIFACT in the
    # run's artifacts.
    for job_no, customer_artifact in enumerate(customer_artifacts):
        LOGGER.info(f"Parsing result for artifact ARN: {customer_artifact['arn']}")

        unzip_result = dload.save_unzip(customer_artifact["url"], extract_path=logs_dir, delete_after=True)
        if unzip_result is None or unzip_result == "":
            LOGGER.error("Unzip of test run artifacts failed")
            break

        parser = Parser(args.module_name)
        metrics = []
        try:
            # Open the provided file and then start to parse it
            with open(log_file, "r") as file:
                for line in file:
                    try:
                        parser.parse_line(line.strip())
                    except Exception as e:
                        exception_value, exception_location = get_exception(sys.exc_info())
                        LOGGER.error(f"Encountered an exception trying to parse the results: {exception_value} at [{exception_location}] for line:\n{line.strip()}")

            module_passing_tests = module_failing_tests = 0
            LOGGER.info(f"\n--------------------------\nTest Suite Statistics\n--------------------------")
            test_run_passing_tests = test_run_failing_tests = 0

            # The Device Farm run ARN is in the format of:
            # arn:aws:devicefarm:us-west-2:ACCOUNT_ID:job:PROJECT_ARN_ID/RUN_ARN_ID
            # So split the run ARN by ':', take the last element, split it by '/' and then use each
            # component to format a URL to add to the test report for easy access to the logs and output files
            arn_components = args.run_arn.split(":")[-1].split("/")
            run_url = f"https://us-west-2.console.aws.amazon.com/devicefarm/home#/mobile/projects/{arn_components[0]}/runs/{arn_components[1]}/jobs/00000"
            debug_messaging = f"You can find the detailed logs and output files at {run_url}"

            # Run through the parser results and translate them into the junit_xml data classes
            # while also constructing the CloudWatch metrics
            for test_suite_name, test_suite in parser.test_run.test_suites.items():
                test_cases = []
                for test_name, test in test_suite.tests.items():
                    if test.status_code == 0 or test.status_code == -3 or test.status_code == -4:
                        test_status = "PASSED"
                    elif test.status_code == -1:
                        test_status = "ERROR"
                    else:
                        test_status = "FAILED"

                    tc = TestCase(test_name,
                        classname=test_suite_name,
                        stdout=f"{debug_messaging}\n{test.stack_trace}",
                        status=test_status
                    )

                    if test_status == "FAILED":
                        tc.add_failure_info(test.stack_trace)
                    elif test_status == "ERROR":
                        tc.add_error_info(test.stack_trace)

                    test_cases.append(tc)

                # Because a test run can have N number of test jobs, we need to distinguish each job's
                # test result so we'll append an integer to it.
                ts = TestSuite(test_suite_name + "-" + str(job_no), test_cases=test_cases)
                ts_output = TestSuite.to_xml_string([ts])
                LOGGER.info(f"Saving test suite {test_suite_name} report.")

                if not os.path.exists(args.output_path):
                    os.makedirs(args.output_path)
                f = open(args.output_path + test_suite_name + "-" + str(job_no) + ".xml", "w")
                f.write(ts_output)
                f.close()

                success_percentage = test_suite.passing_tests/(test_suite.passing_tests + test_suite.failing_tests)
                LOGGER.info(f"Name: {test_suite_name}")
                LOGGER.info(f"Passing Tests: {test_suite.passing_tests}")
                LOGGER.info(f"Failing Tests: {test_suite.failing_tests}")
                LOGGER.info(f"Success Percentage: {success_percentage}")
                LOGGER.info(f"------------------------------------------------")
                test_run_passing_tests += test_suite.passing_tests
                module_passing_tests += test_suite.passing_tests
                test_run_failing_tests += test_suite.failing_tests
                module_failing_tests += test_suite.failing_tests
                if (success_percentage < 1.0):
                    parser.get_stack_traces(test_suite, metrics)

                test_suite_dimension = [ get_dimension("Module", args.module_name), get_dimension("Test Suite", test_suite_name) ]

                # Test Suite Success Percentage
                metrics.append(get_metric("Test Success Percentage", test_suite_dimension, success_percentage, "Count"))

                # Test Suite Success Count
                metrics.append(get_metric("Tests Succeeded", test_suite_dimension, test_run_passing_tests, "Count"))

                # Test Suite Failure Count
                metrics.append(get_metric("Tests Failed", test_suite_dimension, test_run_failing_tests, "Count"))

            LOGGER.info(f"\n--------------------------\nTest Run Statistics\n--------------------------")
            LOGGER.info(f"Run Name: {args.module_name}")
            LOGGER.info(f"Test Successes: {test_run_passing_tests}")
            LOGGER.info(f"Test Failures: {test_run_failing_tests}")
            success_percentage = test_run_passing_tests/(test_run_passing_tests + test_run_failing_tests)
            LOGGER.info(f"Success Percentage: {success_percentage}")
            LOGGER.info(f"Test Run Execution Time: {parser.execution_time}")

            module_dimension = [ get_dimension("Module", args.module_name) ]
            success_percentage = module_passing_tests/(module_passing_tests + module_failing_tests)
            # Test Run Success Percentage
            metrics.append(get_metric("Test Success Percentage", module_dimension, success_percentage, "Count"))
            # Test Run Success Count
            metrics.append(get_metric("Tests Succeeded", module_dimension, module_passing_tests, "Count"))
            # Test Run Failure Count
            metrics.append(get_metric("Tests Failed", module_dimension, module_failing_tests, "Count"))
            # Test Run Execution Time
            metrics.append(get_metric("Execution Time", module_dimension, float(parser.execution_time), "Seconds"))
        except Exception as e:
            exception_value, exception_location = get_exception(sys.exc_info())

            LOGGER.error(f"Encountered an exception trying to parse the results: {exception_value} at [{exception_location}]")
            exception_dimensions = [ get_dimension("Exception", exception_value), get_dimension("Line Number", exception_location) ]
            metrics.append(get_metric("Test Run Reporting Error", exception_dimensions, 1, "Count"))
            print(f"Adding metric [{get_metric('Test Run Reporting Error', exception_dimensions, 1, 'Count')}]")

        # Now that the logs have been parsed and metrics have been gathered, we publish the metrics
        # to CloudWatch.
        try:
            cw_client = boto3.client(
                'cloudwatch',
                region_name='us-west-2'
            )

            response = cw_client.put_metric_data(
                Namespace='AmplifyAndroidV2-IntegTests',
                MetricData=metrics
            )
            LOGGER.info(response)
        except Exception as e:
            exception_value, exception_location = get_exception(sys.exc_info())
            LOGGER.error(f"Encountered an exception trying to parse the results: {exception_value} at [{exception_location}]")
            LOGGER.error(f"The metrics that were attempted to be published: {metrics}")

if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))