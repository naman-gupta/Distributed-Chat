#Distributed Chat with Ordering and Atomicity guarantees

Implementation of Distributed chat application that provides:

1. Atomicity of  message delivery (show the chat message only if everyone has goten it)

2. Causal and total ordering of messages (i.e., causal chains must appear in the order they were sent everywhere and all chat clients must see the same order of messages).


For chat clients to know of each other, multicast on a well known port (say 9999) and listen for replies. The first client to come up will time out and wait for others to join.  When a client wishes to leave, they must inform everyone else via a message so that the atomicity requirements will not be broken. Be careful about ordering leaving and joining messages along with application messages. i.e., when a chat client joins, the order in which others see the join message relative to other chat messages must be the same everywhere.  The same thing when a chat client leaves. If a chat client leaving messages reaches a live chat client, the client must not see anymore chat messages coming from the client who has left. 


Implement the solution in Java and use RMI. There must be one program that takes some command line arguments (such as listening port and client ID) that we can run multiple times (possibly on different machines) to test your program.  You MAY NOT use Java groups or any other group management software.

##Execution and Other Details
Refer to Report.txt 
