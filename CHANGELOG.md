Changelog
---------
* 2.15.1 (2016-01-02)
  * Bug fixes
* 2.15.0 (2015-12-24)
  * Add walk, bike and car mode
  * Add new icons and colors
  * Add Bosnian (Bosnia and Herzegovina) translation by Zlatan Klapic
  * Add Hebrew translation by David Allon
  * Update German, Russian and Swedish translations, thanks Anja and Denis
* 2.14.2 (2015-10-30)
  * Add initial work on real-time support to the journey planner
* 2.14.0 (2015-10-25)
  * Add Russian translation by Denis Laure
  * Add new search for transit stops, address and places
  * Add backstack for going back to the start activity when opening departure shortcuts by
    [Victor Häggqvist](https://github.com/victorhaggqvist)
  * Add toolbar for settings view by [Victor Häggqvist](https://github.com/victorhaggqvist)
  * Add new layout and styling for departures and traffic status
  * Add support for selecting deviation and traffic status texts
  * Fix keylines
  * Fix date representation on date picker to not always show as 12hr format
  * Fix crash when opening some sl.se links
  * Update German and Swedish translations, by Anja & Torkel.
* 2.13.0 (2015-09-20)
  * Add Arabic translation by Simon Benyo
  * Add Italian translation by Sergio Cucinella
  * Update German translation by Anja Mesenholl and Holtzbahn
  * Update Dutch translations by Bastiaan Franssen
  * Fix crash when changing the date of departure / arrival on some Samsung devices
  * Update third party libraries (Google Play Services, OkHttpClient & Katalysator)
* 2.12.0 (2015-05-10)
  * Fix white text on white background on some HTC devices
  * Update to recent AppCompat library
  * Fix crash in departures
  * Fix styling, mainly for 2.3 devices
  * Fix to not show dialog about google play services if the library is not needed
  * Fix issue with description when distance between current location and first step was short
  * Add grace period for ads if the ads has been dismissed
  * Remove old error reporter
  * Add French translation by Sebastien
  * Add Chinese translation by Tangood
  * Add German translation by Anja
  * Update Swedish and English translations
* 2.11.0 (2015-03-31)
  * Add Dutch translations by Bastiaan Franssen
  * Update third party apis, Google Play Services is now on 7.0.0.
  * Fix issue with using via with the planner
  * Fix falsy validation when changing preferences for a journey
  * Change to use location lookups using the fused location provider from play services
  * Remove Flurry Analytics

* 2.10.0
  * Update the general design of the app
  * Add Google Analytics
  * Add support for showing ads

* 2.9.0 (2014-07-11)
  * Fix order of nearby stops. Thanks [Henrik Sandström](https://github.com/heinrisch)
  * New icon for SMS Ticket and button in route details
  * Add arrival stop to the line in route details
  * Change Google Play Services to version 5
  * Change support library to version 20
  * Change target sdk to 20
  * Change min sdk version to 9 (2.3.x)
  * Add Google Analytics
* 2.8.1 (2014-06-14)
  * Add text to show where to get off in route details.
  * Fix crash when clicking multiple times on intermediate stops.
  * Fix crash when resuming the planner and a dialog was open.
* 2.8.0
  * Change design.
  * Add swipe between traffic types in departures.
  * Update all icons to support higher resolutions.
  * Change to use okhttp for http request.
  * Add http caching.
  * Change to build releases with proguard.
  * Add crashlytics for crash reports.
  * Change to hide the keyboard when app is launched.
* 2.7.2
  * Fix issue with no departures showing.
  * Fix crash that occured when performing a search with no values.
* 2.7.1
  * Fix issue with start and endpoint not being properly set before fetching routes.
  * Fix so that legal notices show properly.
  * Remove AdMob library from source.
* 2.7.0
  * Change to hide search history from journey planner when creating a shortcut.
  * Add support for searching by site id from journey planner shortcuts.
  * Remove Flygbussarna and Arlanda Express, these are not supported by SL anymore.
  * Remove search and create shortcut button from the Departures view. Search is now triggered by selecting an item from the auto complete suggestions instead.
  * Change to use Gradle and Android Studio by [Per Jonsson](https://github.com/pertyjons).
  * Change to use address information provided by the site API.
  * Change to validate origin, destination & via before performing a search.
  * Change to show boats correctly labeled in traffic status.
  * Change via to visa in journey planner, reported by Anders.
  * Change to break if a deviation criteria matches to avoid adding the same deviation multiple times, reported and fixed by Christoffer Jonsson.
* 2.6.2
  * Remove references to previous maps implementation.
  * Change number for SMS tickets.
  * Change icon for deviations in route views.
* 2.6.1
  * Fix shortcut for Nearby.
* 2.6.0
  * Change min sdk API level to 8 (2.2.x).
  * Change to more consitent style.
  * Udate maps implementation to Maps V2 API.
  * Add Nearby locations to depatures.
  * Remove beta flag from Nearby Activity.
  * Fix NullPointerException in RoutesActivities. 
* 2.5.3
  * Fix issue with shortcuts.
* 2.5.2
  * Add ActionBarSherlock.
  * Update ViewPagerIndicator to latest version.
  * Add new logo from [Olof Brickarp](http://www.yay.se).
* 2.5.1
  * Fix issue with black text on black background in dialogs on Samsung
    Galaxy S2 devices that has been updated to ICS.
* 2.5.0
    * Add Gzip support for API request/response.
    * Add map of a selected route.
    * Add link to Kundo support forum.
    * Change to not autocomplete addresses if a number is not provided. 
* 2.4.0
    * Add swipe between tabs and port to fragments by [Robert Johansson](https://twitter.com/#!/likebobby).
    * Add support for Arlanda Express.
    * Add support for intermediate stops.
    * Add Holo theme for API level 14+.
    * Various style fixes.
    * Add proper default selection in departures.
    * Add proper name of My Location when used as destination.
    * Add support for using the keyboard software search button from departure
      search.
    * Fixed bug in departure when no Site was selected.
    * Improve validation of user input.
* 2.3.10
    * Adapted deviation to new API.
    * Adapted traffic status to new API.
    * Fixed loading indicator in deviation.
    * Fixed some translations.
* 2.3.9
    * Adapted departures to new API.
    * Time displayed for departures now comes directly from SL which should
      give more accurate times.
    * Removed red indicator in departures for "now" because it did not work.
* 2.3.8
    * Renamed transport types for TRAIN and TRAM to work with the new API.
* 2.3.7
    * New API for sites and journey planner.
    * Initial support for intermediate stops (not available yet).
    * Restyled tabs on the front activities.
* 2.3.6
    * Removed ads
* 2.3.5
    * Added ads
* 2.3.4
    * New SMS prices
    * Fixed issue with SMS now showing
    * Enabled nearby sites as an experimental function, available through a
      shortcut.
* 2.3.3
    * Fixed bug with alternative stops not being accepted even if it was
      checked.
* 2.3.2
    * Fixed crash in Deviations that could occurr during network problems.
    * Possible fix for not finding any routes.
    * Excluded sites with geo data for the via field.
    * Fixed problem with via site history being mixed with the departure site
      history.
    * Fixed crash in favorites that occured when favoriting a journey with no
      transports.
* 2.3.1
    * Fixed crash related to migration of favorites to the new format.
* 2.3.0
    * New journey provider that holds starred journey, including traffic modes
      and other search criterias. This replaces the favorites database.
    * Added support for via to the journey planner.
    * Added possibility to search for different transport modes and alternative
      stops.
    * Added a action bar.
    * Updated icons.
    * Added journey history, visible below the journey planner.
    * Changed target sdk version to 10 (2.3.3) and min sdk version to 4 (1.6).
    * Moved files from the assets directory (that shouldn't be there) into the
      extras directory.
* 2.2.2
    * Last selected transport mode for a specific site is now automatically
      selected for Departures.
* 2.2.1
    * Fixed bug that crashed the app when changing transport type in the
      departure activity before the data was loaded.
* 2.2.0
    * New departures view. Departures are now grouped by transport type.
    * Departures for metros are pulled from a separate api and should now be
      based on realtime data.
    * If the departure time is not based on realtime data it's now displayed
      as the time for departure instead of eg. "2 min" in the departure view.
    * Fixed bug when "Now" was not respected in the journey planner.
    * Added traffic line to the traffic status view.
    * The detailed journey view now displays the correct time in the header.
    * UI tweaks.
* 2.1.0
    * Changed to use json instead of xml for getting site data from the backend.
    * Mapped the bus icon for närtrafiken.  
    * Added Flurry Analytics v1.24. We need analytics to be able to improve
      the application and know how users use it.
    * Using default Theme in preferences because [issue](http://twitter.com/johanni/status/26117527135) with Sony Ericsson devices.
    * Date of Trip added to the detailed view.
    * New arrows and clock icons in the journey planner by [Sara Smedman](http://joforik.net/).
    * Spanish translation by Fredrik Jönsson.
    * SMS tickets is back, now with price and extra information. Patch by [Adam Nybäck](http://anyro.se/).
    * Increased text sizes and margins.
    * Invalid encoding for origin and destination name in the detailed
      result, closes [#25](http://github.com/johannilsson/sthlmtraveling/issues#issue/25).
    * Replaced the token MY_LOCATION with a localized string in the detailed
      result, closes [#35](http://github.com/johannilsson/sthlmtraveling/issues#issue/35). 
    * Target sdk version is now 8 (2.2) min sdk version is still 3 (1.5).
    * Possibility to change install location (Install on SD card), closes [#34](http://github.com/johannilsson/sthlmtraveling/issues#issue/34).
    * Make sure tasks are canceled when user leaves the RoutesActivity, closes [#27](http://github.com/johannilsson/sthlmtraveling/issues#issue/27).
    * Fixed bug when restoring GetEarlierRoutesTask, closes [#17](http://github.com/johannilsson/sthlmtraveling/issues#issue/17).
    * Fixed issue with MY_LOCATION being added to the text input when selecting
      an item from the history, thanks droidgren. Closes [#25](http://github.com/johannilsson/sthlmtraveling/issues#issue/25)
    * Improved error handling when the DeparturesActivity got restored after an
      orientation change. This closes [#16](http://github.com/johannilsson/sthlmtraveling/issues#issue/16) and as a side effect is also closes [#11](http://github.com/johannilsson/sthlmtraveling/issues#issue/11).  
* 2.0.0
    * Search history is visible in the "get" dialog direct
    * Address search integrated in auto completion
    * Reworked history to allow storage of latitude and longitude
    * Various layout fixes 
    * New traffic status view
    * Fixed my location bug
    * Complete rewrite of the journey planner, new backend etc
    * Various icons has been replaced creds to Olof Brickarp
    * Changed to use SL standard icons for transports (with their permission)
    * Switched to use the light theme
* 1.8.1
    * Reverted version of 1.8.0, just upped version to be able to release it
* 1.8.0
    * Reworked detailed view
    * Added buy sms-ticket from the detailed view
    * Added possibility to show stop on a map from the detailed view
    * New marker for text on the map view
    * The hardware search button will bring up the planner view
    * Various layout fixes
* 1.7.7
    * Added possibility to share deviations
    * Added possibility to share routes
    * Search improvements, if the start or end point matches a name in the
      list of suggestions re-query direct instead of showing the suggestions
      for the end user
    * Fixed NullPointerException that occurred when searching and the
      communication failed, closes #10.
* 1.7.6
    * Fixed issue with changing locale on Android 2.0.x, closes #9
    * Added double tap to zoom on map
    * Removed center to geo point when clicking on the map
* 1.7.5
    * Fixed bug related to alternatives when one stop had an location
* 1.7.4
    * Wrong map key
* 1.7.3
    * Added site suggestions if a routes was not found because sl.se mixed up
      some sites, or if the user misspelled the site name. Resolves #3
    * Bug fix for networks problems in the RoutesActivity
    * Various clean up
    * Removed QR code, need to rework that part a bit
* 1.7.2
    * New app icon by [Olof Brickarp](http://www.yay.se)
    * Improvements for hdpi screens
    * Bug fix for network problem that occurred when switching between different
      networks.
    * Bug fix for maps, that caused the my location indicator to not show up
      direct
* 1.5.2
    * Bug fix for transports without line numbers
    * Bug fix for line numbers, now Bus 123A will be parsed correct
    * Bug fix when my location sources is turned of in system settings
    * Added transport for the airport coach
* 1.5.1
    * Added line numbers in the routes view
    * Added a crash reporter
    * Added icon for boat
* 1.5.0
    * Added departures
    * Changed to use the Apache http client
* 1.4.2
    * Added support for setting arrival time
* 1.4.1
    * Added fallback on point on map when failed to determine "My location"
* 1.4.0
    * Point on map
    * Improved My location, now passing the real position to sl.se
    * Added support for lat and lng to favorites
    * Added retry button to the network problem dialog
    * Fixed bug with screen rotation when performing background jobs
    * Added support for named shortcuts
* 1.3.6
    * Fix for barcode generator
* 1.3.5
    * New app icon by [Olof Brickarp](http://www.yay.se)
    * Choose departure time from the planner
    * Shortcuts for routes
    * Share search via QR-code
    * Public intent for searching routes, other apps can now trigger a search
      for routes. See RoutesActivity for documentation
    * Reverse search from the routes list 
    * Show alert if we having problems with the network, closes issue [#6](http://github.com/johannilsson/sthlmtraveling/issues/#issue/6) 
    * Added start and end point to routes list
    * Moved actually searching to RoutesActivity
    * Moved planner code to own package
    * Replaced handlers with AsyncTask, now located in the tasks package
    * Fixed bug with menu for search did not launch the tabbed StartActivity, 
      patch by [Morgan Christiansson](http://www.screeninteraction.com/)
    * Moved all hard coded strings to strings.xml
* 1.3.0
    * Favorite routes
    * History for start and end point
    * Fixed force close when location is not available, closes issue [#4](http://github.com/johannilsson/sthlmtraveling/issues/#issue/4)
    * Compiling against 1.6
    * Tested for QVGA screens
    * Improved ui with icons from FatCow
    * Refactored search routes to a AsyncTask
    * Renamed SearchActivity to PlannerActivity, patch by [Morgan Christiansson](http://www.screeninteraction.com/)
* 1.2.2
    * Added Saltsjöbanan as an transport, closes issue [#2](http://github.com/johannilsson/sthlmtraveling/issues/#issue/2)
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


