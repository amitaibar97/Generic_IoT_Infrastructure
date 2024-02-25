# Generic_IoT_Infrastructure
## Description
A web service designed for IoT manufacturers to gather data about their device’s usage. This project, developed in Java, HTML, CSS, and JavaScript, consists of two main components: a website and a Gateway server. The website provides a user interface for company registration, using HTML, CSS, and JavaScript for the frontend, and Tomcat (servlets) for the backend. The Gateway server, written in Java, acts as a concurrent, configurable, and scalable server, serving as the backbone of the IoT infrastructure.

## Features
- **Configurability**: The server is easily configurable, allowing for the addition of new functionalities through the Factory module which I imlplemented, which is loaded in Command object recipes to create a functionality of Database manipulation.
- **Plug & Play**: The server is easily configurable to allow the addition of new functionalities through a "Plug and Play" approach by dropping a jar file into a specified folder. The classes which are located inside this jar is loaded into the Factory and lets the user to use the new functionality.
- **Generic Data Handling**: Supports a generic, schema-less data approach, allowing devices to send updates in any format, with MongoDB used to maintain flexibility in data storage and structure.
- **Concurrency**: Utilizes a Thread Pool which I imlplemented, to process requests concurrently, enhancing the system's efficiency and responsiveness.
- **Scalability**: Achieves vertical scaling with a thread pool that adapts to the system's core count, ensuring optimal resource utilization.
- **Fault Tolerance**: Incorporates fault tolerance mechanisms to provide feedback to clients in case of misuse or any fault during request execution, preventing server crashes.
- **Integrated Watchdog Service**: Utilizes an external watchdog service to monitor the Gateway server, ensuring its continuous operation by restarting it if it stops responding, further enhancing system reliability.

## Functionalities
- **Register Company**: Companies can register to the IoT infrastructure, with details stored in an SQL database for adminisration needs.
- **Register Product**: Companies can register their product models to the service.
- **Register IoT Devices**: Enables the registration of IoT devices under registered products.
- **Data Update**: Devices can send usage information to be stored within the system.

## Installation
Clone the repository to your local machine using:
```bash
git clone https://github.com/amitaibar97/Generic_IoT_Infrastructure.git
```
## Environment Setup
- This project requires Java Development Kit (JDK) version 8 or higher due to the use of lambda expressions and other Java 8 features. Ensure your JDK version meets this requirement.

### Integrated Development Environment (IDE)
- Any modern IDE that supports Java development (like IntelliJ IDEA, Eclipse, or VS Code with the Java extension) should be suitable.
- Configure your IDE to use the correct JDK version (8 or higher) in the project settings.

### Build Tools and Dependency Management
- If you're using build tools like Maven or Gradle, ensure your IDE supports these and is configured to use the project's build file (e.g., `pom.xml` for Maven projects).
- Ensure all project dependencies are correctly resolved through your IDE's dependency management system or build tool configuration.

### Servers
- The website back-end is build on TomCat9 server with servlets. make sure your IDE enables it.

## Usage
1. **Start the Website**: Begin by ensuring the website is operational, serving as the frontend for company and product registrations.
2. **Start the Tomcat Server**: Launch the Tomcat server, which should run on a distinct port and manage backend processes.
3. **Launch the Gateway Server**: Initiate the Gateway server, which is central to handling data communication and processing.
4. **Verify Database Connectivity**: Ensure that both the SQL database (for company information) and MongoDB (for device usage data) are correctly configured and accessible by the Gateway server.
5. make the request by the website forms fill or by sending a request to the Gateway server in the format provided in the attached test files.

A web service that enables IoT manufacturers to gather data about their device’s usage.
![GIoT_flow](https://github.com/amitaibar97/Generic_IoT_Infrastructure/assets/89575092/c6640299-229e-43af-a8d8-56f40e630f68)

## Contributing
contributions to this project are welcomed! Please open an issue to discuss proposed changes.

## License
This version of the README includes more detailed information about the API functionalities. if you have some creative ideas for improvements , let me know!
