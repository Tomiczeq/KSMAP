import json
import sys
import numpy as np
from datetime import datetime

from staypoints import getTrajectoryPoints
from staypoints import get_staypoints
from significant import get_clusters
from significant import label_clusters
from timelines import getTimelines

from total_topn import topN, topWN, topDN, topHN, topWHN, topDHN

#from neural import simpleNN
#from neural import train_model


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
    if prediction is None:
        return correct

    for i in range(24):
        if prediction in timeline[1].get(i, [-2]):
            correct += 1

    return correct


def predict_topwn(topwn, timeline):
    day = timeline[0]
    datetime_object = datetime.strptime(day, '%Y-%m-%d')
    weekday = datetime_object.weekday()
    if weekday >= 5:
        dayType = "weekend"
    else:
        dayType = "workday"

    topn = topwn[dayType]
    return predict_topn(topn, timeline)


def predict_topdn(topdn, timeline):
    day = timeline[0]
    datetime_object = datetime.strptime(day, '%Y-%m-%d')
    weekday = datetime_object.weekday()
    topn = topdn[weekday]
    return predict_topn(topn, timeline)


def predict_tophn(tophn, timeline):
    correct = 0
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
    return correct


def predict_topwhn(topwhn, timeline):
    day = timeline[0]
    datetime_object = datetime.strptime(day, '%Y-%m-%d')
    weekday = datetime_object.weekday()
    if weekday >= 5:
        dayType = "weekend"
    else:
        dayType = "workday"
    tophn = topwhn[dayType]
    return predict_tophn(tophn, timeline)


def predict_topdhn(topdhn, timeline):
    day = timeline[0]
    datetime_object = datetime.strptime(day, '%Y-%m-%d')
    weekday = datetime_object.weekday()
    tophn = topdhn[weekday]
    return predict_tophn(tophn, timeline)


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


#def simple_nn(continuous_lst, num_clusters, back):
#    ahead = 1
#    train_data = []
#    labels = []
#    for i in range(0, len(continuous_lst) - back - ahead):
#        history = continuous_lst[i:back+i]
#        label = continuous_lst[back + i + ahead]
#
#        array = []
#        for j in range(len(history)):
#            onehot_array = [0 for i in range(num_clusters)]
#            if history[j] != -1:
#                onehot_array[history[j]] = 1
#            array.append(onehot_array)
#        label_lst = [0 for i in range(num_clusters)]
#        label_lst[label] = 1
#        labels.append(label_lst)
#        #prediction_hour = np.zeros(24)
#        #prediction_hour[(i + back + ahead) % 24] = 1
#        ##train_data.append([array, prediction_hour])
#        train_data.append(array)
#
#    train_data = np.array(train_data).reshape(len(train_data), back * num_clusters)
#    labels = np.array(labels).reshape(len(train_data), num_clusters)
#
#    model = simpleNN(10, back, num_clusters)
#    train_model(model, train_data, labels, 1, 5)
#
#    return model, train_data


#def predict_simple_nn(model, train_data, timeline, num_clusters, back, ahead):
#    labels = []
#    for clId in timeline:
#        label_lst = [0 for i in range(num_clusters)]
#
#        if clId != -1:
#            label_lst[clId] = 1
#        labels.append(label_lst)
#
#    merged = []
#    merged.extend(train_data[-1 * num_clusters * (back + ahead - 1)])
#
#    p_timeline = []
#    for i in range(len(timeline)):
#        lst = [0 for j in range(num_clusters)]
#        if timeline[i] != -1:
#            lst[timeline[i]] = 1
#        p_timeline.extend(lst)
#    merged.extend(p_timeline)
#
#    total_correct = 0
#    
#    # print("hah")
#    # print(timeline)
#    for i in range(len(timeline)):
#        if timeline[i] != -1:
#            back_data = merged[i * num_clusters:(back + i) * num_clusters]
#            to_predict = merged[(back + i) * num_clusters:(back + i + 1) * num_clusters]
#            correct = to_predict.index(1)
#            back_data = np.array([back_data]).reshape(1, len(back_data))
#            # print(f"i: {i}")
#            # print(f"back_data: {back_data}")
#            prediction = model.predict(back_data)
#            predicted = np.argmax(prediction[0])
#            # print(f"to_predict: {to_predict}")
#            # print(f"prediction: {prediction}")
#            # print(f"correct: {correct}")
#            # print(f"predicted: {predicted}")
#            if correct == predicted:
#                total_correct += 1
#    return total_correct


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


