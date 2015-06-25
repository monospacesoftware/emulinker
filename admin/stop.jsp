<%@ page import="org.emulinker.kaillera.model.impl.KailleraServerImpl,
                 org.emulinker.kaillera.model.KailleraUser"
%><%
	KailleraServerImpl kailleraServer = (KailleraServerImpl) getServletContext().getAttribute("kailleraServer");
    KailleraUser user = kailleraServer.getUser(Integer.parseInt(request.getParameter("user")));

    kailleraServer.quit(user, "You have been disconnected from the game server by the server administrator.");
    
    response.sendRedirect("index.jsp");
%>