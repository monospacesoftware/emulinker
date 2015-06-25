<%@ page import="org.emulinker.kaillera.controller.KailleraController,
	             org.emulinker.kaillera.controller.connectcontroller.ConnectController,
	             java.util.Iterator"
%>
<jsp:include page="header.jsp"/>
<%
    KailleraController controller = null;
	ConnectController connectController = (ConnectController) getServletContext().getAttribute("connectController");
    Iterator<KailleraController> controllersIter = connectController.getControllers().iterator();
    while(controllersIter.hasNext()) {
             controller = (KailleraController) controllersIter.next();
             if (controller.getVersion().equals(request.getParameter("version"))) break;
    }
%>
<!-- BEGIN BODY -->


<table class="main">
<tr>
<td><%=controller.getVersion()%></td>
</tr>
<tr>
<td>Buffer size: <%=controller.getBufferSize()%></td>
</tr>
<tr>
<td>Number of clients: <%=controller.getNumClients()%></td>
</tr>
<%
String[] clientTypes = controller.getClientTypes();
StringBuffer sb = new StringBuffer();
String sep = "";
for (int i = 0; i < clientTypes.length; i++)
{
    sb.append(sep).append(clientTypes[i]);
    sep = " / ";
}
%>
<tr>
<td>Client types: <%=sb%></td>
</tr>
</table>

<br>
- <a href="index.jsp">Main Screen</a> -
<!-- END BODY -->
<jsp:include page="footer.jsp"/>
