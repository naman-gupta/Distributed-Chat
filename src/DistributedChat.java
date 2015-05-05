import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Scanner;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author anshulcse,naman
 */
public class DistributedChat extends UnicastRemoteObject implements Runnable, ChatInterface
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    // Maximum number of process. 
    private final int NUMPROCESS = 100;

    private final int REGISTRY_PORT = 1098;
    
    //client Id : IP Address
    Map<Integer, String> mapClientIP;
    
    //Queues for maintaining Causal Order
    List<CausalMessage> causalDeliveryQ, causalPendingQ;
    
    //Queues for maintaining Total Order
    List<Message> totalHoldBackQueue, totalDeliveryQueue;
   
    int agreedSeqNo, proposedSeqNo, myClientId;
    
    // For Causal Ordering : Process Matrix
    int[][] M;
    
    //Number of Messages sent by this process.
    int numMsgSent=0,numClientsLeft=0,numClientsConnected=0;
    
    String ipAddr = "";
    
    boolean isControlLeaveInitiated = false;

    
    public DistributedChat(int myClientId,String ipAddr) throws IOException
    {
        this.myClientId = myClientId;
        this.ipAddr = ipAddr;
        
        M = new int[NUMPROCESS][NUMPROCESS];
        
        //Initialize matrix to Zero.
        M = Helper.initializeMatrix(M, NUMPROCESS, NUMPROCESS);
        
        mapClientIP = new HashMap<Integer, String>();
        
        agreedSeqNo = 0;
        
        proposedSeqNo = 0;
        
        totalHoldBackQueue = Collections.synchronizedList(new ArrayList<Message>());
        
        totalDeliveryQueue = Collections.synchronizedList(new ArrayList<Message>());
        
        causalDeliveryQ = Collections.synchronizedList(new LinkedList<CausalMessage>());
        
        causalPendingQ = Collections.synchronizedList(new LinkedList<CausalMessage>());

        register();
    }

    /**
     * Bind the RMI stub to Registry with name as "Peer<ClientId>"
     * 
     * @throws RemoteException
     * @throws MalformedURLException
     */
    public void register() throws RemoteException, MalformedURLException
    {
    //    ChatInterface stub = (ChatInterface) UnicastRemoteObject.exportObject(this, 0);
        //Naming.rebind("Peer" + myClientId, stub);
        Registry registry = LocateRegistry.createRegistry(REGISTRY_PORT);
        registry.rebind("Peer"+myClientId, this); 
    }
    
    public void deRegister() throws NotBoundException, RemoteException
    {
    	
    }

    public void recieveMessage(Message message, int[][] W) throws RemoteException
    {
    	if(message.tag.equalsIgnoreCase("Leaving"))
    	{
    		mapClientIP.remove(message.getSrcId());
    	}
        CausalMessage cm = new CausalMessage(message, NUMPROCESS, W);
        causalPendingQ.add(cm);
    }

    public boolean okToRemove(CausalMessage causalMessage)
    {
        
        int[][] W = causalMessage.getMatrix();
        int srcClientId = causalMessage.getMessage().getSrcId();
       
        if (W[srcClientId][myClientId] > M[srcClientId][myClientId] +1)
        {
            return false;
        }
        
        for (int k = 0; k < NUMPROCESS; k++)
        {
            if (k != srcClientId && W[k][myClientId] > M[k][myClientId])
            {
                return false;
            }
        }

        if(null==causalMessage.m.causalDep)
        {
        	return true;
        }
        
        Iterator<Message> iter = totalDeliveryQueue.iterator();
        while (iter.hasNext())
        {
            Message temp = iter.next();
            if (temp.messageId.equals(causalMessage.m.causalDep))
            {
                return true;
            }
        }

        return false;
    }

    public void checkPendingQ()
    {
        if (!causalPendingQ.isEmpty())
        {
            ListIterator<CausalMessage> iter = causalPendingQ.listIterator(0);
            while (iter.hasNext())
            {
                CausalMessage causalMessage = iter.next();
                if (okToRemove(causalMessage))
                {
                   	causalPendingQ.remove(causalMessage);
                	System.out.println("ReplyTo "+causalMessage.getMessage().getMessage());
                	M=Helper.maxMatrix(causalMessage.getMatrix(),M,NUMPROCESS,NUMPROCESS);
                	totalDeliveryQueue.add(causalMessage.getMessage());
                
                }
            }
        }
    }

    /**
     * Send Message to Client (Unicast Message)
     *
     * @param msg
     * @param recieverClientId
     * @param msgType
     * @throws MalformedURLException
     * @throws RemoteException
     * @throws NotBoundException
     */
    public void sendCausalMessage(String msg, int recieverClientId, String msgType) throws MalformedURLException, RemoteException, NotBoundException
    {
        Message message = null;

        Registry registry = null;
        int s = myClientId;
        int r = recieverClientId;
        M[s][r] = M[s][r] + 1;

        //Message Id Format : ClientId_UniqueRandomNumber
        String messageId = String.valueOf(myClientId) + "_" + Helper.getRandomNumber(numMsgSent);
        numMsgSent++;
        
        String causalMsgDependentId = getLatestSentMsgId();

        message = new Message(myClientId, recieverClientId, "Causal", msg, messageId, causalMsgDependentId);
        
        for (Map.Entry<Integer, String> entry : mapClientIP.entrySet())
        {
            int receiverId = entry.getKey();
            
            //Getting Receivers Registry
            registry = LocateRegistry.getRegistry(entry.getValue(),REGISTRY_PORT);
            
            //Receivers Stub
            ChatInterface receiverStub = (ChatInterface) registry.lookup("Peer" + receiverId);
            
            //Sending Message to Receiver
            receiverStub.recieveMessage(message, M);

        }
    }

    public void sendLeaveMessageToClients(String msg, int recieverClientId, String msgType) throws MalformedURLException, RemoteException, NotBoundException
    {
    	Message message = null;

        String messageId = String.valueOf(myClientId) + "_" + Helper.getRandomNumber(numMsgSent);
        numMsgSent++;
        
        message = new Message(myClientId, "Leaving", msg, messageId);
       
        for (Map.Entry<Integer, String> entry : mapClientIP.entrySet())
        {
        	if(entry.getKey()!=myClientId)
        	{
        		Registry registry = LocateRegistry.getRegistry(entry.getValue(),REGISTRY_PORT);
        		ChatInterface receiverStub = (ChatInterface) registry.lookup("Peer" + entry.getKey());
        		receiverStub.recieveLeaveMessage(message);
        	}
        }

    }
    /**
     * @return Returns the Message Id of the latest message whose status is Delivered.
     */
    public String getLatestSentMsgId()
    {
    	if (!totalDeliveryQueue.isEmpty())
    		return totalDeliveryQueue.get(totalDeliveryQueue.size() - 1).getMid();
    	
    	return null;
    }

    public void sendBroadcastMessage(String msg) throws MalformedURLException, RemoteException, NotBoundException
    {
	int[] propSeqnoArr = new int[NUMPROCESS];
        int i = 0;
        
        
        int finalPropSeqno = 0;

      //  Iterator iterator = mapClientIP.entrySet().iterator();
        String messageId = String.valueOf(myClientId) + "_" + Helper.getRandomNumber(numMsgSent);
        numMsgSent++;

        //-1 here means Broadcast Message
        Message message = new Message(myClientId, -1, "Total", msg, messageId,getLatestSentMsgId());

        for (Map.Entry<Integer, String> entry : mapClientIP.entrySet())
        {
            int receiverId = entry.getKey();
            M[myClientId][receiverId] += 1;
        }

        for (Map.Entry<Integer, String> entry : mapClientIP.entrySet())
        {
            Registry registry = LocateRegistry.getRegistry(entry.getValue(),REGISTRY_PORT);
            ChatInterface receiverStub = (ChatInterface) registry.lookup("Peer" + entry.getKey());
            propSeqnoArr[i++] = receiverStub.getProposedSeqNumber(message);
        }

        finalPropSeqno = Helper.maxArr(propSeqnoArr);

        for (Map.Entry<Integer, String> entry : mapClientIP.entrySet())
        {
        	Registry registry = LocateRegistry.getRegistry(entry.getValue(),REGISTRY_PORT);
            ChatInterface receiverStub = (ChatInterface) registry.lookup("Peer" + entry.getKey());
            receiverStub.recieveBroadcastMessage3(message, finalPropSeqno,M);
        }

    }

    public int getProposedSeqNumber(Message receivedMessage) throws RemoteException
    {
    	
    	proposedSeqNo = Math.max(proposedSeqNo, agreedSeqNo)+1;
    	receivedMessage.propSeqno =  proposedSeqNo;
        totalHoldBackQueue.add(receivedMessage);//new Message(msg, messageId, proposedSeqNo, "undeliverable"));
        return proposedSeqNo;

    }

    public synchronized void recieveBroadcastMessage3(Message receivedMessage, int largestSequenceNo,int[][] mat) throws RemoteException
    {

	for(int i=0;i<NUMPROCESS;i++)
	{
		for(int j=0;j<NUMPROCESS;j++)
		{
			M[i][j] = mat[i][j];
		}
	}

	for (Message temp : totalHoldBackQueue)
    {
	    if (temp.getMid().equals(receivedMessage.getMid()))
        {
           if (temp.getpropSeqno() != largestSequenceNo)
            {
            	temp.propSeqno = largestSequenceNo;
//                    totalHoldBackQueue.get(temp).setpropSeqno(largestSequenceNo);
            }
            temp.setStatus("deliverable");
            totalHoldBackQueue.remove(temp);
            totalHoldBackQueue.add(temp);
            agreedSeqNo = largestSequenceNo;
            break;

        }
    }

	if(!totalHoldBackQueue.isEmpty())
	{
           Collections.sort(totalHoldBackQueue);
	}

    }

    public synchronized void transferTotalMessage()
    {
	
        if (!totalHoldBackQueue.isEmpty() && totalHoldBackQueue.get(0).getStatus().equalsIgnoreCase("deliverable") )
        {
        	Message newMessage = totalHoldBackQueue.get(0);
        	
        	if(totalDeliveryQueue.isEmpty())
        	{
        		totalHoldBackQueue.remove(0);
        		totalDeliveryQueue.add(newMessage);
  	          	System.out.println("Client"+newMessage.getSrcId() +" : "+ newMessage.getMessage());
        	}else
        	{
        		boolean flag=false;
        		for(Message m : totalDeliveryQueue)
        		{	
        			if(null == newMessage.getCausalDependentMsgId() || m.getMid().equals(newMessage.getCausalDependentMsgId()))
        			{
        			  totalHoldBackQueue.remove(0);
        			  System.out.println("Client"+newMessage.getSrcId() +" : "+ newMessage.getMessage());
        			  flag=true;
        	          break;
        			}
        		}
        		if(flag)
        		{
        			totalDeliveryQueue.add(newMessage);
        		}
        	}
        }
   }

   

    public static void main(String[] args) throws NotBoundException, NumberFormatException, IOException
    {
        if (args.length != 1)
        {
            System.out.println("Error -- Usage: java MulticastChat <clientId>");
            System.exit(0);
        }
        
        String command = "";

        String ipAddr= Helper.getIPAddress();
        if(null==ipAddr)
        {
        	System.out.println("IPAddr Not Discoverable");
        	System.exit(0);
        }
       
       
        DistributedChat peer = new DistributedChat(Integer.parseInt(args[0]),ipAddr);
        Thread t = new Thread(peer);
        t.start();
        
        peer.mapClientIP.put(peer.myClientId, ipAddr);
        ConnectionHandler connectionHandler = new ConnectionHandler(peer, peer.myClientId,ipAddr);
        Thread connectionThread = new Thread(connectionHandler);
        connectionThread.start();

        Scanner s = new Scanner(System.in);
        int choice = -1;
        System.out.println("Enter Command \n 1. Control Join \n 2. Control Leave \n 3. Reply to Client \n 4. Reply to Everyone \n Enter your choice");
        while (true)
        {
        	choice = Integer.parseInt(s.nextLine().trim());
            switch (choice)
            {
                case 1:
                    connectionHandler.controlConnect(peer.myClientId);
                    break;
                case 2:
                	peer.deRegister();
                    command = peer.myClientId + " Left";
                    peer.numClientsConnected=peer.mapClientIP.size()-1;
                    if(peer.numClientsConnected==0)
                    {
                    	System.exit(0);
                    }
                    peer.isControlLeaveInitiated= true;
                    peer.sendLeaveMessageToClients(command, 0, "Leaving");
                    break;
                case 3:
                    System.out.println("Enter ClientId & Message (Usage : <ClientId Message>)");
                    command = s.nextLine().trim();
                    int index = command.indexOf(" ");
                    int destClientId = Integer.parseInt(command.substring(0, index));
                    if(!peer.mapClientIP.containsKey(destClientId))
                    {
                    	System.out.println("Destination Client is not Available");
                    	break;
                    }
                    String msg = command.substring(index + 1);
                    peer.sendCausalMessage(msg, destClientId, "Causal");
                    break;
                case 4:
                    System.out.println("Enter Message to Multicast in the group");
                    command = s.nextLine().trim();
                    if(peer.mapClientIP.size()>1)
                    	peer.sendBroadcastMessage(command);
                    else
                    	System.out.println("No Client Connected");
                    break;
                default:
                	System.out.println("Invalid Choice. Enter Between 1-4");
             }
        }
    }

    @Override
    public void run()
    {
        while (true)
        {
            //	System.out.println("Hello");
            checkPendingQ();
            transferTotalMessage();
            if(isControlLeaveInitiated && numClientsConnected==numClientsLeft && numClientsConnected!=0)
            {
            	Registry registry;
				try {
					 registry = LocateRegistry.getRegistry(ipAddr,REGISTRY_PORT);
					 registry.unbind("Peer"+myClientId);
					 System.exit(1);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NotBoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
               
            }
           
        }

    }

    @Override
    public void recieveConnection(int clientId, String ipAddr)
            throws RemoteException
    {
    	
        if(clientId!=myClientId && !mapClientIP.containsKey(clientId))
        {
        	mapClientIP.put(clientId, ipAddr);
        	System.out.println("Client "+clientId+" Available");
        }

    }
    
    public void addConnection(int clientId, String ipAddr) throws RemoteException
    {
    	mapClientIP.put(clientId, ipAddr);
    }

	@Override
	public synchronized void recieveLeaveMessage(Message leaveMsg) throws RemoteException, NotBoundException {
		
		int srcId =  leaveMsg.getSrcId();
		Registry registry = LocateRegistry.getRegistry(mapClientIP.get(srcId),REGISTRY_PORT);
        ChatInterface receiverStub = (ChatInterface) registry.lookup("Peer" + srcId);
        mapClientIP.remove(srcId);
        System.out.println("Client "+srcId+ " has Left");
        receiverStub.leaveConfirmation();
	}
	
	public void leaveConfirmation()
	{
		numClientsLeft++;
	}

}