<%@ page import="org.emulinker.kaillera.model.impl.KailleraServerImpl,
                 org.emulinker.kaillera.model.impl.KailleraUserImpl,
                 org.emulinker.kaillera.model.KailleraGame,
                 java.util.Iterator"
%>
<jsp:include page="header.jsp"/>
<%
	KailleraServerImpl kailleraServer = (KailleraServerImpl) getServletContext().getAttribute("kailleraServer");
    KailleraGame game = kailleraServer.getGame(Integer.parseInt(request.getParameter("id")));
%>
<!-- BEGIN BODY -->


<table class="main">
<tr>
<td><%=game.getRomName()%></td>
</tr>
<tr>
<td>This game has <%=game.getNumPlayers()%> players.</td>
</tr>
<tr>
<td>Owner: <%=game.getOwner()%></td>
</tr>
<tr>
<td>Status: <%=game.getStatus()%></td>
</tr>

<% Iterator usersIter = game.getPlayers().iterator();
   while(usersIter.hasNext()) {
       KailleraUserImpl user = (KailleraUserImpl) usersIter.next();
%>
<tr>
<td>
<a href="user.jsp?id=<%=user.getID()%>"><%=user.getName()%></a>
&nbsp;&nbsp;
<% if (game.getOwner().equals(user)) 
   {
%>
<i>Game owner</i>
<% }
   else
   {
%>   
<a href="kick.jsp?game=<%=game.getID()%>&user=<%=user.getID()%>">[Kick user]</a>
<%  }  %>
</td>
</tr>
<%
}
%>
</table>

<br>
- <a href="index.jsp">Main Screen</a> -
<!-- END BODY -->
<jsp:include page="footer.jsp"/>
