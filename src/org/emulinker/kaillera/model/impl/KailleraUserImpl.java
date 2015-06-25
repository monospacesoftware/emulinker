package org.emulinker.kaillera.model.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.*;

import org.apache.commons.logging.*;

import org.emulinker.kaillera.access.*;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.*;
import org.emulinker.util.*;

public final class KailleraUserImpl implements KailleraUser, Executable
{
	private static Log						log						= LogFactory.getLog(KailleraUserImpl.class);
	private static final String				EMULINKER_CLIENT_NAME 	= "Emulinker Suprclient";

	private KailleraServerImpl				server;
	private KailleraGameImpl				game;

	private int								id;
	private String							name;
	private String							protocol;
	private String							clientType;
	private byte							connectionType;
	private int								ping;
	private InetSocketAddress				connectSocketAddress;
	private InetSocketAddress				clientSocketAddress;
	private int								status;
	private boolean							loggedIn;
	private String							toString;
	private int								access;
	private boolean							emulinkerClient;

	private long							connectTime;
	private long							lastActivity;
	private long							lastKeepAlive;
	private long							lastChatTime;
	private long							lastCreateGameTime;
	private long							lastTimeout;

	private int								playerNumber		= -1;

	private long							gameDataErrorTime	= -1;

	private boolean							isRunning			= false;
	private boolean							stopFlag			= false;

	private KailleraEventListener			listener;
	private BlockingQueue<KailleraEvent>	eventQueue			= new LinkedBlockingQueue<KailleraEvent>();

	public KailleraUserImpl(int userID, String protocol, InetSocketAddress connectSocketAddress, KailleraEventListener listener, KailleraServerImpl server)
	{
		this.id = userID;
		this.protocol = protocol;
		this.connectSocketAddress = connectSocketAddress;
		this.server = server;
		this.listener = listener;

		toString = "User" + userID + "(" + connectSocketAddress.getAddress().getHostAddress() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		lastChatTime = lastCreateGameTime = lastTimeout = 0;
		lastActivity = lastKeepAlive = connectTime = System.currentTimeMillis();
	}

	public int getID()
	{
		return id;
	}

	public InetSocketAddress getConnectSocketAddress()
	{
		return connectSocketAddress;
	}

	public String getProtocol()
	{
		return protocol;
	}

	public long getConnectTime()
	{
		return connectTime;
	}

	public int getStatus()
	{
		return status;
	}

	public boolean isLoggedIn()
	{
		return loggedIn;
	}

	public void setLoggedIn()
	{
		loggedIn = true;
	}
	
