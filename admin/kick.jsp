<%@ page import="org.emulinker.kaillera.model.impl.KailleraServerImpl,
                 org.emulinker.kaillera.model.KailleraGame"
%><%
	KailleraServerImpl kailleraServer = (KailleraServerImpl) getServletContext().getAttribute("kailleraServer");
	int gameid = Integer.parseInt(request.getParameter("game"));
    KailleraGame game = kailleraServer.getGame(gameid);	
    int userid = Integer.parseInt(request.getParameter("user"));
    
    game.kick(game.getOwner(), userid);
    
    response.sendRedirect("game.jsp?id=" + gameid);
%>