from subprocess import Popen, PIPE
import subprocess
from datetime import datetime

def execute_command(command, timeout=0, pipein=None, pipeout =  None, logcommandline = True,  workingdirectory=None):
    if logcommandline:
        print("running command: ", command, "......")
    process = Popen(command, shell=True, stdin=pipein, stdout = pipeout, cwd = workingdirectory)
    wait_times = 0
    while True:
        try:
            process.communicate(timeout = 10)
        except subprocess.TimeoutExpired:
            #tell circleci I am still alive, don't kill me
            if wait_times % 30 == 0 :
                print(str(datetime.now())+ ": I am still alive")
            # if time costed exceed timeout, quit
            if timeout >0 and wait_times > timeout * 6 :
                print(str(datetime.now())+ ": time out")
                return 1
            wait_times+=1

            continue
        break
    exit_code = process.wait()
    return exit_code

