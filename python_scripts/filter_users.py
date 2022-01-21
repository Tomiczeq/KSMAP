import os
import json

from datetime import datetime
from collections import defaultdict


DATA_DIR = "/home/tomas/git/smap/data/geolife"
SAVE_FILE = "/home/tomas/git/smap/python_scripts/filtered_users.json"


def getDirectories(path):
    directories = [
        os.path.join(path, dir_path) for dir_path in os.listdir(path)
        if os.path.isdir(os.path.join(path, dir_path))
    ]
    return directories


def getTrajectories(path):
    trajectories = [
        os.path.join(path, trajectory) for trajectory in os.listdir(path)
        if os.path.isfile(os.path.join(path, trajectory))
    ]
    return trajectories


def getLineFields(line):
    dct = dict()
    fields = line.strip().split(",")
    dct["latitude"] = float(fields[0])
    dct["longitude"] = float(fields[1])
    formatted = fields[-2] + "T" + fields[-1]
    date = datetime.fromisoformat(formatted)
    timestamp = date.timestamp()
    dct["timestamp"] = timestamp
    dct["date"] = date.strftime("%Y-%m-%d")
    dct["hour"] = int(date.hour)
    dct["weekday"] = date.weekday()
    return dct


def getTimestamp(line):
    fields = getLineFields(line)
    return fields["timestamp"]


def skipFirstLines(f):
    for i in range(6):
        f.readline()

def getTrajectoryPoints(trajectory):

    points = []
    with open(trajectory, "r") as f:
        skipFirstLines(f)

        for line in f:
            point = getLineFields(line)
            points.append(point)
    return points

def filterTrajectories(directory, min_period=60*60*3, min_days=90):
    path = os.path.join(directory, "Trajectory")
    trajectories = getTrajectories(path)

    points = []
    for trajectory in trajectories:
        t_points = getTrajectoryPoints(trajectory)
        points.extend(t_points)

    points = sorted(points, key=lambda x: x["timestamp"])
    by_date = defaultdict(set)
    for point in points:
        by_date[point["date"]].add(point["hour"])

    total_days = 0
    for k, v in by_date.items():
        if len(v) >= 12:
            total_days += 1
        #print(directory, f"date: {k} delta: {(max(v) - min(v))/3600} hodin")
    if total_days > 15:
        print(directory, f"num_long_traj: {total_days}")


def filterByPeriod(directory, min_period=60*60*3):
    path = os.path.join(directory, "Trajectory")
    trajectories = getTrajectories(path)

    filtered_trajectories = []
    for trajectory in trajectories:
        with open(trajectory, "r") as f:
            skipFirstLines(f)
            firstline = f.readline()
            for line in f:
                pass
            lastline = line

            first_timestamp = getTimestamp(firstline)
            last_timestamp = getTimestamp(lastline)

        delta = last_timestamp - first_timestamp
        if delta >= min_period:
            filtered_trajectories.append(trajectory)
    return filtered_trajectories


def filterByDays(directory, min_days=90):
    trajectories = filterByPeriod(directory)

    timestamps = []
    for trajectory in trajectories:
        with open(trajectory, "r") as f:
            skipFirstLines(f)
            first_timestamp = getTimestamp(f.readline())
            timestamps.append(first_timestamp)

    timestamps = sorted(timestamps)

    days = set()
    for timestamp in timestamps:
        date = datetime.fromtimestamp(timestamp)
        date_string = date.date().strftime("%Y-%m-%d")
        days.add(date_string)

    if len(days) >= min_days:
        return True
    return False


if __name__ == "__main__":
    directories = getDirectories(DATA_DIR)

    filtered_directories = dict()
    for directory in directories:
        #if not filterByDays(directory):
        #    continue

        #trajectories = filterByPeriod(directory)
        #filtered_directories[directory] = trajectories

        filterTrajectories(directory)

    #with open(SAVE_FILE, "w") as f:
    #    f.write(json.dumps(filtered_directories))
