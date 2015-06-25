package org.emulinker.kaillera.model.impl;

import java.util.*;
import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.master.StatsCollector;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.*;
import org.emulinker.util.*;

public final class KailleraGameImpl implements KailleraGame
{
	private static Log	log							= LogFactory.getLog(KailleraGameImpl.class);

	private int						id;
	private String					romName;
	private String					toString;
	private Date					startDate;
	
	private int						bufferSize;
	private int 					timeoutMillis;
	private int						desynchTimeouts;

	private KailleraServerImpl		server;
	private KailleraUserImpl		owner;
	private List<KailleraUserImpl>	players				= new CopyOnWriteArrayList<KailleraUserImpl>();
	private StatsCollector			statsCollector;

	private List<Integer>			kickedUsers			= new ArrayList<Integer>();

	private int						status				= KailleraGame.STATUS_WAITING;
	private boolean					synched				= false;
	private int						actionsPerMessage;
	private PlayerActionQueue[]		playerActionQueues;
	private AutoFireDetector		autoFireDetector;
	
	public KailleraGameImpl(int gameID, String romName, KailleraUserImpl owner, KailleraServerImpl server, int bufferSize, int timeoutMillis, int desynchTimeouts)
	{
		this.id = gameID;
		this.romName = romName;
		this.owner = owner;
		this.server = server;
		this.actionsPerMessage = owner.getConnectionType();
		this.bufferSize = bufferSize;
		this.timeoutMillis = timeoutMillis;
		this.desynchTimeouts = desynchTimeouts;
		
		toString = "Game" + id + "(" + (romName.length() > 15 ? (romName.substring(0, 15) + "...") : romName) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		startDate = new Date();

		statsCollector = server.getStatsCollector();
		autoFireDetector = server.getAutoFireDetector(this);
	}

	public int getID()
	{
		return id;
	}

	public String getRomName()
	{
		return romName;
	}
	
	public Date getStartDate()
	{
		return startDate;
	}

	public KailleraUser getOwner()
	{
		return owner;
	}

	public int getPlayerNumber(KailleraUser user)
	{
		return (players.indexOf(user) + 1);
	}

	public KailleraUser getPlayer(int playerNumber)
	{
		if (playerNumber > players.size())
		{
			log.error(this + ": getPlayer(" + playerNumber + ") failed! (size = " + players.size() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return null;
		}

		return players.get((playerNumber - 1));
	}

	public int getNumPlayers()
	{
		return players.size();
	}

	public List<KailleraUserImpl> getPlayers()
	{
		return players;
	}

	public int getStatus()
	{
		return status;
	}

	public boolean isSynched()
	{
		return synched;
	}

	public KailleraServerImpl getServer()
	{
		return server;
	}

	void setStatus(int status)
	{
		this.status = status;
		server.addEvent(new GameStatusChangedEvent(server, this));
	}

	public String getClientType()
	{
		return getOwner().getClientType();
	}

	public String toString()
	{
		return toString;
	}

	public String toDetailedString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("KailleraGame[id="); //$NON-NLS-1$
		sb.append(getID());
		sb.append(" romName="); //$NON-NLS-1$
		sb.append(getRomName());
		sb.append(" owner="); //$NON-NLS-1$
		sb.append(getOwner());
		sb.append(" numPlayers="); //$NON-NLS-1$
		sb.append(getNumPlayers());
		sb.append(" status="); //$NON-NLS-1$
		sb.append(KailleraGame.STATUS_NAMES[getStatus()]);
		sb.append("]"); //$NON-NLS-1$
		return sb.toString();
	}

	int getPlayingCount()
	{
		int count = 0;
		for (KailleraUserImpl player : players)
		{
			if (player.getStatus() == KailleraUser.STATUS_PLAYING)
				count++;
		}

		return count;
	}

	int getSynchedCount()
	{
		if (playerActionQueues == null)
			return 0;

		int count = 0;
		for (int i = 0; i < playerActionQueues.length; i++)
		{
			if (playerActionQueues[i].isSynched())
				count++;
		}

		return count;

		// return dataQueues.size();
		//		return readyCount;
	}

	void addEvent(GameEvent event)
	{
		for (KailleraUserImpl player : players)
			player.addEvent(event);
	}

