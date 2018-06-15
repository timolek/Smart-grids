import math
inputfile = open('KNMI_20180516_hourly.txt', 'r')
data_file = inputfile.read()
lines = data_file.split('\n')

run_file = open(str('Solarpower')+ str('.txt'), 'w')

EFFICIENCY = 0.20


for line in lines:
    data_line = line.split(',')
    if len(line) > 1:
        solar_strength = int(data_line[3].replace(" ", ""))
        solarpower = solar_strength * 100000 #to convert the data to 10 m2
        effective_solar_power = solarpower*EFFICIENCY
        solarpower_watt = effective_solar_power / 3600
        run_file.write('%.3f\n' % solarpower_watt)