	public void setLoggedIn(boolean loggedIn)
	{
		loggedIn = false;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		toString = "User" + id + "(" + (name.length() > 15 ? (name.substring(0, 15) + "...") : name) + "/" + connectSocketAddress.getAddress().getHostAddress() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	public String getClientType()
	{
		return clientType;
	}

	public void setClientType(String clientType)
	{
		this.clientType = clientType;
		if(clientType != null && clientType.startsWith(EMULINKER_CLIENT_NAME))
			emulinkerClient = true;
	}
	
	public boolean isEmuLinkerClient()
	{
		return emulinkerClient;
	}

	public byte getConnectionType()
	{
		return connectionType;
	}

	public void setConnectionType(byte connectionType)
	{
		this.connectionType = connectionType;
	}

	public InetSocketAddress getSocketAddress()
	{
		return clientSocketAddress;
	}

	public void setSocketAddress(InetSocketAddress clientSocketAddress)
	{
		this.clientSocketAddress = clientSocketAddress;
	}

	public int getPing()
	{
		return ping;
	}

	public void setPing(int ping)
	{
		this.ping = ping;
	}

	public long getLastActivity()
	{
		return lastActivity;
	}

	public long getLastKeepAlive()
	{
		return lastKeepAlive;
	}

	public void updateLastKeepAlive()
	{
		this.lastKeepAlive = System.currentTimeMillis();
	}

	public KailleraEventListener getListener()
	{
		return listener;
	}

	public KailleraServerImpl getServer()
	{
		return server;
	}

	public KailleraGameImpl getGame()
	{
		return game;
	}

	protected void setGame(KailleraGameImpl game)
	{
		this.game = game;
		if (game == null)
			playerNumber = -1;
	}

	protected void setStatus(int status)
	{
		this.status = status;
	}

	protected long getLastChatTime()
	{
		return lastChatTime;
	}

	protected long getLastCreateGameTime()
	{
		return lastCreateGameTime;
	}

	protected long getLastTimeout()
	{
		return lastTimeout;
	}

	protected void setLastTimeout()
	{
		lastTimeout = System.currentTimeMillis();
	}

	public int getAccess()
	{
		return access;
	}
	
	public String getAccessStr()
	{
		return AccessManager.ACCESS_NAMES[access];
	}

	protected void setAccess(int access)
	{
		this.access = access;
	}

	public int getPlayerNumber()
	{
		return playerNumber;
	}

	protected void setPlayerNumber(int playerNumber)
	{
		this.playerNumber = playerNumber;
	}

	public void updateLastActivity()
	{
		lastActivity = lastKeepAlive = System.currentTimeMillis();
	}

	public boolean equals(Object obj)
	{
		if (obj instanceof KailleraUserImpl && ((KailleraUserImpl) obj).getID() == getID())
			return true;
		return false;
	}

	public String toString()
	{
		return toString;
	}

	public String toDetailedString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("KailleraUserImpl[id="); //$NON-NLS-1$
		sb.append(getID());
		sb.append(" protocol="); //$NON-NLS-1$
		sb.append(getProtocol());
		sb.append(" status="); //$NON-NLS-1$
		sb.append(KailleraUser.STATUS_NAMES[getStatus()]);
		sb.append(" name="); //$NON-NLS-1$
		sb.append(getName());
		sb.append(" clientType="); //$NON-NLS-1$
		sb.append(getClientType());
		sb.append(" ping="); //$NON-NLS-1$
		sb.append(getPing());
		sb.append(" connectionType="); //$NON-NLS-1$
		sb.append(KailleraUser.CONNECTION_TYPE_NAMES[getConnectionType()]);
		sb.append(" remoteAddress="); //$NON-NLS-1$
		sb.append((getSocketAddress() == null ? EmuUtil.formatSocketAddress(getConnectSocketAddress()) : EmuUtil.formatSocketAddress(getSocketAddress())));
		sb.append("]"); //$NON-NLS-1$
		return sb.toString();
	}

	public void stop()
	{
		synchronized(this)
		{
			if (!isRunning)
			{
				log.debug(this + "  thread stop request ignored: not running!"); //$NON-NLS-1$
				return;
			}
	
			if (stopFlag)
			{
				log.debug(this + "  thread stop request ignored: already stopping!"); //$NON-NLS-1$
				return;
			}
	
			stopFlag = true;
	
			try
			{
				Thread.sleep(500);
			}
			catch (Exception e)
			{
			}
	
			addEvent(new StopFlagEvent());
		}
		
		listener.stop();
	}

	public synchronized void droppedPacket()
	{
		if (game != null)
			game.droppedPacket(this);
	}

	public boolean isRunning()
	{
		return isRunning;
	}

	// server actions

	public synchronized void login() throws PingTimeException, ClientAddressException, ConnectionTypeException, UserNameException, LoginException
	{
		updateLastActivity();
		server.login(this);
	}

	public synchronized void chat(String message) throws ChatException, FloodException
	{
		updateLastActivity();
		server.chat(this, message);
		lastChatTime = System.currentTimeMillis();
	}

	public synchronized void gameKick(int userID) throws GameKickException
	{
		updateLastActivity();

		if (game == null)
		{
			log.warn(this + " kick User " + userID + " failed: Not in a game"); //$NON-NLS-1$ //$NON-NLS-2$
			throw new GameKickException(EmuLang.getString("KailleraUserImpl.KickErrorNotInGame")); //$NON-NLS-1$
		}

		game.kick(this, userID);
	}

