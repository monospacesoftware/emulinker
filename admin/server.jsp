<%@ page import="org.emulinker.kaillera.model.impl.KailleraServerImpl,
                 org.emulinker.kaillera.model.KailleraGame"
%><%
	KailleraServerImpl kailleraServer = (KailleraServerImpl) getServletContext().getAttribute("kailleraServer");
	String action = request.getParameter("action");
	
	if ("start".equalsIgnoreCase(action))
	{
	    System.out.println("Starting server");
	    kailleraServer.start();
	}
	
	if ("stop".equalsIgnoreCase(action))
	{
	    System.out.println("Stopping server");
	    kailleraServer.stop();
	}
    
    response.sendRedirect("index.jsp");
%>