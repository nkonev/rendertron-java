Rendertron Java
===============

It's fork of [prerender-java](https://github.com/greengerong/prerender-java).

1:Add this line to your web.xml:
```xml
<filter>
    <filter-name>rendertron</filter-name>
    <filter-class>SeoFilter</filter-class>
    <init-param>
        <param-name>serviceUrl</param-name>
        <param-value>http://127.0.0.1:3000/render</param-value>
    </init-param>
    <init-param>
        <param-name>forwardedURLPrefix</param-name>
        <param-value>https://mysite.example.com</param-value>
    </init-param>
</filter>
<filter-mapping>
    <filter-name>rendertron</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```
2:add repo & dependency on your project pom:
```xml
    <repositories>
        <repository>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
            <id>bintray-nkonev-m2</id>
            <name>bintray</name>
            <url>https://dl.bintray.com/nkonev/m2</url>
        </repository>
    </repositories>
    
    <dependency>
        <groupId>com.github.nkonev</groupId>
        <artifactId>rendertron-java</artifactId>
        <version>1.0.0</version>
    </dependency>
```
## How it works
1. Check to make sure we should show a prerendered page
	1. Check if the request is from a crawler (useragent string)
	2. Check to make sure we aren't requesting a resource (js, css, etc...)
	3. (optional) Check to make sure the url is in the whitelist
	4. (optional) Check to make sure the url isn't in the blacklist
2. Make a `GET` request to the [rendertron service](https://github.com/GoogleChrome/rendertron)(headless Chrome) for the page's prerendered HTML
3. Return that HTML to the crawler

## Customization

### crawlerUserAgents
example: someproxy,someproxy1

### whitelist

### blacklist

### forwardedURLHeader
Important for servers behind reverse proxy that need the public url to be used for pre-rendering.
We usually set the original url in an http header which is added by the reverse proxy (similar to the more standard `x-forwarded-proto` and `x-forwarded-for`)


### event handler

If you want to cache the caching, analytics, log or others, you can config it. It should be instance of "EventHandler"
