package org.emulinker.kaillera.controller.v086.action;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.exception.GameDataException;
import org.emulinker.util.*;

public class CachedGameDataAction implements V086Action
{
	private static Log					log			= LogFactory.getLog(CachedGameDataAction.class);
	private static final String			desc		= "CachedGameDataAction";
	private static CachedGameDataAction	singleton	= new CachedGameDataAction();

	private static Log				keyLog	= LogFactory.getLog("KEYLOG");

	public static CachedGameDataAction getInstance()
	{
		return singleton;
	}

	private int	actionCount	= 0;

	private CachedGameDataAction()
	{

	}

	public int getActionPerformedCount()
	{
		return actionCount;
	}

	public String toString()
	{
		return desc;
	}

	public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler) throws FatalActionException
	{
		//		actionCount++;

		try
		{
			int key = ((CachedGameData) message).getKey();
			byte[] data = clientHandler.getClientGameDataCache().get(key);
//			keyLog.debug(clientHandler.getUser() + " RCV CACH: oldkey=" + key + " data=" + EmuUtil.bytesToHex(data));
			clientHandler.getUser().addGameData(data);
			//clientHandler.getUser().addGameData(clientHandler.getClientGameDataCache().get(((CachedGameData) message).getKey()));
		}
		catch (GameDataException e)
		{
			log.debug("Game data error: " + e.getMessage());
			
			if(e.hasResponse())
			{
				try
				{
					clientHandler.send(new GameData(clientHandler.getNextMessageNumber(), e.getResponse()));
				}
				catch (MessageFormatException e2)
				{
					log.error("Failed to contruct GameData message: " + e2.getMessage(), e2);
				}
			}

			/*
			if (e.getReflectData())
			{
				try
				{
					clientHandler.send(new CachedGameData(clientHandler.getNextMessageNumber(), ((CachedGameData) message).getKey()));
				}
				catch (MessageFormatException e2)
				{
					log.error("Failed to contruct CachedGameData message: " + e2.getMessage(), e2);
				}
			}
*/
			/*			
			 // This may not always be the best thing to do...
			 try
			 {
			 clientHandler.send(new GameChat_Notification(clientHandler.getNextMessageNumber(), "Error", e.getMessage()));
			 }
			 catch (MessageFormatException e2)
			 {
			 log.error("Failed to contruct new GameChat_Notification", e);
			 }
			 */
		}
		catch (IndexOutOfBoundsException e)
		{
			log.error("Game data error!  The client cached key " + ((CachedGameData) message).getKey() + " was not found in the cache!", e);

			// This may not always be the best thing to do...
			try
			{
				clientHandler.send(new GameChat_Notification(clientHandler.getNextMessageNumber(), "Error", "Game Data Error!  Game state will be inconsistent!"));
			}
			catch (MessageFormatException e2)
			{
				log.error("Failed to contruct new GameChat_Notification", e);
			}
		}
	}
}
