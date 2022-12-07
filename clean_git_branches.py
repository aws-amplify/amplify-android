import os
if __name__ == '__main__':
    lines = os.popen("git branch -r").read().rstrip("\n").replace(" ", "").split("\n")
    for line in lines:
        if not line.startswith("origin/"):
            continue
        branch_name = line.replace("origin/", "")
        if branch_name == "atlasv" or branch_name == "develop" or branch_name == "main":
            continue
        if branch_name.startswith("HEAD"):
            continue
        delete_cmd = "git push origin --delete %s" % branch_name
        print(delete_cmd)
        os.system(delete_cmd)
