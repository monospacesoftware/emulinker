<%@ page language="java" import="
    java.text.DecimalFormat,
    java.util.Iterator,
    java.util.concurrent.ThreadPoolExecutor,
    java.util.concurrent.TimeUnit,
    java.net.InetAddress,
	org.emulinker.kaillera.controller.KailleraController,
	org.emulinker.kaillera.controller.connectcontroller.ConnectController,
	org.emulinker.kaillera.model.impl.KailleraServerImpl,
	org.emulinker.kaillera.model.impl.KailleraGameImpl,
	org.emulinker.kaillera.model.impl.KailleraUserImpl,
	org.emulinker.kaillera.model.KailleraGame,
	org.emulinker.kaillera.model.KailleraUser
" %>
<jsp:include page="header.jsp"/>
<!-- BEGIN BODY -->

<%
	DecimalFormat numberFormatter = (DecimalFormat) DecimalFormat.getInstance();
	numberFormatter.applyPattern("####0.00");

	ThreadPoolExecutor threadPool = (ThreadPoolExecutor) getServletContext().getAttribute("threadPool");
	ConnectController connectController = (ConnectController) getServletContext().getAttribute("connectController");
	KailleraServerImpl kailleraServer = (KailleraServerImpl) getServletContext().getAttribute("kailleraServer");
	
	InetAddress locaddr = InetAddress.getLocalHost();
	String locname = locaddr.getHostName();
%>

<table class="main">
 <tr>
  <td>
   <table class="inside" width="180">
    <tr>
     <th colspan="2" nowrap>Server Info</th>
    </tr>
    <tr>
