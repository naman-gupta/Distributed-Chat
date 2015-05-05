import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ConnectionHandler implements Runnable {

	MulticastSocket socket;
	DatagramPacket incoming, outgoing;
	private final InetAddress group = InetAddress.getByName("225.4.5.6");
	private final int port = 9999;
	private final int REGISTRY_PORT =1098;
	DistributedChat peer= null;
	int myClientId;
	String ipAddr="";

	public ConnectionHandler(DistributedChat peer,int myClientId,String ipAddr) throws IOException {
		this.myClientId=myClientId;
		this.ipAddr=ipAddr;
		this.peer = peer;
		socket = new MulticastSocket(port);
		socket.joinGroup(group);
		outgoing = new DatagramPacket(new byte[1000], 1000, group, port);
		incoming = new DatagramPacket(new byte[65508], 65508);
	}

	@Override
	public void run() {
		try {
			while (!Thread.interrupted()) {
				incoming.setLength(incoming.getData().length);
				socket.receive(incoming);
				String joinMsg = new String(incoming.getData(), 0,incoming.getLength(), "UTF8");
			    String[] st= joinMsg.split(" ");
                if(joinMsg.startsWith("Join") && Integer.parseInt(st[1])!=myClientId)
				{
                	int clientId = Integer.parseInt(st[1]);
					String ip = st[2];
                    
                    System.out.println("Client "+clientId+" Joined");
                    peer.addConnection(clientId, ip);
                    
                    Registry registry = LocateRegistry.getRegistry(ip,REGISTRY_PORT);
                    ChatInterface stub = (ChatInterface)registry.lookup("Peer"+clientId);
                    stub.recieveConnection(myClientId, ipAddr);
				    
				}
			}
		} catch (IOException ex) {
			handleIOException(ex);
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected synchronized void handleIOException(IOException ex) {
		try {
			socket.leaveGroup(group);
            ex.printStackTrace();
		} catch (IOException ignored) {
			ignored.printStackTrace();
		}
		socket.close();
	}

	public synchronized void controlConnect(int clientId) throws IOException {
		String message = "Join " + clientId +" "+ipAddr;
		outgoing.setData(message.getBytes());
		outgoing.setLength(message.getBytes().length);
		socket.send(outgoing);
	}

}