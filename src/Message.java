/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package dsassignment2;

/**
 *
 * @author 
 */
import java.io.Serializable;
import java.util.StringTokenizer;

public class Message implements Comparable<Message>, Serializable
{

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int srcId, destId;
    String tag;
    String actualMessage;
    String messageId;
    String causalDep;
    int propSeqno;
    String status;

    public Message()
    {
        srcId = 0;
        destId = 0;
        tag = null;
        actualMessage = null;
        messageId = null;
        causalDep = null;
        propSeqno = 0;
        status = "undeliverable";

    }

    public Message(int s, String msgType, String buf, String msgId)
    {
        srcId = s;
        destId = 0;
        tag = msgType;
        actualMessage = buf;
        messageId = msgId;
        causalDep = null;
        propSeqno = 0;
        status = "undeliverable";
    }

    public Message(int s, int d, String msgType, String buf, String msgId, String causalMid)
    {
        srcId = s;
        destId = d;
        tag = msgType;
        actualMessage = buf;
        messageId = msgId;
        causalDep = causalMid;
        propSeqno = 0;
        status = "undeliverable";
    }

    public void setpropSeqno(int Seqno)
    {
        propSeqno = Seqno;

    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getMid()
    {
        return messageId;
    }

    public int getpropSeqno()
    {
        return propSeqno;
    }

    public String getStatus()
    {
        return status;
    }

    public String getcausalDep()
    {
        return causalDep;
    }

    public int getSrcId()
    {
        return srcId;
    }

    public int getDestId()
    {
        return destId;
    }

    public String getTag()
    {
        return tag;
    }

    public String getMessage()
    {
        return actualMessage;
    }

    public int getMessageInt()
    {
        StringTokenizer st = new StringTokenizer(actualMessage);
        return Integer.parseInt(st.nextToken());
    }

    public String getCausalDependentMsgId()
    {
    	return causalDep;
    }
    
    public void setCausalDependentMsgId(String msgId)
    {
    	causalDep=msgId;
    }
    
    @Override
    public int compareTo(Message t)
    {
        return this.propSeqno - t.propSeqno;

    }

}