	public AutoFireDetector getAutoFireDetector()
	{
		return autoFireDetector;
	}

	public synchronized void chat(KailleraUser user, String message) throws GameChatException
	{
		if (!players.contains(user))
		{
			log.warn(user + " game chat denied: not in " + this); //$NON-NLS-1$
			throw new GameChatException(EmuLang.getString("KailleraGameImpl.GameChatErrorNotInGame")); //$NON-NLS-1$
		}

		log.info(user + ", " + this + " gamechat: " + message); //$NON-NLS-1$ //$NON-NLS-2$
		addEvent(new GameChatEvent(this, user, message));
	}

	public synchronized void announce(String announcement)
	{
		addEvent(new GameInfoEvent(this, announcement));
	}

	public synchronized void kick(KailleraUser user, int userID) throws GameKickException
	{
		if (!user.equals(getOwner()))
		{
			log.warn(user + " kick denied: not the owner of " + this); //$NON-NLS-1$
			throw new GameKickException(EmuLang.getString("KailleraGameImpl.GameKickDeniedNotGameOwner")); //$NON-NLS-1$
		}

		if (user.getID() == userID)
		{
			log.warn(user + " kick denied: attempt to kick self"); //$NON-NLS-1$
			throw new GameKickException(EmuLang.getString("KailleraGameImpl.GameKickDeniedCannotKickSelf")); //$NON-NLS-1$
		}

		for (KailleraUserImpl player : players)
		{
			if (player.getID() == userID)
			{
				try
				{
					log.info(user + " kicked: " + userID + " from " + this); //$NON-NLS-1$ //$NON-NLS-2$
					kickedUsers.add(userID);
					player.quitGame();
					return;
				}
				catch (Exception e)
				{
					// this shouldn't happen
					log.error("Caught exception while making user quit game! This shouldn't happen!", e); //$NON-NLS-1$
				}
			}
		}

		log.warn(user + " kick failed: user " + userID + " not found in: " + this); //$NON-NLS-1$ //$NON-NLS-2$
		throw new GameKickException(EmuLang.getString("KailleraGameImpl.GameKickErrorUserNotFound")); //$NON-NLS-1$
	}

	public synchronized int join(KailleraUser user) throws JoinGameException
	{
		if (players.contains(user))
		{
			log.warn(user + " join game denied: already in " + this); //$NON-NLS-1$
			throw new JoinGameException(EmuLang.getString("KailleraGameImpl.JoinGameErrorAlreadyInGame")); //$NON-NLS-1$
		}

		int access = server.getAccessManager().getAccess(user.getSocketAddress().getAddress());

		if (access == AccessManager.ACCESS_NORMAL && kickedUsers.contains(user.getID()))
		{
			log.warn(user + " join game denied: previously kicked: " + this); //$NON-NLS-1$
			throw new JoinGameException(EmuLang.getString("KailleraGameImpl.JoinGameDeniedPreviouslyKicked")); //$NON-NLS-1$
		}

		if (access == AccessManager.ACCESS_NORMAL && getStatus() != KailleraGame.STATUS_WAITING)
		{
			log.warn(user + " join game denied: attempt to join game in progress: " + this); //$NON-NLS-1$
			throw new JoinGameException(EmuLang.getString("KailleraGameImpl.JoinGameDeniedGameIsInProgress")); //$NON-NLS-1$
		}

		players.add((KailleraUserImpl) user);
		server.addEvent(new GameStatusChangedEvent(server, this));

		log.info(user + " joined: " + this); //$NON-NLS-1$
		addEvent(new UserJoinedGameEvent(this, user));

		if(user.equals(owner))
		{
			if(autoFireDetector != null)
			{
				if(autoFireDetector.getSensitivity() > 0)
				{
					announce(EmuLang.getString("KailleraGameImpl.AutofireDetectionOn")); //$NON-NLS-1$
					announce(EmuLang.getString("KailleraGameImpl.AutofireCurrentSensitivity", autoFireDetector.getSensitivity())); //$NON-NLS-1$
				}
				else
				{
					announce(EmuLang.getString("KailleraGameImpl.AutofireDetectionOff")); //$NON-NLS-1$
				}
				announce(EmuLang.getString("KailleraGameImpl.GameHelp")); //$NON-NLS-1$
			}
		}

		return (players.indexOf(user) + 1);
	}

