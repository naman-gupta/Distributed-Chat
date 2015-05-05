import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;


public class Helper {
	
	static List<Integer> random;
	
	static
	{
		random = new ArrayList<Integer>();
		for(int i=0;i<100000;i++)
			random.add(i);
		Collections.shuffle(random);
	}
	
	public static int[][] initializeMatrix (int[][] mat,int rows,int cols)
	{
		for(int i=0;i<rows;i++)
		{
			for(int j=0;j<cols;j++)
			{
				mat[i][j]=0;
			}
		}
		
		return mat;
	}
	
	public static int[][] maxMatrix (int[][] mat1,int mat2[][],int rows,int cols)
	{
		for(int i=0;i<rows;i++)
		{
			for(int j=0;j<cols;j++)
			{
				mat1[i][j]=Math.max(mat1[i][j], mat2[i][j]);
			}
		}
		
		return mat1;
	}
	
	public static int getRandomNumber(int pos)
	{
		
		return random.get(pos);
		
	}
	
	 public static int maxArr(int[] propSeqnoArr)
	    {
	        int maxValue = propSeqnoArr[0];
	        for (int i = 1; i < propSeqnoArr.length; i++)
	        {
	            if (propSeqnoArr[i] > maxValue)
	            {
	                maxValue = propSeqnoArr[i];
	            }

	        }

	        return maxValue;

	    }
	 
	 public static String getIPAddress() throws SocketException {
	        
		 NetworkInterface ni = NetworkInterface.getByName("eth0");
		 
		 if(null==ni)
		 {
			 return null;
		 }
	        
	     Enumeration<InetAddress> inetAddresses =  ni.getInetAddresses();

	        String ipAddr="";
	        while(inetAddresses.hasMoreElements()) {
	            InetAddress ia = inetAddresses.nextElement();
	            if(!ia.isLinkLocalAddress()) {
	            	ipAddr = ia.getHostAddress();
	            	break;
	            }
	        }
	        return ipAddr;
	    }
	 
	 public static void main(String[] args) throws SocketException {
		Helper.getIPAddress();
	}

}
