from utils import execute_command
import time

count = 0
print("unlocking emulator screen ...")
while True:
    rn = execute_command("adb shell input keyevent 82")
    if rn == 0 :
        print("Unlocked emulator screen")
        exit(0)
    if count > 10 :
        print("Failed to unlock emulator screen")
        exit(1)
    time.sleep(10)
    count = count + 1