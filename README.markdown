STHLM Traveling
===============

Travel in Stockholm with your Android phone.

STHLM Traveling helps you plan your journeys using data from Stockholm Public 
Transport (SL, Stockholms Lokaltrafik).

Features include Auto complete of station and addresses. Usage of GPS for 
finding stops near your location. Route alternatives and detail description 
of a route.

More about this project at <http://markupartist.com/sthlmtraveling> or follow 
me on [twitter](http://twitter.com/johanni) where I announce new versions.

Icons by [Fredrik Broman](http://fredrikbroman.com).

Setting up the project
----------------------

Once forked and imported to eclipse you need to run "Fix Project Properties".
This is found under Android Tools. 

You then need to add the following class that holds the api endpoint and point 
out where the api is located.

    public class ApiSettings {
        public static String STHLM_TRAVELING_API_ENDPOINT = "";
    }

Run tests
---------

To be able to run the tests from Eclipse follow these instructions. These 
instructions assume that you alrady have STHLM Traveling setup as a project and
deployed. 

* Create a new Android Project.
* Choose create project from existing source. Choose tests/src as project root.
* Under project properties and Java Build Path choose Projects and add a 
  dependency to STHLM Traveling.
* If necessary run Fix Project Properties under Android Tools.
* Now you should be able to run the tests. Choose Run As Android JUnit Test.

To run the tests from a terminal just type in. Note, you must have both the 
application and the tests application deployed before.

    adb shell am instrument -w com.markupartist.sthlmtraveling.tests/android.test.InstrumentationTestRunner

License
-------
STHLM Traveling is open source and licenced under 
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
Icons by Fredrik Broman is licensed under [CC Attribution 3.0](http://creativecommons.org/licenses/by/3.0/).

Changelog
---------
* 1.2.2
    * Added Saltsjöbanan as an transport, closes issue #2
* 1.2.1
    * Added support for icons in routes list. Icons by 
      [Fredrik Broman](http://fredrikbroman.com)
    * Replaced earlier/later routes text with arrows, as suggested by 
      [fohlin](http://twitter.com/fohlin)
    * Added setup for unit tests and some tests
    * Fixed issue with "My Location" returning null instead of city sometimes
* 1.2.0
    * Added support for searching for earlier and later routes in the routes 
      view
    * Added support for changing the department time
* 1.1.0
    * Improved the ux by doing searches in the current activity
    * Added ApiSettings that holds the api endpoint
    * Introduced Planner groups all journey planning functionality
    * Renamed *Finder to *Parser and moved all http queries to Planner
    * Suffixed all activities to *Activity    
    * Renamed SimpleStopAdapter to AutoCompleteStopAdapter
    * Added dialog if no routes was found and a hint of why this occurred
* 1.0.0
    * Released on Android Market
    * Added about dialog
* 0.2
    * Added search based on the current postion
    * Fixed problems around finding stops, some searches did not return the 
      expected values
    * Made the search button bigger
* 0.1
    * Initial release
