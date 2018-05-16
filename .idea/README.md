### How to configure IntelliJ IDEA for Learnweb

1) Clone repository
2) Open Learnweb folder (folder which contains .idea folder) in IntelliJ IDEA
3) Go to File -> Project Structure -> Libraries (Project Settings) and do next steps:
- Add local jars to the project structure:
    - Click "Add" (green plus icon)
	- Select "Java"
	- Select "$PROJECT_DIR$/WebContent/WEB-INF/lib" directory (e.g. "E:/projects/Learnweb/WebContent/WEB-INF/lib")
	- When you prompted whether add the library to module or not, click "Cancel"
	- Rename the "lib" to "localLibs" **(important!)**

- Add tomcat jars to the project structure:
    - Click "Add" (green plus icon)
	- Select "Java"
	- Select "$APACHE_TOMCAT_DIR$/lib" directory (e.g. "C:\apache-tomcat-8.5.23\lib")
	- When you prompted whether add the library to module or not, click "Cancel"
	- Rename the "lib" to "tomcatLibs" **(important!)**

4) Update "Run configuration" (The configuration already exists, we just need to choose a server)
	- In Run configuration (right top corner), click on "Run locally" and select "Edit Configurations..."
	- Find "Application server" selector and click "Configure..." button right to it
	- Add new server by selecting Tomcat server directory (same as above)
	- Select it and click "Ok"