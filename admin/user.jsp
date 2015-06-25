<%@ page import="org.emulinker.kaillera.model.impl.KailleraServerImpl,
                 org.emulinker.kaillera.model.impl.KailleraUserImpl,
                 org.emulinker.kaillera.model.KailleraUser,
                 java.util.Iterator"
%>
<jsp:include page="header.jsp"/>
<%
	KailleraServerImpl kailleraServer = (KailleraServerImpl) getServletContext().getAttribute("kailleraServer");
    KailleraUser user = kailleraServer.getUser(Integer.parseInt(request.getParameter("id")));
%>
<!-- BEGIN BODY -->


<table class="main">
<tr>
<td><%=user.getName()%>
&nbsp;&nbsp;
<a href="stop.jsp?user=<%=user.getID()%>">[Stop user]</a></td>
</tr>
<tr>
<td>Client type: <%=user.getClientType()%></td>
</tr>
<tr>
<td>Connection type: <%=user.getConnectionType()%></td>
</tr>
<tr>
<td>Connection address: <%=user.getConnectSocketAddress()%></td>
</tr>
<tr>
<td>Connect time: <%=user.getConnectTime()%></td>
</tr>
<tr>
<td>ID: <%=user.getID()%></td>
</tr>
<tr>
<td>Last activitiy: <%=user.getLastActivity()%></td>
</tr>
<tr>
<td>Last KeepAlive: <%=user.getLastKeepAlive()%></td>
</tr>
<tr>
<td>Ping: <%=user.getPing()%></td>
</tr>
<tr>
<td>Protocol: <%=user.getProtocol()%></td>
</tr>
<tr>
<td>Socket address: <%=user.getSocketAddress()%></td>
</tr>
<tr>
<td>Status: <%=user.getStatus()%></td>
</tr>
</table>

<br>
- <a href="index.jsp">Main Screen</a> -
<!-- END BODY -->
<jsp:include page="footer.jsp"/>
