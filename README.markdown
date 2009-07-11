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

Setting up the project
----------------------

Once forked and imported to eclipse you need to run "Fix Project Properties".
This is found under Android Tools. 

You then need to add the following class that holds the api endpoint and point 
out where the api is located.

    public class ApiSettings {
        public static String STHLM_TRAVELING_API_ENDPOINT = "";
    }

Changelog
---------

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
    * Fixed problems around finding stops, some searches did not return the expected values
    * Made the search button bigger
* 0.1
    * Initial release
