import os
import json
import sys
import numpy as np
from datetime import datetime
from filter_users import getTrajectories

from staypoints import getTrajectoryPoints
from staypoints import get_staypoints
from significant import get_clusters
from significant import label_clusters
from timelines import getTimelines

from total_topn import topN, topWN, topDN, topHN, topWHN, topDHN

from time import time

#from neural import simpleNN
#from neural import train_model
from collections import defaultdict


INPUT_FILE = "/home/tomas/git/smap/python_scripts/filtered_users.json"
STAYPOINTS_PATH = "/home/tomas/git/smap/python_scripts/users_data/staypoints.json"


def predict_topn(topn, timeline):

    topn_sorted = sorted(topn.items(), key=lambda x: x[1], reverse=True)
    prediction = None
    for clusterId, num in topn_sorted:
        if clusterId == -2:
            continue
        else:
            prediction = clusterId
            break
    correct = 0
    num_sig = 0
    if prediction is None:
        return correct, num_sig

    for i in range(24):
        if prediction in timeline[1].get(i, [-2]):
            if prediction != -1:
                num_sig += 1
            correct += 1

    return correct, num_sig


def predict_topwn(topwn, timeline):
    day = timeline[0]
    datetime_object = datetime.strptime(day, '%Y-%m-%d')
    weekday = datetime_object.weekday()
    if weekday >= 5:
        dayType = "weekend"
    else:
        dayType = "workday"

    topn = topwn[dayType]
    prediction, num_sig = predict_topn(topn, timeline)
    return prediction


def predict_topdn(topdn, timeline):
    day = timeline[0]
    datetime_object = datetime.strptime(day, '%Y-%m-%d')
    weekday = datetime_object.weekday()
    topn = topdn[weekday]
    prediction, num_sig = predict_topn(topn, timeline)
    return prediction


def predict_tophn(tophn, timeline):
    correct = 0
    num_sig = 0
    for i in range(24):
        toph_sorted = sorted(tophn[i].items(), key=lambda x: x[1], reverse=True)
        prediction = None
        for clusterId, num in toph_sorted:
            if clusterId == -2:
                continue
            else:
                prediction = clusterId
                break
        if prediction is None:
            continue
        if prediction in timeline[1].get(i, [-2]):
            correct += 1
            if prediction != -1:
                num_sig += 1
    return correct, num_sig


def predict_topwhn(topwhn, timeline):
    day = timeline[0]
    datetime_object = datetime.strptime(day, '%Y-%m-%d')
    weekday = datetime_object.weekday()
    if weekday >= 5:
        dayType = "weekend"
    else:
        dayType = "workday"
    tophn = topwhn[dayType]
    prediction, num_sig = predict_tophn(tophn, timeline)
    print(f"correct: {prediction} num_sig: {num_sig}")
    return prediction, num_sig


def predict_topdhn(topdhn, timeline):
    day = timeline[0]
    datetime_object = datetime.strptime(day, '%Y-%m-%d')
    weekday = datetime_object.weekday()
    tophn = topdhn[weekday]
    prediction, num_sig = predict_tophn(tophn, timeline)
    return prediction


def getContinuousTimelines(timelines):
    clusters = set()
    continuous_lst = []
    for timeline in timelines:
        for i in range(0, 24):
            cl = timeline[1].get(i, [])
            for clId in cl:
                if clId != -2:
                    clusters.add(clId)

            if len(set(cl)) == 0:
                continuous_lst.append(-2)
            elif len(set(cl)) == 1:
                continuous_lst.append(cl[0])
            elif len(set(cl)) > 1:
                found = None
                for clId in set(cl):
                    if clId != -1:
                        continuous_lst.append(clId)
                        found = True
                        break
                if not found:
                    continuous_lst.append(-1)

    for i in range(len(continuous_lst)):
        continuous_lst[i] += 1

    num_clusters = len(clusters)
    return continuous_lst, num_clusters


def getContinuousTimeline(timeline):
    continuous_timeline = []
    for i in range(0, 24):
        cl = timeline[1].get(i, [])
        if len(set(cl)) == 0:
            continuous_timeline.append(-2)
        elif len(set(cl)) == 1:
            continuous_timeline.append(cl[0])
        elif len(set(cl)) > 1:
            found = None
            for clId in set(cl):
                if clId != -1:
                    continuous_timeline.append(clId)
                    found = True
                    break
            if not found:
                continuous_timeline.append(-1)

    for i in range(len(continuous_timeline)):
        continuous_timeline[i] += 1
    return continuous_timeline


