import os
import json

from datetime import datetime
from collections import defaultdict

INPUT_FILE = "/home/tomas/git/smap/python_scripts/users_data/timelines.json"
SAVE_FILE = "/home/tomas/git/smap/python_scripts/users_data/total_topn.json"


def topN(timelines):

    topN = defaultdict(int)

    for date, timeline in timelines:

        for hour in range(0, 24):
            ids = timeline.get(hour, [-2])
            for i in ids:
                topN[i] += 1

    return topN


def topWN(timelines):

    topWN = {
        "workday": defaultdict(int),
        "weekend": defaultdict(int)
    }

    for date, timeline in timelines:
        datetime_object = datetime.strptime(date, '%Y-%m-%d')
        if datetime_object.weekday() >= 5:
            dayType = "weekend"
        else:
            dayType = "workday"

        for hour in range(0, 24):
            ids = timeline.get(hour, [-2])
            for i in ids:
                topWN[dayType][i] += 1

    return topWN


def topDN(timelines):
    topDN = {i: defaultdict(int) for i in range(7)}

    for date, timeline in timelines:
        datetime_object = datetime.strptime(date, '%Y-%m-%d')
        day = datetime_object.weekday()
        for hour in range(0, 24):
            ids = timeline.get(hour, [-2])
            for i in ids:
                topDN[day][i] += 1

    return topDN


def topHN(timelines):
    topHN = {i: defaultdict(int) for i in range(24)}
    for date, timeline in timelines:
        for hour in range(0, 24):
            ids = timeline.get(hour, [-2])
            for i in ids:
                topHN[hour][i] += 1
    return topHN


def topWHN(timelines):
    topWHN = {
        "workday": {i: defaultdict(int) for i in range(24)},
        "weekend": {i: defaultdict(int) for i in range(24)}
    }

    for date, timeline in timelines:
        datetime_object = datetime.strptime(date, '%Y-%m-%d')
        if datetime_object.weekday() >= 5:
            dayType = "weekend"
        else:
            dayType = "workday"
        for hour in range(0, 24):
            ids = timeline.get(hour, [-2])
            for i in ids:
                topWHN[dayType][hour][i] += 1
    return topWHN


def topDHN(timelines):
    topDHN = {k: {i: defaultdict(int) for i in range(24)} for k in range(7)}

    for date, timeline in timelines:
        datetime_object = datetime.strptime(date, '%Y-%m-%d')
        day = datetime_object.weekday()
        for hour in range(0, 24):
            ids = timeline.get(hour, [-2])
            for i in ids:
                topDHN[day][hour][i] += 1
    return topDHN


if __name__ == "__main__":

    with open(INPUT_FILE, "r") as f:
        timelines = json.load(f)

        topNs = dict()
        for user, user_timelines in timelines.items():
            if user != "017":
                continue
            #res = topN(user_timelines)
            res = topWN(user_timelines)
            #res = topDN(user_timelines)
            #res = topHN(user_timelines)
            #res = topWHN(user_timelines)
            print(json.dumps(res))

    with open(SAVE_FILE, "w") as f:
        f.write(json.dumps(topNs))
