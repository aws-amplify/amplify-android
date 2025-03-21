"""
Python script that parses the Android Test Orchestrator's instrumentation logs line by line
and stores the results in memory.
"""
import re
from dataclasses import dataclass
from typing import Dict
from metrics import *

# Android InstrumentationResultParser for inspiration
# https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:ddmlib/src/main/java/com/android/ddmlib/testrunner/InstrumentationResultParser.java;l=85?q=InstrumentationResultParser.java
CLASS_PREFIX = "INSTRUMENTATION_STATUS: class="
CURRENT_TEST_PREFIX = "INSTRUMENTATION_STATUS: current="
NUM_TESTS_PREFIX = "INSTRUMENTATION_STATUS: numtests="
STREAM_PREFIX = "INSTRUMENTATION_STATUS: stream="
TEST_NAME_PREFIX = "INSTRUMENTATION_STATUS: test="
STATUS_CODE = "INSTRUMENTATION_STATUS_CODE:"
STACK_TRACE_PREFIX = "INSTRUMENTATION_STATUS: stack="

# These prefixes will always show up but we don't care about them for metrics reasoning
# Instead we use these to see if there are any other instrumentation logs that we don't recognize as known
ID_PREFIX = "INSTRUMENTATION_STATUS: id="
RESULT_STREAM_PREFIX = "INSTRUMENTATION_RESULT: stream="
CODE_PREFIX = "INSTRUMENTATION_CODE:"

PACKAGE_NAMESPACE_PREFIX = "com.amplifyframework."
TIME_PREFIX = "Time: "

@dataclass
class Test:
    """The atomic test"""
    # test_name
    name: str
    stack_trace: str = None

    """
    Instrumentation Status Code meanings:
    1: Start
    2: In Progress
    -4: Assumption failed
    -3: Ignored
    -2: Failure
    -1: Error
    0: OK

    https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:ddmlib/src/main/java/com/android/ddmlib/testrunner/IInstrumentationResultParser.java;l=62?q=StatusKey
    """
    status_code: int = 1

@dataclass
class TestSuite:
    """A suite that contains many tests (i.e. the class)"""
    # class_name
    name: str
    # test_name: test
    tests: Dict[str, Test]
    passing_tests: int = 0
    failing_tests: int = 0

@dataclass
class TestRun:
    """A test run that contains many test suites (i.e. the module)"""
    # module_name
    name: str
    # class_name: test_suite
    test_suites: Dict[str, TestSuite]

    def contains_suite(name):
        return test_suites.get(name)

class Parser:
    def __init__(
        self,
        module_name
    ):
        self.module_name = module_name
        self.stack_trace = ""
        self.execution_time = 0
        self.class_name = ""
        self.test_run = None

    def is_relevant_stacktrace(self, line):
        return "error" in line.lower() or "exception" in line.lower() or PACKAGE_NAMESPACE_PREFIX in line.lower()

    def get_stack_traces(self, test_suite, metrics):
        pattern = r"@(\w{7})"
        replacement = "@[JAVA_HASH_CODE]"

        failure_status_codes = {-1, -2}
        filtered_tests = {k: v for k, v in test_suite.tests.items() if v.status_code in failure_status_codes}
        for test_name, test in filtered_tests.items():
            sanitized_error = re.sub(pattern, replacement, ascii(test.stack_trace[0:500]))
            stack_trace_dimensions = [
                get_dimension("Module", self.module_name),
                get_dimension("Test Suite", test_suite.name),
                get_dimension("Test", test_name),
                get_dimension("Exception", sanitized_error)
            ]
            metrics.append(get_metric("Test Failure", stack_trace_dimensions, 1.0, "Count"))

    def parse_line(self, line):
        line = line.strip()

        global test_num, num_tests, test_name, status_code
        global test_suite, test, test_run_error, instrumentation_failure

        if CLASS_PREFIX in line:
            class_tokens = line.replace(CLASS_PREFIX + PACKAGE_NAMESPACE_PREFIX, "").strip().split(".")
            # Class Name == Test Suite name
            self.class_name = class_tokens.pop()

            if self.test_run is None:
                # Module doesn't exist yet which means the test suite and test don't either
                test_suite = TestSuite(name=self.class_name, tests={})
                self.test_run = TestRun(name=self.module_name, test_suites={})
            else:
                if self.test_run.test_suites.get(self.class_name) is None:
                    # Module exists but Test Suite doesn't
                    test_suite = TestSuite(name=self.class_name, tests={})
                else:
                    test_suite = self.test_run.test_suites.get(self.class_name)
        elif CURRENT_TEST_PREFIX in line:
            test_num = line.replace(CURRENT_TEST_PREFIX, "").strip()
        elif NUM_TESTS_PREFIX in line:
            num_tests = line.replace(NUM_TESTS_PREFIX, "").strip()
        elif STREAM_PREFIX in line:
            read_line = line.replace(STREAM_PREFIX, "").strip()
            self.stack_trace = read_line
        elif STACK_TRACE_PREFIX in line:
            read_line = line.replace(STACK_TRACE_PREFIX, "").strip()
            self.stack_trace = read_line
        elif TEST_NAME_PREFIX in line:
            test_name = line.replace(TEST_NAME_PREFIX, "").strip()
            if test_suite.tests.get(test_name) is None:
                # First check if the test exists already
                # Initialize the new test
                test = Test(name=test_name)
                # Update it in the test suite
                test_suite.tests[test_name] = test
                self.test_run.test_suites[self.class_name] = test_suite
        elif STATUS_CODE in line:
            status_code = line.replace(STATUS_CODE, "").strip()
            self.test_run.test_suites.get(self.class_name).tests.get(test_name).status_code = int(status_code)
            if status_code == "0":
                self.test_run.test_suites.get(self.class_name).passing_tests += 1
            if status_code == "-2":
                print(f"Test #{test_num}: [{self.module_name}] // [{self.class_name}#{test_name}] FAILED")
                print(f"--- Stacktrace: [{self.stack_trace}]")
                self.test_run.test_suites.get(self.class_name).tests.get(test_name).stack_trace = self.stack_trace
                self.test_run.test_suites.get(self.class_name).failing_tests += 1
            # The status code acts as a delimiter for a test case so we can clear out the stack trace
                self.stack_trace = ""
        elif TIME_PREFIX in line:
            self.execution_time = line.replace(TIME_PREFIX, "").strip().replace(',','')
            print(f"Setting time: {self.execution_time}")
        elif "INSTRUMENTATION_" not in line:
            # This line is likely a continuation of the ongoing stream so append to it
            if self.is_relevant_stacktrace(line):
                if self.stack_trace.isspace() or self.stack_trace == "":
                    self.stack_trace = line.replace("Error in ", "").strip()
                else:
                    self.stack_trace = self.stack_trace + " // " + line
        elif ID_PREFIX not in line and RESULT_STREAM_PREFIX not in line and CODE_PREFIX not in line:
            # If there is a line that we don't expect, print it out for debugging
            print(f"Found a line that hasn't been parsed: {line}")