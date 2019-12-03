/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

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