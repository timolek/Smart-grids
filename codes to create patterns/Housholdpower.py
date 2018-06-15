import math
inputfile = open('profielen elektriciteit 2017.txt', 'r')
data_file = inputfile.read()
lines = data_file.split('\n')

run_file = open(str('Housholdpower')+ str('.txt'), 'w')
count = 0
watt_list = []
for line in lines:
    data_line = line.split(',')
    if len(line) > 1:
        watt_list.append([float(data_line[3])*3500000, float(data_line[4])*3500000])
count = 0
watt_hour = 0
for quarter in watt_list:
    count += 1
    if count < 4:
        watt_hour += quarter[0]
        watt_hour += quarter[1]
    else:
        watt_hour += quarter[0]
        watt_hour += quarter[1]
        watt_hour = watt_hour / 8
        run_file.write('%.3f\n' % watt_hour)
        count = 0
        watt_hour = 0