	public synchronized void start(KailleraUser user) throws StartGameException
	{
		if (!user.equals(getOwner()))
		{
			log.warn(user + " start game denied: not the owner of " + this); //$NON-NLS-1$
			throw new StartGameException(EmuLang.getString("KailleraGameImpl.StartGameDeniedOnlyOwnerMayStart")); //$NON-NLS-1$
		}

		if (status == KailleraGame.STATUS_SYNCHRONIZING)
		{
			log.warn(user + " start game failed: " + this + " status is " + KailleraGame.STATUS_NAMES[status]); //$NON-NLS-1$ //$NON-NLS-2$
			throw new StartGameException(EmuLang.getString("KailleraGameImpl.StartGameErrorSynchronizing")); //$NON-NLS-1$
		}
		else if (status == KailleraGame.STATUS_PLAYING)
		{
			log.warn(user + " start game failed: " + this + " status is " + KailleraGame.STATUS_NAMES[status]); //$NON-NLS-1$ //$NON-NLS-2$
			throw new StartGameException(EmuLang.getString("KailleraGameImpl.StartGameErrorStatusIsPlaying")); //$NON-NLS-1$
		}

		int access = server.getAccessManager().getAccess(user.getSocketAddress().getAddress());
		if (access == AccessManager.ACCESS_NORMAL && getNumPlayers() < 2 && !server.getAllowSinglePlayer())
		{
			log.warn(user + " start game denied: " + this + " needs at least 2 players"); //$NON-NLS-1$ //$NON-NLS-2$
			throw new StartGameException(EmuLang.getString("KailleraGameImpl.StartGameDeniedSinglePlayerNotAllowed")); //$NON-NLS-1$
		}

		for (KailleraUser player : players)
		{
			if (player.getConnectionType() != owner.getConnectionType())
			{
				log.warn(user + " start game denied: " + this + ": All players must use the same connection type"); //$NON-NLS-1$ //$NON-NLS-2$
				addEvent(new GameInfoEvent(this, EmuLang.getString("KailleraGameImpl.StartGameConnectionTypeMismatchInfo", KailleraUser.CONNECTION_TYPE_NAMES[owner.getConnectionType()]))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				throw new StartGameException(EmuLang.getString("KailleraGameImpl.StartGameDeniedConnectionTypeMismatch")); //$NON-NLS-1$
			}

			if (!player.getClientType().equals(getClientType()))
			{
				log.warn(user + " start game denied: " + this + ": All players must use the same emulator!"); //$NON-NLS-1$ //$NON-NLS-2$
				addEvent(new GameInfoEvent(this, EmuLang.getString("KailleraGameImpl.StartGameEmulatorMismatchInfo", getClientType()))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				throw new StartGameException(EmuLang.getString("KailleraGameImpl.StartGameDeniedEmulatorMismatch")); //$NON-NLS-1$
			}
		}

		log.info(user + " started: " + this); //$NON-NLS-1$
		setStatus(KailleraGame.STATUS_SYNCHRONIZING);

		if(autoFireDetector != null)
			autoFireDetector.start(players.size());

		playerActionQueues = new PlayerActionQueue[players.size()];
		for (int i = 0; i < playerActionQueues.length; i++)
		{
			KailleraUserImpl player = players.get(i);
			int playerNumber = (i + 1);
			playerActionQueues[i] = new PlayerActionQueue(playerNumber, player, getNumPlayers(), bufferSize, timeoutMillis, true);
			//playerActionQueues[i] = new PlayerActionQueue(playerNumber, player, getNumPlayers(), GAME_BUFFER_SIZE, (player.getPing()*3));
			player.setPlayerNumber(playerNumber);
			log.info(this + ": " + player + " is player number " + playerNumber); //$NON-NLS-1$ //$NON-NLS-2$
			
			if(autoFireDetector != null)
				autoFireDetector.addPlayer(player, playerNumber);
		}

		if (statsCollector != null)
			statsCollector.gameStarted(server, this);

		addEvent(new GameStartedEvent(this));
	}

	public synchronized void ready(KailleraUser user, int playerNumber) throws UserReadyException
	{
		if (!players.contains(user))
		{
			log.warn(user + " ready game failed: not in " + this); //$NON-NLS-1$
			throw new UserReadyException(EmuLang.getString("KailleraGameImpl.ReadyGameErrorNotInGame")); //$NON-NLS-1$
		}

		if (status != KailleraGame.STATUS_SYNCHRONIZING)
		{
			log.warn(user + " ready failed: " + this + " status is " + KailleraGame.STATUS_NAMES[status]); //$NON-NLS-1$ //$NON-NLS-2$
			throw new UserReadyException(EmuLang.getString("KailleraGameImpl.ReadyGameErrorIncorrectState")); //$NON-NLS-1$
		}

		if (playerActionQueues == null)
		{
			log.error(user + " ready failed: " + this + " playerActionQueues == null!"); //$NON-NLS-1$ //$NON-NLS-2$
			throw new UserReadyException(EmuLang.getString("KailleraGameImpl.ReadyGameErrorInternalError")); //$NON-NLS-1$
		}

		log.info(user + " (player " + playerNumber + ") is ready to play: " + this); //$NON-NLS-1$ //$NON-NLS-2$
		playerActionQueues[(playerNumber - 1)].setSynched(true);

		if (getSynchedCount() == getNumPlayers())
		{
			log.info(this + " all players are ready: starting..."); //$NON-NLS-1$

			setStatus(KailleraGame.STATUS_PLAYING);
			synched = true;
			addEvent(new AllReadyEvent(this));
		}
	}

	public synchronized void drop(KailleraUser user, int playerNumber) throws DropGameException
	{
		if (!players.contains(user))
		{
			log.warn(user + " drop game failed: not in " + this); //$NON-NLS-1$
			throw new DropGameException(EmuLang.getString("KailleraGameImpl.DropGameErrorNotInGame")); //$NON-NLS-1$
		}

		if (playerActionQueues == null)
		{
			log.error(user + " drop failed: " + this + " playerActionQueues == null!"); //$NON-NLS-1$ //$NON-NLS-2$
			throw new DropGameException(EmuLang.getString("KailleraGameImpl.DropGameErrorInternalError")); //$NON-NLS-1$
		}

		log.info(user + " dropped: " + this); //$NON-NLS-1$
		playerActionQueues[(playerNumber - 1)].setSynched(false);

		if (getSynchedCount() < 2 && synched)
		{
			synched = false;
			for (PlayerActionQueue q : playerActionQueues)
				q.setSynched(false);
			log.info(this + ": game desynched: less than 2 players playing!"); //$NON-NLS-1$
		}

		if(autoFireDetector != null)
			autoFireDetector.stop(playerNumber);

		if (getPlayingCount() == 0)
			setStatus(KailleraGame.STATUS_WAITING);

		addEvent(new UserDroppedGameEvent(this, user, playerNumber));
	}

	public void quit(KailleraUser user, int playerNumber) throws DropGameException, QuitGameException, CloseGameException
	{
		synchronized(this)
		{
			if (!players.remove(user))
			{
				log.warn(user + " quit game failed: not in " + this); //$NON-NLS-1$
				throw new QuitGameException(EmuLang.getString("KailleraGameImpl.QuitGameErrorNotInGame")); //$NON-NLS-1$
			}
	
			log.info(user + " quit: " + this); //$NON-NLS-1$
	
			addEvent(new UserQuitGameEvent(this, user));
		}

		if (user.equals(owner))
			server.closeGame(this, user);
		else
			server.addEvent(new GameStatusChangedEvent(server, this));
	}

	synchronized void close(KailleraUser user) throws CloseGameException
	{
		if (!user.equals(owner))
		{
			log.warn(user + " close game denied: not the owner of " + this); //$NON-NLS-1$
			throw new CloseGameException(EmuLang.getString("KailleraGameImpl.CloseGameErrorNotGameOwner")); //$NON-NLS-1$
		}

		if (synched)
		{
			synched = false;
			for (PlayerActionQueue q : playerActionQueues)
				q.setSynched(false);
			log.info(this + ": game desynched: game closed!"); //$NON-NLS-1$
		}
		

		for (KailleraUserImpl player : players)
			player.setGame(null);

		if(autoFireDetector != null)
			autoFireDetector.stop();
		
		players.clear();
	}

	public synchronized void droppedPacket(KailleraUser user)
	{
		if (!synched)
			return;

		int playerNumber = user.getPlayerNumber();
		if (playerActionQueues != null && playerActionQueues[(playerNumber - 1)].isSynched())
		{
			playerActionQueues[(playerNumber - 1)].setSynched(false);
			log.info(this + ": " + user + ": player desynched: dropped a packet!"); //$NON-NLS-1$ //$NON-NLS-2$
			addEvent(new PlayerDesynchEvent(this, user, EmuLang.getString("KailleraGameImpl.DesynchDetectedDroppedPacket", user.getName()))); //$NON-NLS-1$

			if (getSynchedCount() < 2 && synched)
			{
				synched = false;
				for (PlayerActionQueue q : playerActionQueues)
					q.setSynched(false);
				log.info(this + ": game desynched: less than 2 players synched!"); //$NON-NLS-1$
			}
		}
	}

	public void addData(KailleraUser user, int playerNumber, byte[] data) throws GameDataException
	{
		if(playerActionQueues == null)
			return;
		
		int bytesPerAction = (data.length / actionsPerMessage);
		int timeoutCounter = 0;
		int actionCounter;
		int playerCounter;
		int arraySize = (playerActionQueues.length * actionsPerMessage * bytesPerAction);
		
		if (!synched)
		{
			throw new GameDataException(EmuLang.getString("KailleraGameImpl.DesynchedWarning"), data, actionsPerMessage, playerNumber, playerActionQueues.length); //$NON-NLS-1$
		}
		
		playerActionQueues[(playerNumber - 1)].addActions(data);
		
		if(autoFireDetector != null)
			autoFireDetector.addData(playerNumber, data, bytesPerAction);

		byte[] response = new byte[arraySize];
		for (actionCounter = 0; actionCounter < actionsPerMessage; actionCounter++)
		{
			for (playerCounter = 0; playerCounter < playerActionQueues.length; playerCounter++)
			{
				while (synched)
				{
					try
					{
						playerActionQueues[playerCounter].getAction(playerNumber, response, ((actionCounter * (playerActionQueues.length * bytesPerAction)) + (playerCounter * bytesPerAction)), bytesPerAction);
						break;
					}
					catch (PlayerTimeoutException e)
					{
						e.setTimeoutNumber(++timeoutCounter);
						handleTimeout(e);
					}
				}
			}
		}

		if (!synched)
			throw new GameDataException(EmuLang.getString("KailleraGameImpl.DesynchedWarning"), data, bytesPerAction, playerNumber, playerActionQueues.length); //$NON-NLS-1$
		
		

		((KailleraUserImpl) user).addEvent(new GameDataEvent(this, response));
	}

	// it's very important this method is synchronized
	private synchronized void handleTimeout(PlayerTimeoutException e)
	{
		if (!synched)
			return;

		int playerNumber = e.getPlayerNumber();
		int timeoutNumber = e.getTimeoutNumber();
		PlayerActionQueue playerActionQueue = playerActionQueues[(playerNumber - 1)];

		if (!playerActionQueue.isSynched() || e.equals(playerActionQueue.getLastTimeout()))
			return;

		playerActionQueue.setLastTimeout(e);

		KailleraUser player = e.getPlayer();
		if (timeoutNumber < desynchTimeouts)
		{
			log.info(this + ": " + player + ": Timeout #" + timeoutNumber); //$NON-NLS-1$ //$NON-NLS-2$
			addEvent(new GameTimeoutEvent(this, player, timeoutNumber));
		}
		else
		{
			log.info(this + ": " + player + ": Timeout #" + timeoutNumber); //$NON-NLS-1$ //$NON-NLS-2$
			playerActionQueue.setSynched(false);
			log.info(this + ": " + player + ": player desynched: Lagged!"); //$NON-NLS-1$ //$NON-NLS-2$
			addEvent(new PlayerDesynchEvent(this, player, EmuLang.getString("KailleraGameImpl.DesynchDetectedPlayerLagged", player.getName()))); //$NON-NLS-1$

			if (getSynchedCount() < 2)
			{
				synched = false;
				for (PlayerActionQueue q : playerActionQueues)
					q.setSynched(false);
				log.info(this + ": game desynched: less than 2 players synched!"); //$NON-NLS-1$
			}
		}
	}
}
