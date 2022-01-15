import os
import json

from datetime import datetime

INPUT_FILE = "/home/tomas/git/smap/python_scripts/users_data/labeled_staypoints.json"
SAVE_FILE = "/home/tomas/git/smap/python_scripts/users_data/timelines.json"


def getTimelines(user_staypoints):

    timelines = dict()
    for staypoint in user_staypoints:
        dates = staypoint["dates"]

        # TODO what if there is difference greater than one day between
        # arrival and leaving?
        if dates[0] == dates[1]:
            date = dates[0]
            timeline = timelines.get(date, dict())
            hours = staypoint["hours"]

            for hour in range(hours[0], hours[1] + 1):
                timeline.setdefault(hour, [])
                timeline[hour].append(staypoint["cluster_id"])

            if "timestamp" not in timeline:
                timestamp = datetime.strptime(date, "%Y-%m-%d").timestamp()
                timeline["timestamp"] = timestamp

            timelines[date] = timeline
        else:
            date = dates[0]
            timeline = timelines.get(date, dict())
            hours = staypoint["hours"]

            for hour in range(hours[0], 24):
                timeline.setdefault(hour, [])
                timeline[hour].append(staypoint["cluster_id"])

            if "timestamp" not in timeline:
                timestamp = datetime.strptime(date, "%Y-%m-%d").timestamp()
                timeline["timestamp"] = timestamp

            timelines[date] = timeline

            date = dates[1]
            timeline = timelines.get(date, dict())

            for hour in range(0, hours[1] + 1):
                timeline.setdefault(hour, [])
                timeline[hour].append(staypoint["cluster_id"])

            if "timestamp" not in timeline:
                timestamp = datetime.strptime(date, "%Y-%m-%d").timestamp()
                timeline["timestamp"] = timestamp

            timelines[date] = timeline

    return sorted(timelines.items(), key=lambda x: x[1]["timestamp"])


if __name__ == "__main__":

    with open(INPUT_FILE, "r") as f:
        staypoints = json.load(f)

        timelines = dict()
        for user, user_staypoints in staypoints.items():
            user_timelines = getTimelines(user_staypoints)
            timelines[user] = user_timelines

    with open(SAVE_FILE, "w") as f:
        f.write(json.dumps(timelines))
