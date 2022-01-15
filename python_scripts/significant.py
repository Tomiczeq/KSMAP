import os
import json
import numpy as np
from sklearn.cluster import DBSCAN

INPUT_FILE = "/home/tomas/git/smap/python_scripts/users_data/staypoints.json"
SAVE_FILE = "/home/tomas/git/smap/python_scripts/users_data/labeled_staypoints.json"


def dbscan(data, meters, min_samples):
    coordinates = []
    for staypoint in data:
        coordinates.append([staypoint["latitude"], staypoint["longitude"]])

    # convert the list of lat/lon coordinates to radians
    X = np.radians(coordinates)

    # calculate meters epsilon threshold
    earth_radius = 6371
    epsilon = (meters/1000.)/earth_radius
    db = DBSCAN(eps=epsilon, min_samples=min_samples).fit(X)

    labels = db.labels_

    # Number of clusters in labels, ignoring noise if present.
    n_clusters_ = len(set(labels)) - (1 if -1 in labels else 0)
    n_noise_ = list(labels).count(-1)

    print("Estimated number of clusters: %d" % n_clusters_)
    print("Estimated number of noise points: %d" % n_noise_)
    print(f"Total points: {len(labels)}")
    return labels


def get_clusters(data, meters=1000, min_samples=5):
    labels = dbscan(data, meters, min_samples)
    assert len(data) == len(labels)
    for i in range(len(data)):
        data[i]["cluster_id"] = int(labels[i])


if __name__  == "__main__":

    with open(INPUT_FILE, "r") as f:
        staypoints = json.load(f)

        for user, user_staypoints in staypoints.items():
            print(user)
            get_clusters(user_staypoints)
            staypoints[user] = user_staypoints

    with open(SAVE_FILE, "w") as f:
        f.write(json.dumps(staypoints))
