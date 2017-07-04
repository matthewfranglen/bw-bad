Supermarkets
============

This is the 'bad' submission for the brandwatch tech test.

I'm releasing this because the tech test this is based on is not used. It will also be pretty obvious if you use this code.

Approach
--------

This works like the good submission, and it gathers data on the events that occur.
It uses these events to determine the random seed of two of the random number generators used by the simulation.

Output
------

This takes some time to run, as events typically happen 1 per second and this can take 600 events to stabilize.
The calculation of the seed is not efficient so that takes a reasonable amount of time too.

To demonstrate that this actually works there is a log of a run.
This can be found in the stdout.log file in the root of this repo.
