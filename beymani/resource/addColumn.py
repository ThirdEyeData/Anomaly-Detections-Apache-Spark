import pandas as pd

data = pd.read_csv("cusage.csv")
data['weekDayOrWeekendOfWeek']="weekDayOrWeekendOfWeek"
data.to_csv("cusage.csv")
