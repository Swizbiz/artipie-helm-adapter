# It is supposed that results are located in folder `scan_path`.
# Each result represents a folder with name which is equal to version.
# After commiting some results it walks through folder `scan_path` 
# and selects the latest version (in alphabetical order).

from os import listdir, scandir
from os.path import isfile, join

PLUS_MINUS = '±'

scan_path = 'benchmarks/results'
dirs = [f.path for f in scandir(scan_path) if f.is_dir()]
# choose only path which last folder name consists of only dots and digits
dirs = list(filter(lambda dir: dir.split('/')[-1].replace('.', '').isdecimal(), dirs))
# write results for latest version
dirs = sorted(dirs)
path = dirs[-1]

res = {}
vers = path.split('/')[-1]
files = [f for f in listdir(path) if isfile(join(path, f))]
for file in files:
    with open(join(path, file), 'r') as f:
        lines = f.readlines()
        for line in lines:
            # extract result from file required line
            if PLUS_MINUS in line and '.run' in line:
                words = line.split()
                idx = words.index(PLUS_MINUS)
                res[file.split('-')[0]] = ''.join(words[idx-1:idx+2])
res = dict(sorted(res.items()))
vals = list(res.values())
vals.insert(0, vers)
line = ' | '.join(vals)
# target line which would be added to readme
line = f'\n| {line} |'

with open(join(scan_path, 'README.md'), 'a') as f:
    print(f.write(line))