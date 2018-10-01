Job-Scheduler
=============
A task scheduling program written in Java that uses JSON responses. 
Optimized for multi-threaded handling of many simultaneous requests. 
This application aims to categorize tasks based on what other tasks each task is dependent on having 
finished before they can be made available. It aims to prioritize tasks that unlock the most other 
tasks first in order to prevent bottlenecks. Thus this is not a FIFO style queing program, but one 
built for overall speed.#

Deploy With Docker
================
* Navigate to the `docker` folder at the top of the source tree.
* Execute `bash build.sh` to build the docker image.
* Deploy the container using the built image with `bash deploy.sh`
    * This will start the scheduler service on port 3901.
    * You can use your own docker deployment script/command to have the service listen on a different port if you wish.

Warnings & Limitations
======================
* This software is developed in a manner that intends to be deployed on a computer/server that has 
at least two cores, or a single core with hyperthreading. Although it may be able to cope with 
running on a single-core computer, that should never be a requirement of the system that restricts 
the software development in any way.

* The software is designed to hold everything in memory rather than using any databases for 
performance reasons. This makes installation easy, but causes loss of data if the program exits 
unexpectedly, such as a due to a reboot. There are plans to extend the application to allow the 
user to send a shutdown request to gracefully stop the scheduler, and a request to export or save 
all the tasks so that it can carry on again at a later point in time.

Reason For Development
======================
I needed an application that would be able to gracefully handle many simultaneous requests for work, 
whilst having greater control over how tasks were orgainzed. This application's primary goal was to 
have a concept of tasks being dependent on other tasks having been completed before being available 
to be executed. Initially this project was written in PHP for ease of development, but was moved to 
Java for the the performance and multi-threaded capabilities. Other languages could have been used, 
but Java has a similar syntax to PHP, and is fairly easy to build multi-threaded applications with. 

Licencing Information
=====================
This project currently uses the Google-gson 2.2.4 library and has been fully included with licencing 
in the libs folder. That part of the project uses Apache 2.0 licence which is why this project uses 
the GPL v3.0 licence which is stated to be compatible. The reason I chose this licence is on the 
understanding that it expects extensions/updates of the software to also be made opensource. 
It appears that opensource licences have become extremely complicated, and dull the joys of 
developing opensource software for others to use.
