import math

demands = []


firstfile = open('EV1.txt', 'r')
data_firstfile = firstfile.read()
lines = data_firstfile.split('\n')
for line in lines:
    if line != 'Demand':
        demands.append(float(line))

for i in range(2,201):
    filename = str('EV') + str(i) + str('.txt')
    inputfile = open(filename, 'r')
    data_file = inputfile.read()
    lines_file = data_file.split('\n')
    count = 0

    for line_file in lines_file:
        if line_file != 'Demand':
            current_demand = demands[count]
            del demands[count]
            new_demand = float(current_demand) + float(line_file)
            demands.insert(count, new_demand)
            count += 1



run_file = open(str('TotalEV')+ str('.txt'), 'w')
for i in demands:
    run_file.write('%f \n' % i)