<% if (kailleraServer.isRunning())
   {
%>    
     <th colspan="2" nowrap><a href="server.jsp?action=stop">Stop server</a></th>
<% }
   else
   {
%>    
     <th colspan="2" nowrap><a href="server.jsp?action=start">Start server</a></th>
<% } %>    
    </tr>
    <tr>
     <td nowrap>Connect Port</td>
     <td nowrap><%=connectController.getBindPort() %></td>
    </tr>
    <tr>
     <td nowrap>Connect Buffer</td>
     <td nowrap><%=connectController.getBufferSize() %></td>
    </tr>
    <tr>
     <td nowrap>Uptime</td>
     <td nowrap><%=(System.currentTimeMillis()-connectController.getStartTime())/60000 %>m</td>
    </tr>
    <tr>
     <td nowrap>User Limit</td>
     <td nowrap><%=kailleraServer.getMaxUsers() %></td>
    </tr>
    <tr>
     <td nowrap>Ping Limit</td>
     <td nowrap><%=kailleraServer.getMaxPing() %></td>
    </tr>
   </table>
   <table class="inside" width="180">
    <tr>
     <th colspan="2" nowrap>Statistics</th>
    </tr>
    <tr>
     <td nowrap>Connect Attempts</td>
     <td nowrap><%=connectController.getRequestCount() %></td>
    </tr>
    <tr>
     <td nowrap>Connect OK</td>
     <td nowrap><%=connectController.getConnectCount() %></td>
    </tr>
    <tr>
     <td nowrap>Protocol Errors</td>
     <td nowrap><%=connectController.getProtocolErrorCount() %></td>
    </tr>
    <tr>
     <td nowrap>Server Full</td>
     <td nowrap><%=connectController.getDeniedServerFullCount() %></td>
    </tr>
    <tr>
     <td nowrap>Denied</td>
     <td nowrap><%=connectController.getDeniedOtherCount() %></td>
    </tr>
   </table>
   <table class="inside" width="180">
    <tr>
     <th colspan="2" nowrap>Thread Pool State</th>
    </tr>
    <tr>
     <td>Core Size</td>
     <td><%=threadPool.getCorePoolSize() %></td>
    </tr>
    <tr>
     <td>Current Size</td>
     <td><%=threadPool.getPoolSize() %></td>
    </tr>
    <tr>
     <td>Size Limit</td>
     <td><%=threadPool.getMaximumPoolSize() %></td>
    </tr>
    <tr>
     <td>Largest Size</td>
     <td><%=threadPool.getLargestPoolSize() %></td>
    </tr>
    <tr>
     <td>Task Count</td>
     <td><%=threadPool.getTaskCount() %></td>
    </tr>
    <tr>
     <td>Keep Alive</td>
     <td><%=(threadPool.getKeepAliveTime(TimeUnit.SECONDS) + "s") %></td>
    </tr>
   </table>
  </td>
  <td width="100%">
   <table class="inside" width="100%">
    <tr>
     <td colspan="2">
      <table class="inside" width="100%">
       <tr>
        <th>Controller Version</th>
        <th>Controller Buffer</th>
        <th>Clients Connected</th>
        <th>Client Protocols Handled</th>
       </tr>
        <% Iterator<KailleraController> controllersIter = connectController.getControllers().iterator();
           while(controllersIter.hasNext()) {
             KailleraController controller = (KailleraController) controllersIter.next(); %>
       <tr>
        <td><a href="controller.jsp?version=<%=controller.getVersion()%>"><%=controller.getVersion() %></a></td>
        <td><%=controller.getBufferSize() %></td>
        <td><%=controller.getNumClients() %></td>
        <td>
        <% String[] clientTypes = controller.getClientTypes();
           for(int i=0; i<clientTypes.length; i++) { %>
         <%=clientTypes[i]%><%=((i<clientTypes.length-1)?",":"")%>
        <% } %>
        </td>
       </tr>
       <% } %>
      </table>
     </td>
    </tr>
    <tr>
     <th colspan="2">EmuLinker Kaillera Server: <%=locname%>&nbsp;</th>
    </tr>
    <tr>
     <td colspan="2"><%=kailleraServer.getNumUsers()%> Users Connected</td>
    </tr>
    <tr>
     <td colspan="2">
      <table class="inside" width="100%">
       <tr>
        <th>ID</th>
        <th>Name</th>
        <th>Status</th>
        <th>Connection Type</th>
        <th>Ping</th>
        <th>Remote Address</th>
       </tr>
        <% Iterator<KailleraUserImpl> usersIter = kailleraServer.getUsers().iterator();
           while(usersIter.hasNext()) {
             KailleraUserImpl user = (KailleraUserImpl) usersIter.next();
        %>
       <tr>
        <td><%=user.getID()%></td>
        <td><a href="user.jsp?id=<%=user.getID()%>"><%=user.getName() %></a></td>
        <td><%=KailleraUser.STATUS_NAMES[user.getStatus()] %></td>
        <td><%=KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()] %></td>
        <td><%=user.getPing() %></td>
        <td><%=user.getSocketAddress().getAddress().getHostAddress() %>:<%=user.getSocketAddress().getPort()%></td>
       </tr>
       <% } %>
      </table>
     </td>
    </tr>
    <tr>
     <td><%=kailleraServer.getNumGames()%> Games Created </td>
     <td>? Games Playing </td>
    </tr>
    <tr>
     <td colspan="2">
      <table class="inside" width="100%">
       <tr>
        <th>ID</th>
        <th>ROM</th>
        <th>Status</th>
        <th>Players</th>
       </tr>
        <% Iterator<KailleraGameImpl> gamesIter = kailleraServer.getGames().iterator();
           while(gamesIter.hasNext()) {
             KailleraGameImpl game = (KailleraGameImpl) gamesIter.next();
        %>
       <tr>
        <td><%=game.getID() %></td>
        <td><a href="game.jsp?id=<%=game.getID()%>"><%=game.getRomName() %></a></td>
        <td><%=KailleraGame.STATUS_NAMES[game.getStatus()] %></td>
        <td><%=game.getNumPlayers() %></td>
       </tr>
       <% } %>
      </table>
     </td>
    </tr>
   </table>
  </td>
 </tr> 
</table>

<!-- END BODY -->
<jsp:include page="footer.jsp"/>
