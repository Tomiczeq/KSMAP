import json
import haversine as hs
# import numpy as np
# from sklearn.cluster import DBSCAN

INPUT_FILE = "/home/tomas/git/smap/python_scripts/users_data/staypoints.json"
SAVE_FILE = "/home/tomas/git/smap/python_scripts/users_data/labeled_staypoints.json"


def haversineDist(x, y):
    dist = hs.haversine(x, y, unit=hs.Unit.METERS)
    return dist


def getNeighbors(currentPoint, coordinates, distance):

    neighborsIds = []
    for i, point in enumerate(coordinates):

        if haversineDist(currentPoint, point) < distance:
            neighborsIds.append(i)
    return neighborsIds


def stayTime(ids, data):
    hours = 0
    for i in ids:
        staypoint = data[i]
        arrival = staypoint["arrival"]
        leaving = staypoint["leaving"]
        delta = (leaving - arrival) / 3600.
        hours += delta
    return hours


def dbscan(data, distance, minHours):
    coordinates = []
    for staypoint in data:
        coordinates.append([staypoint["latitude"], staypoint["longitude"]])

    labels = [-2 for i in range(len(coordinates))]

    clusterId = -1
    for i in range(len(coordinates)):

        if labels[i] != -2:
            continue

        currentPoint = coordinates[i]
        neighborsIds = getNeighbors(currentPoint, coordinates, distance)

        # mark noise
        # if len(neighborsIds) < minPts:
        if stayTime(neighborsIds, data) < minHours:
            labels[i] = -1
            continue

        data[i]["core"] = True
        clusterId += 1
        labels[i] = clusterId

        j = 0
        while j < len(neighborsIds):
            index = neighborsIds[j]

            # if label is noise, mark him
            if labels[index] == -1:
                labels[index] = clusterId

            # do not search for neighbors of already processed points
            if labels[index] != -2:
                j += 1
                continue

            # label him with clusterId
            labels[index] = clusterId

            # get neighbors
            anotherNeighbors = getNeighbors(coordinates[index],
                                            coordinates, distance)

            # is he corepoint? if yes then add them all to neighbors
            # if len(anotherNeighbors) >= minPts:
            if stayTime(anotherNeighbors, data) >= minHours:
                data[index]["core"] = True
                neighborsIds.extend(anotherNeighbors)

            # no corepoint, fine
            j += 1

    # Number of clusters in labels, ignoring noise if present.
    n_clusters_ = len(set(labels)) - (1 if -1 in labels else 0)
    n_noise_ = list(labels).count(-1)

    #print("Estimated number of clusters: %d" % n_clusters_)
    #print("Estimated number of noise points: %d" % n_noise_)
    #print(f"Total points: {len(labels)}")
    #print(labels)
    return labels


#def Ndbscan(data, meters, min_samples):
#    coordinates = []
#    for staypoint in data:
#        coordinates.append([staypoint["latitude"], staypoint["longitude"]])
#
#    # convert the list of lat/lon coordinates to radians
#    X = np.radians(coordinates)
#
#    # calculate meters epsilon threshold
#    earth_radius = 6371
#    epsilon = (meters/1000.)/earth_radius
#    db = DBSCAN(eps=epsilon, min_samples=min_samples).fit(X)
#    labels = db.labels_
#
#    # Number of clusters in labels, ignoring noise if present.
#    n_clusters_ = len(set(labels)) - (1 if -1 in labels else 0)
#    n_noise_ = list(labels).count(-1)
#
#    print("Estimated number of clusters: %d" % n_clusters_)
#    print("Estimated number of noise points: %d" % n_noise_)
#    print(f"Total points: {len(labels)}")
#    return labels


def get_clusters(data, meters=1000, min_samples=24):
    labels = dbscan(data, meters, min_samples)
    assert len(data) == len(labels)
    for i in range(len(data)):
        data[i]["cluster_id"] = int(labels[i])


def label_clusters(training_staypoints, predicting_staypoints, meters=1000):

    for p_staypoint in predicting_staypoints:
        p_coords = [p_staypoint["latitude"], p_staypoint["longitude"]]
        for t_staypoint in training_staypoints:

            if t_staypoint.get("core"):
                t_coords = [t_staypoint["latitude"], t_staypoint["longitude"]]
                distance = haversineDist(p_coords, t_coords)

                if distance < meters:
                    p_staypoint["cluster_id"] = t_staypoint["cluster_id"]
                    break

        if "cluster_id" not in p_staypoint:
            p_staypoint["cluster_id"] = -1


if __name__ == "__main__":

    with open(INPUT_FILE, "r") as f:
        staypoints = json.load(f)

        for user, user_staypoints in staypoints.items():
            print(user)
            get_clusters(user_staypoints, meters=200)
            staypoints[user] = user_staypoints

    with open(SAVE_FILE, "w") as f:
        f.write(json.dumps(staypoints))
