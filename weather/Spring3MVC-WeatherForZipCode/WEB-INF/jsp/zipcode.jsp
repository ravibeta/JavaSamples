<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<html>
<head>
	<title>Spring 3 MVC Series - Weather for ZipCode</title>
</head>
<body>
<h2>Weather for ZipCode</h2>
<form:form method="post" action="addZipCode.html">

	<table>
	<tr>
		<td><form:label path="zipcode">Zip Code</form:label></td>
		<td><form:input path="zipcode" /></td> 
	</tr>
	<tr>
		<td colspan="2">
			<input type="submit" value="Get Weather"/>
		</td>
	</tr>
</table>	
	
</form:form>
</body>
</html>
