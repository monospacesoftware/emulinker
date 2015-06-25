package org.emulinker.kaillera.controller.v086.action;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.ActionException;
import org.emulinker.util.EmuLang;

public class ChatAction implements V086Action, V086ServerEventHandler
{
	public static final String	ADMIN_COMMAND_ESCAPE_STRING	= "/";

	private static Log			log							= LogFactory.getLog(ChatAction.class);
	private static final String	desc						= "ChatAction";
	private static ChatAction	singleton					= new ChatAction();

	public static ChatAction getInstance()
	{
		return singleton;
	}

	private int	actionCount		= 0;
	private int	handledCount	= 0;

	private ChatAction()
	{

	}

	public int getActionPerformedCount()
	{
		return actionCount;
	}

	public int getHandledEventCount()
	{
		return handledCount;
	}

	public String toString()
	{
		return desc;
	}

	public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler) throws FatalActionException
	{
		if(!(message instanceof Chat_Request))
			throw new FatalActionException("Received incorrect instance of Chat: " + message);
		
		if (((Chat) message).getMessage().startsWith(ADMIN_COMMAND_ESCAPE_STRING))
		{
			try
			{
				AdminCommandAction.getInstance().performAction(message, clientHandler);
				return;
			}
			catch (FatalActionException e)
			{
				log.warn("Admin command failed, processing as chat: " + e.getMessage());
			}
		}

		actionCount++;

		try
		{
			clientHandler.getUser().chat(((Chat) message).getMessage());
		}
		catch (ActionException e)
		{
			log.info("Chat Denied: " + clientHandler.getUser() + ": " + ((Chat) message).getMessage());

			try
			{
				clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", EmuLang.getString("ChatAction.ChatDenied", e.getMessage())));
			}
			catch (MessageFormatException e2)
			{
				log.error("Failed to contruct InformationMessage message: " + e.getMessage(), e);
			}
		}
	}

	public void handleEvent(ServerEvent event, V086Controller.V086ClientHandler clientHandler)
	{
		handledCount++;

		try
		{
			ChatEvent chatEvent = (ChatEvent) event;
			clientHandler.send(new Chat_Notification(clientHandler.getNextMessageNumber(), chatEvent.getUser().getName(), chatEvent.getMessage()));
		}
		catch (MessageFormatException e)
		{
			log.error("Failed to contruct Chat_Notification message: " + e.getMessage(), e);
		}
	}
}
