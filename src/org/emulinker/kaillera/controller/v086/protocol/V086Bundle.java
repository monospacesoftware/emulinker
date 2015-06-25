package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

public class V086Bundle extends ByteBufferMessage
{
	public static final String	DESC	= "Kaillera v.086 Message Bundle";

	//protected List<V086Message>	messages;
	protected V086Message[]		messages;
	protected int				numToWrite;
	protected int				length	= -1;

	public V086Bundle(V086Message[] messages)
	{
		this(messages, Integer.MAX_VALUE);
	}

	public V086Bundle(V086Message[] messages, int numToWrite)
	{
		this.numToWrite = messages.length;
		if (numToWrite < this.numToWrite)
			this.numToWrite = numToWrite;

		this.messages = messages;
	}

	public String getDescription()
	{
		return DESC;
	}

	public int getNumMessages()
	{
		return numToWrite;
	}

	public V086Message[] getMessages()
	{
		return messages;
	}

	public int getLength()
	{
		if (length == -1)
		{
			for (int i = 0; i < numToWrite; i++)
			{
				if (messages[i] == null)
					break;

				length += messages[i].getLength();
			}
		}
		return length;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(DESC + " (" + numToWrite + " messages) (" + getLength() + " bytes)");
		sb.append(EmuUtil.LB);
		for (int i = 0; i < numToWrite; i++)
		{
			if (messages[i] == null)
				break;

			sb.append("\tMessage " + (i + 1) + ": " + messages[i].toString() + EmuUtil.LB);
		}
		return sb.toString();
	}

	public void writeTo(ByteBuffer buffer)
	{
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		// no real need for unsigned
		//UnsignedUtil.putUnsignedByte(buffer, numToWrite);
		buffer.put((byte) numToWrite);
		for (int i = 0; i < numToWrite; i++)
		{
			if (messages[i] == null)
				break;

			messages[i].writeTo(buffer);
		}
	}

	public static V086Bundle parse(ByteBuffer buffer) throws ParseException, V086BundleFormatException, MessageFormatException
	{
		return parse(buffer, -1);
	}

	public static V086Bundle parse(ByteBuffer buffer, int lastMessageID) throws ParseException, V086BundleFormatException, MessageFormatException
	{
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		if (buffer.limit() < 5)
			throw new V086BundleFormatException("Invalid buffer length: " + buffer.limit());

		// again no real need for unsigned
		//int messageCount = UnsignedUtil.getUnsignedByte(buffer);
		int messageCount = buffer.get();

		if (messageCount <= 0 || messageCount > 32) // what should the max be?
			throw new V086BundleFormatException("Invalid message count: " + messageCount);

		if (buffer.limit() < (1 + (messageCount * 6)))
			throw new V086BundleFormatException("Invalid bundle length: " + buffer.limit());

		//List<V086Message> messages = new ArrayList<V086Message>(3);
		//Stack<V086Message> messages = new Stack<V086Message>();
		//LinkedList<V086Message> messages = new LinkedList<V086Message>();
		V086Message[] messages = new V086Message[messageCount];
		int parsedCount;
		for (parsedCount = 0; parsedCount < messageCount; parsedCount++)
		{
			// removed to increase speed
			//			if (buffer.remaining() < 6)
			//				throw new V086BundleFormatException("Bundle appears truncated, length = " + buffer.limit());

			int messageNumber = UnsignedUtil.getUnsignedShort(buffer);
			// removed to increase speed
			//			if (messageNumber < 0 || messageNumber > 0xFFFF)
			//				throw new MessageFormatException("Invalid message number: " + messageNumber);

			if (messageNumber <= lastMessageID)
			{
				//buffer.position((buffer.position() + messageLength));

				// will break here instead of looking for more messages, should improve speed slightly
				// there shouldn't be any more valid messages anyway if we just found the last ID
				break;
			}

			// no real need for unsigned
			short messageLength = buffer.getShort();
			if (messageLength < 2 || messageLength > buffer.remaining() || messageLength > buffer.limit())
				throw new ParseException("Invalid message length: " + messageLength);

			// messages are purposely added in reverse order here so it's faster to read out in the controller
			//messages.push(V086Message.parse(messageNumber, messageLength, buffer));
			//messages.addFirst(V086Message.parse(messageNumber, messageLength, buffer));
			//messages.add(V086Message.parse(messageNumber, messageLength, buffer));
			messages[parsedCount] = V086Message.parse(messageNumber, messageLength, buffer);
		}

		return new V086Bundle(messages, parsedCount);
	}
}