	public synchronized KailleraGame createGame(String romName) throws CreateGameException, FloodException
	{
		updateLastActivity();

		if (getStatus() == KailleraUser.STATUS_PLAYING)
		{
			log.warn(this + " create game failed: User status is Playing!"); //$NON-NLS-1$
			throw new CreateGameException(EmuLang.getString("KailleraUserImpl.CreateGameErrorAlreadyInGame")); //$NON-NLS-1$
		}
		else if (getStatus() == KailleraUser.STATUS_CONNECTING)
		{
			log.warn(this + " create game failed: User status is Connecting!"); //$NON-NLS-1$
			throw new CreateGameException(EmuLang.getString("KailleraUserImpl.CreateGameErrorNotFullyConnected")); //$NON-NLS-1$
		}

		KailleraGame game = server.createGame(this, romName);
		lastCreateGameTime = System.currentTimeMillis();
		return game;
	}

	public synchronized void quit(String message) throws QuitException, DropGameException, QuitGameException, CloseGameException
	{
		updateLastActivity();
		server.quit(this, message);
		loggedIn = false;
	}

	public synchronized KailleraGame joinGame(int gameID) throws JoinGameException
	{
		updateLastActivity();
		
		if(game != null)
		{
			log.warn(this + " join game failed: Already in: " + game); //$NON-NLS-1$
			throw new JoinGameException(EmuLang.getString("KailleraUserImpl.JoinGameErrorAlreadyInGame")); //$NON-NLS-1$
		}
		if (getStatus() == KailleraUser.STATUS_PLAYING)
		{
			log.warn(this + " join game failed: User status is Playing!"); //$NON-NLS-1$
			throw new JoinGameException(EmuLang.getString("KailleraUserImpl.JoinGameErrorAnotherGameRunning")); //$NON-NLS-1$
		}
		else if (getStatus() == KailleraUser.STATUS_CONNECTING)
		{
			log.warn(this + " join game failed: User status is Connecting!"); //$NON-NLS-1$
			throw new JoinGameException(EmuLang.getString("KailleraUserImpl.JoinGameErrorNotFullConnected")); //$NON-NLS-1$
		}

		KailleraGameImpl game = (KailleraGameImpl) server.getGame(gameID);
		if (game == null)
		{
			log.warn(this + " join game failed: Game " + gameID + " does not exist!"); //$NON-NLS-1$ //$NON-NLS-2$
			throw new JoinGameException(EmuLang.getString("KailleraUserImpl.JoinGameErrorDoesNotExist")); //$NON-NLS-1$
		}

		playerNumber = game.join(this);
		setGame(game);

		gameDataErrorTime = -1;

		return game;
	}

	// game actions
	public synchronized void gameChat(String message, int messageID) throws GameChatException
	{
		updateLastActivity();

		if (game == null)
		{
			log.warn(this + " game chat failed: Not in a game"); //$NON-NLS-1$
			throw new GameChatException(EmuLang.getString("KailleraUserImpl.GameChatErrorNotInGame")); //$NON-NLS-1$
		}

		game.chat(this, message);
	}

	public synchronized void dropGame() throws DropGameException
	{
		updateLastActivity();
		setStatus(KailleraUser.STATUS_IDLE);

		if (game != null)
			game.drop(this, playerNumber);
		else
			log.debug(this + " drop game failed: Not in a game"); //$NON-NLS-1$
	}

