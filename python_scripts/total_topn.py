import os
import json

from datetime import datetime
from collections import defaultdict

INPUT_FILE = "/home/tomas/git/smap/python_scripts/users_data/timelines.json"
SAVE_FILE = "/home/tomas/git/smap/python_scripts/users_data/total_topn.json"


def getTotalTopN(timelines):

    topN = dict()
    for i in range(24):
        topN.setdefault(str(i), defaultdict(int))

    for timeline in timelines:

        timeline = timeline[1]
        for hour, cluster_ids in timeline.items():

            if not hour.isdigit():
                continue

            for cluster_id in cluster_ids:
                topN[hour][cluster_id] += 1

    for hour, counts in topN.items():
        hour_counts = 0
        for cluster_id, cl_count in counts.items():
            hour_counts += cl_count
        for cluster_id, cl_count in counts.items():
            counts[cluster_id] = cl_count / hour_counts

    return topN


if __name__ == "__main__":

    with open(INPUT_FILE, "r") as f:
        timelines = json.load(f)

        topNs = dict()
        for user, user_timelines in timelines.items():
            topN = getTotalTopN(user_timelines)
            topNs[user] = topN

    with open(SAVE_FILE, "w") as f:
        f.write(json.dumps(topNs))