if __name__ == "__main__":

    with open(INPUT_FILE, "r") as f:
        data = json.load(f)

    current_user = None
    for user, trajectories in data.items():
        user = user.split("/")[-1]
        current_user = user

        #if user == "003":
        #    paths = trajectories
        #    current_user = user
        #    break

        trajectory_dates = []

        for trajectory in trajectories:
            date = trajectory.split("/")[-1][:8]
            year = date[:4]
            month = date[4:6]
            day = date[6:8]
            date = year + "-" + month + "-" + day
            datetime_object = datetime.strptime(date, '%Y-%m-%d')
            timestamp = datetime_object.timestamp()
            trajectory_dates.append((date, timestamp, trajectory))

        trajectory_dates.sort(key=lambda x: x[1])

        processing = set()

        paths = []
        processing_points = []
        previous_timelines = None
        topn = None

        topn_total = 0
        topwn_total = 0
        topdn_total = 0
        tophn_total = 0
        topwhn_total = 0
        topdhn_total = 0
        simple_nn_total = 0
        predictable_total = 0

        training_paths = []

        processed_paths = set()
        training_dates = set()
        predicting_date = None
        training_points = []

        distance = 200
        time_thresh = 60*15
        min_hours = 24

        i = 0
        while len(processed_paths) != len(trajectory_dates):
            print()
            print(f"{len(processed_paths)}/{len(trajectory_dates)}")
            for trajectory in trajectory_dates:
                if trajectory[0] not in processing:
                    processing.add(trajectory[0])
                    predicting_date = trajectory[0]
                    break
                else:
                    training_dates.add(trajectory[0])

            # print(f"training_dates: {training_dates}")
            # print(f"predicting_date: {predicting_date}")

            training_paths = []
            predicting_paths = []
            for trajectory in trajectory_dates:
                if trajectory[0] in training_dates:
                    training_paths.append(trajectory[2])
                elif trajectory[0] == predicting_date:
                    predicting_paths.append(trajectory[2])

            # print(f"training_paths: {training_paths}")
            # print(f"predicting_paths: {predicting_paths}")

            for path in training_paths:
                if path not in processed_paths:
                    points = getTrajectoryPoints(path)
                    training_points.extend(points)
                    processed_paths.add(path)

            predicting_points = []
            for path in predicting_paths:
                points = getTrajectoryPoints(path)
                predicting_points.extend(points)

            training_points = sorted(training_points, key=lambda x: x["timestamp"])
            predicting_points = sorted(predicting_points, key=lambda x: x["timestamp"])

            training_staypoints = get_staypoints(training_points, time_thresh=time_thresh)
            predicting_staypoints = get_staypoints(predicting_points, time_thresh=time_thresh)

            get_clusters(training_staypoints, meters=distance, min_samples=min_hours)
            label_clusters(training_staypoints, predicting_staypoints, meters=distance)

            num_cores = 0
            for staypoint in training_staypoints:
                if staypoint.get("core"):
                    num_cores += 1

            num_labeled = 0
            for staypoint in predicting_staypoints:
                if staypoint["cluster_id"] != -2:
                    num_labeled += 1


            # print(f"cores: {num_cores}")
            # print(f"labeled: {num_labeled}")

            training_timelines = getTimelines(training_staypoints)
            predicting_timeline = getTimelines(predicting_staypoints)

            # print(f"training_timelines: {training_timelines}")
            # print(f"predicting_timeline: {predicting_timeline}")

            if not predicting_timeline:
                continue
            else:
                predicting_timeline = predicting_timeline[0]

            predictable = getPredictable(predicting_timeline)
            predictable_total += predictable

            if not predictable:
                continue

            # TODO predictable may not be 0
            if not training_timelines:
                continue

            topn = topN(training_timelines)
            topwn = topWN(training_timelines)
            topdn = topDN(training_timelines)
            tophn = topHN(training_timelines)
            topwhn = topWHN(training_timelines)
            topdhn = topDHN(training_timelines)

            #continuous_timelines, num_clusters = getContinuousTimelines(training_timelines)
            #c_predicting_timeline = getContinuousTimeline(predicting_timeline)

            #back = 5
            #ahead = 1
            #model_simple_nn, train_data = simple_nn(continuous_timelines, num_clusters, back)
            #result_simple_nn = predict_simple_nn(model_simple_nn, train_data, c_predicting_timeline, num_clusters, back, ahead)
            #simple_nn_total += result_simple_nn


            topn_result = predict_topn(topn, predicting_timeline)
            topwn_result = predict_topwn(topwn, predicting_timeline)
            topdn_result = predict_topdn(topdn, predicting_timeline)
            tophn_result = predict_tophn(tophn, predicting_timeline)
            topwhn_result = predict_topwhn(topwhn, predicting_timeline)
            topdhn_result = predict_topdhn(topdhn, predicting_timeline)

            topn_total += topn_result
            topwn_total += topwn_result
            topdn_total += topdn_result
            tophn_total += tophn_result
            topwhn_total += topwhn_result
            topdhn_total += topdhn_result

            predictable = getPredictable(predicting_timeline)
            predictable_total += predictable
            print(f"topn: {topn_result} topwn: {topwn_result} topdn: {topdn_result} tophn: {tophn_result} topwhn: {topwhn_result}  topdhn: {topdhn_result} predictable: {predictable}")
        print(f"topn: {topn_total} topwn: {topwn_total} topdn: {topdn_total} tophn: {tophn_total} topwhn: {topwhn_total} topdhn: {topdhn_total} simple_nn: {simple_nn_total} predictable_total: {predictable_total}")

        results = {
            "topn": topn_total,
            "topwn": topwn_total,
            "topdn": topdn_total,
            "tophn": tophn_total,
            "topwhn": topwhn_total,
            "topdhn": topdhn_total,
            "predictable": predictable_total
        }

        with open(f"results/{current_user}.json", "w") as f:
            f.write(json.dumps(results))
