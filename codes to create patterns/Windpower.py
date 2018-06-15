import math
inputfile = open('KNMI_20180515_hourly.txt', 'r')
data_file = inputfile.read()
lines = data_file.split('\n')

run_file = open(str('Windpower')+ str('.txt'), 'w')

A = math.pi * ((90/2)**2)

for line in lines:
    data_line = line.split(',')
    if len(line) > 1:
        wind_speed = int(data_line[3].replace(" ", ""))
        wind_speed = wind_speed * 0.1 * 1.5
        if wind_speed >= 25 or wind_speed < 1.5: # above this windspeed the turbine isn't turning to avoid damage
            run_file.write('0\n')
        else:
            if wind_speed > 10:
                wind_speed = 10
            P = 0.5 * 1.225 * A * (wind_speed ** 3) * 0.4
            run_file.write('%f\n' % (P / 2))