def getPredictable(timeline):

    predictable = 0
    for k, v in timeline[1].items():
        if not isinstance(k, int):
            continue
        if len(v) == 1 and -2 not in v:
            predictable += 1
        elif len(v) > 1:
            st = set(v)
            if -2 in st:
                st.remove(-2)

            if len(st) > 0:
                predictable += 1

    return predictable


def getPoints(directory, min_hours=12):
    path = os.path.join(directory, "Trajectory")
    trajectories = getTrajectories(path)

    points = []
    for trajectory in trajectories:
        t_points = getTrajectoryPoints(trajectory)
        points.extend(t_points)

    by_date = defaultdict(set)
    for point in points:
        by_date[point["date"]].add(point["hour"])

    days = set()
    for k, v in by_date.items():
        if len(v) >= 12:
            days.add(k)

    points_by_date = defaultdict(list)
    points = sorted(points, key=lambda x: x["timestamp"])
    for point in points:
        if point["date"] in days:
            points_by_date[point["date"]].append(point)

    points_by_date = sorted(points_by_date.items(), key=lambda x: x[0])

   # print(directory, f"num_long_traj: {len(days)}")
    return points_by_date


if __name__ == "__main__":

    user_path = "/home/tomas/git/smap/data/geolife/128"
    user_points = getPoints(user_path)

    time_thresh = 60*30
    staypoint_dist = 200
    min_hours = 24
    dbscan_distance = 200

    topn_total = 0
    topn_sig_total = 0
    topwn_total = 0
    topdn_total = 0
    tophn_total = 0
    topwhn_total = 0
    topwhn_sig_total = 0
    topdhn_total = 0
    simple_nn_total = 0
    predictable_total = 0

    training_staypoints = []
    predicting_points = None

    pp = 0
    for day, predicting_points in user_points:
        pp += 1

        if len(training_staypoints) == 0:
            training_staypoints.extend(get_staypoints(predicting_points, time_thresh=time_thresh, dist_thresh=staypoint_dist))
            continue

        stime = time()
        predicting_staypoints = get_staypoints(predicting_points, time_thresh=time_thresh, dist_thresh=staypoint_dist)
        #print(f"staypoints duration: {time() - stime}")
        #print(f"Day: {pp} predicting staypoints: {len(predicting_staypoints)} num train staypoints: {len(training_staypoints)}")
        
        stime = time()
        get_clusters(training_staypoints, meters=dbscan_distance, min_samples=min_hours)
        label_clusters(training_staypoints, predicting_staypoints, meters=dbscan_distance)
        #print(f"DBSCAN duration: {time() - stime}")

        #print(predicting_staypoints)
        training_timelines = getTimelines(training_staypoints)
        #print(training_timelines)
        predicting_timeline = getTimelines(predicting_staypoints)

        training_staypoints.extend(predicting_staypoints)

        #print(predicting_staypoints)
        if not predicting_timeline:
            continue
        else:
            predicting_timeline = predicting_timeline[0]

        predictable = getPredictable(predicting_timeline)
        if not predictable:
            continue

        topn = topN(training_timelines)
        topwn = topWN(training_timelines)
        topdn = topDN(training_timelines)
        tophn = topHN(training_timelines)
        topwhn = topWHN(training_timelines)
        topdhn = topDHN(training_timelines)

        topn_result, topn_sig = predict_topn(topn, predicting_timeline)
        topwn_result = predict_topwn(topwn, predicting_timeline)
        topdn_result = predict_topdn(topdn, predicting_timeline)
        tophn_result, tophn_sig = predict_tophn(tophn, predicting_timeline)
        topwhn_result, topwhn_sig = predict_topwhn(topwhn, predicting_timeline)
        topdhn_result = predict_topdhn(topdhn, predicting_timeline)

        topn_total += topn_result
        topn_sig_total += topn_sig
        topwn_total += topwn_result
        topdn_total += topdn_result
        tophn_total += tophn_result
        topwhn_total += topwhn_result
        topwhn_sig_total += topwhn_sig
        topdhn_total += topdhn_result
        predictable_total += predictable
        print(f"day: {day} topn: {topn_result} topwn: {topwn_result} topdn: {topdn_result} tophn: {tophn_result} topwhn: {topwhn_result}  topdhn: {topdhn_result} predictable: {predictable}")
    print(f"topn: {topn_total} topwn: {topwn_total} topdn: {topdn_total} tophn: {tophn_total} topwhn: {topwhn_total} topdhn: {topdhn_total} simple_nn: {simple_nn_total} predictable_total: {predictable_total}")

    results = {
        "topn": topn_total,
        "topn_sig": topn_sig_total,
        "topwn": topwn_total,
        "topdn": topdn_total,
        "tophn": tophn_total,
        "topwhn": topwhn_total,
        "topwhn_sig": topwhn_sig_total,
        "topdhn": topdhn_total,
        "predictable": predictable_total
    }

    with open("results/128.json", "w") as f:
        f.write(json.dumps(results))


    #        training_points = sorted(training_points, key=lambda x: x["timestamp"])
    #        predicting_points = sorted(predicting_points, key=lambda x: x["timestamp"])

    #with open(INPUT_FILE, "r") as f:
    #    data = json.load(f)

    #current_user = None
    #for user, trajectories in data.items():
    #    user = user.split("/")[-1]
    #    current_user = user

    #    #if user == "003":
    #    #    paths = trajectories
    #    #    current_user = user
    #    #    break

    #    trajectory_dates = []

    #    for trajectory in trajectories:
    #        date = trajectory.split("/")[-1][:8]
    #        year = date[:4]
    #        month = date[4:6]
    #        day = date[6:8]
    #        date = year + "-" + month + "-" + day
    #        datetime_object = datetime.strptime(date, '%Y-%m-%d')
    #        timestamp = datetime_object.timestamp()
    #        trajectory_dates.append((date, timestamp, trajectory))

    #    trajectory_dates.sort(key=lambda x: x[1])

    #    processing = set()

    #    paths = []
    #    processing_points = []
    #    previous_timelines = None
    #    topn = None

    #    topn_total = 0
    #    topwn_total = 0
    #    topdn_total = 0
    #    tophn_total = 0
    #    topwhn_total = 0
    #    topdhn_total = 0
    #    simple_nn_total = 0
    #    predictable_total = 0

    #    training_paths = []

    #    processed_paths = set()
    #    training_dates = set()
    #    predicting_date = None
    #    #training_points = []
    #    training_staypoints = []

    #    staypoint_dist = 100
    #    time_thresh = 60*15
    #    dbscan_distance = 150
    #    min_hours = 24

    #    i = 0
    #    while len(processed_paths) != len(trajectory_dates):
    #        print()
    #        print(f"{len(processed_paths)}/{len(trajectory_dates)}")
    #        for trajectory in trajectory_dates:
    #            if trajectory[0] not in processing:
    #                processing.add(trajectory[0])
    #                predicting_date = trajectory[0]
    #                break
    #            else:
    #                training_dates.add(trajectory[0])

    #        # print(f"training_dates: {training_dates}")
    #        # print(f"predicting_date: {predicting_date}")

    #        training_paths = []
    #        predicting_paths = []
    #        for trajectory in trajectory_dates:
    #            if trajectory[0] in training_dates:
    #                training_paths.append(trajectory[2])
    #            elif trajectory[0] == predicting_date:
    #                predicting_paths.append(trajectory[2])

    #        # print(f"training_paths: {training_paths}")
    #        # print(f"predicting_paths: {predicting_paths}")

    #        training_points = []
    #        for path in training_paths:
    #            if path not in processed_paths:
    #                points = getTrajectoryPoints(path)
    #                training_points.extend(points)
    #                processed_paths.add(path)

    #        predicting_points = []
    #        for path in predicting_paths:
    #            points = getTrajectoryPoints(path)
    #            predicting_points.extend(points)

    #        training_points = sorted(training_points, key=lambda x: x["timestamp"])
    #        predicting_points = sorted(predicting_points, key=lambda x: x["timestamp"])

    #        stime = time()
    #        training_staypoints.extend(get_staypoints(training_points, time_thresh=time_thresh, dist_thresh=staypoint_dist))
    #        predicting_staypoints = get_staypoints(predicting_points, time_thresh=time_thresh, dist_thresh=staypoint_dist)
    #        print(f"staypoints duration: {time() - stime}")

    #        stime = time()
    #        get_clusters(training_staypoints, meters=dbscan_distance, min_samples=min_hours)
    #        label_clusters(training_staypoints, predicting_staypoints, meters=dbscan_distance)
    #        print(f"DBSCAN duration: {time() - stime}")

    #        num_cores = 0
    #        for staypoint in training_staypoints:
    #            if staypoint.get("core"):
    #                num_cores += 1

    #        num_labeled = 0
    #        for staypoint in predicting_staypoints:
    #            if staypoint["cluster_id"] != -2:
    #                num_labeled += 1


    #        # print(f"cores: {num_cores}")
    #        # print(f"labeled: {num_labeled}")

    #        training_timelines = getTimelines(training_staypoints)
    #        predicting_timeline = getTimelines(predicting_staypoints)

    #        # print(f"training_timelines: {training_timelines}")
    #        # print(f"predicting_timeline: {predicting_timeline}")

    #        if not predicting_timeline:
    #            continue
    #        else:
    #            predicting_timeline = predicting_timeline[0]

    #        predictable = getPredictable(predicting_timeline)
    #        predictable_total += predictable

    #        if not predictable:
    #            continue

    #        # TODO predictable may not be 0
    #        if not training_timelines:
    #            continue

    #        topn = topN(training_timelines)
    #        topwn = topWN(training_timelines)
    #        topdn = topDN(training_timelines)
    #        tophn = topHN(training_timelines)
    #        topwhn = topWHN(training_timelines)
    #        topdhn = topDHN(training_timelines)

    #        #continuous_timelines, num_clusters = getContinuousTimelines(training_timelines)
    #        #c_predicting_timeline = getContinuousTimeline(predicting_timeline)

    #        #back = 5
    #        #ahead = 1
    #        #model_simple_nn, train_data = simple_nn(continuous_timelines, num_clusters, back)
    #        #result_simple_nn = predict_simple_nn(model_simple_nn, train_data, c_predicting_timeline, num_clusters, back, ahead)
    #        #simple_nn_total += result_simple_nn


    #        topn_result = predict_topn(topn, predicting_timeline)
    #        topwn_result = predict_topwn(topwn, predicting_timeline)
    #        topdn_result = predict_topdn(topdn, predicting_timeline)
    #        tophn_result = predict_tophn(tophn, predicting_timeline)
    #        topwhn_result = predict_topwhn(topwhn, predicting_timeline)
    #        topdhn_result = predict_topdhn(topdhn, predicting_timeline)

    #        topn_total += topn_result
    #        topwn_total += topwn_result
    #        topdn_total += topdn_result
    #        tophn_total += tophn_result
    #        topwhn_total += topwhn_result
    #        topdhn_total += topdhn_result

    #        predictable = getPredictable(predicting_timeline)
    #        predictable_total += predictable
    #        print(f"topn: {topn_result} topwn: {topwn_result} topdn: {topdn_result} tophn: {tophn_result} topwhn: {topwhn_result}  topdhn: {topdhn_result} predictable: {predictable}")
    #    print(f"topn: {topn_total} topwn: {topwn_total} topdn: {topdn_total} tophn: {tophn_total} topwhn: {topwhn_total} topdhn: {topdhn_total} simple_nn: {simple_nn_total} predictable_total: {predictable_total}")

    #    results = {
    #        "topn": topn_total,
    #        "topwn": topwn_total,
    #        "topdn": topdn_total,
    #        "tophn": tophn_total,
    #        "topwhn": topwhn_total,
    #        "topdhn": topdhn_total,
    #        "predictable": predictable_total
    #    }

    #    directory = f"results_{staypoint_dist}_{time_thresh/60}_{dbscan_distance}_{min_hours}"
    #    if not os.path.exists(directory):
    #        os.makedirs(directory)

    #    path = os.path.join(directory, f"{current_user}.json")
    #    with open(path, "w") as f:
    #        f.write(json.dumps(results))
