package org.emulinker.kaillera.model;

import java.net.InetSocketAddress;

import org.emulinker.kaillera.model.event.KailleraEventListener;
import org.emulinker.kaillera.model.exception.*;

public interface KailleraUser
{
	public static final byte		CONNECTION_TYPE_LAN			= 1;
	public static final byte		CONNECTION_TYPE_EXCELLENT	= 2;
	public static final byte		CONNECTION_TYPE_GOOD		= 3;
	public static final byte		CONNECTION_TYPE_AVERAGE		= 4;
	public static final byte		CONNECTION_TYPE_LOW			= 5;
	public static final byte		CONNECTION_TYPE_BAD			= 6;

	public static final String[]	CONNECTION_TYPE_NAMES		= { "DISABLED", "Lan", "Excellent", "Good", "Average", "Low", "Bad" };

	public static final byte		STATUS_PLAYING				= 0;
	public static final byte		STATUS_IDLE					= 1;
	public static final byte		STATUS_CONNECTING			= 2;
	public static final String[]	STATUS_NAMES				= { "Playing", "Idle", "Connecting" };

	public int getID();

	public InetSocketAddress getConnectSocketAddress();

	public String getProtocol();

	public long getConnectTime();

	public int getStatus();

	public String getName();

	public void setName(String name);

	public String getClientType();
	
	public boolean isEmuLinkerClient();

	public void setClientType(String clientType);

	public byte getConnectionType();

	public void setConnectionType(byte connectionType);

	public InetSocketAddress getSocketAddress();

	public void setSocketAddress(InetSocketAddress clientSocketAddress);

	public int getPing();

	public void setPing(int ping);

	public void login() throws PingTimeException, ClientAddressException, ConnectionTypeException, UserNameException, LoginException;

	public long getLastActivity();

	public void updateLastActivity();

	public void updateLastKeepAlive();

	public long getLastKeepAlive();

	public boolean isLoggedIn();

	public KailleraServer getServer();

	public KailleraEventListener getListener();

	public void chat(String message) throws ChatException, FloodException;

	public KailleraGame createGame(String romName) throws CreateGameException, FloodException;

	public void quit(String message) throws QuitException, DropGameException, QuitGameException, CloseGameException;

	public KailleraGame joinGame(int gameID) throws JoinGameException;

	public int getPlayerNumber();

	public void startGame() throws StartGameException;

	public void gameChat(String message, int messageID) throws GameChatException;

	public void gameKick(int userID) throws GameKickException;

	public void playerReady() throws UserReadyException;

	public void addGameData(byte[] data) throws GameDataException;

	public void dropGame() throws DropGameException;

	public void quitGame() throws DropGameException, QuitGameException, CloseGameException;

	public void droppedPacket();

	public void stop();
}
