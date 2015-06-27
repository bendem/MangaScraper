# MangaScraper

Super easy and modular web scraper to download scans from online manga services like mangaeden or mangareader.

```shellsession
$ java -jar build/libs/MangaScraper-0.1-all.jar -h

Usage: java -jar jarfile.jar [-i <implementation>] [-r <range>] <url>
    <range>          is either a number (like 1) or two numbers separated with a
                     dash (like 1-5). Default value is 0-INFINITY

    <implementation> Specify the FQN (or internal name) of the class implementing
                     Scraper to use

    <url>            is a valid url for the chosen implementation
```
