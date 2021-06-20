#!/usr/bin/env python3


def printo(iters, ins, outs):
    print("-------------", iters, "-------------")
    for i in range(len(ins)):
        print(i + 1, ":\t", ins[i], "\t\t", outs[i], sep="")
    print()


uses = []
uses.append(set(["m"]))
uses.append(set(["i", "N"]))
uses.append(set(["i"]))
uses.append(set(["t3"]))
uses.append(set(["m", "t3"]))
uses.append(set(["t2"]))
uses.append(set(["A", "t1"]))
uses.append(set(["i"]))
uses.append(set(["N"]))
uses.append(set())
uses.append(set())
uses.append(set())
uses.reverse()
defs = []
defs.append(set())
defs.append(set())
defs.append(set(["i"]))
defs.append(set(["m"]))
defs.append(set())
defs.append(set(["t3"]))
defs.append(set(["t2"]))
defs.append(set(["t1"]))
defs.append(set())
defs.append(set(["i"]))
defs.append(set(["m"]))
defs.append(set(["A", "N"]))
defs.reverse()
succs = []
succs.append([])
succs.append([5, 12])
succs.append([11])
succs.append([10])
succs.append([9, 10])
succs.append([8])
succs.append([7])
succs.append([6])
succs.append([5, 12])
succs.append([4])
succs.append([3])
succs.append([2])
succs.reverse()

ins = []
outs = []
for i in range(12):
    ins.append(set())
    outs.append(set())

in_prime = []
out_prime = []
iteration = 1
while in_prime != ins or out_prime != outs:
    in_prime = [set()] * 12
    out_prime = [set()] * 12
    for i in range(11, -1, -1):
        in_prime[i] = ins[i].copy()
        out_prime[i] = outs[i].copy()
        new_outs = set()
        for j in succs[i]:
            new_outs = new_outs.union(ins[j - 1])
        outs[i] = new_outs
        ins[i] = uses[i].union(outs[i] - defs[i])
    printo(iteration, ins, outs)
    iteration += 1
