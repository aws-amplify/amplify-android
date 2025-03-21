import re

def get_dimension(name, value):
    return {
        "Name": name,
        "Value": value
    }

def get_metric(name, dimensions, value, unit):
    return {
        "MetricName": name,
        "Dimensions": dimensions,
        "Value": value,
        "Unit": unit
    }

def get_exception(exc_info):
    filename_regex = "(\w+\.py)"
    exception_location = ""
    exc_type, exc_value, exc_traceback = exc_info
    tb = exc_traceback
    while tb is not None:
        frame = tb.tb_frame
        exception_location += re.findall(filename_regex, frame.f_code.co_filename)[0] + " @ " + \
            frame.f_code.co_name + "#" + str(tb.tb_lineno)
        tb = tb.tb_next
        if tb is not None:
            exception_location += " >> "

    return exc_value, exception_location