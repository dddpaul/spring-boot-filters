# Spring Boot filter examples

## CoyoteRequestManipulator

It's used for the same purpose as [multiple HTTP connectors](https://github.com/dddpaul/spring-boot-connectors).
To be exact, when your application handles GET requests containing [percent-encoded](https://en.wikipedia.org/wiki/Percent-encoding) non-ASCII data in different charsets. For example, one HTTP endpoint uses standard UTF-8 while the other uses [Windows-1251](https://en.wikipedia.org/wiki/Windows-1251).

At this time it's achieved by using private API of [org.apache.coyote.Request](https://tomcat.apache.org/tomcat-8.0-doc/api/org/apache/coyote/Request.html) class to decode query string conditionally.

Implementation is quite simple as you can see from [Application.java](src/main/java/com/github/dddpaul/filters/Application.java).

Pros:
* no multiple connectors, listen on single port;
* controller unit tests are passed;
* integration tests are passed.
  
Cons:
* reflection and private API usage :)
* query string could be handled twice.
