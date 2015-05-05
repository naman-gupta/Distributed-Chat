import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;


public interface ChatInterface extends Remote{
	
	public void recieveMessage(Message message, int[][] W) throws RemoteException;
	public void recieveConnection(int clientId,String ipAddr) throws RemoteException;
	public int getProposedSeqNumber(Message receivedMessage) throws RemoteException;
	public void recieveLeaveMessage(Message leaveMsg) throws RemoteException, NotBoundException;
	public void leaveConfirmation() throws RemoteException;
	public void recieveBroadcastMessage3(Message receivedMessage, int largestSequenceNo,int[][] mat) throws RemoteException;

}
