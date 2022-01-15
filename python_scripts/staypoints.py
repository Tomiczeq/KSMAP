import os
import json
import haversine as hs
from datetime import datetime


from filter_users import skipFirstLines
from filter_users import getLineFields


INPUT_FILE = "/home/tomas/git/smap/python_scripts/filtered_users.json"
SAVE_DIR = "/home/tomas/git/smap/python_scripts/users_data"


def getTrajectoryPoints(trajectory):

    points = []
    with open(trajectory, "r") as f:
        skipFirstLines(f)

        for line in f:
            point = getLineFields(line)
            points.append(point)
    return points


def print_progress(i, max_item, step):
    if i % step == 0:
        print(f"{i}", end='\r')


def comp_distance(x, y):
    loc1 = (x["latitude"], x["longitude"])
    loc2 = (y["latitude"], y["longitude"])
    dist = hs.haversine(loc1, loc2, unit=hs.Unit.METERS)
    return dist


def compute_mean_coord(data, i, j):
    latitudes = []
    longitudes = []

    while i <= j:
        latitudes.append(data[i]["latitude"])
        longitudes.append(data[i]["longitude"])
        i += 1

    mean_lat = sum(latitudes)/len(latitudes)
    mean_lon = sum(longitudes)/len(longitudes)

    return mean_lat, mean_lon


def get_staypoints(data, dist_thresh=30, time_thresh=60*30):
    i = 0
    num = len(data)
    staypoints = []

    while i < num:
        j = i + 1

        while j < num:
            dist = comp_distance(data[i], data[j])

            if dist > dist_thresh:
                time_delta = data[j-1]["timestamp"] - data[i]["timestamp"]
                if time_delta > time_thresh:
                    latitude, longitude = compute_mean_coord(data, i, j - 1)

                    staypoint = dict()
                    staypoint["latitude"] = latitude
                    staypoint["longitude"] = longitude
                    staypoint["arrival"] = data[i]["timestamp"]
                    staypoint["leaving"] = data[j - 1]["timestamp"]
                    staypoint["hours"] = [data[i]["hour"], data[j - 1]["hour"]]
                    staypoint["dates"] = [data[i]["date"], data[j - 1]["date"]]
                    staypoints.append(staypoint)
                break
            j += 1
            print_progress(j, num, 1000)
        i = j

    staypoints = sorted(staypoints, key=lambda x: x["arrival"])
    return staypoints


if __name__ == "__main__":

    with open(INPUT_FILE, "r") as f:
        data = json.load(f)

    staypoints = dict()
    for user, trajectories in data.items():
        user = user.split("/")[-1]
        user_points = []
        for trajectory in trajectories:
            points = getTrajectoryPoints(trajectory)
            # TODO diky spojeni trajektorii muze vzniknout staypoint trvajici
            # dele nez den. Je to to co chceme? Muze to reprezentovat, ze od
            # vypnuti a zapnuti GPS uzivatel zustal na jednom miste, ale take
            # nemusi
            user_points.extend(points)

        user_points = sorted(user_points, key=lambda x: x["timestamp"])
        user_staypoints = get_staypoints(user_points, time_thresh=60*15)

        for i in range(min(5, len(user_staypoints))):
            print(user_staypoints[i])

        staypoints[user] = user_staypoints

    with open(os.path.join(SAVE_DIR, "staypoints.json"), "w") as f:
        f.write(json.dumps(staypoints))
