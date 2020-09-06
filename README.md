![fritz2](https://www.fritz2.dev/images/fritz2_logo_grey.png)

# Ktor TodoMVC

This project is an example for using [fritz2](https://www.fritz2.dev/) with in a ktor web-application.
It will demonstrate how-to interact with a backend application in fritz2 and how-to setup this kind of project.

This project is a kotlin multi platform project which contains every thong in once.
In the `commonMain` section is the shared model and validation of the application.
The `jsMain` section contains the complete code of the client-application written with fritz2. 
The backend part is in the `jvmMain` section where all the code for the server is.

This project uses the following libraries:
* [fritz2](https://github.com/jwstegemann/fritz2) - mainly in the frontend, except validation and model in both sides
* [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) - used in frontend and backend for serializing the shared model
* [Ktor](https://ktor.io/) - running the server with
* [Exposed](https://github.com/JetBrains/Exposed) - making CRUD operations to the database
* [H2 Database](https://www.h2database.com/html/main.html) - running as in-memory-db which stores the data

# Features
* running full-stack-example based on the [TodoMVC](http://todomvc.com/)
* working validation provided by fritz2 extension on client- and server-side
* complete gradle configuration
* some tests

# Run
To run this application you only need to run the following gradle task:
```
> ./gradlew run 
``` 
Then navigate in your browser to [localhost:8080](http://localhost:8080/)

# Contribution
If you like this example and how fritz2 works it would be great 
when you give us a &#11088; at our [fritz2 github page](https://github.com/jwstegemann/fritz2).

When you find some bugs or improvements please let us know by reporting an issue 
[here](https://github.com/jamowei/fritz2-spring-todomvc/issues).
Also, feel free to use this project as template for your own applications.