import matplotlib.pyplot as plt
import matplotlib.dates as mdates
import csv
import datetime as dt

x = []
y = []

with open('multiTimeline.csv','r') as csvfile:
    plots = csv.reader(csvfile, delimiter=',')
    for row in plots:
        x.append(dt.datetime.strptime(row[0],'%Y-%m-%d').date())
        y.append(int(row[1]))

plt.gca().xaxis.set_major_formatter(mdates.DateFormatter('%Y-%m-%d'))
plt.gca().xaxis.set_major_locator(mdates.YearLocator())
plt.plot(x, y)
plt.ylabel('Percentage of interest')
plt.xlabel('Time period')
plt.title('Interest in blockchain technology over the last 5 years')
plt.gcf().autofmt_xdate()
plt.show()