	public synchronized void quitGame() throws DropGameException, QuitGameException, CloseGameException
	{
		updateLastActivity();

		if (game == null)
		{
			log.debug(this + " quit game failed: Not in a game"); //$NON-NLS-1$
			//throw new QuitGameException("You are not in a game!");
			return;
		}

		if (status == KailleraUser.STATUS_PLAYING)
		{
			game.drop(this, playerNumber);
			setStatus(KailleraUser.STATUS_IDLE);
		}

		game.quit(this, playerNumber);
		
		if(status != KailleraUser.STATUS_IDLE)
			setStatus(KailleraUser.STATUS_IDLE);

		setGame(null);
		addEvent(new UserQuitGameEvent(game, this));
	}

	public synchronized void startGame() throws StartGameException
	{
		updateLastActivity();

		if (game == null)
		{
			log.warn(this + " start game failed: Not in a game"); //$NON-NLS-1$
			throw new StartGameException(EmuLang.getString("KailleraUserImpl.StartGameErrorNotInGame")); //$NON-NLS-1$
		}

		game.start(this);
	}

	public synchronized void playerReady() throws UserReadyException
	{
		updateLastActivity();

		if (game == null)
		{
			log.warn(this + " player ready failed: Not in a game"); //$NON-NLS-1$
			throw new UserReadyException(EmuLang.getString("KailleraUserImpl.PlayerReadyErrorNotInGame")); //$NON-NLS-1$
		}

		game.ready(this, playerNumber);
	}

	public void addGameData(byte[] data) throws GameDataException
	{
		updateLastActivity();

		try
		{
			if (game == null)
				throw new GameDataException(EmuLang.getString("KailleraUserImpl.GameDataErrorNotInGame"), data, getConnectionType(), 1, 1); //$NON-NLS-1$
			game.addData(this, playerNumber, data);
			gameDataErrorTime = 0;
		}
		catch (GameDataException e)
		{
			// this should be warn level, but it creates tons of lines in the log
			log.debug(this + " add game data failed: " + e.getMessage()); //$NON-NLS-1$

			// i'm going to reflect the game data packet back at the user to prevent game lockups, 
			// but this uses extra bandwidth, so we'll set a counter to prevent people from leaving 
			// games running for a long time in this state

			if (gameDataErrorTime > 0)
			{
				if ((System.currentTimeMillis() - gameDataErrorTime) > 30000) // give the user time to close the game
				{
					// this should be warn level, but it creates tons of lines in the log
					log.debug(this + ": error game data exceeds drop timeout!"); //$NON-NLS-1$
					//e.setReflectData(false);
					throw new GameDataException(e.getMessage());
				}
				else
				{
					//e.setReflectData(true);
					throw e;
				}
			}
			else
			{
				gameDataErrorTime = System.currentTimeMillis();
				//e.setReflectData(true);
				throw e;
			}
		}
	}

	void addEvent(KailleraEvent event)
	{
		if (event == null)
		{
			log.error(this + ": ignoring null event!"); //$NON-NLS-1$
			return;
		}

		eventQueue.offer(event);
	}

	public void run()
	{
		isRunning = true;
		log.debug(this + " thread running..."); //$NON-NLS-1$

		try
		{
			while (!stopFlag)
			{
				KailleraEvent event = eventQueue.poll(200, TimeUnit.SECONDS);

				if (event == null)
					continue;
				else if (event instanceof StopFlagEvent)
					break;

				listener.actionPerformed(event);

				if (event instanceof GameStartedEvent)
				{
					setStatus(KailleraUser.STATUS_PLAYING);
				}
				else if (event instanceof UserQuitEvent && ((UserQuitEvent) event).getUser().equals(this))
				{
					stop();
				}
			}
		}
		catch (InterruptedException e)
		{
			log.error(this + " thread interrupted!"); //$NON-NLS-1$
		}
		catch (Throwable e)
		{
			log.fatal(this + " thread caught unexpected exception!", e); //$NON-NLS-1$
		}
		finally
		{
			isRunning = false;
			log.debug(this + " thread exiting..."); //$NON-NLS-1$
		}
	}

	private static class StopFlagEvent implements KailleraEvent
	{
		public String toString()
		{
			return "StopFlagEvent"; //$NON-NLS-1$
		}
	}

}