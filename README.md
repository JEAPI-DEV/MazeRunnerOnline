# Game Engine rewrite of the SE Pr.

This is a rewrite of the FHMaze Engine written from scratch to support the map format and game mechanics.
It is online instead of local, and supports multiplayer games, is faster since it does not have the overhead of a 
generalized game engine.  
Tbh. the server part got larger than the actual game engine which is scary, and I still hate you js. 
The main inspiration was to allow me to write and train a model to make it play by itself, but timeouts were a problem, 
for that reason I gave up the idea. An older version of the runner exists on the MazeCreator repo under the second branch,
that does not run on a vps. 
I'm thinking of maybe using a local client and server combination so that the jar files are run on the users machines instead
of on the server, and simply send move commands, this is not optimal since it causes more compute and delay but makes the
whole thing much more saver. Please don't use this software expecting it to be secure, it's not, it uses the Security manager,
tls if you want for client server communication but that's basically it.

## How to compile

````bash
mvn clean install
````

## How to run
````bash
java -jar MazeRunner-0_5.jar --server
````

## To configure 

Since nobody is going to run this anyway I'm not going to write much more documentation. 
But you can configure some things in server.properties like ssl and address and port.

