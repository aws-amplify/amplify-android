import subprocess

def syncModel(modelName):
	subprocess.call("amplifyds sync %s staging dev --delete" % modelName, shell=True)

if __name__ == '__main__':
	with open ("models.txt", 'r') as f:
		lines = f.readlines()
		for line in lines:
			line = line.rstrip("\n")
			syncModel(line)